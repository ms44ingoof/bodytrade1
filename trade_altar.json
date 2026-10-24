package com.example.bodytrade.mixin;

import com.example.bodytrade.client.BodyTradeClient;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Прячет руки и ноги на модели игрока, если они отданы (в том числе от первого лица). */
@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void bodytrade$hideLimbs(LivingEntity entity, float f1, float f2, float f3, float f4, float f5, CallbackInfo ci) {
        Integer mask = BodyTradeClient.LIMBS.get(entity.getUuid());
        if (mask == null || mask == 0) return;
        PlayerEntityModel<?> m = (PlayerEntityModel<?>) (Object) this;
        if ((mask & 1) != 0) { m.leftArm.visible = false; m.leftSleeve.visible = false; }
        if ((mask & 2) != 0) { m.rightArm.visible = false; m.rightSleeve.visible = false; }
        if ((mask & 4) != 0) { m.leftLeg.visible = false; m.leftPants.visible = false; }
        if ((mask & 8) != 0) { m.rightLeg.visible = false; m.rightPants.visible = false; }
    }
}
