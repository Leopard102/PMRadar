package com.leopard.pmradar.mixin;

import com.leopard.pmradar.server.DistantTornadoChunkLoading;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.storms.Supercell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Supercell.class, remap = false)
public abstract class SupercellDistantChunkLoadingMixin {
    @Inject(method = "shouldLoadChunks", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void pmradar$allowDistantTornadoChunkLoading(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (Boolean.TRUE.equals(callbackInfo.getReturnValue())) {
            return;
        }

        if (DistantTornadoChunkLoading.shouldKeepLoaded((Storm) (Object) this)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
