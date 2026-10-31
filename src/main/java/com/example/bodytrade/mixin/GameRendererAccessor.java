package com.example.bodytrade.mixin;

import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Доступ к пост-обработке экрана (ванильные шейдеры из shaders/post). */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("postProcessor")
    PostEffectProcessor bodytrade$postProcessor();

    @Invoker("loadPostProcessor")
    void bodytrade$loadPostProcessor(Identifier id);
}
