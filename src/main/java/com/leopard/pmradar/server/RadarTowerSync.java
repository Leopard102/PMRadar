package com.leopard.pmradar.server;

import com.leopard.pmradar.PMRadar;
import com.leopard.pmradar.RadarTowerScanner;
import com.leopard.pmradar.network.RadarSitesPayload;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.event.BlockDamageEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(modid = PMRadar.MODID)
public final class RadarTowerSync {
    private static final int TOWER_SEARCH_RADIUS_BLOCKS = 6;
    private static final int TORNADO_DAMAGE_SEARCH_RADIUS_BLOCKS = 16;
    private static final int RADAR_DISPLAY_SEARCH_RADIUS_BLOCKS = 128;
    private static final int PENDING_SCAN_DELAY_TICKS = 2;
    private static final int PERIODIC_SCAN_INTERVAL_TICKS = 100;
    private static final int PERIODIC_SCAN_RADIUS_CHUNKS = 12;
    private static final int MAX_PENDING_SCANS_PER_TICK = 256;

    private static final Map<ResourceKey<Level>, Map<BlockPos, RadarSitesPayload.Entry>> radarSites = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<PendingScan> pendingScans = new ConcurrentLinkedQueue<>();
    private static long ticks;

    private RadarTowerSync() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        clearServerState();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearServerState();
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (isRadarStructureBlock(event.getPlacedBlock())) {
            queueScan(event.getLevel(), event.getPos(), scanRadiusFor(event.getPlacedBlock(), TOWER_SEARCH_RADIUS_BLOCKS));
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (isRadarStructureBlock(event.getState())) {
            queueScan(event.getLevel(), event.getPos(), scanRadiusFor(event.getState(), TOWER_SEARCH_RADIUS_BLOCKS));
        }
    }

