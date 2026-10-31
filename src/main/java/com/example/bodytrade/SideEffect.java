package com.example.bodytrade;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    FEVER("Лихорадка", "Жар, озноб, периодическая тошнота.", 120),
    TUBERCULOSIS("Туберкулёз", "Мучительный кашель и слабость: бег невозможен.", 300),
    SEPSIS("Сепсис", "Заражение крови: слабость, отравление, тошнота.", 120),
    MIGRAINE("Мигрень", "Раскалывается голова: тошнота и мрак перед глазами.", 120),
    EPILEPSY("Эпилепсия", "Внезапные припадки: вас трясёт, предметы выпадают из рук.", 240),
    DEMENTIA("Деменция", "Вы забываете слова: все названия в игре превращаются в иероглифы.", 180),
    INCONTINENCE("Недержание", "Вы не можете себя контролировать, а окружающим становится дурно от запаха.", 180),
    ACHROMATOPSIA("Ахроматопсия", "Мир теряет цвета: вы видите всё в оттенках серого.", 240),
    CAPGRAS("Синдром Капгра", "Вам кажется, что людей подменили двойниками: имена над ними не те.", 240),
    TREMOR("Тремор", "Руки и голова дрожат: целиться и копать трудно.", 240),
    PROSOPAGNOSIA("Прозопагнозия", "Вы не узнаёте лиц: вместо морд животных вы видите чьи-то головы.", 240),
    NERVE_LEFT_ARM("Оголённый нерв (левая рука)", "Любое действие левой рукой причиняет боль: держать, бить, ставить, ломать.", 240),
    NERVE_RIGHT_ARM("Оголённый нерв (правая рука)", "Любое действие правой рукой причиняет боль: держать, бить, ставить, ломать.", 240),
    NERVE_LEFT_LEG("Оголённый нерв (левая нога)", "Прыжки и ходьба причиняют боль.", 240),
    NERVE_RIGHT_LEG("Оголённый нерв (правая нога)", "Прыжки и ходьба причиняют боль.", 240),
    // редкие (шанс за один обмен указан в последнем аргументе)
    PSYCHOSIS("Психоз", "Мир погружается во тьму, а чудовища приходят за вами.", 60, 0.005),
    CANCER("Рак", "Тяжёлая болезнь: вы постепенно слабеете и теряете здоровье.", 600, 0.003),
    RABIES("Бешенство", "Животные нападают на вас, вода вызывает ужас и тошноту, накатывают судороги.", 300, 0.004),
    POSSESSION("Одержимость", "Вам шепчут голоса, рассудок тает, а тьма смотрит в ответ.", 240, 0.004);

    public final String title, message;
    public final int seconds;
    /** Шанс получить эффект за один обмен (0 - обычный эффект, выбирается случайно из общего списка). */
    public final double rareChance;

    SideEffect(String title, String message, int seconds) { this(title, message, seconds, 0.0); }
    SideEffect(String title, String message, int seconds, double rareChance) {
        this.title = title; this.message = message; this.seconds = seconds; this.rareChance = rareChance;
    }

    public boolean isRare() { return rareChance > 0; }

    private static final Map<UUID, Integer> SEIZURE_END = new HashMap<>();

    static void forget(UUID id) { SEIZURE_END.remove(id); }

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
            case TUBERCULOSIS -> {
                p.setSprinting(false);
                if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 0); fx(p, StatusEffects.SLOWNESS, 60, 0); }
                if (a % 400 == 0) {
                    play(p, SoundEvents.ENTITY_PLAYER_BREATH, 0f, 0.5f);
                    fx(p, StatusEffects.SLOWNESS, 40, 2);
                    if (p.getHealth() > 4f) p.damage(p.getDamageSources().generic(), 1f);
                    bar(p, "Кха-кха-кха!");
                }
            }
            case SEPSIS -> {
                if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 1); fx(p, StatusEffects.SLOWNESS, 60, 0); fx(p, StatusEffects.HUNGER, 60, 1); }
                if (a % 200 == 0) { fx(p, StatusEffects.POISON, 60, 0); fx(p, StatusEffects.NAUSEA, 80, 0); }
            }
            case MIGRAINE -> {
                if (sec) fx(p, StatusEffects.SLOWNESS, 60, 0);
                if (a % 200 == 0) fx(p, StatusEffects.NAUSEA, 100, 0);
                if (a % 400 == 0) fx(p, StatusEffects.DARKNESS, 100, 0);
            }
            case EPILEPSY -> seizure(p, a, r, 600, 0.6);
            case TREMOR -> { if (sec) fx(p, StatusEffects.MINING_FATIGUE, 60, 0); }
            case DEMENTIA -> { if (a % 600 == 0) fx(p, StatusEffects.NAUSEA, 60, 0); }
            case INCONTINENCE -> {
                if (a % 400 == 0 && r.nextDouble() < 0.8 && p.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BROWN_CONCRETE_POWDER.getDefaultState()),
                            p.getX(), p.getY() + 0.3, p.getZ(), 30, 0.25, 0.05, 0.25, 0.08);
                    sw.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 1f, 0.6f);
                    fx(p, StatusEffects.SLOWNESS, 40, 1);
                    for (ServerPlayerEntity other : sw.getPlayers(o -> o != p && o.squaredDistanceTo(p) < 16.0))
                        fx(other, StatusEffects.NAUSEA, 80, 0);
                    bar(p, "Вы не удержались...");
                }
            }
            case PSYCHOSIS -> {
                if (sec) fx(p, StatusEffects.DARKNESS, 60, 0); // тьма держится, пока длится болезнь
                if (a % 300 == 0 && r.nextDouble() < 0.75 && spawnMonsters(p)) {
                    play(p, SoundEvents.ENTITY_WARDEN_NEARBY_CLOSER, 0f, 0.8f);
                    bar(p, "Что-то идёт за вами...");
                }
            }
            case CANCER -> {
                if (sec) { fx(p, StatusEffects.WEAKNESS, 60, 1); fx(p, StatusEffects.SLOWNESS, 60, 0); fx(p, StatusEffects.HUNGER, 60, 1); }
                if (a % 600 == 0 && p.getHealth() > 3f) { p.damage(p.getDamageSources().generic(), 1f); bar(p, "Болезнь забирает силы..."); }
            }
            case RABIES -> {
                // водобоязнь: вода и дождь вызывают ужас
                if (sec && (p.isTouchingWater() || p.isSubmergedInWater())) {
                    fx(p, StatusEffects.NAUSEA, 100, 0); fx(p, StatusEffects.SLOWNESS, 60, 1); fx(p, StatusEffects.WEAKNESS, 60, 1);
                    if (a % 100 == 0) bar(p, "Вода! Не могу...");
                } else if (sec && p.getWorld().hasRain(p.getBlockPos())) {
                    fx(p, StatusEffects.NAUSEA, 80, 0);
                }
                // судороги
                seizure(p, a, r, 500, 0.5);
                // агрессия животных
                if (a % 10 == 0) animalsAttack(p, r);
            }
            case POSSESSION -> {
                if (a % 100 == 0 && r.nextDouble() < 0.7) {
                    play(p, ModSounds.WHISPER, 1.2f + r.nextFloat() * 3f, 0.9f + r.nextFloat() * 0.25f);
                    if (r.nextDouble() < 0.5) {
                        String[] voices = {"...иди к нам...", "...ты не один...", "...открой глаза...", "...отдай нам тело...", "...мы здесь...", "...не оглядывайся..."};
                        p.sendMessage(Text.literal(voices[r.nextInt(voices.length)]).formatted(Formatting.DARK_GRAY, Formatting.ITALIC), true);
                    }
                }
                if (a % 400 == 0 && r.nextDouble() < 0.6) { fx(p, StatusEffects.DARKNESS, 80, 0); fx(p, StatusEffects.NAUSEA, 60, 0); }
                if (sec && p.getServer() != null) {   // голоса точат рассудок
                    BodyState st = BodyState.of(p.getServer());
                    st.setSanity(p.getUuid(), st.sanity(p.getUuid()) - 1);
                }
            }
            default -> { } // оголённые нервы обрабатываются в BodyTradeMod (нужны действия игрока)
        }
    }

    /** Припадок: время от времени выпадает предмет, а несколько секунд игрока трясёт. */
    private static void seizure(ServerPlayerEntity p, int a, Random r, int period, double chance) {
        Integer end = SEIZURE_END.get(p.getUuid());
        if (end == null) {
            if (a % period == 0 && r.nextDouble() < chance) {
                SEIZURE_END.put(p.getUuid(), a + 60);
                if (!p.getMainHandStack().isEmpty()) {
                    p.dropItem(p.getMainHandStack().copy(), false);
                    p.getInventory().setStack(p.getInventory().selectedSlot, ItemStack.EMPTY);
                }
                bar(p, "Припадок!");
            }
        } else if (a >= end || a < end - 100) {
            SEIZURE_END.remove(p.getUuid());
        } else {
            fx(p, StatusEffects.SLOWNESS, 10, 4);
            fx(p, StatusEffects.NAUSEA, 60, 0);
            if (a % 4 == 0) p.networkHandler.requestTeleport(p.getX(), p.getY(), p.getZ(),
                    p.getYaw() + (r.nextFloat() - 0.5f) * 80f, p.getPitch() + (r.nextFloat() - 0.5f) * 30f);
        }
    }

    /** Бешенство: животные поблизости (кроме прирученных) бегут к игроку и кусают. */
    private static void animalsAttack(ServerPlayerEntity p, Random r) {
        if (!(p.getWorld() instanceof ServerWorld sw)) return;
        int n = 0;
        for (net.minecraft.entity.passive.AnimalEntity an : sw.getEntitiesByClass(net.minecraft.entity.passive.AnimalEntity.class,
                p.getBoundingBox().expand(12),
                x -> x.isAlive() && !x.hasPassengers() && x != p.getVehicle()
                        && !(x instanceof net.minecraft.entity.passive.TameableEntity t && t.isTamed()))) {
            if (n++ >= 8) break;
            an.getNavigation().startMovingTo(p, 1.3);
            an.getLookControl().lookAt(p, 30f, 30f);
            if (an.squaredDistanceTo(p) < 5.5 && r.nextFloat() < 0.4f) {
                p.damage(sw.getDamageSources().mobAttack(an), 2f);
                sw.playSound(null, an.getBlockPos(), SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.HOSTILE, 0.8f, 0.8f + r.nextFloat() * 0.4f);
            }
        }
    }

    /** Появляется чудовище-галлюцинация (только одно одновременно), охотится только за этим игроком. */
    static boolean spawnMonsters(ServerPlayerEntity p) {
        if (!(p.getWorld() instanceof ServerWorld sw)) return false;
        Random r = p.getRandom();
        Box box = p.getBoundingBox().expand(48);
        int alive = sw.getEntitiesByClass(HallucinationEntity.class, box, e -> p.getUuid().equals(e.getOwnerId())).size();
        if (alive > 0) return false;
        for (int tries = 0; tries < 12; tries++) {
            double ang = r.nextDouble() * Math.PI * 2, dist = 5 + r.nextDouble() * 5;
            double x = p.getX() + Math.cos(ang) * dist, z = p.getZ() + Math.sin(ang) * dist;
            EntityType<HallucinationEntity> t = r.nextBoolean() ? BodyTradeMod.SMILER : BodyTradeMod.SCREAMER;
            HallucinationEntity e = t.create(sw);
            if (e == null) continue;
            e.refreshPositionAndAngles(x, p.getY(), z, r.nextFloat() * 360f, 0f);
            if (!sw.isSpaceEmpty(e)) continue;
            e.setOwner(p.getUuid());
            e.setPersistent();
            sw.spawnEntity(e);
            return true;
        }
        return false;
    }

    /** Звук только для этого игрока, "позади" него (offset = расстояние, 0 - в его позиции). */
    /** Ложный звук и шёпот для низкого рассудка. */
    static void whisper(ServerPlayerEntity p) {
        Random r = p.getRandom();
        SoundEvent[] pool = {SoundEvents.ENTITY_ENDERMAN_AMBIENT, SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundEvents.ENTITY_SKELETON_AMBIENT,
                SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundEvents.ENTITY_CREEPER_PRIMED, SoundEvents.ENTITY_PHANTOM_AMBIENT};
        play(p, pool[r.nextInt(pool.length)], 3f + r.nextFloat() * 3f, 0.7f + r.nextFloat() * 0.5f);
        String[] lines = {"Кто-то шепчет за спиной...", "Вам послышались шаги.", "Что-то скребётся в стене.", "Вы не одни."};
        bar(p, lines[r.nextInt(lines.length)]);
    }

    static void play(ServerPlayerEntity p, SoundEvent sound, float offset, float pitch) {
        double yaw = Math.toRadians(p.getYaw());
        double x = p.getX() + Math.sin(yaw) * offset, z = p.getZ() - Math.cos(yaw) * offset;
        p.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(sound), SoundCategory.HOSTILE,
                x, p.getY(), z, 1.0f, pitch, p.getRandom().nextLong()));
    }
}
