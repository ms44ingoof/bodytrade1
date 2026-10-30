package com.example.bodytrade.client;

import com.example.bodytrade.mixin.AnimalModelAccessor;
import com.example.bodytrade.mixin.ModelPartAccessor;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.util.Identifier;

/**
 * Прозопагнозия: вместо головы животного рисуется голова игрока (скин с оверлеем).
 * В одиночной игре - ваша голова, в мультиплеере - головы других игроков.
 * Размер и положение берутся из границ настоящей головы животного.
 */
public class ProsopagnosiaFeature<T extends LivingEntity, M extends AnimalModel<T>> extends FeatureRenderer<T, M> {
    private static ModelPart[] cube;

    private static ModelPart[] cube() {
        if (cube == null) {
            ModelData md = new ModelData();
            ModelPartData r = md.getRoot();
            r.addChild("h", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, -4f, -4f, 8f, 8f, 8f), ModelTransform.NONE);
            r.addChild("hat", ModelPartBuilder.create().uv(32, 0).cuboid(-4f, -4f, -4f, 8f, 8f, 8f, new Dilation(0.5f)), ModelTransform.NONE);
            ModelPart root = TexturedModelData.of(md, 64, 64).createModel();
            cube = new ModelPart[]{root.getChild("h"), root.getChild("hat")};
        }
        return cube;
    }

    public ProsopagnosiaFeature(FeatureRendererContext<T, M> context) { super(context); }

    /** Первая часть головы, у которой есть собственные кубоиды (по ней считаем размер и положение). */
    public static ModelPart headPart(AnimalModel<?> model) {
        for (ModelPart p : ((AnimalModelAccessor) model).bodytrade$headParts())
            if (!((ModelPartAccessor) (Object) p).bodytrade$cuboids().isEmpty()) return p;
        return null;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vcp, int light, T entity, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (!(entity instanceof AnimalEntity) || entity.isInvisible() || !BodyTradeClient.prosopagnosiaActive()) return;
        M model = getContextModel();
        ModelPart head = headPart(model);
        if (head == null) return;

        // границы настоящей головы
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (ModelPart.Cuboid c : ((ModelPartAccessor) (Object) head).bodytrade$cuboids()) {
            minX = Math.min(minX, c.minX); minY = Math.min(minY, c.minY); minZ = Math.min(minZ, c.minZ);
            maxX = Math.max(maxX, c.maxX); maxY = Math.max(maxY, c.maxY); maxZ = Math.max(maxZ, c.maxZ);
        }
        float edge = (float) Math.cbrt(Math.max(1f, (maxX - minX) * (maxY - minY) * (maxZ - minZ)));
        float k = Math.max(0.55f, Math.min(1.6f, edge / 8f));

        Identifier skin = BodyTradeClient.prosopagnosiaSkin(entity);
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(skin));
        int overlay = LivingEntityRenderer.getOverlay(entity, 0f);

        matrices.push();
        if (model.child) {   // детёныши: голова крупнее и сдвинута (значения как у четвероногих)
            boolean chicken = entity instanceof ChickenEntity;
            if (!chicken) { float g = 1.5f / 2.0f; matrices.scale(g, g, g); }
            matrices.translate(0f, (chicken ? 5f : 8f) / 16f, (chicken ? 2f : 4f) / 16f);
        }
        head.rotate(matrices);
        matrices.translate((minX + maxX) / 32f, (minY + maxY) / 32f, (minZ + maxZ) / 32f);
        matrices.scale(k, k, k);
        for (ModelPart part : cube()) part.render(matrices, vc, light, overlay, 1f, 1f, 1f, 1f);
        matrices.pop();
    }
}
