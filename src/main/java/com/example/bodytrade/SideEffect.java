package com.example.bodytrade;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public enum SideEffect {
    HANGOVER("Похмелье", "Вас мутит, голова раскалывается."),
    INSOMNIA("Недосып", "Вы клюёте носом - всё получается медленно."),
    ILLNESS("Болезнь", "После операции вас лихорадит.");

    public final String name, message;
    SideEffect(String name, String message) { this.name = name; this.message = message; }

    /** Накладывает эффекты, возвращает длительность в секундах. */
    public int apply(ServerPlayerEntity p) {
        switch (this) {
            case HANGOVER -> { add(p, StatusEffects.NAUSEA, 400, 0); add(p, StatusEffects.WEAKNESS, 1200, 0); return 60; }
            case INSOMNIA -> { add(p, StatusEffects.SLOWNESS, 2400, 0); add(p, StatusEffects.MINING_FATIGUE, 2400, 0); return 120; }
            default -> { add(p, StatusEffects.POISON, 100, 0); add(p, StatusEffects.HUNGER, 1200, 0); add(p, StatusEffects.WEAKNESS, 1800, 0); return 90; }
        }
    }
    private static void add(ServerPlayerEntity p, StatusEffect e, int ticks, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, ticks, amp));
    }
}
