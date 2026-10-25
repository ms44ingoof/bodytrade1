package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.ProsthetistEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** 3D-модель протезиста: механик в белом халате с очками, рюкзаком-шестернёй и бионической левой рукой. */
public class ProsthetistModel extends SinglePartEntityModel<ProsthetistEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(new Identifier(BodyTradeMod.ID, "prosthetist"), "main");

    private final ModelPart root, head, rightArm, leftArm;

    public ProsthetistModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData md = new ModelData();
        ModelPartData r = md.getRoot();
        ModelPartData head = r.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f), ModelTransform.NONE);
        head.addChild("goggles", ModelPartBuilder.create().uv(32, 0).cuboid(-4.5f, -5.5f, -4.8f, 9f, 3f, 1f), ModelTransform.NONE);
        r.addChild("body", ModelPartBuilder.create().uv(16, 16).cuboid(-4f, 0f, -2f, 8f, 12f, 4f), ModelTransform.NONE);
        r.addChild("backpack", ModelPartBuilder.create().uv(0, 32).cuboid(-3f, 1f, 2f, 6f, 8f, 3f), ModelTransform.NONE);
        r.addChild("gear", ModelPartBuilder.create().uv(0, 48).cuboid(-2f, 3f, 5f, 4f, 4f, 1f), ModelTransform.NONE);
        r.addChild("right_arm", ModelPartBuilder.create().uv(40, 16).cuboid(-3f, -2f, -2f, 4f, 12f, 4f), ModelTransform.pivot(-5f, 2f, 0f));
        ModelPartData la = r.addChild("left_arm", ModelPartBuilder.create().uv(32, 48).cuboid(-1f, -2f, -2f, 4f, 12f, 4f), ModelTransform.pivot(5f, 2f, 0f));
        la.addChild("pipe", ModelPartBuilder.create().uv(48, 32).cuboid(3f, 0f, -1f, 2f, 6f, 2f), ModelTransform.NONE);
        r.addChild("right_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2f, 0f, -2f, 4f, 12f, 4f), ModelTransform.pivot(-1.9f, 12f, 0f));
        r.addChild("left_leg", ModelPartBuilder.create().uv(16, 48).cuboid(-2f, 0f, -2f, 4f, 12f, 4f), ModelTransform.pivot(1.9f, 12f, 0f));
        return TexturedModelData.of(md, 64, 64);
    }

    @Override
    public ModelPart getPart() { return root; }

    @Override
    public void setAngles(ProsthetistEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        head.yaw = headYaw * ((float) Math.PI / 180f);
        head.pitch = headPitch * ((float) Math.PI / 180f);
        rightArm.pitch = MathHelper.cos(animationProgress * 0.067f) * 0.05f;
        rightArm.roll = 0.05f;
        leftArm.pitch = -MathHelper.cos(animationProgress * 0.067f) * 0.05f;
        leftArm.roll = -0.05f;
    }
}
