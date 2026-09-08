package com.leopard.pmradar.client;

import com.leopard.pmradar.network.RadarDebrisPayload;
import com.leopard.pmradar.network.RadarSitesPayload;

import java.util.ArrayList;
import java.util.List;

public final class ClientRadarSitesHandler {
    private ClientRadarSitesHandler() {
    }

    public static void handle(RadarSitesPayload payload) {
        List<StormOverlayData.SyncedRadarSite> sites = new ArrayList<>(payload.entries().size());
        for (RadarSitesPayload.Entry entry : payload.entries()) {
            sites.add(new StormOverlayData.SyncedRadarSite(entry.pos(), entry.visible(), entry.operational()));
        }

        if (StormOverlayData.applySyncedRadarSites(payload.dimension(), sites)) {
            WorldMapRadarLegendOverlay.clearRadarTextures();
        }
    }

    public static void handle(RadarDebrisPayload payload) {
        List<StormOverlayData.SyncedDebrisCluster> clusters = new ArrayList<>(payload.entries().size());
        for (RadarDebrisPayload.Entry entry : payload.entries()) {
            clusters.add(new StormOverlayData.SyncedDebrisCluster(
                    entry.stormId(),
                    entry.x(),
                    entry.z(),
                    entry.radius(),
                    entry.strength(),
                    entry.count()
            ));
        }

        if (StormOverlayData.applySyncedDebrisClusters(payload.dimension(), clusters)) {
            WorldMapRadarLegendOverlay.clearRadarTextures();
        }
    }
}
