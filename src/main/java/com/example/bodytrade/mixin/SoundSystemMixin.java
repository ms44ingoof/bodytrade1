package com.example.bodytrade.mixin;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.client.BodyTradeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Глушит звуки со стороны потерянного уха (оба уха - глухота). */
@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void bodytrade$deafen(SoundInstance s, CallbackInfo ci) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || s.isRelative()) return;
        boolean l = BodyTradeClient.MISSING.contains(BodyPart.LEFT_EAR);
        boolean r = BodyTradeClient.MISSING.contains(BodyPart.RIGHT_EAR);
        if (!l && !r) return;
        if (l && r) { ci.cancel(); return; }
        double dx = s.getX() - p.getX(), dz = s.getZ() - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5) return;
        float yaw = p.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        double side = (dx * MathHelper.cos(yaw) + dz * MathHelper.sin(yaw)) / dist; // >0 - слева
        if ((l && side > 0.25) || (r && side < -0.25)) ci.cancel();
    }
}
