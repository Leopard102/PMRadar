package com.leopard.pmradar.client;

import com.leopard.pmradar.PMRadar;
import com.leopard.pmradar.network.RadarSitesPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = PMRadar.MODID, value = Dist.CLIENT)
public final class PMRadarClient {
    // Sample PMWeather's mutable storm state every client tick so the map keeps pace with
    // the radar block's continuously rendered view.
    private static final long RADAR_UPDATE_INTERVAL_TICKS = 1L;

    private static long ticks;
    private static boolean siteHintsSent;

    private PMRadarClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            clearClientState();
            return;
        }

        long tick = ticks++;
        if (tick % RADAR_UPDATE_INTERVAL_TICKS == 0L) {
            StormOverlayData.update(minecraft);
            if (!siteHintsSent && minecraft.player != null) {
                RadarSitesPayload hints = StormOverlayData.rememberedRadarSiteHints();
                if (hints != null) {
                    PacketDistributor.sendToServer(hints);
                    siteHintsSent = true;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        WorldMapRadarLegendOverlay.render(event);
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        WorldMapRadarLegendOverlay.handleScroll(event);
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        WorldMapRadarLegendOverlay.handleMouseClick(event);
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        WorldMapRadarLegendOverlay.handleMouseDrag(event);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        WorldMapRadarLegendOverlay.handleMouseRelease(event);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        StormOverlayData.prepareForDisconnect();
        clearClientState();
    }

    private static void clearClientState() {
        siteHintsSent = false;
        StormOverlayData.clear();
        WorldMapRadarLegendOverlay.reset();
    }
}
