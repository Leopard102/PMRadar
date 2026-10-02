package com.leopard.pmradar.client;

import com.leopard.pmradar.PMRadar;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = PMRadar.MODID, value = Dist.CLIENT)
public final class PMRadarClient {
    private static final long RADAR_UPDATE_INTERVAL_TICKS = 20L;

    private static long ticks;

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
        StormOverlayData.clear();
        WorldMapRadarLegendOverlay.reset();
    }
}