    @SubscribeEvent
    public static void onBlockDamaged(BlockDamageEvent event) {
        if (isRadarStructureBlock(event.getState())) {
            queueScan(event.getLevel(), event.getPos(), scanRadiusFor(event.getState(), TORNADO_DAMAGE_SEARCH_RADIUS_BLOCKS));
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (isRadarStructureBlock(event.getState())) {
            queueScan(event.getLevel(), event.getPos(), scanRadiusFor(event.getState(), TOWER_SEARCH_RADIUS_BLOCKS));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scanLoadedTowersAroundPlayer(player);
            sendKnownSites(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long tick = ticks++;
        processPendingScans();

        if (tick % PERIODIC_SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            scanLoadedTowersAroundPlayer(player);
        }
    }

    private static boolean isRadarStructureBlock(BlockState state) {
        return RadarTowerScanner.isTowerCore(state)
                || state.is(ModBlocks.RADOME.get())
                || state.is(ModBlocks.RADAR.get())
                || state.is(ModBlocks.RANGE_UPGRADE_MODULE.get());
    }

    private static int scanRadiusFor(BlockState state, int defaultRadius) {
        return state.is(ModBlocks.RADAR.get()) || state.is(ModBlocks.RANGE_UPGRADE_MODULE.get())
                ? RADAR_DISPLAY_SEARCH_RADIUS_BLOCKS
                : defaultRadius;
    }

    private static void queueScan(LevelAccessor level, BlockPos origin) {
        queueScan(level, origin, TOWER_SEARCH_RADIUS_BLOCKS);
    }

    private static void queueScan(LevelAccessor level, BlockPos origin, int radius) {
        if (level instanceof ServerLevel serverLevel) {
            pendingScans.add(new PendingScan(
                    serverLevel,
                    origin.immutable(),
                    Math.max(TOWER_SEARCH_RADIUS_BLOCKS, radius),
                    PENDING_SCAN_DELAY_TICKS
            ));
        }
    }

    private static void processPendingScans() {
        int processed = 0;
        while (processed++ < MAX_PENDING_SCANS_PER_TICK) {
            PendingScan scan = pendingScans.poll();
            if (scan == null) {
                return;
            }

            if (scan.delayTicks() > 0) {
                pendingScans.add(new PendingScan(scan.level(), scan.origin(), scan.radius(), scan.delayTicks() - 1));
                continue;
            }

            processScan(scan.level(), scan.origin(), scan.radius());
        }
    }

    private static void processScan(ServerLevel level, BlockPos origin, int radius) {
        List<RadarSitesPayload.Entry> changes = new ArrayList<>();
        Set<BlockPos> checked = new HashSet<>();
        Map<BlockPos, RadarSitesPayload.Entry> sites = sitesFor(level);

        for (BlockPos pos : List.copyOf(sites.keySet())) {
            if (near(origin, pos, radius)) {
                evaluateSite(level, pos, checked, changes);
            }
        }

        int minX = origin.getX() - radius;
        int maxX = origin.getX() + radius;
        int minY = Math.max(level.getMinBuildHeight(), origin.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + radius);
        int minZ = origin.getZ() - radius;
        int maxZ = origin.getZ() + radius;
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (level.hasChunk(chunkX, chunkZ)) {
                    scanTowerCores(level, level.getChunk(chunkX, chunkZ), minX, maxX, minY, maxY, minZ, maxZ, checked, changes);
                }
            }
        }

        broadcastChanges(level, changes);
    }

    private static boolean near(BlockPos first, BlockPos second, int radius) {
        return Math.abs(first.getX() - second.getX()) <= radius
                && Math.abs(first.getY() - second.getY()) <= radius
                && Math.abs(first.getZ() - second.getZ()) <= radius;
    }

    private static void scanLoadedTowersAroundPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int centerChunkX = Math.floorDiv(player.getBlockX(), 16);
        int centerChunkZ = Math.floorDiv(player.getBlockZ(), 16);
        int minChunkX = centerChunkX - PERIODIC_SCAN_RADIUS_CHUNKS;
        int maxChunkX = centerChunkX + PERIODIC_SCAN_RADIUS_CHUNKS;
        int minChunkZ = centerChunkZ - PERIODIC_SCAN_RADIUS_CHUNKS;
        int maxChunkZ = centerChunkZ + PERIODIC_SCAN_RADIUS_CHUNKS;

        List<RadarSitesPayload.Entry> changes = new ArrayList<>();
        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos pos : List.copyOf(sitesFor(level).keySet())) {
            int chunkX = Math.floorDiv(pos.getX(), 16);
            int chunkZ = Math.floorDiv(pos.getZ(), 16);
            if (chunkX < minChunkX || chunkX > maxChunkX || chunkZ < minChunkZ || chunkZ > maxChunkZ || !level.hasChunk(chunkX, chunkZ)) {
                continue;
            }

            evaluateSite(level, pos, checked, changes);
        }

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                try {
                    scanTowerCores(level, level.getChunk(chunkX, chunkZ), checked, changes);
                } catch (RuntimeException ignored) {
                }
            }
        }

        broadcastChanges(level, changes);
    }

    private static void scanTowerCores(
            ServerLevel level,
            LevelChunk chunk,
            Set<BlockPos> checked,
            List<RadarSitesPayload.Entry> changes
    ) {
        int blockX = chunk.getPos().x << 4;
        int blockZ = chunk.getPos().z << 4;
        scanTowerCores(
                level,
                chunk,
                blockX,
                blockX + 15,
                level.getMinBuildHeight(),
                level.getMaxBuildHeight() - 1,
                blockZ,
                blockZ + 15,
                checked,
                changes
        );
    }

    private static void scanTowerCores(
            ServerLevel level,
            LevelChunk chunk,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            Set<BlockPos> checked,
            List<RadarSitesPayload.Entry> changes
    ) {
        LevelChunkSection[] sections = chunk.getSections();
        int blockX = chunk.getPos().x << 4;
        int blockZ = chunk.getPos().z << 4;
        int minLocalX = Math.max(0, minX - blockX);
        int maxLocalX = Math.min(15, maxX - blockX);
        int minLocalZ = Math.max(0, minZ - blockZ);
        int maxLocalZ = Math.min(15, maxZ - blockZ);
        if (minLocalX > maxLocalX || minLocalZ > maxLocalZ) {
            return;
        }

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(RadarTowerScanner::isTowerCore)) {
                continue;
            }

            int blockY = level.getSectionYFromSectionIndex(sectionIndex) << 4;
            if (blockY > maxY || blockY + 15 < minY) {
                continue;
            }

            int minLocalY = Math.max(0, minY - blockY);
            int maxLocalY = Math.min(15, maxY - blockY);
            for (int localY = minLocalY; localY <= maxLocalY; localY++) {
                for (int localX = minLocalX; localX <= maxLocalX; localX++) {
                    for (int localZ = minLocalZ; localZ <= maxLocalZ; localZ++) {
                        BlockState blockState = section.getBlockState(localX, localY, localZ);
                        if (RadarTowerScanner.isTowerCore(blockState)) {
                            evaluateSite(level, new BlockPos(blockX + localX, blockY + localY, blockZ + localZ), checked, changes);
                        }
                    }
                }
            }
        }
    }

    private static void evaluateSite(
            ServerLevel level,
            BlockPos pos,
            Set<BlockPos> checked,
            List<RadarSitesPayload.Entry> changes
    ) {
        BlockPos immutable = pos.immutable();
        if (!checked.add(immutable)) {
            return;
        }

        if (!hasChunkAt(level, immutable)) {
            return;
        }

        Map<BlockPos, RadarSitesPayload.Entry> sites = sitesFor(level);
        RadarTowerScanner.TowerState towerState = RadarTowerScanner.stateAt(level, immutable);
        if (towerState == RadarTowerScanner.TowerState.UNKNOWN) {
            return;
        }

        RadarSitesPayload.Entry previous = sites.get(immutable);
        if (!towerState.visible()) {
            if (previous != null) {
                sites.remove(immutable);
                changes.add(RadarSitesPayload.Entry.removed(immutable));
            }
            return;
        }

        RadarSitesPayload.Entry next = RadarSitesPayload.Entry.visible(immutable, towerState.operational());
        if (!next.equals(previous)) {
            sites.put(immutable, next);
            changes.add(next);
        }
    }

    private static boolean hasChunkAt(ServerLevel level, BlockPos pos) {
        return level.hasChunk(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
    }

    private static Map<BlockPos, RadarSitesPayload.Entry> sitesFor(ServerLevel level) {
        return radarSites.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>());
    }

    private static void broadcastChanges(ServerLevel level, List<RadarSitesPayload.Entry> changes) {
        if (changes.isEmpty()) {
            return;
        }

        PacketDistributor.sendToPlayersInDimension(
                level,
                new RadarSitesPayload(level.dimension().location(), List.copyOf(changes))
        );
    }

    private static void sendKnownSites(ServerPlayer player) {
        List<RadarSitesPayload.Entry> entries = List.copyOf(sitesFor(player.serverLevel()).values());
        if (entries.isEmpty()) {
            return;
        }

        PacketDistributor.sendToPlayer(
                player,
                new RadarSitesPayload(player.serverLevel().dimension().location(), entries)
        );
    }

    private static void clearServerState() {
        radarSites.clear();
        pendingScans.clear();
        ticks = 0L;
    }

    private record PendingScan(ServerLevel level, BlockPos origin, int radius, int delayTicks) {
    }
}
