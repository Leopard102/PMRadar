package com.leopard.pmradar.client;

import com.leopard.pmradar.PMRadar;
import com.leopard.pmradar.client.highlight.MinimapStormHighlighter;
import com.leopard.pmradar.client.highlight.WorldMapStormHighlighter;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.common.minimap.write.MinimapWriter;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public final class XaeroStormOverlayRegistrar {
    private static final MinimapStormHighlighter MINIMAP_HIGHLIGHTER = new MinimapStormHighlighter();
    private static final WorldMapStormHighlighter WORLD_MAP_HIGHLIGHTER = new WorldMapStormHighlighter();
    private static final int WORLD_MAP_REFRESH_BATCH_SIZE = 128;

    private static Field minimapRegistryField;
    private static Field minimapHighlightersField;
    private static Field worldMapHighlightersField;
    private static final Queue<MapRegion> worldMapRefreshQueue = new ArrayDeque<>();
    private static MinimapWriter registeredMinimapWriter;
    private static xaero.map.highlight.HighlighterRegistry registeredWorldMapRegistry;
    private static boolean warnedMinimapRegistration;
    private static boolean warnedWorldMapRegistration;
    private static boolean warnedWorldMapRefresh;

    private XaeroStormOverlayRegistrar() {
    }

    public static void tick(boolean refreshMinimapHighlights, boolean refreshWorldMapHighlights) {
    }

    public static void reset() {
        registeredMinimapWriter = null;
        registeredWorldMapRegistry = null;
        worldMapRefreshQueue.clear();
    }

    private static void registerMinimap() {
        try {
            XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
            if (session == null) {
                registeredMinimapWriter = null;
                return;
            }

            MinimapProcessor processor = session.getMinimapProcessor();
            if (processor == null) {
                return;
            }

            MinimapWriter writer = processor.getMinimapWriter();
            if (writer == null || writer == registeredMinimapWriter) {
                return;
            }

            Object registryObject = minimapRegistryField().get(writer);
            if (registryObject instanceof xaero.common.minimap.highlight.HighlighterRegistry registry) {
                ensureMinimapHighlighter(registry);

                registeredMinimapWriter = writer;
                requestMinimapRefresh(writer);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnMinimapRegistration(exception);
        }
    }

    private static void registerWorldMap() {
        try {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session == null) {
                registeredWorldMapRegistry = null;
                return;
            }

            MapProcessor processor = session.getMapProcessor();
            if (processor == null) {
                return;
            }

            xaero.map.highlight.HighlighterRegistry registry = processor.getHighlighterRegistry();
            if (registry == null || registry == registeredWorldMapRegistry) {
                return;
            }

            ensureWorldMapHighlighter(registry);

            registeredWorldMapRegistry = registry;
            refreshWorldMap(processor);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnWorldMapRegistration(exception);
        }
    }

    private static void refreshMinimap() {
        XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
        if (session == null || session.getMinimapProcessor() == null) {
            return;
        }

        MinimapWriter writer = session.getMinimapProcessor().getMinimapWriter();
        if (writer != null) {
            requestMinimapRefresh(writer);
        }
    }

    private static void requestMinimapRefresh(MinimapWriter writer) {
        DimensionHighlighterHandler handler = writer.getDimensionHighlightHandler();
        if (handler != null) {
            handler.requestRefresh();
        }
    }

    private static void refreshWorldMap() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return;
        }

        refreshWorldMap(session.getMapProcessor());
    }

    private static void refreshWorldMap(MapProcessor processor) {
        try {
            if (processor == null) {
                return;
            }

            MapWorld mapWorld = processor.getMapWorld();
            if (mapWorld == null) {
                return;
            }

            mapWorld.clearAllCachedHighlightHashes();
            queueWorldMapRefreshes(processor, mapWorld);
        } catch (RuntimeException exception) {
            if (!warnedWorldMapRefresh) {
                PMRadar.LOGGER.warn("Could not refresh PMWeather storm highlights in Xaero's World Map.", exception);
                warnedWorldMapRefresh = true;
            }
        }
    }

    private static void queueWorldMapRefreshes(MapProcessor processor, MapWorld mapWorld) {
        MapDimension dimension = mapWorld.getCurrentDimension();
        if (dimension == null) {
            worldMapRefreshQueue.clear();
            return;
        }

        if (!worldMapRefreshQueue.isEmpty() && StormOverlayData.isDisplayEnabled()) {
            return;
        }

        worldMapRefreshQueue.clear();
        List<LeveledRegion<?>> loadedRegions = new ArrayList<>(dimension.getLayeredMapRegions().getLoadedListUnsynced());
        for (LeveledRegion<?> region : loadedRegions) {
            if (region instanceof MapRegion mapRegion) {
                worldMapRefreshQueue.offer(mapRegion);
            }
        }

        processQueuedWorldMapRefreshes(processor);
    }

    private static void processQueuedWorldMapRefreshes() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            worldMapRefreshQueue.clear();
            return;
        }

        processQueuedWorldMapRefreshes(session.getMapProcessor());
    }

    private static void processQueuedWorldMapRefreshes(MapProcessor processor) {
        try {
            if (processor == null || worldMapRefreshQueue.isEmpty()) {
                return;
            }

            int refreshed = 0;
            while (refreshed < WORLD_MAP_REFRESH_BATCH_SIZE && !worldMapRefreshQueue.isEmpty()) {
                MapRegion mapRegion = worldMapRefreshQueue.poll();
                if (mapRegion != null) {
                    mapRegion.requestRefresh(processor, false);
                    refreshed++;
                }
            }
        } catch (RuntimeException exception) {
            worldMapRefreshQueue.clear();
            if (!warnedWorldMapRefresh) {
                PMRadar.LOGGER.warn("Could not refresh PMWeather storm highlights in Xaero's World Map.", exception);
                warnedWorldMapRefresh = true;
            }
        }
    }

    private static Field minimapRegistryField() throws NoSuchFieldException {
        if (minimapRegistryField == null) {
            minimapRegistryField = MinimapWriter.class.getDeclaredField("highlighterRegistry");
            minimapRegistryField.setAccessible(true);
        }

        return minimapRegistryField;
    }

    private static boolean ensureMinimapHighlighter(
            xaero.common.minimap.highlight.HighlighterRegistry registry
    ) throws ReflectiveOperationException {
        List<xaero.common.minimap.highlight.AbstractHighlighter> highlighters = registry.getHighlighters();
        if (highlighters.contains(MINIMAP_HIGHLIGHTER)) {
            return false;
        }

        try {
            registry.register(MINIMAP_HIGHLIGHTER);
        } catch (UnsupportedOperationException exception) {
            List<xaero.common.minimap.highlight.AbstractHighlighter> replacement = new ArrayList<>(highlighters);
            replacement.add(MINIMAP_HIGHLIGHTER);
            minimapHighlightersField().set(registry, Collections.unmodifiableList(replacement));
        }

        return true;
    }

    private static boolean ensureWorldMapHighlighter(
            xaero.map.highlight.HighlighterRegistry registry
    ) throws ReflectiveOperationException {
        List<xaero.map.highlight.AbstractHighlighter> highlighters = registry.getHighlighters();
        if (highlighters.contains(WORLD_MAP_HIGHLIGHTER)) {
            return false;
        }

        try {
            registry.register(WORLD_MAP_HIGHLIGHTER);
        } catch (UnsupportedOperationException exception) {
            List<xaero.map.highlight.AbstractHighlighter> replacement = new ArrayList<>(highlighters);
            replacement.add(WORLD_MAP_HIGHLIGHTER);
            worldMapHighlightersField().set(registry, Collections.unmodifiableList(replacement));
        }

        return true;
    }

    private static Field minimapHighlightersField() throws NoSuchFieldException {
        if (minimapHighlightersField == null) {
            minimapHighlightersField = xaero.common.minimap.highlight.HighlighterRegistry.class.getDeclaredField("highlighters");
            minimapHighlightersField.setAccessible(true);
        }

        return minimapHighlightersField;
    }

    private static Field worldMapHighlightersField() throws NoSuchFieldException {
        if (worldMapHighlightersField == null) {
            worldMapHighlightersField = xaero.map.highlight.HighlighterRegistry.class.getDeclaredField("highlighters");
            worldMapHighlightersField.setAccessible(true);
        }

        return worldMapHighlightersField;
    }

    private static void warnMinimapRegistration(Exception exception) {
        if (!warnedMinimapRegistration) {
            PMRadar.LOGGER.warn("Could not register PMWeather storm highlights with Xaero's Minimap.", exception);
            warnedMinimapRegistration = true;
        }
    }

    private static void warnWorldMapRegistration(Exception exception) {
        if (!warnedWorldMapRegistration) {
            PMRadar.LOGGER.warn("Could not register PMWeather storm highlights with Xaero's World Map.", exception);
            warnedWorldMapRegistration = true;
        }
    }
}
