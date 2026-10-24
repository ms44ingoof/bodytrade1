package com.example.bodytrade.client;

import com.example.bodytrade.HallucinationEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Рисует чудовище плоской картинкой, всегда повёрнутой к камере. */
public class BillboardRenderer extends EntityRenderer<HallucinationEntity> {
    private static final int FULL_BRIGHT = 15728880;
    private final Identifier texture;
    private final float width, height;

    public BillboardRenderer(EntityRendererFactory.Context ctx, Identifier texture, float width, float height) {
        super(ctx);
        this.texture = texture; this.width = width; this.height = height;
        this.shadowRadius = 0.4f;
    }

    @Override
    public Identifier getTexture(HallucinationEntity entity) { return texture; }

    @Override
    public void render(HallucinationEntity entity, float yaw, float delta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.multiply(this.dispatcher.getRotation());
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pos = e.getPositionMatrix();
        Matrix3f normal = e.getNormalMatrix();
        VertexConsumer buf = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        float hw = width / 2f;
        vertex(buf, pos, normal, -hw, 0f, 0f, 1f);
        vertex(buf, pos, normal, hw, 0f, 1f, 1f);
        vertex(buf, pos, normal, hw, height, 1f, 0f);
        vertex(buf, pos, normal, -hw, height, 0f, 0f);
        matrices.pop();
        super.render(entity, yaw, delta, matrices, vertexConsumers, light);
    }

    private static void vertex(VertexConsumer b, Matrix4f pos, Matrix3f n, float x, float y, float u, float v) {
        b.vertex(pos, x, y, 0f).color(255, 255, 255, 255).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(FULL_BRIGHT).normal(n, 0f, 0f, 1f).next();
    }
}
