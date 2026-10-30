package com.example.bodytrade.mixin;

import com.example.bodytrade.client.ClientHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Потеря глаза: чёрная половина экрана рисуется ДО всего интерфейса, чтобы не закрывать панель предметов и сердца. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void bodytrade$eyeBlackout(DrawContext context, float tickDelta, CallbackInfo ci) {
        ClientHud.blackout(context);
    }
}
