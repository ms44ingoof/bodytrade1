package com.example.bodytrade.mixin;

import com.example.bodytrade.client.BodyTradeClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Прячет отданные руки и ноги; при потере ступни показывает укороченную ногу. */
@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {
    @Unique private boolean bodytrade$footL, bodytrade$footR;
    @Unique private static ModelPart[] bodytrade$stumps;

    @Unique
    private static ModelPart[] bodytrade$stumps() {
        if (bodytrade$stumps == null) {
            ModelData md = new ModelData();
            ModelPartData r = md.getRoot();
            Dilation d = new Dilation(0.25f);
            r.addChild("l", ModelPartBuilder.create().uv(16, 48).cuboid(-2f, 0f, -2f, 4f, 9f, 4f), ModelTransform.NONE);
            r.addChild("r", ModelPartBuilder.create().uv(0, 16).cuboid(-2f, 0f, -2f, 4f, 9f, 4f), ModelTransform.NONE);
            r.addChild("lp", ModelPartBuilder.create().uv(0, 48).cuboid(-2f, 0f, -2f, 4f, 9f, 4f, d), ModelTransform.NONE);
            r.addChild("rp", ModelPartBuilder.create().uv(0, 32).cuboid(-2f, 0f, -2f, 4f, 9f, 4f, d), ModelTransform.NONE);
            ModelPart root = TexturedModelData.of(md, 64, 64).createModel();
            bodytrade$stumps = new ModelPart[]{root.getChild("l"), root.getChild("r"), root.getChild("lp"), root.getChild("rp")};
        }
        return bodytrade$stumps;
    }

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void bodytrade$hideLimbs(LivingEntity entity, float f1, float f2, float f3, float f4, float f5, CallbackInfo ci) {
        bodytrade$footL = false;
        bodytrade$footR = false;
        Integer mask = BodyTradeClient.LIMBS.get(entity.getUuid());
        if (mask == null || mask == 0) return;
        PlayerEntityModel<?> m = (PlayerEntityModel<?>) (Object) this;
        if ((mask & 1) != 0) { m.leftArm.visible = false; m.leftSleeve.visible = false; }
        if ((mask & 2) != 0) { m.rightArm.visible = false; m.rightSleeve.visible = false; }
        if ((mask & 4) != 0) { m.leftLeg.visible = false; m.leftPants.visible = false; }
        if ((mask & 8) != 0) { m.rightLeg.visible = false; m.rightPants.visible = false; }
        bodytrade$footL = (mask & 16) != 0 && (mask & 4) == 0;
        bodytrade$footR = (mask & 32) != 0 && (mask & 8) == 0;
    }

    @Inject(method = "getBodyParts", at = @At("RETURN"), cancellable = true, require = 0)
    private void bodytrade$stumpLegs(CallbackInfoReturnable<Iterable<ModelPart>> cir) {
        if (!bodytrade$footL && !bodytrade$footR) return;
        PlayerEntityModel<?> m = (PlayerEntityModel<?>) (Object) this;
        ModelPart[] s = bodytrade$stumps();
        List<ModelPart> out = new ArrayList<>();
        for (ModelPart p : cir.getReturnValue()) {
            if (bodytrade$footL && p == m.leftLeg) { s[0].copyTransform(p); out.add(s[0]); }
            else if (bodytrade$footR && p == m.rightLeg) { s[1].copyTransform(p); out.add(s[1]); }
            else if (bodytrade$footL && p == m.leftPants) { s[2].copyTransform(p); out.add(s[2]); }
            else if (bodytrade$footR && p == m.rightPants) { s[3].copyTransform(p); out.add(s[3]); }
            else out.add(p);
        }
        cir.setReturnValue(out);
    }
}
