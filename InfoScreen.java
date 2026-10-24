package com.example.bodytrade;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

/** Побочные эффекты и болезни. Действуют определённое время, сохраняются в мире. */
public enum SideEffect {
    HANGOVER("Похмелье", "Вас мутит, голова раскалывается.", 60),
    INSOMNIA("Недосып", "Вы клюёте носом: всё получается медленно.", 120),
    ILLNESS("Болезнь", "После операции вас лихорадит и знобит.", 90),
    OCD("ОКР", "Навязчивые мысли: вы то и дело перебираете предметы в панели быстрого доступа.", 300),
    PTSD("ПТСР", "Вас накрывают флешбэки: внезапные звуки, темнота, паника.", 300),
    ADHD("СДВГ", "Вы не можете сосредоточиться: копаете хуже, то и дело дёргаетесь и срываетесь с места.", 240),
    DEPRESSION("Депрессия", "Ничего не хочется: слабость, вялость, бег невозможен.", 300),
    PARANOIA("Паранойя", "Вам слышатся шаги и шорохи позади.", 240),
    CLAUSTROPHOBIA("Клаустрофобия", "Под крышей вам становится страшно и тяжело.", 300),
    AGORAPHOBIA("Агорафобия", "Под открытым небом вас охватывает паника.", 300),
    ANEMIA("Анемия", "Малокровие: слабость и вялость.", 240),
    FEVER("Лихорадка", "Жар, озноб, периодическая тошнота.", 120);

    public final String title, message;
    public final int seconds;

    SideEffect(String title, String message, int seconds) { this.title = title; this.message = message; this.seconds = seconds; }

    private static void fx(ServerPlayerEntity p, StatusEffect e, int ticks, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, ticks, amp, true, false, true));
    }
    private static void bar(ServerPlayerEntity p, String s) { p.sendMessage(Text.literal(s).formatted(Formatting.YELLOW), true); }

    /** Вызывается каждый серверный тик, пока эффект активен. */
    public void tick(ServerPlayerEntity p) {
        int a = p.age;
        Random r = p.getRandom();
        boolean sec = a % 20 == 0;
        switch (this) {
            case HANGOVER -> { if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 0); if (a % 200 == 0) fx(p, StatusEffects.NAUSEA, 120, 0); } }
            case INSOMNIA -> { if (sec) { fx(p, StatusEffects.SLOWNESS, 60, 0); fx(p, StatusEffects.MINING_FATIGUE, 60, 0); } }
            case ILLNESS -> { if (sec) { fx(p, StatusEffects.HUNGER, 60, 0); fx(p, StatusEffects.WEAKNESS, 60, 0); } if (a % 300 == 0) fx(p, StatusEffects.POISON, 60, 0); }
            case OCD -> {
                if (a % 300 == 0 && r.nextDouble() < 0.7) {
                    int s = (p.getInventory().selectedSlot + 1 + r.nextInt(8)) % 9;
                    p.getInventory().selectedSlot = s;
                    p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(s));
                    bar(p, "Нужно ещё раз всё перепроверить...");
                }
            }
            case PTSD -> {
                if (a % 1200 == 0 && r.nextDouble() < 0.7) {
                    play(p, SoundEvents.ENTITY_CREEPER_PRIMED, 0f, 1f);
                    fx(p, StatusEffects.DARKNESS, 120, 0);
                    fx(p, StatusEffects.SLOWNESS, 100, 1);
                    fx(p, StatusEffects.NAUSEA, 100, 0);
                    bar(p, "Флешбэк...");
                }
            }
            case ADHD -> {
                if (sec) fx(p, StatusEffects.MINING_FATIGUE, 60, 0);
                if (a % 600 == 0) {
                    fx(p, StatusEffects.SPEED, 100, 1);
                    p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(), p.getYaw() + (r.nextFloat() - 0.5f) * 180f, p.getPitch());
                    bar(p, "Что? Куда? Не могу сосредоточиться!");
                }
            }
            case DEPRESSION -> { if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 0); fx(p, StatusEffects.SLOWNESS, 60, 0); fx(p, StatusEffects.MINING_FATIGUE, 60, 0); } p.setSprinting(false); }
            case PARANOIA -> {
                if (a % 300 == 0 && r.nextDouble() < 0.6) {
                    SoundEvent[] pool = {SoundEvents.ENTITY_CREEPER_PRIMED, SoundEvents.ENTITY_ZOMBIE_AMBIENT,
                            SoundEvents.ENTITY_SKELETON_AMBIENT, SoundEvents.ENTITY_ENDERMAN_STARE, SoundEvents.BLOCK_WOODEN_DOOR_OPEN};
                    play(p, pool[r.nextInt(pool.length)], 4f, 1f);
                }
            }
            case CLAUSTROPHOBIA -> {
                if (sec && !p.getWorld().isSkyVisible(p.getBlockPos())) { fx(p, StatusEffects.SLOWNESS, 60, 0); fx(p, StatusEffects.WEAKNESS, 60, 1); }
            }
            case AGORAPHOBIA -> {
                if (sec && p.getWorld().isSkyVisible(p.getBlockPos())) { fx(p, StatusEffects.SLOWNESS, 60, 0); fx(p, StatusEffects.MINING_FATIGUE, 60, 1); }
            }
            case ANEMIA -> { if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 1); fx(p, StatusEffects.SLOWNESS, 60, 0); } }
            case FEVER -> { if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 0); fx(p, StatusEffects.HUNGER, 60, 0); } if (a % 400 == 0) fx(p, StatusEffects.NAUSEA, 100, 0); }
        }
    }

    /** Звук только для этого игрока, "позади" него (offset = расстояние, 0 - в его позиции). */
    private static void play(ServerPlayerEntity p, SoundEvent sound, float offset, float pitch) {
        double yaw = Math.toRadians(p.getYaw());
        double x = p.getX() + Math.sin(yaw) * offset, z = p.getZ() - Math.cos(yaw) * offset;
        p.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(sound), SoundCategory.HOSTILE,
                x, p.getY(), z, 1.0f, pitch, p.getRandom().nextLong()));
    }
}
