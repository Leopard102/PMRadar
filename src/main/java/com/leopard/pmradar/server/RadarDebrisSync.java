package com.leopard.pmradar.server;

import com.leopard.pmradar.PMRadar;
import com.leopard.pmradar.RadarDebrisFilter;
import com.leopard.pmradar.network.RadarDebrisPayload;
import dev.protomanly.pmweather.event.BlockDamageEvent;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = PMRadar.MODID)
public final class RadarDebrisSync {
    private static final int BROADCAST_INTERVAL_TICKS = 20;
    private static final int CLUSTER_LIFETIME_TICKS = 80;
    private static final int MAX_CLUSTERS_PER_DIMENSION = 64;
    private static final float MIN_SYNC_STRENGTH = 0.030F;

    private static final Map<ResourceKey<Level>, Map<Long, AccumulatedDebris>> debrisClusters = new ConcurrentHashMap<>();
    private static long ticks;

    private RadarDebrisSync() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        clear();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clear();
    }

    @SubscribeEvent
    public static void onBlockDamaged(BlockDamageEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isSignificantDebrisBlock(event.getState())) {
            return;
        }

        Storm storm = nearestDamagingTornado(level, event.getPos());
        if (storm == null) {
            return;
        }

        Map<Long, AccumulatedDebris> levelClusters = debrisClusters.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>());
        if (levelClusters.size() >= MAX_CLUSTERS_PER_DIMENSION && !levelClusters.containsKey(storm.ID)) {
            pruneOldest(levelClusters);
        }

        float width = Float.isFinite(storm.width) ? Math.max(storm.width, 32.0F) : 32.0F;
        float windFactor = Math.clamp((storm.windspeed - 45.0F) / 115.0F, 0.0F, 1.0F);
        float sampleStrength = Math.clamp(0.20F + windFactor * 0.68F, 0.0F, 1.0F);
        double radius = Math.max(20.0D, Math.min(width * 1.35D, 34.0D + Math.sqrt(width) * 8.0D));
        long gameTime = level.getGameTime();

        levelClusters
                .computeIfAbsent(storm.ID, id -> new AccumulatedDebris(storm.ID, event.getPos().getX() + 0.5D, event.getPos().getZ() + 0.5D))
                .record(event.getPos(), radius, sampleStrength, gameTime);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long tick = ticks++;
        if (tick % BROADCAST_INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            broadcast(level);
        }
    }

    private static Storm nearestDamagingTornado(ServerLevel level, BlockPos pos) {
        WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(level.dimension());
        if (weatherHandler == null) {
            return null;
        }

        Storm nearest = null;
        double bestScore = Double.MAX_VALUE;
        Vec3 center = pos.getCenter();
        Vec3 flatCenter = center.multiply(1.0D, 0.0D, 1.0D);
        for (Storm storm : weatherHandler.getStorms()) {
            if (!DistantTornadoChunkLoading.shouldKeepLoaded(storm)) {
                continue;
            }

            double width = Math.max(Float.isFinite(storm.width) ? storm.width : 0.0F, 40.0D);
            double maxDistance = Math.max(96.0D, width * 1.45D);
            double distanceSqr = storm.position.multiply(1.0D, 0.0D, 1.0D).distanceToSqr(flatCenter);
            if (distanceSqr > maxDistance * maxDistance) {
                continue;
            }

            try {
                float wind = storm.getTornadicWind(center);
                if (!Float.isFinite(wind) || wind < 40.0F) {
                    continue;
                }

                double score = distanceSqr / Math.max(wind, 1.0F);
                if (score < bestScore) {
                    nearest = storm;
                    bestScore = score;
                }
            } catch (RuntimeException ignored) {
            }
        }

        return nearest;
    }

    private static void broadcast(ServerLevel level) {
        Map<Long, AccumulatedDebris> levelClusters = debrisClusters.get(level.dimension());
        if (levelClusters == null || levelClusters.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();
        List<RadarDebrisPayload.Entry> entries = new ArrayList<>();
        Iterator<Map.Entry<Long, AccumulatedDebris>> iterator = levelClusters.entrySet().iterator();
        while (iterator.hasNext()) {
            AccumulatedDebris cluster = iterator.next().getValue();
            if (cluster.expired(gameTime)) {
                iterator.remove();
                continue;
            }

            RadarDebrisPayload.Entry entry = cluster.entry(gameTime);
            if (entry.strength() >= MIN_SYNC_STRENGTH) {
                entries.add(entry);
            }
            cluster.afterBroadcast();
        }

        if (entries.isEmpty()) {
            return;
        }

        PacketDistributor.sendToPlayersInDimension(
                level,
                new RadarDebrisPayload(level.dimension().location(), List.copyOf(entries))
        );
    }

    private static void pruneOldest(Map<Long, AccumulatedDebris> levelClusters) {
        Long oldestStormId = null;
        long oldestTick = Long.MAX_VALUE;
        for (Map.Entry<Long, AccumulatedDebris> entry : levelClusters.entrySet()) {
            if (entry.getValue().lastDamageTick < oldestTick) {
                oldestTick = entry.getValue().lastDamageTick;
                oldestStormId = entry.getKey();
            }
        }

        if (oldestStormId != null) {
            levelClusters.remove(oldestStormId);
        }
    }

    private static boolean isSignificantDebrisBlock(BlockState blockState) {
        return RadarDebrisFilter.isCandidate(blockState) && !blockState.canBeReplaced();
    }

    private static void clear() {
        debrisClusters.clear();
        ticks = 0L;
    }

    private static final class AccumulatedDebris {
        private final long stormId;
        private double x;
        private double z;
        private double radius = 24.0D;
        private float strength;
        private int count;
        private long lastDamageTick;

        private AccumulatedDebris(long stormId, double x, double z) {
            this.stormId = stormId;
            this.x = x;
            this.z = z;
        }

        private void record(BlockPos pos, double nextRadius, float sampleStrength, long gameTime) {
            double sampleX = pos.getX() + 0.5D;
            double sampleZ = pos.getZ() + 0.5D;
            if (count <= 0) {
                x = sampleX;
                z = sampleZ;
            } else {
                x = x * 0.82D + sampleX * 0.18D;
                z = z * 0.82D + sampleZ * 0.18D;
            }

            radius = Math.max(radius * 0.90D, nextRadius);
            strength = Math.max(strength * 0.88F, sampleStrength);
            count = Math.min(count + 1, 240);
            lastDamageTick = gameTime;
        }

        private boolean expired(long gameTime) {
            return gameTime - lastDamageTick > CLUSTER_LIFETIME_TICKS;
        }

        private RadarDebrisPayload.Entry entry(long gameTime) {
            float ageFade = 1.0F - Math.clamp((gameTime - lastDamageTick) / (float) CLUSTER_LIFETIME_TICKS, 0.0F, 1.0F);
            float countFactor = (float) Math.sqrt(Math.clamp(count / 12.0F, 0.0F, 1.0F));
            float syncedStrength = Math.clamp(strength * (0.38F + countFactor * 0.62F) * ageFade, 0.0F, 1.0F);
            double syncedRadius = Math.max(18.0D, radius * (0.55D + countFactor * 0.45D));
            return new RadarDebrisPayload.Entry(stormId, x, z, syncedRadius, syncedStrength, count);
        }

        private void afterBroadcast() {
            strength *= 0.68F;
            count = Math.max(0, count / 2);
            radius = Math.max(18.0D, radius * 0.92D);
        }
    }
}
