package com.leopard.pmradar.server;

import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.weather.Storm;
import net.minecraft.server.level.ServerLevel;

public final class DistantTornadoChunkLoading {
    private DistantTornadoChunkLoading() {
    }

    public static boolean shouldKeepLoaded(Storm storm) {
        if (storm == null
                || storm.level == null
                || storm.level.isClientSide()
                || !(storm.level instanceof ServerLevel)
                || storm.dead
                || storm.visualOnly
                || storm.position == null
                || !ServerConfig.doDamage
                || storm.stage < 3
                || storm.windspeed < 40) {
            return false;
        }

        try {
            return storm.isTornadic();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
