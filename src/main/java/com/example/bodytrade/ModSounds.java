package com.example.bodytrade;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Собственные звуки мода (файлы - assets/bodytrade/sounds, список - sounds.json). */
public final class ModSounds {
    private ModSounds() {}

    /** Шёпот голосов (4 варианта, выбирается случайный). */
    public static SoundEvent WHISPER;

    public static void init() {
        Identifier id = new Identifier(BodyTradeMod.ID, "possession.whisper");
        WHISPER = Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
