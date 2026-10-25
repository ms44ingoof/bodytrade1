package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Рисует металлические протезы поверх скрытых (отданных) рук и ног игрока. */
public class ProsthesisFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static final Identifier TEXTURE = new Identifier(BodyTradeMod.ID, "textures/entity/prosthesis.png");
    private final PlayerEntityModel<AbstractClientPlayerEntity> wide, slim;

    public ProsthesisFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, EntityModelLoader loader) {
        super(ctx);
        this.wide = new PlayerEntityModel<>(loader.getModelPart(EntityModelLayers.PLAYER), false);
        this.slim = new PlayerEntityModel<>(loader.getModelPart(EntityModelLayers.PLAYER_SLIM), true);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        Integer mask = BodyTradeClient.PROS.get(entity.getUuid());
        if (mask == null || mask == 0 || entity.isInvisible()) return;
        PlayerEntityModel<AbstractClientPlayerEntity> m = "slim".equals(entity.getModel()) ? slim : wide;
        this.getContextModel().copyBipedStateTo(m);
        m.setVisible(false);
        if ((mask & 1) != 0) m.leftArm.visible = true;
        if ((mask & 2) != 0) m.rightArm.visible = true;
        if ((mask & 4) != 0) m.leftLeg.visible = true;
        if ((mask & 8) != 0) m.rightLeg.visible = true;
        m.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f);
    }
}
