package com.example.bodytrade.mixin;

import com.example.bodytrade.client.BodyTradeClient;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Деменция: все переводимые названия превращаются в иероглифы. */
@Mixin(TranslationStorage.class)
public class TranslationStorageMixin {
    @Inject(method = "get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 0)
    private void bodytrade$dementia(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (BodyTradeClient.dementiaActive()) cir.setReturnValue(BodyTradeClient.scramble(cir.getReturnValue()));
    }
}
