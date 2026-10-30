package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.DoctorEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/** Доктор выглядит как игрок со своим скином (textures/entity/doctor.png, формат обычного скина 64x64). */
public class DoctorRenderer extends BipedEntityRenderer<DoctorEntity, PlayerEntityModel<DoctorEntity>> {
    private static final Identifier TEXTURE = new Identifier(BodyTradeMod.ID, "textures/entity/doctor.png");
    /** true - если у скина тонкие руки (Alex, 3 пикселя), false - обычные (Steve, 4 пикселя). */
    private static final boolean SLIM = false;

    public DoctorRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel<>(ctx.getPart(SLIM ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), SLIM), 0.5f);
    }

    @Override
    public Identifier getTexture(DoctorEntity entity) { return TEXTURE; }
}
