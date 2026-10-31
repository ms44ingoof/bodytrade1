package com.example.bodytrade.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Положение окна инвентаря (смещается, когда открыта книга рецептов). */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int bodytrade$x();

    @Accessor("y")
    int bodytrade$y();
}
