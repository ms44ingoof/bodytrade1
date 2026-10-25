package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.ProsthetistEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class ProsthetistRenderer extends MobEntityRenderer<ProsthetistEntity, ProsthetistModel> {
    private static final Identifier TEXTURE = new Identifier(BodyTradeMod.ID, "textures/entity/prosthetist.png");

    public ProsthetistRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new ProsthetistModel(ctx.getPart(ProsthetistModel.LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(ProsthetistEntity entity) { return TEXTURE; }
}
