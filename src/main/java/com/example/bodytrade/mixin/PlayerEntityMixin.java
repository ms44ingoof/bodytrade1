package com.example.bodytrade.mixin;

import com.example.bodytrade.BodyTradeMod;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Без обеих ног персонаж лежит и может только ползти. */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "updatePose", at = @At("TAIL"), require = 0)
    private void bodytrade$crawl(CallbackInfo ci) {
        PlayerEntity p = (PlayerEntity) (Object) this;
        if (BodyTradeMod.isCrawler(p)) p.setPose(EntityPose.SWIMMING);
    }
}
