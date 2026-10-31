package com.example.bodytrade.mixin;

import com.example.bodytrade.client.BodyTradeClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Синдром Капгра: имена над другими игроками подменяются на чужие. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow protected abstract void renderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vcp, int light);

    @Unique private boolean bodytrade$busy;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void bodytrade$capgras(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        if (bodytrade$busy) return;
        Text swapped = BodyTradeClient.capgrasName(entity, text);
        if (swapped == null) return;
        bodytrade$busy = true;
        try { renderLabelIfPresent(entity, swapped, matrices, vcp, light); } finally { bodytrade$busy = false; }
        ci.cancel();
    }
}
