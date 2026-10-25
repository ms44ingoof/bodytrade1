package com.example.bodytrade.mixin;

import com.example.bodytrade.client.BodyTradeClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Деменция: весь отрисовываемый текст превращается в постоянно меняющиеся нечитаемые буквы. */
@Mixin(TextRenderer.class)
public class TextRendererMixin {
    @ModifyVariable(method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private String bodytrade$scrambleString(String text) {
        return BodyTradeClient.dementiaActive() ? BodyTradeClient.scrambleLive(text) : text;
    }

    @ModifyVariable(method = "drawInternal(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private OrderedText bodytrade$scrambleOrdered(OrderedText text) {
        return BodyTradeClient.dementiaActive() ? BodyTradeClient.scrambleOrdered(text) : text;
    }
}
