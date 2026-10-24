package com.example.bodytrade.mixin;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Потеря полушария мозга: реверсное управление. */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void bodytrade$reverse(boolean slowDown, float f, CallbackInfo ci) {
        KeyboardInput in = (KeyboardInput) (Object) this;
        if (BodyTradeMod.CLIENT_MISSING.contains(BodyPart.LEFT_HEMISPHERE)) in.movementSideways = -in.movementSideways;
        if (BodyTradeMod.CLIENT_MISSING.contains(BodyPart.RIGHT_HEMISPHERE)) in.movementForward = -in.movementForward;
    }
}
