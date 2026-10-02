package com.leopard.pmradar.network;

import com.leopard.pmradar.PMRadar;
import com.leopard.pmradar.server.RadarTowerSync;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = PMRadar.MODID)
public final class PMRadarNetworking {
    private PMRadarNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                RadarSitesPayload.TYPE,
                RadarSitesPayload.STREAM_CODEC,
                (payload, context) -> handleClientbound(payload)
        );
        event.registrar("1").playToClient(
                RadarDebrisPayload.TYPE,
                RadarDebrisPayload.STREAM_CODEC,
                (payload, context) -> handleClientbound(payload)
        );
        event.registrar("1").playToServer(
                RadarSitesPayload.TYPE,
                RadarSitesPayload.STREAM_CODEC,
                PMRadarNetworking::handleServerbound
        );
    }

    private static void handleClientbound(RadarSitesPayload payload) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        try {
            Class<?> handler = Class.forName("com.leopard.pmradar.client.ClientRadarSitesHandler");
            handler.getMethod("handle", RadarSitesPayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            PMRadar.LOGGER.warn("Failed to apply synced radar sites", exception);
        }
    }

    private static void handleClientbound(RadarDebrisPayload payload) {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        try {
            Class<?> handler = Class.forName("com.leopard.pmradar.client.ClientRadarSitesHandler");
            handler.getMethod("handle", RadarDebrisPayload.class).invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            PMRadar.LOGGER.warn("Failed to apply synced radar debris", exception);
        }
    }

    private static void handleServerbound(RadarSitesPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        context.enqueueWork(() -> RadarTowerSync.acceptClientHints(player, payload));
    }
}
