package com.example.bodytrade.mixin;

import com.example.bodytrade.client.BodyTradeClient;
import com.example.bodytrade.client.ProsopagnosiaFeature;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Прозопагнозия: на время отрисовки животного прячем его настоящую голову (вместо неё слой рисует голову игрока). */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique private final List<ModelPart> bodytrade$hidden = new ArrayList<>();

    private static final String RENDER = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void bodytrade$hideHead(LivingEntity entity, float yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices,
                                    net.minecraft.client.render.VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        bodytrade$hidden.clear();
        if (!(entity instanceof AnimalEntity) || entity.isInvisible() || !BodyTradeClient.prosopagnosiaActive()) return;
        var model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
        if (!(model instanceof AnimalModel<?> am) || ProsopagnosiaFeature.headPart(am) == null) return;
        for (ModelPart part : ((AnimalModelAccessor) am).bodytrade$headParts()) {
            if (part.visible) { part.visible = false; bodytrade$hidden.add(part); }
        }
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void bodytrade$restoreHead(LivingEntity entity, float yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices,
                                       net.minecraft.client.render.VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        for (ModelPart part : bodytrade$hidden) part.visible = true;
        bodytrade$hidden.clear();
    }
}
