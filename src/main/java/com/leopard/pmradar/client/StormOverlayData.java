package com.leopard.pmradar.client;

import com.leopard.pmradar.RadarTowerScanner;
import com.leopard.pmradar.RadarDebrisFilter;
import com.leopard.pmradar.network.RadarSitesPayload;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.entity.MovingBlock;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.multiblock.wsr88d.WSR88DCore;
import dev.protomanly.pmweather.particle.EntityRotFX;
import dev.protomanly.pmweather.util.ColorTables;
import dev.protomanly.pmweather.weather.Clouds;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import dev.protomanly.pmweather.weather.WindEngine;
import dev.protomanly.pmweather.weather.effects.ClientLightning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StormOverlayData {
    private static final int REGION_SIZE_CHUNKS = 8;
    private static final int MAX_LOADED_AREA_RADIUS_CHUNKS = 12;
    private static final int RADAR_SITE_VALIDATION_RADIUS_CHUNKS = 8;
    // Fallback depth (blocks straight down from the core) used to look for a range-upgrade
    // module only if the tower's structure data reports no radome shell below the core.
    private static final int RANGE_UPGRADE_FALLBACK_SEARCH_DEPTH_BLOCKS = 3;
    private static final int SITE_SCAN_INTERVAL_TICKS = 20;
    // PMWeather's RadarRenderer rebuilds its radar texture every 20 * 3 ticks.
    private static final int RADAR_FRAME_INTERVAL_TICKS = 20 * 3;
    private static final int LIGHTNING_MARKER_LIFETIME_TICKS = 20 * 30;
    private static final long LIGHTNING_MARKER_LIFETIME_MILLIS = LIGHTNING_MARKER_LIFETIME_TICKS * 50L;
    private static final double BASE_RADAR_RADIUS_BLOCKS = 2048.0D;
    private static final double RADAR_BLIND_SPOT_RADIUS_BLOCKS = 48.0D;
    private static final float MIN_VISIBLE_DBZ = 3.0F;
    private static final float MAX_VISIBLE_DBZ = 70.0F;
    public static final float CORRELATION_MIN = 0.20F;
    public static final float CORRELATION_MAX = 1.05F;
    private static final int MIN_DEBRIS_MOVING_BLOCKS = 1;
    private static final int MIN_DEBRIS_PARTICLE_CLUSTER_COUNT = 6;
    private static final int MIN_DEBRIS_PARTICLE_BOOST_COUNT = 6;
    private static final int MIN_DEBRIS_ON_GROUND_TICKS = 0;
    private static final float MIN_DEBRIS_CLUSTER_STRENGTH = 0.025F;
    private static final int RADAR_SITE_MARKER_COLOR = 0xFFC850E0;
    private static final int BROKEN_RADAR_SITE_MARKER_COLOR = 0xFFFF3030;
    private static final Color REFLECTIVITY_BASE = new Color(12, 28, 32);
    // NOAA WSR-88D 2620003R, section 49.2.2: recommended CC bins and RGB values.
    private static final float[] CORRELATION_THRESHOLDS = {
            0.20F, 0.45F, 0.65F, 0.75F, 0.80F, 0.85F, 0.90F,
            0.93F, 0.95F, 0.96F, 0.97F, 0.98F, 0.99F, 1.00F
    };
    private static final Color[] CORRELATION_COLORS = {
            new Color(0x95949C), new Color(0x16148C), new Color(0x0902D9),
            new Color(0x8987D6), new Color(0x5CFF59), new Color(0x8BCF02),
            new Color(0xFFFB00), new Color(0xFFC400), new Color(0xFF8903),
            new Color(0xFF2B00), new Color(0xE30000), new Color(0xA10000),
            new Color(0x970556), new Color(0xFAACD1)
    };
    private static final String STATION_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int STATION_CODE_RANDOM_ATTEMPTS = 128;
    private static final Random STATION_CODE_RANDOM = new Random();
    private static final double[][] CHUNK_SAMPLE_OFFSETS = {
            {2.0D, 8.0D},
            {8.0D, 8.0D},
            {14.0D, 8.0D},
            {8.0D, 2.0D},
            {8.0D, 14.0D}
    };

    private static final Map<ChunkKey, RadarReturn> chunkCache = new ConcurrentHashMap<>();
    private static final Map<RegionKey, Boolean> regionCache = new ConcurrentHashMap<>();
    private static final Map<BlockPos, RadarSite> rememberedRadarSites = new ConcurrentHashMap<>();
    private static final Map<Long, LightningMarker> lightningMarkers = new ConcurrentHashMap<>();
    private static final Map<Long, SyncedDebrisClusterState> syncedDebrisClusters = new ConcurrentHashMap<>();

    private static volatile RadarState state = RadarState.empty();
    private static volatile RadarMode radarMode = RadarMode.REFLECTIVITY;
    private static volatile boolean displayEnabled = true;
    private static volatile boolean dualModeEnabled = false;
    private static volatile RadarMode dualUpperMode = RadarMode.REFLECTIVITY;
    private static volatile RadarMode dualLowerMode = RadarMode.REFLECTIVITY;
    private static volatile boolean dualModesInitialized = false;
    private static volatile boolean radarLocationsAlwaysVisible = false;
    private static volatile boolean lightningEnabled = false;
    private static volatile boolean stationLabelsOpen = false;
    private static volatile boolean menuAnimationsEnabled = true;
    private static volatile boolean textSwapAnimationsEnabled = true;
    private static volatile BlockPos selectedRadarSitePos;
    private static volatile List<RadarSite> cachedRadarSites = List.of();
    private static volatile Level cachedRadarSiteLevel;
    private static volatile ResourceKey<Level> cachedRadarSiteDimension;
    private static volatile String cachedRadarSiteStorageKey;
    private static volatile int lastRadarSiteScanTick = Integer.MIN_VALUE;
    private static volatile int lastRadarTick = Integer.MIN_VALUE;
    private static volatile boolean disconnectSnapshotSaved = false;
    private static boolean clientSettingsLoaded;
    private static RadarBlockEntity radarSampler;

    private StormOverlayData() {
    }

    public static boolean update(Minecraft minecraft) {
        ensureClientSettingsLoaded();
        if (minecraft.level == null) {
            return clear();
        }

        try {
            Level level = minecraft.level;
            ResourceKey<Level> dimension = level.dimension();
            int radarTick = (int) (minecraft.level.getGameTime() & Integer.MAX_VALUE);
            lastRadarTick = radarTick;
            disconnectSnapshotSaved = false;
            resetIfWorldChanged(minecraft, level, dimension, radarTick);

            WeatherHandlerClient weather = GameBusClientEvents.getClientWeather();
            if (weather == null || weather.getWorld() == null) {
                return clearWeatherOnly();
            }

            RadarBlockEntity sampler = radarSampler();
            sampler.tickCount = radarTick;

            double originX = minecraft.player == null ? 0.0D : minecraft.player.getX();
            double originZ = minecraft.player == null ? 0.0D : minecraft.player.getZ();
            int centerChunkX = floor(originX / 16.0D);
            int centerChunkZ = floor(originZ / 16.0D);
            int loadedRadiusChunks = clientLoadedRadius(minecraft);
            int radarFrame = radarTick / RADAR_FRAME_INTERVAL_TICKS;
            List<Storm> storms = List.copyOf(new ArrayList<>(weather.getStorms()));
            pruneSyncedDebrisClusters(dimension, radarTick);
            List<DebrisCluster> debrisClusters = debrisClusters(level, storms);
            updateLightningMarkers(dimension, weather, radarTick);
            List<RadarSite> radarSites = radarSites(level, dimension, centerChunkX, centerChunkZ, loadedRadiusChunks, radarTick);
            RadarMode mode = radarMode;
            boolean hasRadarReturns = hasRadarReturns(storms);
            RadarState next = new RadarState(
                    level,
                    dimension,
                    weather,
                    storms,
                    debrisClusters,
                    hasRadarReturns,
                    sampler,
                    radarSites,
                    radarFrame,
                    mode,
                    hash(storms, debrisClusters, radarFrame, mode, radarSites, hasRadarReturns)
            );

            return replace(next);
        } catch (RuntimeException exception) {
            return clearWeatherOnly();
        }
    }

    private static boolean clearWeatherOnly() {
        boolean hadData = !state.isEmpty()
                || !chunkCache.isEmpty()
                || !regionCache.isEmpty()
                || !syncedDebrisClusters.isEmpty();
        saveLightningMarkers();
        chunkCache.clear();
        regionCache.clear();
        syncedDebrisClusters.clear();
        return replace(RadarState.empty()) || hadData;
    }

    public static boolean clear() {
        ensureClientSettingsLoaded();
        boolean hadData = !state.isEmpty()
                || !cachedRadarSites.isEmpty()
                || !rememberedRadarSites.isEmpty()
                || !chunkCache.isEmpty()
                || !regionCache.isEmpty()
                || !lightningMarkers.isEmpty()
                || !syncedDebrisClusters.isEmpty();
        prepareForDisconnect();
        clearRadarSiteCache();
        selectedRadarSitePos = null;
        chunkCache.clear();
        regionCache.clear();
        lightningMarkers.clear();
        syncedDebrisClusters.clear();
        return replace(RadarState.empty()) || hadData;
    }

    public static void prepareForDisconnect() {
        ensureClientSettingsLoaded();
        if (disconnectSnapshotSaved) {
            return;
        }

        saveClientSettings();
        saveRememberedRadarSites();
        saveLightningMarkers();
        disconnectSnapshotSaved = true;
    }

    public static RadarMode getRadarMode() {
        ensureClientSettingsLoaded();
        return radarMode;
    }

    public static boolean isDisplayEnabled() {
        ensureClientSettingsLoaded();
        return displayEnabled;
    }

    public static boolean setDisplayEnabled(boolean enabled) {
        ensureClientSettingsLoaded();
        if (displayEnabled == enabled) {
            return false;
        }

        displayEnabled = enabled;
        chunkCache.clear();
        regionCache.clear();
        saveClientSettings();
        return true;
    }

    public static boolean isDualModeEnabled() {
        ensureClientSettingsLoaded();
        return dualModeEnabled;
    }

    public static boolean toggleDualModeEnabled() {
        ensureClientSettingsLoaded();
        dualModeEnabled = !dualModeEnabled;
        if (dualModeEnabled) {
            seedDualModesFromCurrentMode();
        }
        saveClientSettings();
        return dualModeEnabled;
    }

    public static RadarMode getDualUpperMode() {
        ensureClientSettingsLoaded();
        return dualUpperMode;
    }

    public static RadarMode getDualLowerMode() {
        ensureClientSettingsLoaded();
        return dualLowerMode;
    }

    public static boolean setDualUpperMode(RadarMode next) {
        ensureClientSettingsLoaded();
        if (next == null || dualUpperMode == next) {
            return false;
        }

        dualUpperMode = next;
        dualModesInitialized = true;
        saveClientSettings();
        return true;
    }

    public static boolean setDualLowerMode(RadarMode next) {
        ensureClientSettingsLoaded();
        if (next == null || dualLowerMode == next) {
            return false;
        }

        dualLowerMode = next;
        dualModesInitialized = true;
        saveClientSettings();
        return true;
    }

    private static void seedDualModesFromCurrentMode() {
        dualUpperMode = radarMode;
        if (!dualModesInitialized) {
            dualLowerMode = radarMode;
            dualModesInitialized = true;
        }
    }

    public static boolean isRadarLocationsAlwaysVisible() {
        ensureClientSettingsLoaded();
        return radarLocationsAlwaysVisible;
    }

    public static boolean toggleRadarLocationsAlwaysVisible() {
        ensureClientSettingsLoaded();
        radarLocationsAlwaysVisible = !radarLocationsAlwaysVisible;
        chunkCache.clear();
        regionCache.clear();
        saveClientSettings();
        return radarLocationsAlwaysVisible;
    }

    public static boolean shouldShowRadarLocations() {
        ensureClientSettingsLoaded();
        return displayEnabled || radarLocationsAlwaysVisible;
    }

    public static boolean isLightningEnabled() {
        ensureClientSettingsLoaded();
        return lightningEnabled;
    }

    public static boolean toggleLightningEnabled() {
        ensureClientSettingsLoaded();
        lightningEnabled = !lightningEnabled;
        saveClientSettings();
        return lightningEnabled;
    }

    public static boolean isStationLabelsOpen() {
        ensureClientSettingsLoaded();
        return stationLabelsOpen;
    }

    public static boolean setStationLabelsOpen(boolean open) {
        ensureClientSettingsLoaded();
        if (stationLabelsOpen == open) {
            return false;
        }

        stationLabelsOpen = open;
        saveClientSettings();
        return true;
    }

    public static boolean isMenuAnimationsEnabled() {
        ensureClientSettingsLoaded();
        return menuAnimationsEnabled;
    }

    public static boolean toggleMenuAnimationsEnabled() {
        ensureClientSettingsLoaded();
        menuAnimationsEnabled = !menuAnimationsEnabled;
        saveClientSettings();
        return menuAnimationsEnabled;
    }

    public static boolean isTextSwapAnimationsEnabled() {
        ensureClientSettingsLoaded();
        return textSwapAnimationsEnabled;
    }

    public static boolean toggleTextSwapAnimationsEnabled() {
        ensureClientSettingsLoaded();
        textSwapAnimationsEnabled = !textSwapAnimationsEnabled;
        saveClientSettings();
        return textSwapAnimationsEnabled;
    }

    public static boolean hasRadarData(ResourceKey<Level> dimension) {
        RadarState current = state;
        return current.matches(dimension)
                && !current.radarSites().isEmpty()
                && shouldShowRadarLocations();
    }

    public static boolean cycleRadarMode(int direction) {
        if (direction == 0) {
            return false;
        }

        RadarMode[] modes = RadarMode.values();
        RadarMode current = radarMode;
        RadarMode next = modes[Math.floorMod(current.ordinal() + (direction > 0 ? 1 : -1), modes.length)];
        return setRadarMode(next);
    }

    public static boolean setRadarMode(RadarMode next) {
        ensureClientSettingsLoaded();
        if (next == null) {
            return false;
        }

        RadarMode current = radarMode;
        if (next == current) {
            return false;
        }

        radarMode = next;
        RadarState currentState = state;
        if (!currentState.isEmpty()) {
            state = currentState.withMode(next);
        }

        chunkCache.clear();
        regionCache.clear();
        saveClientSettings();
        return true;
    }

    public static boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        RadarState current = state;
        if ((!displayEnabled && !shouldShowRadarLocations())
                || !current.matches(dimension)
                || current.radarSites().isEmpty()
                || !regionCanContainRadarCoverage(current, regionX, regionZ)) {
            return false;
        }

        return regionCache.computeIfAbsent(
                new RegionKey(current.hash(), dimension, regionX, regionZ),
                key -> (displayEnabled && current.hasRadarReturns()) || regionContainsRadarSite(current, regionX, regionZ)
        );
    }

    public static boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (!displayEnabled && !shouldShowRadarLocations()) {
            return false;
        }

        RadarState current = state;
        return displayEnabled && radarReturnForChunk(current, dimension, chunkX, chunkZ).visible()
                || radarSiteForChunk(current, dimension, chunkX, chunkZ) != null;
    }

    public static int[] colorsForChunk(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (!displayEnabled && !shouldShowRadarLocations()) {
            return null;
        }

        RadarState current = state;
        RadarReturn center = displayEnabled ? radarReturnForChunk(current, dimension, chunkX, chunkZ) : RadarReturn.empty();
        if (!center.visible()) {
            RadarSite site = radarSiteForChunk(current, dimension, chunkX, chunkZ);
            if (site == null) {
                return null;
            }

            int markerColor = site.markerColor();
            return new int[]{markerColor, markerColor, markerColor, markerColor, markerColor};
        }

        int transparent = center.color() & 0xFFFFFF00;
        int north = sideColorOrTransparent(dimension, chunkX, chunkZ - 1, transparent);
        int east = sideColorOrTransparent(dimension, chunkX + 1, chunkZ, transparent);
        int south = sideColorOrTransparent(dimension, chunkX, chunkZ + 1, transparent);
        int west = sideColorOrTransparent(dimension, chunkX - 1, chunkZ, transparent);

        return new int[]{center.color(), north, east, south, west};
    }

    public static int regionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        RadarState current = state;
        if ((!displayEnabled && !shouldShowRadarLocations()) || !current.matches(dimension) || !regionHasHighlights(dimension, regionX, regionZ)) {
            return 0;
        }

        return 31 * current.hash()
                + Boolean.hashCode(displayEnabled)
                + Boolean.hashCode(radarLocationsAlwaysVisible) * 17
                + regionX * 7349
                + regionZ * 9151;
    }

    public static Component tooltipForChunk(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (!displayEnabled && !shouldShowRadarLocations()) {
            return null;
        }

        RadarState current = state;
        RadarSite site = radarSiteForChunk(current, dimension, chunkX, chunkZ);
        if (site != null) {
            return Component.literal(site.displayName() + " Radar - " + (site.operational() ? "Active" : "Broken"));
        }

        if (!displayEnabled) {
            return null;
        }

        RadarReturn radarReturn = radarReturnForChunk(current, dimension, chunkX, chunkZ);
        if (!radarReturn.visible()) {
            return null;
        }

        return Component.literal(current.mode().tooltip(Math.round(radarReturn.amount())));
    }

    public static Component tooltipForBlock(ResourceKey<Level> dimension, int blockX, int blockZ) {
        return tooltipForChunk(dimension, Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    }

    public static int argbForBlock(ResourceKey<Level> dimension, double blockX, double blockZ) {
        if (!displayEnabled) {
            return 0;
        }

        RadarState current = state;
        if (!current.matches(dimension)) {
            return 0;
        }

        RadarReturn radarReturn = radarReturnAt(current, blockX, blockZ);
        return radarReturn.visible() ? xaeroToArgb(radarReturn.color()) : 0;
    }

    public static int argbForRadarSite(ResourceKey<Level> dimension, BlockPos sitePos, double blockX, double blockZ) {
        return argbForRadarSite(dimension, sitePos, blockX, blockZ, true);
    }

    public static int argbForRadarSiteTexture(ResourceKey<Level> dimension, BlockPos sitePos, double blockX, double blockZ) {
        return argbForRadarSite(dimension, sitePos, blockX, blockZ, false, null);
    }

    public static int argbForRadarSiteTexture(ResourceKey<Level> dimension, BlockPos sitePos, double blockX, double blockZ, RadarMode mode) {
        return argbForRadarSite(dimension, sitePos, blockX, blockZ, false, mode);
    }

    private static int argbForRadarSite(
            ResourceKey<Level> dimension,
            BlockPos sitePos,
            double blockX,
            double blockZ,
            boolean applyBlindSpot
    ) {
        return argbForRadarSite(dimension, sitePos, blockX, blockZ, applyBlindSpot, null);
    }

    private static int argbForRadarSite(
            ResourceKey<Level> dimension,
            BlockPos sitePos,
            double blockX,
            double blockZ,
            boolean applyBlindSpot,
            RadarMode mode
    ) {
        if (!displayEnabled || sitePos == null) {
            return 0;
        }

        RadarState current = state;
        if (!current.matches(dimension)) {
            return 0;
        }

        RadarState sampledState = mode == null || current.mode() == mode ? current : current.withMode(mode);
        RadarSite site = radarSiteForPos(sampledState, sitePos);
        if (site == null || !site.operational()) {
            return 0;
        }

        RadarReturn radarReturn = radarReturnAt(sampledState, site, blockX, blockZ, applyBlindSpot);
        return radarReturn.visible() ? xaeroToArgb(radarReturn.color()) : 0;
    }

    public static int stateHash(ResourceKey<Level> dimension) {
        return stateHash(dimension, null);
    }

    public static int stateHash(ResourceKey<Level> dimension, RadarMode mode) {
        RadarState current = state;
        if (!displayEnabled || !current.matches(dimension)) {
            return 0;
        }

        RadarState hashedState = mode == null || current.mode() == mode ? current : current.withMode(mode);
        BlockPos selected = selectedRadarSitePos;
        return selected == null ? hashedState.hash() : 31 * hashedState.hash() + selected.hashCode();
    }

    public static List<RadarSiteView> radarSiteViews(ResourceKey<Level> dimension) {
        RadarState current = state;
        if (!shouldShowRadarLocations() || !current.matches(dimension)) {
            return List.of();
        }

        BlockPos selected = selectedRadarSiteForViews(current);
        List<RadarSiteView> views = new ArrayList<>(current.radarSites().size());
        boolean selectedFound = selected == null;
        for (RadarSite site : current.radarSites()) {
            boolean siteSelected = site.pos().equals(selected);
            selectedFound |= siteSelected;
            views.add(site.view(siteSelected));
        }

        if (!selectedFound) {
            selectedRadarSitePos = null;
        }

        return List.copyOf(views);
    }

    public static List<StormView> stormViews(ResourceKey<Level> dimension) {
        RadarState current = state;
        if (!displayEnabled || !current.matches(dimension)) {
            return List.of();
        }

        List<StormView> views = new ArrayList<>();
        for (Storm storm : current.storms()) {
            if (storm == null || storm.dead || storm.visualOnly || storm.position == null) {
                continue;
            }

            boolean tornadic;
            try {
                tornadic = storm.isTornadic();
            } catch (RuntimeException exception) {
                tornadic = storm.stage >= 3;
            }

            float width = Float.isFinite(storm.width) ? Math.max(16.0F, storm.width) : 16.0F;
            views.add(new StormView(
                    storm.position.x,
                    storm.position.z,
                    width,
                    storm.stage,
                    storm.windspeed,
                    tornadic
            ));
        }

        return List.copyOf(views);
    }

    public static List<LightningStrikeView> lightningStrikeViews(ResourceKey<Level> dimension) {
        RadarState current = state;
        if (!displayEnabled || !lightningEnabled || !current.matches(dimension)) {
            return List.of();
        }

        int radarTick = (int) (current.level().getGameTime() & Integer.MAX_VALUE);
        List<LightningStrikeView> views = new ArrayList<>();
        for (LightningMarker marker : lightningMarkers.values()) {
            if (!marker.matches(dimension)) {
                continue;
            }

            int age = marker.ageTicks(radarTick);
            if (age < 0 || age > LIGHTNING_MARKER_LIFETIME_TICKS) {
                continue;
            }

            float alpha = (float) clamp(1.0D - age / (double) LIGHTNING_MARKER_LIFETIME_TICKS, 0.0D, 1.0D);
            views.add(new LightningStrikeView(marker.x(), marker.z(), marker.strength(), alpha));
        }

        return List.copyOf(views);
    }

    private static boolean updateLightningMarkers(ResourceKey<Level> dimension, WeatherHandlerClient weather, int radarTick) {
        boolean changed = false;
        for (ClientLightning lightning : new ArrayList<>(weather.lightnings)) {
            if (lightning == null || lightning.position == null || lightning.dead) {
                continue;
            }

            if (lightning.level != null && !dimension.equals(lightning.level.dimension())) {
                continue;
            }

            LightningMarker marker = new LightningMarker(
                    dimension,
                    lightning.position.x,
                    lightning.position.z,
                    Math.max(0.0F, lightning.strength),
                    radarTick
            );
            changed |= lightningMarkers.putIfAbsent(lightning.seed, marker) == null;
        }

        int maxAge = LIGHTNING_MARKER_LIFETIME_TICKS;
        for (Iterator<Map.Entry<Long, LightningMarker>> iterator = lightningMarkers.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Long, LightningMarker> entry = iterator.next();
            LightningMarker marker = entry.getValue();
            int age = marker.ageTicks(radarTick);
            if (!marker.matches(dimension) || age < 0 || age > maxAge) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            saveLightningMarkers();
        }

        return changed;
    }

    private static boolean isTornadic(Storm storm) {
        try {
            return storm.isTornadic();
        } catch (RuntimeException exception) {
            return storm.stage >= 3;
        }
    }

    private static String stormTypeId(Storm storm) {
        try {
            return storm.stormType == null ? "" : storm.stormType.getId();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static BlockPos selectedRadarSiteForViews(RadarState current) {
        BlockPos selected = selectedRadarSitePos;
        if (selected != null && radarSiteForPos(current, selected) != null) {
            return selected;
        }

        RadarSite nearest = nearestRadarSiteToPlayer(current);
        if (nearest != null) {
            setSelectedRadarSite(nearest.pos());
            return nearest.pos();
        }

        if (!current.radarSites().isEmpty()) {
            setSelectedRadarSite(current.radarSites().getFirst().pos());
            return selectedRadarSitePos;
        }

        selectedRadarSitePos = null;
        return null;
    }

    private static RadarSite nearestRadarSiteToPlayer(RadarState current) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !current.matches(minecraft.level.dimension())) {
            return null;
        }

        RadarSite nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        double playerX = minecraft.player.getX();
        double playerZ = minecraft.player.getZ();
        for (RadarSite site : current.radarSites()) {
            if (!site.operational()) {
                continue;
            }

            double distance = site.distanceSqr(playerX, playerZ);
            if (distance < nearestDistance) {
                nearest = site;
                nearestDistance = distance;
            }
        }

        if (nearest != null) {
            return nearest;
        }

        nearestDistance = Double.MAX_VALUE;
        for (RadarSite site : current.radarSites()) {
            double distance = site.distanceSqr(playerX, playerZ);
            if (distance < nearestDistance) {
                nearest = site;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    public static boolean selectRadarSite(BlockPos pos) {
        if (pos == null) {
            return false;
        }

        BlockPos immutable = pos.immutable();
        if (immutable.equals(selectedRadarSitePos)) {
            return false;
        }

        selectedRadarSitePos = immutable;
        chunkCache.clear();
        regionCache.clear();
        saveRememberedRadarSites();
        return true;
    }

    public static boolean applySyncedRadarSites(
            ResourceLocation dimensionLocation,
            List<SyncedRadarSite> syncedSites,
            boolean fullSync
    ) {
        if (dimensionLocation == null || syncedSites == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || !dimensionLocation.equals(level.dimension().location())) {
            return false;
        }

        String storageKey = radarSiteStorageKey(minecraft, level.dimension());
        if (!Objects.equals(cachedRadarSiteStorageKey, storageKey)) {
            saveRememberedRadarSites();
            clearRadarSiteCache();
            cachedRadarSiteStorageKey = storageKey;
            cachedRadarSiteDimension = level.dimension();
            loadRememberedRadarSites();
        }

        boolean changed = false;
        if (fullSync) {
            Set<BlockPos> syncedPositions = new HashSet<>();
            for (SyncedRadarSite syncedSite : syncedSites) {
                if (syncedSite != null && syncedSite.pos() != null && syncedSite.visible()) {
                    syncedPositions.add(syncedSite.pos().immutable());
                }
            }

            for (BlockPos rememberedPos : List.copyOf(rememberedRadarSites.keySet())) {
                if (!syncedPositions.contains(rememberedPos)) {
                    changed |= rememberedRadarSites.remove(rememberedPos) != null;
                    if (rememberedPos.equals(selectedRadarSitePos)) {
                        selectedRadarSitePos = null;
                    }
                }
            }
        }

        for (SyncedRadarSite syncedSite : syncedSites) {
            if (syncedSite == null || syncedSite.pos() == null) {
                continue;
            }

            BlockPos pos = syncedSite.pos().immutable();
            if (!syncedSite.visible()) {
                changed |= rememberedRadarSites.remove(pos) != null;
                if (pos.equals(selectedRadarSitePos)) {
                    selectedRadarSitePos = null;
                }
                continue;
            }

            RadarSite next = radarSiteForTower(
                    level,
                    pos,
                    syncedSite.operational(),
                    syncedSite.stationCode(),
                    null,
                    syncedSite.rangeUpgraded()
            );
            RadarSite previous = rememberedRadarSites.put(pos, next);
            changed |= previous == null || !previous.equals(next);
        }

        if (!changed) {
            return false;
        }

        cachedRadarSites = sortedRememberedRadarSites();
        cachedRadarSiteLevel = level;
        cachedRadarSiteDimension = level.dimension();
        if (!state.isEmpty() && state.matches(level.dimension())) {
            state = state.withRadarSites(cachedRadarSites);
        }

        chunkCache.clear();
        regionCache.clear();
        saveRememberedRadarSites();
        return true;
    }

    public static RadarSitesPayload rememberedRadarSiteHints() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            return null;
        }

        List<RadarSitesPayload.Entry> entries = new ArrayList<>(rememberedRadarSites.size());
        for (RadarSite site : rememberedRadarSites.values()) {
            entries.add(RadarSitesPayload.Entry.visible(
                    site.pos(),
                    site.operational(),
                    site.radiusBlocks() > BASE_RADAR_RADIUS_BLOCKS,
                    site.stationCode()
            ));
        }

        return new RadarSitesPayload(level.dimension().location(), List.copyOf(entries), false);
    }

    public static boolean applySyncedDebrisClusters(ResourceLocation dimensionLocation, List<SyncedDebrisCluster> syncedClusters) {
        if (dimensionLocation == null || syncedClusters == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || !dimensionLocation.equals(level.dimension().location())) {
            return false;
        }

        int radarTick = (int) (level.getGameTime() & Integer.MAX_VALUE);
        boolean changed = pruneSyncedDebrisClusters(level.dimension(), radarTick);
        for (SyncedDebrisCluster syncedCluster : syncedClusters) {
            if (syncedCluster == null || syncedCluster.strength() <= 0.0F || syncedCluster.radius() <= 0.0D) {
                continue;
            }

            SyncedDebrisClusterState next = new SyncedDebrisClusterState(
                    level.dimension(),
                    syncedCluster.stormId(),
                    syncedCluster.x(),
                    syncedCluster.z(),
                    syncedCluster.radius(),
                    clamp(syncedCluster.strength(), 0.0F, 1.0F),
                    Math.max(0, syncedCluster.count()),
                    radarTick + 70
            );
            SyncedDebrisClusterState previous = syncedDebrisClusters.put(syncedCluster.stormId(), next);
            changed |= previous == null || !previous.equals(next);
        }

        if (changed) {
            chunkCache.clear();
            regionCache.clear();
        }

        return changed;
    }

    private static void setSelectedRadarSite(BlockPos pos) {
        BlockPos immutable = pos == null ? null : pos.immutable();
        if (immutable == null ? selectedRadarSitePos == null : immutable.equals(selectedRadarSitePos)) {
            return;
        }

        selectedRadarSitePos = immutable;
        chunkCache.clear();
        regionCache.clear();
        saveRememberedRadarSites();
    }

    public static boolean selectedRadarSiteMatches(BlockPos pos) {
        return pos != null && pos.equals(selectedRadarSitePos);
    }

    private static boolean replace(RadarState next) {
        RadarState previous = state;
        boolean changed = previous.hash() != next.hash() || !previous.sameDimension(next);
        state = next;

        if (changed) {
            chunkCache.clear();
            regionCache.clear();
        }

        return changed;
    }

    private static int clientLoadedRadius(Minecraft minecraft) {
        try {
            return Math.max(4, Math.min(MAX_LOADED_AREA_RADIUS_CHUNKS, minecraft.options.renderDistance().get() + 2));
        } catch (RuntimeException exception) {
            return 16;
        }
    }

    private static void resetIfWorldChanged(Minecraft minecraft, Level level, ResourceKey<Level> dimension, int radarTick) {
        String storageKey = radarSiteStorageKey(minecraft, dimension);
        RadarState current = state;
        boolean changedWorld = (!current.isEmpty() && current.level() != level)
                || (cachedRadarSiteLevel != null && cachedRadarSiteLevel != level);
        boolean changedDimension = (!current.isEmpty() && (current.dimension() == null || !current.dimension().equals(dimension)))
                || (cachedRadarSiteDimension != null && !cachedRadarSiteDimension.equals(dimension));
        boolean changedStorage = !Objects.equals(cachedRadarSiteStorageKey, storageKey);
        if (changedWorld || changedDimension || changedStorage) {
            saveRememberedRadarSites();
            saveLightningMarkers();
            clearRadarSiteCache();
            cachedRadarSiteStorageKey = storageKey;
            cachedRadarSiteDimension = dimension;
            selectedRadarSitePos = null;
            loadRememberedRadarSites();
            chunkCache.clear();
            regionCache.clear();
            lightningMarkers.clear();
            syncedDebrisClusters.clear();
            loadLightningMarkers(dimension, radarTick);
        }
    }

    private static List<RadarSite> radarSites(
            Level level,
            ResourceKey<Level> dimension,
            int centerChunkX,
            int centerChunkZ,
            int loadedRadiusChunks,
            int radarTick
    ) {
        boolean stale = cachedRadarSiteDimension == null
                || cachedRadarSiteLevel != level
                || !cachedRadarSiteDimension.equals(dimension)
                || radarTick < lastRadarSiteScanTick
                || radarTick - lastRadarSiteScanTick >= SITE_SCAN_INTERVAL_TICKS;

        if (stale) {
            cachedRadarSites = scanRadarSites(level, centerChunkX, centerChunkZ, loadedRadiusChunks);
            cachedRadarSiteLevel = level;
            cachedRadarSiteDimension = dimension;
            lastRadarSiteScanTick = radarTick;
        }

        return cachedRadarSites;
    }

    private static List<RadarSite> scanRadarSites(Level level, int centerChunkX, int centerChunkZ, int loadedRadiusChunks) {
        List<RadarSite> foundSites = new ArrayList<>();
        Set<String> reservedStationCodes = new HashSet<>();
        int minChunkX = centerChunkX - loadedRadiusChunks;
        int maxChunkX = centerChunkX + loadedRadiusChunks;
        int minChunkZ = centerChunkZ - loadedRadiusChunks;
        int maxChunkZ = centerChunkZ + loadedRadiusChunks;

        boolean changed = validateRememberedRadarSites(level, centerChunkX, centerChunkZ);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                try {
                    LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    scanTowerCores(level, chunk, foundSites, reservedStationCodes);
                } catch (RuntimeException ignored) {
                }
            }
        }

        for (RadarSite site : foundSites) {
            RadarSite previous = rememberedRadarSites.put(site.pos(), site);
            changed |= previous == null || !previous.equals(site);
        }

        if (changed) {
            saveRememberedRadarSites();
        }

        return sortedRememberedRadarSites();
    }

    private static boolean validateRememberedRadarSites(Level level, int centerChunkX, int centerChunkZ) {
        List<BlockPos> removed = new ArrayList<>();
        List<RadarSite> updated = new ArrayList<>();
        Set<String> reservedStationCodes = new HashSet<>();
        for (Map.Entry<BlockPos, RadarSite> entry : rememberedRadarSites.entrySet()) {
            BlockPos pos = entry.getKey();
            int chunkX = Math.floorDiv(pos.getX(), 16);
            int chunkZ = Math.floorDiv(pos.getZ(), 16);
            if (Math.abs(chunkX - centerChunkX) > RADAR_SITE_VALIDATION_RADIUS_CHUNKS
                    || Math.abs(chunkZ - centerChunkZ) > RADAR_SITE_VALIDATION_RADIUS_CHUNKS
                    || !level.hasChunk(chunkX, chunkZ)) {
                continue;
            }

            RadarTowerScanner.TowerState towerState = RadarTowerScanner.stateAt(level, pos);
            if (towerState == RadarTowerScanner.TowerState.UNKNOWN) {
                continue;
            }

            if (!towerState.visible()) {
                removed.add(pos);
                continue;
            }

            RadarSite next = radarSiteForTower(level, pos, towerState.operational(), null, reservedStationCodes);
            if (!next.equals(entry.getValue())) {
                updated.add(next);
            }
        }

        for (BlockPos pos : removed) {
            rememberedRadarSites.remove(pos);
        }

        for (RadarSite site : updated) {
            rememberedRadarSites.put(site.pos(), site);
        }

        return !removed.isEmpty() || !updated.isEmpty();
    }

    private static void scanTowerCores(Level level, LevelChunk chunk, List<RadarSite> sites, Set<String> reservedStationCodes) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(RadarTowerScanner::isTowerCore)) {
                continue;
            }

            int blockY = level.getSectionYFromSectionIndex(sectionIndex) << 4;
            int blockX = chunk.getPos().x << 4;
            int blockZ = chunk.getPos().z << 4;

            for (int localY = 0; localY < 16; localY++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        BlockState blockState = section.getBlockState(localX, localY, localZ);
                        if (RadarTowerScanner.isTowerCore(blockState)) {
                            BlockPos pos = new BlockPos(blockX + localX, blockY + localY, blockZ + localZ);
                            RadarTowerScanner.TowerState towerState = RadarTowerScanner.stateAt(level, pos);
                            if (towerState.visible()) {
                                sites.add(radarSiteForTower(level, pos, towerState.operational(), null, reservedStationCodes));
                            }
                        }
                    }
                }
            }
        }
    }

    private static RadarSite radarSiteForTower(BlockPos pos, boolean operational) {
        return radarSiteForTower(pos, operational, null, null);
    }

    private static RadarSite radarSiteForTower(Level level, BlockPos pos, boolean operational) {
        return radarSiteForTower(level, pos, operational, null, null);
    }

    private static RadarSite radarSiteForTower(BlockPos pos, boolean operational, String preferredStationCode) {
        return radarSiteForTower(pos, operational, preferredStationCode, null);
    }

    private static RadarSite radarSiteForTower(
            BlockPos pos,
            boolean operational,
            String preferredStationCode,
            Set<String> reservedStationCodes
    ) {
        return radarSiteForTower(null, pos, operational, preferredStationCode, reservedStationCodes);
    }

    private static RadarSite radarSiteForTower(
            Level level,
            BlockPos pos,
            boolean operational,
            String preferredStationCode,
            Set<String> reservedStationCodes
    ) {
        return radarSiteForTower(level, pos, operational, preferredStationCode, reservedStationCodes, null);
    }

    private static RadarSite radarSiteForTower(
            Level level,
            BlockPos pos,
            boolean operational,
            String preferredStationCode,
            Set<String> reservedStationCodes,
            Boolean rangeUpgradeOverride
    ) {
        BlockPos immutable = pos.immutable();
        RadarSite existing = rememberedRadarSites.get(immutable);
        String normalizedPreferredStationCode = normalizeStationCode(preferredStationCode);
        boolean authoritativeStationCode = normalizedPreferredStationCode != null;
        String stationCode = authoritativeStationCode
                ? normalizedPreferredStationCode
                : existing == null ? null : existing.stationCode();
        Set<String> usedCodes = usedStationCodes(immutable);
        if (reservedStationCodes != null) {
            usedCodes.addAll(reservedStationCodes);
        }

        stationCode = normalizeStationCode(stationCode);
        if (stationCode == null || (!authoritativeStationCode && usedCodes.contains(stationCode))) {
            stationCode = generateStationCode(usedCodes);
        }

        if (reservedStationCodes != null) {
            reservedStationCodes.add(stationCode);
        }

        // The tower's marker on the map always sits on the WSR-88D core itself. A nearby
        // "Radar" display block is a separate in-world screen and must never relocate or
        // otherwise stand in for the actual radar station.
        Vec3 center = immutable.getCenter();
        boolean rangeUpgraded = rangeUpgradeOverride != null
                ? rangeUpgradeOverride
                : level != null && hasRangeUpgrade(level, immutable);
        double radiusBlocks = rangeUpgraded
                ? BASE_RADAR_RADIUS_BLOCKS * 4.0D
                : BASE_RADAR_RADIUS_BLOCKS;
        return RadarSite.tower(immutable, center, radiusBlocks, operational, stationCode);
    }

    /**
     * A range-upgrade module only boosts a tower's radius while it sits directly beneath the
     * WSR-88D core, inside the radome shell - not merely somewhere near a "Radar" block.
     * The shell's vertical extent is read from the core's own structure data so this works
     * regardless of exactly how tall the built radome is.
     */
    private static boolean hasRangeUpgrade(Level level, BlockPos corePos) {
        if (!(level.getBlockState(corePos).getBlock() instanceof WSR88DCore core)) {
            return false;
        }

        int shellBottomOffset = 0;
        for (Map.Entry<BlockPos, Block> entry : core.getStructure().entrySet()) {
            if (entry.getValue() == ModBlocks.RADOME.get()) {
                shellBottomOffset = Math.min(shellBottomOffset, entry.getKey().getY());
            }
        }

        if (shellBottomOffset == 0) {
            shellBottomOffset = -RANGE_UPGRADE_FALLBACK_SEARCH_DEPTH_BLOCKS;
        }

        for (int dy = -1; dy >= shellBottomOffset; dy--) {
            BlockPos pos = corePos.offset(0, dy, 0);
            if (hasChunkAt(level, pos) && level.getBlockState(pos).is(ModBlocks.RANGE_UPGRADE_MODULE.get())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasChunkAt(Level level, BlockPos pos) {
        return level.hasChunk(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
    }

    private static Set<String> usedStationCodes(BlockPos excludedPos) {
        Set<String> usedCodes = new HashSet<>();
        for (Map.Entry<BlockPos, RadarSite> entry : rememberedRadarSites.entrySet()) {
            if (entry.getKey().equals(excludedPos)) {
                continue;
            }

            String stationCode = normalizeStationCode(entry.getValue().stationCode());
            if (stationCode != null) {
                usedCodes.add(stationCode);
            }
        }

        return usedCodes;
    }

    private static String generateStationCode(Set<String> usedCodes) {
        for (int attempt = 0; attempt < STATION_CODE_RANDOM_ATTEMPTS; attempt++) {
            String stationCode = randomStationCode(3);
            if (!usedCodes.contains(stationCode)) {
                return stationCode;
            }
        }

        for (int first = 0; first < STATION_CODE_ALPHABET.length(); first++) {
            for (int second = 0; second < STATION_CODE_ALPHABET.length(); second++) {
                for (int third = 0; third < STATION_CODE_ALPHABET.length(); third++) {
                    String stationCode = "K"
                            + STATION_CODE_ALPHABET.charAt(first)
                            + STATION_CODE_ALPHABET.charAt(second)
                            + STATION_CODE_ALPHABET.charAt(third);
                    if (!usedCodes.contains(stationCode)) {
                        return stationCode;
                    }
                }
            }
        }

        int letterCount = 4;
        while (letterCount < 12) {
            String stationCode = randomStationCode(letterCount);
            if (!usedCodes.contains(stationCode)) {
                return stationCode;
            }
            letterCount++;
        }

        return "K" + Long.toUnsignedString(System.nanoTime(), Character.MAX_RADIX).toUpperCase(Locale.ROOT);
    }

    private static String randomStationCode(int letterCount) {
        StringBuilder code = new StringBuilder(letterCount + 1);
        code.append('K');
        synchronized (STATION_CODE_RANDOM) {
            for (int i = 0; i < letterCount; i++) {
                code.append(STATION_CODE_ALPHABET.charAt(STATION_CODE_RANDOM.nextInt(STATION_CODE_ALPHABET.length())));
            }
        }

        return code.toString();
    }

    private static String normalizeStationCode(String stationCode) {
        if (stationCode == null) {
            return null;
        }

        String normalized = stationCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 4 || normalized.charAt(0) != 'K') {
            return null;
        }

        for (int i = 1; i < normalized.length(); i++) {
            char character = normalized.charAt(i);
            if (character < 'A' || character > 'Z') {
                return null;
            }
        }

        return normalized;
    }

    private static boolean regionCanContainRadarCoverage(RadarState current, int regionX, int regionZ) {
        double minBlockX = regionX * REGION_SIZE_CHUNKS * 16.0D;
        double minBlockZ = regionZ * REGION_SIZE_CHUNKS * 16.0D;
        double maxBlockX = minBlockX + REGION_SIZE_CHUNKS * 16.0D;
        double maxBlockZ = minBlockZ + REGION_SIZE_CHUNKS * 16.0D;

        for (RadarSite site : current.radarSites()) {
            double nearestX = clamp(site.x(), minBlockX, maxBlockX);
            double nearestZ = clamp(site.z(), minBlockZ, maxBlockZ);
            if (site.distanceSqr(nearestX, nearestZ) <= site.radiusSqr()) {
                return true;
            }
        }

        return false;
    }

    private static boolean regionContainsRadarSite(RadarState current, int regionX, int regionZ) {
        int startChunkX = regionX * REGION_SIZE_CHUNKS;
        int startChunkZ = regionZ * REGION_SIZE_CHUNKS;
        int endChunkX = startChunkX + REGION_SIZE_CHUNKS;
        int endChunkZ = startChunkZ + REGION_SIZE_CHUNKS;

        for (RadarSite site : current.radarSites()) {
            int siteChunkX = Math.floorDiv(site.pos().getX(), 16);
            int siteChunkZ = Math.floorDiv(site.pos().getZ(), 16);
            if (siteChunkX >= startChunkX && siteChunkX < endChunkX && siteChunkZ >= startChunkZ && siteChunkZ < endChunkZ) {
                return true;
            }
        }

        return false;
    }

    private static RadarReturn radarReturnForChunk(RadarState current, ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (!current.matches(dimension) || !current.hasRadarReturns() || current.radarSites().isEmpty()) {
            return RadarReturn.empty();
        }

        return chunkCache.computeIfAbsent(new ChunkKey(current.hash(), dimension, chunkX, chunkZ), key -> {
            RadarReturn strongest = RadarReturn.empty();
            double baseX = chunkX * 16.0D;
            double baseZ = chunkZ * 16.0D;

            for (double[] offset : CHUNK_SAMPLE_OFFSETS) {
                RadarReturn sampled = radarReturnAt(current, baseX + offset[0], baseZ + offset[1]);
                if (sampled.strength() > strongest.strength()) {
                    strongest = sampled;
                }
            }

            return strongest;
        });
    }

    private static RadarReturn radarReturnAt(RadarState current, double blockX, double blockZ) {
        try {
            RadarSite site = siteForBlock(current, blockX, blockZ);
            if (site == null) {
                return RadarReturn.empty();
            }

            return radarReturnAt(current, site, blockX, blockZ);
        } catch (RuntimeException exception) {
            return RadarReturn.empty();
        }
    }

    private static RadarReturn radarReturnAt(RadarState current, RadarSite site, double blockX, double blockZ) {
        return radarReturnAt(current, site, blockX, blockZ, true);
    }

    private static RadarReturn radarReturnAt(RadarState current, RadarSite site, double blockX, double blockZ, boolean applyBlindSpot) {
        try {
            if (!site.operational()) {
                return RadarReturn.empty();
            }

            if (applyBlindSpot && site.distanceSqr(blockX, blockZ) < RADAR_BLIND_SPOT_RADIUS_BLOCKS * RADAR_BLIND_SPOT_RADIUS_BLOCKS) {
                return RadarReturn.empty();
            }

            Vec3 worldPos = new Vec3(blockX, site.y(), blockZ);
            float radarDbz = reflectivityAt(current, worldPos);
            if (!Float.isFinite(radarDbz)) {
                return RadarReturn.empty();
            }

            return switch (current.mode()) {
                case REFLECTIVITY -> reflectivityReturn(site, blockX, blockZ, radarDbz);
                case VELOCITY -> velocityReturn(current, site, blockX, blockZ, radarDbz);
                case CORRELATION_COEFFICIENT -> correlationCoefficientReturn(current, site, blockX, blockZ, radarDbz);
            };
        } catch (RuntimeException exception) {
            return RadarReturn.empty();
        }
    }

    private static RadarSite siteForBlock(RadarState current, double blockX, double blockZ) {
        RadarSite selected = selectedRadarSite(current);
        if (selected != null) {
            return selected.distanceSqr(blockX, blockZ) <= selected.radiusSqr() ? selected : null;
        }

        RadarSite nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (RadarSite site : current.radarSites()) {
            double distance = site.distanceSqr(blockX, blockZ);
            if (distance > site.radiusSqr()) {
                continue;
            }

            if (distance < nearestDistance) {
                nearest = site;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private static RadarSite selectedRadarSite(RadarState current) {
        BlockPos selected = selectedRadarSitePos;
        if (selected == null) {
            return null;
        }

        for (RadarSite site : current.radarSites()) {
            if (site.pos().equals(selected)) {
                return site;
            }
        }

        selectedRadarSitePos = null;
        return null;
    }

    private static RadarSite radarSiteForPos(RadarState current, BlockPos pos) {
        for (RadarSite site : current.radarSites()) {
            if (site.pos().equals(pos)) {
                return site;
            }
        }

        return null;
    }

    private static float reflectivityAt(RadarState current, Vec3 worldPos) {
        float dbz = stormReflectivity(current, worldPos);
        float clouds = Clouds.getCloudDensity(current.weather(), new Vector2f((float) worldPos.x, (float) worldPos.z), 0.0F);
        float cloudReturn = Math.max(clouds - 0.15F, 0.0F) * 4.0F;

        if (cloudReturn > 0.3F) {
            cloudReturn -= (cloudReturn - 0.4F) / 1.5F;
        }

        dbz = Math.max(dbz, cloudReturn);
        if (dbz > 1.0F) {
            dbz = ((dbz - 1.0F) / 3.0F) + 1.0F;
        }

        return Math.max(0.0F, dbz * 60.0F + stableRadarNoise(worldPos.x, worldPos.z) * 2.6F);
    }

    private static RadarReturn reflectivityReturn(RadarSite site, double blockX, double blockZ, float radarDbz) {
        radarDbz = applyRangeDegradation(site, blockX, blockZ, radarDbz);
        if (radarDbz < MIN_VISIBLE_DBZ) {
            return RadarReturn.empty();
        }

        int color = colorForReflectivity(radarDbz);
        return new RadarReturn(color, radarDbz, radarDbz);
    }

    private static RadarReturn velocityReturn(RadarState current, RadarSite site, double blockX, double blockZ, float radarDbz) {
        radarDbz = applyRangeDegradation(site, blockX, blockZ, radarDbz);
        if (radarDbz < MIN_VISIBLE_DBZ) {
            return RadarReturn.empty();
        }

        float velocity = radialVelocity(current, site, blockX, blockZ);
        float displayVelocity = velocity / 1.75F;
        // Express PMWeather's weak-return weighting as coverage over the map,
        // keeping the palette RGB intact instead of blending it toward black.
        float strength = clamp(Math.max(radarDbz, (Math.abs(displayVelocity) - 90.0F) / 1.5F) / 15.0F, 0.0F, 1.0F);
        int rgb = colorForVelocity(displayVelocity).getRGB();
        int alpha = Math.round(255.0F * strength);
        return new RadarReturn(xaeroColor(red(rgb), green(rgb), blue(rgb), alpha), velocity, radarDbz);
    }

    private static RadarReturn correlationCoefficientReturn(RadarState current, RadarSite site, double blockX, double blockZ, float radarDbz) {
        radarDbz = applyRangeDegradation(site, blockX, blockZ, radarDbz);
        if (radarDbz < MIN_VISIBLE_DBZ) {
            return RadarReturn.empty();
        }

        float coefficient = correlationCoefficientAt(current, blockX, site.y(), blockZ, radarDbz);
        Color color = colorForCorrelationCoefficient(coefficient);
        return new RadarReturn(
                xaeroColor(color.getRed(), color.getGreen(), color.getBlue(), rangeAdjustedAlpha(site, blockX, blockZ, radarDbz)),
                coefficient * 1000.0F,
                radarDbz
        );
    }

    private static float correlationCoefficientAt(RadarState current, double blockX, double blockY, double blockZ, float radarDbz) {
        float signalQuality = clamp((radarDbz - MIN_VISIBLE_DBZ) / 30.0F, 0.0F, 1.0F);
        float weakSignal = 1.0F - signalQuality;
        float heavyUniformReturn = clamp((radarDbz - 42.0F) / 28.0F, 0.0F, 1.0F);
        float weakSignalNoise = stableCorrelationNoise(blockX, blockZ, 18.0D, current.radarFrame() * 53 + 211);
        // CC stays high for uniform rain; only weak signal, mixed targets, hail, or debris should push it down.
        float coefficient = 0.982F
                + signalQuality * 0.016F
                + heavyUniformReturn * 0.006F
                + correlationTexture(current, blockX, blockZ, radarDbz);
        coefficient -= weakSignal * (0.016F + weakSignalNoise * 0.055F);
        coefficient += weatherCorrelationAdjustmentAt(current, blockX, blockY, blockZ, radarDbz);
        float debrisSignature = tornadoDebrisSignatureAt(current, blockX, blockZ, radarDbz);
        if (debrisSignature > 0.0F) {
            float debrisCoefficient = 0.30F + (1.0F - debrisSignature) * 0.50F;
            coefficient = Math.min(coefficient, debrisCoefficient);
        }

        return clamp(coefficient, CORRELATION_MIN, 1.0F);
    }

    private static float tornadoDebrisSignatureAt(RadarState current, double blockX, double blockZ, float radarDbz) {
        float strongest = 0.0F;
        Vec3 samplePos = new Vec3(blockX, 0.0D, blockZ);

        for (DebrisCluster cluster : current.debrisClusters()) {
            double distance = samplePos.distanceTo(new Vec3(cluster.x(), 0.0D, cluster.z()));
            if (distance > cluster.radius()) {
                continue;
            }

            float reflectivityGate = clamp((radarDbz - 10.0F) / 24.0F, 0.0F, 1.0F);
            float distanceFactor = (float) Math.pow(1.0D - distance / cluster.radius(), 0.36D);
            strongest = Math.max(strongest, distanceFactor * cluster.strength() * reflectivityGate);
        }

        return clamp(strongest, 0.0F, 1.0F);
    }

    private static float weatherCorrelationAdjustmentAt(RadarState current, double blockX, double blockY, double blockZ, float radarDbz) {
        if (current.radarSampler() == null || current.storms().isEmpty()) {
            return 0.0F;
        }

        Vec3 worldPos = new Vec3(blockX, blockY, blockZ);
        Vec3 flatWorldPos = worldPos.multiply(1.0D, 0.0D, 1.0D);
        float strongestLift = 0.0F;
        float strongestDrop = 0.0F;

        for (Storm storm : current.storms()) {
            if (storm == null || storm.dead || storm.visualOnly || storm.position == null || !hasRadarRepresentation(storm)) {
                continue;
            }

            try {
                double distance = storm.position.multiply(1.0D, 0.0D, 1.0D).distanceTo(flatWorldPos);
                if (distance > storm.getRadarRenderRange()) {
                    continue;
                }

                Vec3 stormSamplePos = worldPos;
                float stormReturn = storm.getRadarReflectivityReturn(current.radarSampler(), stormSamplePos);
                if (!Float.isFinite(stormReturn) || stormReturn <= 0.005F) {
                    continue;
                }

                float returnFactor = clamp(stormReturn / 0.85F, 0.0F, 1.0F);
                float precipFactor = clamp(stormPrecipitation(storm, stormSamplePos) / 1.50F, 0.0F, 1.0F);
                float hailFactor = clamp(stormHail(storm, stormSamplePos) / 1.15F, 0.0F, 1.0F);
                float windFactor = clamp((stormWindSpeed(storm, stormSamplePos) - 55.0F) / 125.0F, 0.0F, 1.0F);
                float giantDropFactor = clamp((radarDbz - 48.0F) / 22.0F, 0.0F, 1.0F) * precipFactor * (1.0F - hailFactor);
                float rainHailMix = Math.min(precipFactor, hailFactor);
                float mixedPhaseFactor = clamp(hailFactor * 0.74F + rainHailMix * 0.26F + giantDropFactor * 0.18F, 0.0F, 1.0F);
                float severeHailFactor = hailFactor * clamp((windFactor + returnFactor) * 0.5F, 0.0F, 1.0F);
                float texture = stableCorrelationNoise(blockX, blockZ, 42.0D, (int) (storm.ID ^ current.radarFrame() * 97L)) - 0.5F;
                float highTexture = Math.max(texture, 0.0F) * 2.0F;
                float lowTexture = Math.max(-texture, 0.0F) * 2.0F;

                float localLift = returnFactor * (1.0F - mixedPhaseFactor) * (0.010F + precipFactor * 0.014F + lowTexture * 0.006F);
                float localDrop = mixedPhaseFactor * (0.070F + highTexture * 0.018F)
                        + severeHailFactor * 0.082F
                        + giantDropFactor * 0.024F
                        + windFactor * returnFactor * 0.010F;

                if (isCyclone(storm)) {
                    float cycloneBand = cycloneBandFactor(storm, current, blockX, blockZ, distance);
                    localLift += returnFactor * (1.0F - cycloneBand) * (0.010F + precipFactor * 0.006F);
                    localDrop += returnFactor * cycloneBand * (0.012F + windFactor * 0.036F);
                }

                strongestLift = Math.max(strongestLift, localLift);
                strongestDrop = Math.max(strongestDrop, localDrop);
            } catch (RuntimeException ignored) {
            }
        }

        return clamp(strongestLift - strongestDrop, -0.210F, 0.045F);
    }

    private static boolean hasRadarRepresentation(Storm storm) {
        try {
            return storm.hasRadarRepresentation();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static float stormPrecipitation(Storm storm, Vec3 worldPos) {
        try {
            float precipitation = storm.getPrecipitation(worldPos);
            return Float.isFinite(precipitation) ? Math.max(precipitation, 0.0F) : 0.0F;
        } catch (RuntimeException exception) {
            return 0.0F;
        }
    }

    private static float stormHail(Storm storm, Vec3 worldPos) {
        try {
            float hail = storm.getHail(worldPos);
            return Float.isFinite(hail) ? Math.max(hail, 0.0F) : 0.0F;
        } catch (RuntimeException exception) {
            return 0.0F;
        }
    }

    private static float stormWindSpeed(Storm storm, Vec3 worldPos) {
        try {
            Vec3 wind = storm.getRawWind(worldPos);
            return wind == null ? 0.0F : (float) wind.length();
        } catch (RuntimeException exception) {
            return 0.0F;
        }
    }

    private static boolean isCyclone(Storm storm) {
        String stormType = stormTypeId(storm);
        return stormType.endsWith(":cyclone") || stormType.contains("hurricane");
    }

    private static float cycloneBandFactor(Storm storm, RadarState current, double blockX, double blockZ, double distance) {
        double width = cycloneWidth(storm);
        double dx = blockX - storm.position.x;
        double dz = blockZ - storm.position.z;
        double angle = Math.atan2(dz, dx);
        float cellNoise = stableCorrelationNoise(blockX, blockZ, Math.max(12.0D, width * 0.018D), (int) (storm.ID ^ current.radarFrame() * 191L));
        float gapNoise = stableCorrelationNoise(blockX, blockZ, Math.max(32.0D, width * 0.042D), (int) (storm.ID ^ current.radarFrame() * 67L));
        double boundaryWobble = Math.sin(angle * 2.7D + storm.ID * 0.015D) * 0.075D
                + Math.sin(angle * 6.4D - current.radarFrame() * 0.18D + storm.ID * 0.027D) * 0.045D
                + (cellNoise - 0.5D) * 0.080D;
        double normalizedDistance = clamp(distance / (width * (1.0D + boundaryWobble)), 0.0D, 1.25D);
        double spiral = angle * 3.0D - normalizedDistance * 8.0D + current.radarFrame() * 0.45D + (cellNoise - 0.5D) * 1.35D;
        float outerBand = (float) ((Math.sin(spiral) + 1.0D) * 0.5D);
        float fineBand = (float) ((Math.sin(spiral * 1.9D + storm.ID * 0.013D) + 1.0D) * 0.5D);
        double eyeRadius = 0.16D
                + Math.sin(angle * 3.4D + storm.ID * 0.021D) * 0.035D
                + Math.sin(angle * 7.1D - current.radarFrame() * 0.16D + storm.ID * 0.031D) * 0.020D
                + (cellNoise - 0.5D) * 0.045D;
        double eyeThickness = 0.070D + gapNoise * 0.055D;
        float eyewall = 1.0F - (float) clamp(Math.abs(normalizedDistance - eyeRadius) / eyeThickness, 0.0D, 1.0D);
        eyewall = (float) Math.pow(eyewall, 1.35D);
        eyewall *= 0.42F + cellNoise * 0.58F;
        if (gapNoise < 0.22F) {
            eyewall *= 0.32F;
        }

        float rainBandEnvelope = (float) Math.pow(1.0D - clamp(Math.abs(normalizedDistance - 0.50D) / 0.65D, 0.0D, 1.0D), 0.65D);
        float scallopedBands = (outerBand * 0.44F + fineBand * 0.22F) * rainBandEnvelope * (0.72F + gapNoise * 0.36F);
        return clamp(scallopedBands + eyewall * 0.34F, 0.0F, 1.0F);
    }

    private static double cycloneWidth(Storm storm) {
        return Math.max(Float.isFinite(storm.width) ? storm.width : 0.0F, 96.0D);
    }

    private static float correlationTexture(RadarState current, double blockX, double blockZ, float radarDbz) {
        float weakReturn = 1.0F - clamp((radarDbz - MIN_VISIBLE_DBZ) / 22.0F, 0.0F, 1.0F);
        float fine = stableCorrelationNoise(blockX, blockZ, 10.0D, current.radarFrame() * 31 + 17) - 0.5F;
        float coarse = stableCorrelationNoise(blockX, blockZ, 26.0D, current.radarFrame() * 17 + 43) - 0.5F;
        return fine * (0.017F + weakReturn * 0.028F) + coarse * (0.010F + weakReturn * 0.018F);
    }

    private static float applyRangeDegradation(RadarSite site, double blockX, double blockZ, float radarDbz) {
        float quality = rangeQuality(site, blockX, blockZ);
        float farNoise = stableRangeNoise(site, blockX, blockZ) * (1.0F - quality) * 5.0F;
        return Math.max(0.0F, radarDbz * (0.88F + quality * 0.12F) + farNoise);
    }

    private static int rangeAdjustedAlpha(RadarSite site, double blockX, double blockZ, float strength) {
        return alphaForStrength(strength);
    }

    private static float rangeQuality(RadarSite site, double blockX, double blockZ) {
        double range = Math.sqrt(site.distanceSqr(blockX, blockZ)) / site.radiusBlocks();
        double far = clamp((range - 0.25D) / 0.75D, 0.0D, 1.0D);
        return (float) (1.0D - far * 0.18D);
    }

    private static float stableRangeNoise(RadarSite site, double blockX, double blockZ) {
        int x = floor(blockX / 32.0D);
        int z = floor(blockZ / 32.0D);
        int hash = site.pos().hashCode();
        hash = 31 * hash + x * 734287;
        hash = 31 * hash + z * 912271;
        hash ^= hash >>> 16;
        return ((hash & 1023) / 1023.0F) - 0.5F;
    }

    private static float stableRadarNoise(double blockX, double blockZ) {
        int x = floor(blockX / 28.0D);
        int z = floor(blockZ / 28.0D);
        int hash = x * 374761393 + z * 668265263;
        hash = (hash ^ (hash >>> 13)) * 1274126177;
        hash ^= hash >>> 16;
        return ((hash & 1023) / 1023.0F) - 0.5F;
    }

    private static float stableCorrelationNoise(double blockX, double blockZ, double scale, int salt) {
        int x = floor(blockX / scale);
        int z = floor(blockZ / scale);
        int hash = salt;
        hash = 31 * hash + x * 374761393;
        hash = 31 * hash + z * 668265263;
        hash = (hash ^ (hash >>> 13)) * 1274126177;
        hash ^= hash >>> 16;
        return (hash & 1023) / 1023.0F;
    }

    private static List<DebrisCluster> debrisClusters(Level level, List<Storm> storms) {
        if (level == null) {
            return List.of();
        }

        int radarTick = (int) (level.getGameTime() & Integer.MAX_VALUE);
        List<DebrisCluster> clusters = new ArrayList<>();
        addSyncedDebrisClusters(level.dimension(), storms, radarTick, clusters);
        if (storms.isEmpty()) {
            return List.copyOf(clusters);
        }

        for (Storm storm : storms) {
            if (storm == null || storm.dead || storm.visualOnly || storm.position == null || !storm.isTornadic() || storm.windspeed < 40) {
                continue;
            }

            float tornadoWidth = Float.isFinite(storm.width) ? Math.max(storm.width, 32.0F) : 32.0F;
            double searchRadius = Math.max(96.0D, tornadoWidth * 3.0D);
            int movingBlocks = 0;
            double movingX = 0.0D;
            double movingZ = 0.0D;
            int particleDebris = 0;
            double particleX = 0.0D;
            double particleZ = 0.0D;
            AABB searchBox = new AABB(
                    storm.position.x - searchRadius,
                    level.getMinBuildHeight(),
                    storm.position.z - searchRadius,
                    storm.position.x + searchRadius,
                    level.getMaxBuildHeight(),
                    storm.position.z + searchRadius
            );

            try {
                for (Entity entity : level.getEntities(null, searchBox)) {
                    if (!(entity instanceof MovingBlock movingBlock) || !entity.isAlive() || !isSignificantDebrisBlock(movingBlock.getBlockState())) {
                        continue;
                    }

                    double dx = entity.getX() - storm.position.x;
                    double dz = entity.getZ() - storm.position.z;
                    if (dx * dx + dz * dz > searchRadius * searchRadius) {
                        continue;
                    }

                    movingBlocks++;
                    movingX += entity.getX();
                    movingZ += entity.getZ();
                }
            } catch (RuntimeException ignored) {
            }

            if (storm.listParticleDebris != null) {
                try {
                    for (EntityRotFX debris : storm.listParticleDebris) {
                        if (debris == null || !debris.isAlive()) {
                            continue;
                        }

                        Vec3 center = debris.getBoundingBoxForRender().getCenter();
                        double dx = center.x - storm.position.x;
                        double dz = center.z - storm.position.z;
                        if (dx * dx + dz * dz > searchRadius * searchRadius) {
                            continue;
                        }

                        particleDebris++;
                        particleX += center.x;
                        particleZ += center.z;
                    }
                } catch (RuntimeException ignored) {
                }
            }

            boolean hasMovingBlockEvidence = movingBlocks >= MIN_DEBRIS_MOVING_BLOCKS;
            boolean hasParticleDebrisEvidence = particleDebris >= MIN_DEBRIS_PARTICLE_CLUSTER_COUNT;
            if ((!hasMovingBlockEvidence && !hasParticleDebrisEvidence) || storm.tornadoOnGroundTicks < MIN_DEBRIS_ON_GROUND_TICKS) {
                continue;
            }

            float movingBlockFactor = hasMovingBlockEvidence
                    ? (float) Math.sqrt(clamp((movingBlocks - MIN_DEBRIS_MOVING_BLOCKS + 1.0F) / 5.0F, 0.0F, 1.0F))
                    : 0.0F;
            float particleFactor = particleDebris < MIN_DEBRIS_PARTICLE_BOOST_COUNT
                    ? 0.0F
                    : (float) Math.sqrt(clamp((particleDebris - MIN_DEBRIS_PARTICLE_BOOST_COUNT + 1.0F) / 40.0F, 0.0F, 1.0F));
            float windFactor = clamp((storm.windspeed - 45.0F) / 100.0F, 0.0F, 1.0F);
            float groundFactor = clamp((storm.tornadoOnGroundTicks - MIN_DEBRIS_ON_GROUND_TICKS) / 70.0F, 0.0F, 1.0F);
            float stormSupport = clamp(0.58F + windFactor * 0.27F + groundFactor * 0.15F, 0.58F, 1.0F);
            float debrisFactor = hasMovingBlockEvidence
                    ? Math.max(movingBlockFactor, movingBlockFactor * 0.70F + particleFactor * 0.30F)
                    : particleFactor * 0.82F;
            float strength = clamp(debrisFactor * stormSupport, 0.0F, 1.0F);
            if (strength < MIN_DEBRIS_CLUSTER_STRENGTH) {
                continue;
            }

            double centerX = storm.position.x;
            double centerZ = storm.position.z;
            if (movingBlocks > 0) {
                centerX = centerX * 0.75D + (movingX / movingBlocks) * 0.25D;
                centerZ = centerZ * 0.75D + (movingZ / movingBlocks) * 0.25D;
            }
            if (particleDebris > 0) {
                centerX = centerX * 0.65D + (particleX / particleDebris) * 0.35D;
                centerZ = centerZ * 0.65D + (particleZ / particleDebris) * 0.35D;
            }

            double radius = Math.max(18.0D, Math.min(tornadoWidth * 1.25D, 24.0D + Math.sqrt(movingBlocks + particleDebris) * 5.5D));
            clusters.add(new DebrisCluster(storm.ID, centerX, centerZ, radius, strength, movingBlocks + particleDebris));
        }

        return List.copyOf(clusters);
    }

    private static void addSyncedDebrisClusters(
            ResourceKey<Level> dimension,
            List<Storm> storms,
            int radarTick,
            List<DebrisCluster> clusters
    ) {
        boolean changed = pruneSyncedDebrisClusters(dimension, radarTick);
        for (SyncedDebrisClusterState syncedCluster : syncedDebrisClusters.values()) {
            if (syncedCluster == null || !syncedCluster.matches(dimension)) {
                continue;
            }

            float strength = syncedCluster.strength(radarTick);
            if (strength < MIN_DEBRIS_CLUSTER_STRENGTH || (hasStormList(storms) && !hasMatchingStorm(storms, syncedCluster.stormId()))) {
                continue;
            }

            clusters.add(new DebrisCluster(
                    syncedCluster.stormId(),
                    syncedCluster.x(),
                    syncedCluster.z(),
                    syncedCluster.radius(),
                    strength,
                    syncedCluster.count()
            ));
        }

        if (changed) {
            chunkCache.clear();
            regionCache.clear();
        }
    }

    private static boolean pruneSyncedDebrisClusters(ResourceKey<Level> dimension, int radarTick) {
        boolean changed = false;
        for (Iterator<Map.Entry<Long, SyncedDebrisClusterState>> iterator = syncedDebrisClusters.entrySet().iterator(); iterator.hasNext(); ) {
            SyncedDebrisClusterState cluster = iterator.next().getValue();
            if (cluster == null || !cluster.matches(dimension) || cluster.expired(radarTick)) {
                iterator.remove();
                changed = true;
            }
        }

        return changed;
    }

    private static boolean hasStormList(List<Storm> storms) {
        return storms != null && !storms.isEmpty();
    }

    private static boolean hasMatchingStorm(List<Storm> storms, long stormId) {
        for (Storm storm : storms) {
            if (storm != null && !storm.dead && storm.ID == stormId) {
                return true;
            }
        }

        return false;
    }

    private static boolean isSignificantDebrisBlock(BlockState blockState) {
        // Client particles exclude additional terrain tags; keep this distinct from server damage.
        return RadarDebrisFilter.isCandidate(blockState)
                && !blockState.is(BlockTags.REPLACEABLE)
                && !blockState.is(BlockTags.BASE_STONE_OVERWORLD)
                && !blockState.is(BlockTags.BASE_STONE_NETHER)
                && !blockState.is(BlockTags.STONE_ORE_REPLACEABLES)
                && !blockState.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                && !blockState.is(Blocks.MYCELIUM)
                && !blockState.is(Blocks.FARMLAND)
                && !blockState.is(Blocks.MOSS_BLOCK)
                && !blockState.is(Blocks.MOSS_CARPET);
    }

    private static float stormReflectivity(RadarState current, Vec3 worldPos) {
        float dbz = 0.0F;
        Vec3 flatWorldPos = worldPos.multiply(1.0D, 0.0D, 1.0D);

        for (Storm storm : current.storms()) {
            if (storm == null || storm.dead || storm.visualOnly || storm.position == null || !storm.hasRadarRepresentation()) {
                continue;
            }

            double distance = storm.position.multiply(1.0D, 0.0D, 1.0D).distanceTo(flatWorldPos);
            if (distance < storm.getRadarRenderRange()) {
                float stormReturn = storm.getRadarReflectivityReturn(current.radarSampler(), worldPos);
                if (Float.isFinite(stormReturn)) {
                    dbz = Math.max(dbz, stormReturn);
                }
            }
        }

        return dbz;
    }

    private static float radialVelocity(RadarState current, RadarSite site, double blockX, double blockZ) {
        Vec3 wind = windAt(current, blockX, blockZ);
        float deltaX = (float) (blockX - site.x());
        float deltaZ = (float) (blockZ - site.z());
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distance < 0.001F) {
            return 0.0F;
        }

        return ((float) wind.x * deltaX + (float) wind.z * deltaZ) / distance;
    }

    private static Vec3 windAt(RadarState current, double blockX, double blockZ) {
        return WindEngine.getWind(
                new Vec3(blockX, current.level().getMaxBuildHeight() + 1.0D, blockZ),
                current.level(),
                false,
                false,
                false,
                true
        );
    }

    private static boolean hasRadarReturns(List<Storm> storms) {
        if (ServerConfig.overcastPercent > 0.01D || ServerConfig.rainStrength > 0.01D) {
            return true;
        }

        for (Storm storm : storms) {
            if (storm != null && !storm.dead && !storm.visualOnly && storm.position != null && storm.hasRadarRepresentation()) {
                return true;
            }
        }

        return false;
    }

    private static RadarSite radarSiteForChunk(RadarState current, ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (!current.matches(dimension)) {
            return null;
        }

        for (RadarSite site : current.radarSites()) {
            if (Math.floorDiv(site.pos().getX(), 16) == chunkX && Math.floorDiv(site.pos().getZ(), 16) == chunkZ) {
                return site;
            }
        }

        return null;
    }

    private static int sideColorOrTransparent(ResourceKey<Level> dimension, int chunkX, int chunkZ, int transparent) {
        RadarReturn radarReturn = radarReturnForChunk(state, dimension, chunkX, chunkZ);
        return radarReturn.visible() ? radarReturn.color() : transparent;
    }

    private static int colorForReflectivity(float dbz) {
        return colorForReflectivity(dbz, 15.0F, true);
    }

    private static int colorForReflectivity(float dbz, float temperature, boolean hasRangeUpgrade) {
        // PMWeather blends weak echoes into terrain. Use coverage over Xaero's terrain
        // instead of baking a dark substitute background into those echoes.
        Color color = ColorTables.getReflectivity(Math.max(dbz, 19.0F), REFLECTIVITY_BASE);
        if (dbz > 5.0F && !hasRangeUpgrade) {
            if (temperature < 3.0F && temperature > -1.0F) {
                color = ColorTables.getMixedReflectivity(dbz);
            } else if (temperature <= -1.0F) {
                color = ColorTables.getSnowReflectivity(dbz);
            }
        }

        int coverage = Math.round(255.0F * clamp((dbz - MIN_VISIBLE_DBZ) / (19.0F - MIN_VISIBLE_DBZ), 0.0F, 1.0F));
        return xaeroColor(color.getRed(), color.getGreen(), color.getBlue(), coverage);
    }

    public static Color colorForCorrelationCoefficient(float coefficient) {
        if (!Float.isFinite(coefficient) || coefficient < CORRELATION_MIN) {
            return Color.BLACK;
        }
        // Use the NOAA levels as gradient stops for both the returns and legend.
        for (int i = 1; i < CORRELATION_THRESHOLDS.length; i++) {
            if (coefficient < CORRELATION_THRESHOLDS[i]) {
                float amount = (coefficient - CORRELATION_THRESHOLDS[i - 1])
                        / (CORRELATION_THRESHOLDS[i] - CORRELATION_THRESHOLDS[i - 1]);
                return ColorTables.lerp(amount, CORRELATION_COLORS[i - 1], CORRELATION_COLORS[i]);
            }
        }
        return CORRELATION_COLORS[CORRELATION_COLORS.length - 1];
    }

    private static int alphaForStrength(float strength) {
        float normalized = clamp((strength - MIN_VISIBLE_DBZ) / (MAX_VISIBLE_DBZ - MIN_VISIBLE_DBZ), 0.0F, 1.0F);
        return Math.round(125.0F + normalized * 130.0F);
    }

    private static int alphaForCorrelationStrength(float strength) {
        float normalized = clamp((strength - MIN_VISIBLE_DBZ) / (MAX_VISIBLE_DBZ - MIN_VISIBLE_DBZ), 0.0F, 1.0F);
        return Math.round(180.0F + normalized * 75.0F);
    }

    private static int hash(
            List<Storm> storms,
            List<DebrisCluster> debrisClusters,
            int radarFrame,
            RadarMode mode,
            List<RadarSite> radarSites,
            boolean hasRadarReturns
    ) {
        int hash = 31;
        hash = 31 * hash + mode.ordinal();
        hash = 31 * hash + radarFrame;
        hash = 31 * hash + Float.floatToIntBits((float) ServerConfig.overcastPercent);
        hash = 31 * hash + Float.floatToIntBits((float) ServerConfig.rainStrength);

        for (RadarSite site : radarSites) {
            hash = 31 * hash + site.pos().hashCode();
            hash = 31 * hash + Double.hashCode(site.x());
            hash = 31 * hash + Double.hashCode(site.y());
            hash = 31 * hash + Double.hashCode(site.z());
            hash = 31 * hash + Double.hashCode(site.radiusBlocks());
            hash = 31 * hash + Boolean.hashCode(site.operational());
            hash = 31 * hash + site.stationCode().hashCode();
        }

        if (radarSites.isEmpty() || !hasRadarReturns) {
            return hash;
        }

        for (Storm storm : storms) {
            if (storm == null || storm.position == null) {
                continue;
            }

            hash = 31 * hash + Long.hashCode(storm.ID);
            hash = 31 * hash + storm.stage;
            hash = 31 * hash + storm.energy;
            hash = 31 * hash + storm.windspeed;
            hash = 31 * hash + Float.floatToIntBits(storm.width);
            hash = 31 * hash + storm.tornadoOnGroundTicks;
            hash = 31 * hash + (storm.listParticleDebris == null ? 0 : storm.listParticleDebris.size());
        }

        for (DebrisCluster debrisCluster : debrisClusters) {
            hash = 31 * hash + Long.hashCode(debrisCluster.stormId());
            hash = 31 * hash + Double.hashCode(debrisCluster.x());
            hash = 31 * hash + Double.hashCode(debrisCluster.z());
            hash = 31 * hash + Double.hashCode(debrisCluster.radius());
            hash = 31 * hash + Float.floatToIntBits(debrisCluster.strength());
            hash = 31 * hash + debrisCluster.count();
        }

        return hash;
    }

    private static RadarBlockEntity radarSampler() {
        if (radarSampler == null) {
            radarSampler = new RadarBlockEntity(BlockPos.ZERO, ModBlocks.RADAR.get().defaultBlockState());
        }

        return radarSampler;
    }

    public static Color colorForVelocity(float velocity) {
        return ColorTables.getVelocity(Float.isFinite(velocity) ? velocity : 0.0F);
    }

    private static int red(int rgb) {
        return (rgb >>> 16) & 0xFF;
    }

    private static int green(int rgb) {
        return (rgb >>> 8) & 0xFF;
    }

    private static int blue(int rgb) {
        return rgb & 0xFF;
    }

    private static void clearRadarSiteCache() {
        cachedRadarSites = List.of();
        rememberedRadarSites.clear();
        cachedRadarSiteLevel = null;
        cachedRadarSiteDimension = null;
        cachedRadarSiteStorageKey = null;
        lastRadarSiteScanTick = Integer.MIN_VALUE;
    }

    private static String radarSiteStorageKey(Minecraft minecraft, ResourceKey<Level> dimension) {
        if (minecraft == null || dimension == null) {
            return null;
        }

        String world = radarWorldStorageKey(minecraft);
        if (world == null || world.isBlank()) {
            return null;
        }

        return sanitizeStorageKey(world + "_" + dimension.location());
    }

    private static String radarWorldStorageKey(Minecraft minecraft) {
        ServerData server = minecraft.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return "server_" + server.ip;
        }

        if (!minecraft.hasSingleplayerServer() || minecraft.getSingleplayerServer() == null) {
            return null;
        }

        try {
            Path worldPath = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
            if (worldPath != null) {
                return "singleplayer_" + pathIdentity(worldPath);
            }
        } catch (RuntimeException ignored) {
        }

        try {
            String levelName = minecraft.getSingleplayerServer().getWorldData().getLevelName();
            if (levelName != null && !levelName.isBlank()) {
                return "singleplayer_" + levelName;
            }
        } catch (RuntimeException ignored) {
        }

        return null;
    }

    private static String pathIdentity(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        String readableName = fileName == null ? "world" : fileName.toString();
        return readableName + "_" + Integer.toHexString(normalized.toString().hashCode());
    }

    private static void ensureClientSettingsLoaded() {
        if (clientSettingsLoaded) {
            return;
        }

        clientSettingsLoaded = true;
        Path path = clientSettingsPath();
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ("display_enabled".equals(key)) {
                    displayEnabled = Boolean.parseBoolean(value);
                } else if ("dual_mode_enabled".equals(key)) {
                    dualModeEnabled = Boolean.parseBoolean(value);
                } else if ("radar_locations_always_visible".equals(key)) {
                    radarLocationsAlwaysVisible = Boolean.parseBoolean(value);
                } else if ("lightning_enabled".equals(key)) {
                    lightningEnabled = Boolean.parseBoolean(value);
                } else if ("station_labels_open".equals(key)) {
                    stationLabelsOpen = Boolean.parseBoolean(value);
                } else if ("menu_animations_enabled".equals(key)) {
                    menuAnimationsEnabled = Boolean.parseBoolean(value);
                } else if ("text_swap_animations_enabled".equals(key)) {
                    textSwapAnimationsEnabled = Boolean.parseBoolean(value);
                } else if ("radar_mode".equals(key)) {
                    radarMode = parseRadarMode(value, radarMode);
                } else if ("dual_upper_mode".equals(key)) {
                    dualUpperMode = parseRadarMode(value, dualUpperMode);
                } else if ("dual_lower_mode".equals(key)) {
                    dualLowerMode = parseRadarMode(value, dualLowerMode);
                } else if ("dual_modes_initialized".equals(key)) {
                    dualModesInitialized = Boolean.parseBoolean(value);
                }
            }
        } catch (IOException ignored) {
        }

        if (!dualModesInitialized) {
            dualUpperMode = radarMode;
            dualLowerMode = radarMode;
        }
    }

    private static void saveClientSettings() {
        Path path = clientSettingsPath();
        if (path == null) {
            return;
        }

        List<String> lines = List.of(
                "display_enabled=" + displayEnabled,
                "dual_mode_enabled=" + dualModeEnabled,
                "radar_mode=" + radarMode.name(),
                "dual_upper_mode=" + dualUpperMode.name(),
                "dual_lower_mode=" + dualLowerMode.name(),
                "dual_modes_initialized=" + dualModesInitialized,
                "radar_locations_always_visible=" + radarLocationsAlwaysVisible,
                "lightning_enabled=" + lightningEnabled,
                "station_labels_open=" + stationLabelsOpen,
                "menu_animations_enabled=" + menuAnimationsEnabled,
                "text_swap_animations_enabled=" + textSwapAnimationsEnabled
        );

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines);
        } catch (IOException ignored) {
        }
    }

    private static RadarMode parseRadarMode(String value, RadarMode fallback) {
        try {
            return RadarMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static Path clientSettingsPath() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameDirectory == null) {
            return null;
        }

        return minecraft.gameDirectory.toPath()
                .resolve("config")
                .resolve("pmradar")
                .resolve("client_settings.txt");
    }

    private static void loadRememberedRadarSites() {
        Path path = radarSiteStoragePath();
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(path)) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    BlockPos pos = new BlockPos(
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim())
                    );
                    rememberedRadarSites.put(pos, radarSiteForTower(pos, true));
                    continue;
                }

                if (parts.length != 4 && parts.length != 5 && parts.length != 6) {
                    continue;
                }

                String type = parts[0].trim();
                BlockPos pos = new BlockPos(
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim())
                );

                if ("selected".equals(type)) {
                    selectedRadarSitePos = pos.immutable();
                } else if ("site".equals(type)) {
                    boolean operational = parts.length < 5 || Boolean.parseBoolean(parts[4].trim());
                    String stationCode = parts.length >= 6 ? parts[5].trim() : null;
                    rememberedRadarSites.put(pos, radarSiteForTower(pos, operational, stationCode));
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    private static void saveRememberedRadarSites() {
        Path path = radarSiteStoragePath();
        if (path == null) {
            return;
        }

        List<RadarSite> sites = sortedRememberedRadarSites();
        List<String> lines = new ArrayList<>(sites.size() + 1);
        BlockPos selected = selectedRadarSitePos;
        if (selected != null) {
            lines.add("selected," + selected.getX() + "," + selected.getY() + "," + selected.getZ());
        }

        for (RadarSite site : sites) {
            BlockPos pos = site.pos();
            lines.add("site," + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + site.operational() + "," + site.stationCode());
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines);
        } catch (IOException ignored) {
        }
    }

    private static void loadLightningMarkers(ResourceKey<Level> dimension, int radarTick) {
        Path path = lightningStoragePath();
        if (path == null || !Files.isRegularFile(path)) {
            return;
        }

        boolean droppedExpired = false;
        try {
            for (String rawLine : Files.readAllLines(path)) {
                String[] parts = rawLine.split(",");
                if (parts.length != 5) {
                    continue;
                }

                long seed = Long.parseLong(parts[0].trim());
                double x = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                float strength = Float.parseFloat(parts[3].trim());
                long savedLifetime = Long.parseLong(parts[4].trim());
                int remainingTicks = lightningRemainingTicks(savedLifetime);
                if (remainingTicks <= 0) {
                    droppedExpired = true;
                    continue;
                }

                int ageTicks = LIGHTNING_MARKER_LIFETIME_TICKS - remainingTicks;
                lightningMarkers.put(seed, new LightningMarker(
                        dimension,
                        x,
                        z,
                        strength,
                        radarTick - ageTicks
                ));
            }
        } catch (IOException | NumberFormatException ignored) {
        }

        if (droppedExpired) {
            saveLightningMarkers(radarTick);
        }
    }

    private static void saveLightningMarkers() {
        int radarTick = lastRadarTick;
        RadarState current = state;
        if (!current.isEmpty() && current.level() != null) {
            radarTick = (int) (current.level().getGameTime() & Integer.MAX_VALUE);
        }

        if (radarTick == Integer.MIN_VALUE) {
            return;
        }

        saveLightningMarkers(radarTick);
    }

    private static void saveLightningMarkers(int radarTick) {
        Path path = lightningStoragePath();
        if (path == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        ResourceKey<Level> dimension = cachedRadarSiteDimension;
        for (Map.Entry<Long, LightningMarker> entry : lightningMarkers.entrySet()) {
            LightningMarker marker = entry.getValue();
            if (marker == null) {
                continue;
            }

            if (dimension != null && !marker.matches(dimension)) {
                continue;
            }

            int remainingTicks = marker.remainingTicks(radarTick);
            if (remainingTicks <= 0) {
                continue;
            }

            lines.add(entry.getKey()
                    + "," + marker.x()
                    + "," + marker.z()
                    + "," + marker.strength()
                    + "," + remainingTicks);
        }

        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines);
        } catch (IOException ignored) {
        }
    }

    private static int lightningRemainingTicks(long savedLifetime) {
        if (savedLifetime > LIGHTNING_MARKER_LIFETIME_TICKS * 2L) {
            long ageMillis = Math.max(0L, System.currentTimeMillis() - savedLifetime);
            long remainingMillis = LIGHTNING_MARKER_LIFETIME_MILLIS - ageMillis;
            return (int) Math.min(LIGHTNING_MARKER_LIFETIME_TICKS, Math.max(0L, remainingMillis / 50L));
        }

        return (int) Math.min(LIGHTNING_MARKER_LIFETIME_TICKS, Math.max(0L, savedLifetime));
    }

    private static List<RadarSite> sortedRememberedRadarSites() {
        List<RadarSite> sites = new ArrayList<>(rememberedRadarSites.values());
        sites.sort((first, second) -> {
            int compareX = Integer.compare(first.pos().getX(), second.pos().getX());
            if (compareX != 0) {
                return compareX;
            }

            int compareY = Integer.compare(first.pos().getY(), second.pos().getY());
            if (compareY != 0) {
                return compareY;
            }

            return Integer.compare(first.pos().getZ(), second.pos().getZ());
        });
        return List.copyOf(sites);
    }

    private static Path radarSiteStoragePath() {
        String storageKey = cachedRadarSiteStorageKey;
        if (storageKey == null || storageKey.isEmpty()) {
            return null;
        }

        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("pmradar")
                .resolve("radar_sites")
                .resolve(storageKey + ".txt");
    }

    private static Path lightningStoragePath() {
        String storageKey = cachedRadarSiteStorageKey;
        if (storageKey == null || storageKey.isEmpty()) {
            return null;
        }

        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("pmradar")
                .resolve("lightning_markers")
                .resolve(storageKey + ".txt");
    }

    private static String sanitizeStorageKey(String key) {
        StringBuilder builder = new StringBuilder(key.length());
        String lower = key.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char character = lower.charAt(i);
            if (character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '-'
                    || character == '_'
                    || character == '.') {
                builder.append(character);
            } else {
                builder.append('_');
            }
        }

        return builder.isEmpty() ? "unknown" : builder.toString();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static long chunkKey(BlockPos pos) {
        return chunkKey(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static int xaeroColor(int red, int green, int blue, int alpha) {
        return (blue << 24) | (green << 16) | (red << 8) | alpha;
    }

    private static int xaeroToArgb(int color) {
        int alpha = color & 0xFF;
        int red = (color >>> 8) & 0xFF;
        int green = (color >>> 16) & 0xFF;
        int blue = (color >>> 24) & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public record RadarSiteView(
            BlockPos pos,
            double x,
            double z,
            double radiusBlocks,
            String stationCode,
            String displayName,
            boolean operational,
            boolean selected
    ) {
    }

    public record StormView(double x, double z, float width, int stage, int windspeed, boolean tornadic) {
    }

    public record LightningStrikeView(double x, double z, float strength, float alpha) {
    }

    public record SyncedRadarSite(
            BlockPos pos,
            boolean visible,
            boolean operational,
            boolean rangeUpgraded,
            String stationCode
    ) {
    }

    public record SyncedDebrisCluster(long stormId, double x, double z, double radius, float strength, int count) {
    }

    public enum RadarMode {
        REFLECTIVITY("Reflectivity", "dBZ"),
        VELOCITY("Velocity", "mph"),
        CORRELATION_COEFFICIENT("Correlation Coefficient", "%");

        private final String displayName;
        private final String unit;

        RadarMode(String displayName, String unit) {
            this.displayName = displayName;
            this.unit = unit;
        }

        public String displayName() {
            return displayName;
        }

        private String tooltip(int amount) {
            if (this == CORRELATION_COEFFICIENT) {
                return String.format(Locale.ROOT, "PMWeather %s - %.3f", displayName, amount / 1000.0F);
            }

            return "PMWeather " + displayName + " - " + amount + " " + unit;
        }
    }

    private record RadarState(
            Level level,
            ResourceKey<Level> dimension,
            WeatherHandlerClient weather,
            List<Storm> storms,
            List<DebrisCluster> debrisClusters,
            boolean hasRadarReturns,
            RadarBlockEntity radarSampler,
            List<RadarSite> radarSites,
            int radarFrame,
            RadarMode mode,
            int hash
    ) {
        private static RadarState empty() {
            return new RadarState(null, null, null, List.of(), List.of(), false, null, List.of(), 0, RadarMode.REFLECTIVITY, 0);
        }

        private boolean matches(ResourceKey<Level> checkedDimension) {
            return level != null && dimension != null && dimension.equals(checkedDimension) && weather != null && radarSampler != null;
        }

        private boolean sameDimension(RadarState other) {
            return dimension == null ? other.dimension == null : dimension.equals(other.dimension);
        }

        private boolean isEmpty() {
            return level == null;
        }

        private RadarState withMode(RadarMode nextMode) {
            return new RadarState(
                    level,
                    dimension,
                    weather,
                    storms,
                    debrisClusters,
                    hasRadarReturns,
                    radarSampler,
                    radarSites,
                    radarFrame,
                    nextMode,
                    StormOverlayData.hash(storms, debrisClusters, radarFrame, nextMode, radarSites, hasRadarReturns)
            );
        }

        private RadarState withRadarSites(List<RadarSite> nextRadarSites) {
            return new RadarState(
                    level,
                    dimension,
                    weather,
                    storms,
                    debrisClusters,
                    hasRadarReturns,
                    radarSampler,
                    nextRadarSites,
                    radarFrame,
                    mode,
                    StormOverlayData.hash(storms, debrisClusters, radarFrame, mode, nextRadarSites, hasRadarReturns)
            );
        }
    }

    private record DebrisCluster(long stormId, double x, double z, double radius, float strength, int count) {
    }

    private record SyncedDebrisClusterState(
            ResourceKey<Level> dimension,
            long stormId,
            double x,
            double z,
            double radius,
            float strength,
            int count,
            int expiresAtTick
    ) {
        private boolean matches(ResourceKey<Level> checkedDimension) {
            return dimension != null && dimension.equals(checkedDimension);
        }

        private boolean expired(int radarTick) {
            return radarTick >= expiresAtTick;
        }

        private float strength(int radarTick) {
            int remaining = Math.max(0, expiresAtTick - radarTick);
            float fade = clamp(remaining / 70.0F, 0.0F, 1.0F);
            return strength * (0.45F + fade * 0.55F);
        }
    }

    private record RadarSite(BlockPos pos, double x, double y, double z, double radiusBlocks, boolean operational, String stationCode) {
        private static RadarSite tower(BlockPos pos, boolean operational, String stationCode) {
            return tower(pos, pos.getCenter(), BASE_RADAR_RADIUS_BLOCKS, operational, stationCode);
        }

        private static RadarSite tower(BlockPos pos, Vec3 center, double radiusBlocks, boolean operational, String stationCode) {
            return new RadarSite(pos.immutable(), center.x, center.y, center.z, radiusBlocks, operational, stationCode);
        }

        private double radiusSqr() {
            return radiusBlocks * radiusBlocks;
        }

        private double distanceSqr(double blockX, double blockZ) {
            double dx = blockX - x;
            double dz = blockZ - z;
            return dx * dx + dz * dz;
        }

        private String displayName() {
            return stationCode;
        }

        private int markerColor() {
            return operational ? RADAR_SITE_MARKER_COLOR : BROKEN_RADAR_SITE_MARKER_COLOR;
        }

        private RadarSiteView view(boolean selected) {
            return new RadarSiteView(pos, x, z, radiusBlocks, stationCode(), displayName(), operational, selected);
        }
    }

    private record RadarReturn(int color, float amount, float strength) {
        private static RadarReturn empty() {
            return new RadarReturn(0, 0.0F, 0.0F);
        }

        private boolean visible() {
            return color != 0;
        }
    }

    private record LightningMarker(
            ResourceKey<Level> dimension,
            double x,
            double z,
            float strength,
            int createdTick
    ) {
        private boolean matches(ResourceKey<Level> checkedDimension) {
            return dimension != null && dimension.equals(checkedDimension);
        }

        private int ageTicks(int radarTick) {
            return radarTick - createdTick;
        }

        private int remainingTicks(int radarTick) {
            return LIGHTNING_MARKER_LIFETIME_TICKS - ageTicks(radarTick);
        }
    }

    private record ChunkKey(int stateHash, ResourceKey<Level> dimension, int chunkX, int chunkZ) {
    }

    private record RegionKey(int stateHash, ResourceKey<Level> dimension, int regionX, int regionZ) {
    }
}
