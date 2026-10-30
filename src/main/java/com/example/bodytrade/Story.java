package com.example.bodytrade;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Сюжет начала игры.
 * 1) Игрок заходит в мир впервые: выбора сложности нет, этап WAIT_WOOD.
 * 2) Игрок ломает первое бревно: рядом появляется Доктор (этап OFFER) и подходит к нему.
 * 3) Игрок выбирает в диалоге:
 *    - выпить зелье: слепота, тошнота и рвота, затем пропадают конечности (режим «Перерождение»);
 *    - отказаться: Доктор бьёт игрока, оставляя ему полсердечка, и уходит (обычная сложность).
 */
public final class Story {
    private Story() {}

    public static final int NONE = -1, WAIT_WOOD = 0, OFFER = 1, DONE = 2;

    public static final Identifier OPEN_PACKET = new Identifier(BodyTradeMod.ID, "story_open");
    public static final Identifier CHOICE_PACKET = new Identifier(BodyTradeMod.ID, "story_choice");

    public static EntityType<DoctorEntity> DOCTOR;

    /** Идущая сейчас сцена игрока (только в памяти: если сервер выключили посреди сцены, она начнётся заново). */
    private static final class Scene {
        final boolean accept;
        final UUID doctor;
        int t = 0;
        boolean applied = false;
        Scene(boolean accept, UUID doctor) { this.accept = accept; this.doctor = doctor; }
    }

    private static final Map<UUID, Scene> SCENES = new HashMap<>();
    /** Живой Доктор каждого игрока на этапе OFFER. */
    private static final Map<UUID, UUID> DOCTORS = new HashMap<>();

    // ------------------------------------------------------------------ регистрация
    public static void init() {
        DOCTOR = Registry.register(Registries.ENTITY_TYPE, new Identifier(BodyTradeMod.ID, "doctor"),
                FabricEntityTypeBuilder.<DoctorEntity>create(SpawnGroup.MISC, DoctorEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.8f)).trackRangeBlocks(64).build());
        FabricDefaultAttributeRegistry.register(DOCTOR, DoctorEntity.createAttributes());

        ServerPlayNetworking.registerGlobalReceiver(CHOICE_PACKET, (server, player, h, buf, resp) -> {
            String choice = buf.readString(16);
            server.execute(() -> choose(server, player, choice.equals("accept")));
        });

        // /bodystory start - начать сюжет заново, /bodystory doctor - сразу вызвать Доктора, /bodystory reset - убрать сюжет
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                CommandManager.literal("bodystory").requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("action", StringArgumentType.word()).executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            MinecraftServer server = ctx.getSource().getServer();
                            BodyState st = BodyState.of(server);
                            switch (StringArgumentType.getString(ctx, "action")) {
                                case "start" -> { cleanup(p); st.setStory(p.getUuid(), WAIT_WOOD); p.sendMessage(Text.literal("Сюжет перезапущен: сломайте бревно."), false); }
                                case "doctor" -> { cleanup(p); st.setStory(p.getUuid(), WAIT_WOOD); spawnDoctor(p, st); }
                                case "reset" -> { cleanup(p); st.setStory(p.getUuid(), NONE); p.sendMessage(Text.literal("Сюжет отключён для вас."), false); }
                                default -> { p.sendMessage(Text.literal("/bodystory start | doctor | reset"), false); return 0; }
                            }
                            return 1;
                        }))));
    }

    // ------------------------------------------------------------------ этапы
    /** Новый игрок: ждём первое дерево. */
    public static void begin(ServerPlayerEntity p, BodyState st) {
        st.setStory(p.getUuid(), WAIT_WOOD);
    }

    public static void onBlockBroken(ServerPlayerEntity p, BlockState state) {
        if (!state.isIn(BlockTags.LOGS)) return;
        BodyState st = BodyState.of(p.getServer());
        if (st.story(p.getUuid()) != WAIT_WOOD) return;
        spawnDoctor(p, st);
    }

    private static void spawnDoctor(ServerPlayerEntity p, BodyState st) {
        if (!(p.getWorld() instanceof ServerWorld w)) return;
        Vec3d spot = findSpot(p, new int[]{8, 7, 6, 5, 4, 3});
        if (spot == null) return;   // нет места - попробуем на следующем бревне
        DoctorEntity d = DOCTOR.create(w);
        if (d == null) return;
        float yaw = (float) (Math.toDegrees(Math.atan2(p.getZ() - spot.z, p.getX() - spot.x)) - 90.0);
        d.refreshPositionAndAngles(spot.x, spot.y, spot.z, yaw, 0f);
        d.setOwner(p.getUuid());
        w.spawnEntity(d);
        st.setStory(p.getUuid(), OFFER);
        DOCTORS.put(p.getUuid(), d.getUuid());
        poof(d);
        w.playSound(null, d.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.7f, 0.6f);
    }

    /** Доктор подошёл: открываем диалог. */
    public static void offer(ServerPlayerEntity p) {
        net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(true);
        ServerPlayNetworking.send(p, OPEN_PACKET, buf);
    }

    /** Закрывает диалог у игрока (Доктор ушёл или пропал). */
    private static void closeDialog(ServerPlayerEntity p) {
        net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(false);
        ServerPlayNetworking.send(p, OPEN_PACKET, buf);
    }

    /** Игрок слишком долго не отвечал: Доктор уходит, этап возвращается к «сломать бревно». */
    static void expire(ServerPlayerEntity p, DoctorEntity d) {
        closeDialog(p);
        DOCTORS.remove(p.getUuid());
        BodyState.of(p.getServer()).setStory(p.getUuid(), WAIT_WOOD);
        say(p, "Вы не готовы. Я зайду позже.");
        leave(d);
    }

    /** Игрок умер с открытым диалогом: после возрождения показываем его снова. */
    public static void onRespawn(ServerPlayerEntity p) {
        if (SCENES.containsKey(p.getUuid()) || BodyState.of(p.getServer()).story(p.getUuid()) != OFFER) return;
        UUID did = DOCTORS.get(p.getUuid());
        if (did != null && p.getWorld() instanceof ServerWorld w && w.getEntity(did) instanceof DoctorEntity d && d.isOffered() && !d.busy) offer(p);
    }

    /** Очистка при выходе из мира (в одиночной игре статические поля живут дольше мира). */
    public static void clear() { SCENES.clear(); DOCTORS.clear(); }

    private static void choose(MinecraftServer server, ServerPlayerEntity p, boolean accept) {
        UUID id = p.getUuid();
        BodyState st = BodyState.of(server);
        if (st.story(id) != OFFER || SCENES.containsKey(id)) return;
        UUID did = DOCTORS.get(id);
        Entity e = did == null ? null : ((ServerWorld) p.getWorld()).getEntity(did);
        if (!(e instanceof DoctorEntity d) || !d.isAlive() || !d.isOffered() || d.busy || !id.equals(d.getOwnerId()) || d.squaredDistanceTo(p) > 144) return;
        d.busy = true;
        SCENES.put(id, new Scene(accept, did));
    }

    // ------------------------------------------------------------------ тик
    public static void tick(MinecraftServer server) {
        // 1) идущие сцены
        Iterator<Map.Entry<UUID, Scene>> it = SCENES.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            Scene sc = en.getValue();
            sc.t++;
            DoctorEntity d = p.getWorld() instanceof ServerWorld w && w.getEntity(sc.doctor) instanceof DoctorEntity dd ? dd : null;
            if (sc.accept ? tickAccept(server, p, d, sc) : tickRefuse(server, p, d, sc)) it.remove();
        }
        // 2) страховка: этап OFFER, а Доктора уже нет (мир перезагружали, он пропал при смене измерения и т.п.)
        if (server.getTicks() % 100 == 0) {
            BodyState st = null;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (SCENES.containsKey(p.getUuid())) continue;
                if (st == null) st = BodyState.of(server);
                if (st.story(p.getUuid()) != OFFER) continue;
                UUID did = DOCTORS.get(p.getUuid());
                Entity e = did == null || !(p.getWorld() instanceof ServerWorld w) ? null : w.getEntity(did);
                if (e == null || !e.isAlive()) { DOCTORS.remove(p.getUuid()); st.setStory(p.getUuid(), WAIT_WOOD); closeDialog(p); }
            }
        }
    }

    /** Сцена «выпить зелье». true - сцена закончена. */
    private static boolean tickAccept(MinecraftServer server, ServerPlayerEntity p, DoctorEntity d, Scene sc) {
        ServerWorld w = (ServerWorld) p.getWorld();
        switch (sc.t) {
            case 1 -> {
                say(p, "Верное решение. Пейте до дна.");
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 480, 4, false, false));   // мобы и игроки не добьют
                if (d != null) d.swingHand(Hand.MAIN_HAND, true);
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1f, 0.8f);
            }
            case 25 -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 380, 0, false, false));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 420, 0, false, false));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 1, false, false));
                p.sendMessage(Text.literal("Мир темнеет, внутри всё сжимается...").formatted(Formatting.DARK_GRAY, Formatting.ITALIC), true);
            }
            case 90 -> say(p, "Потерпите. Тело само решит, что ему лишнее.");
            case 165 -> {
                applyOnce(server, p, sc, true);
                for (int i = 0; i < 3; i++) w.playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 1.3f, 0.6f + 0.15f * i);
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1f, 0.7f);
            }
            case 195 -> say(p, "Не бойтесь. Это только начало. Рядом с вами стоит блок: он поможет вам встать.");
            case 240 -> { leave(d); return true; }
            default -> {}
        }
        // рвота: с 30-го по 160-й тик
        if (sc.t >= 30 && sc.t <= 160) {
            if (sc.t % 4 == 0) vomitFx(p, w);
            if (sc.t % 22 == 0) {
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1f, 0.5f);
                int food = p.getHungerManager().getFoodLevel();
                if (food > 4) p.getHungerManager().setFoodLevel(food - 1);
            }
        }
        return false;
    }

    /** Сцена «отказаться». true - сцена закончена. */
    private static boolean tickRefuse(MinecraftServer server, ServerPlayerEntity p, DoctorEntity d, Scene sc) {
        ServerWorld w = (ServerWorld) p.getWorld();
        switch (sc.t) {
            case 1 -> say(p, "Жаль. Очень жаль.");
            case 25 -> {
                // Доктор делает рывок вплотную к игроку
                if (d != null) {
                    Vec3d dir = d.getPos().subtract(p.getPos()).multiply(1, 0, 1);
                    dir = dir.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : dir.normalize();
                    d.refreshPositionAndAngles(p.getX() + dir.x * 1.2, d.getY(), p.getZ() + dir.z * 1.2,
                            (float) (Math.toDegrees(Math.atan2(-dir.z, -dir.x)) - 90.0), 0f);
                }
            }
            case 32 -> {
                if (d != null) d.swingHand(Hand.MAIN_HAND, true);
                if (p.isAlive() && !p.isCreative() && !p.isSpectator()) {
                    p.setHealth(1.0f);   // полсердечка
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 4, false, false));   // 10 секунд защиты, чтобы не добили сразу
                }
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.2f, 0.6f);
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1f, 0.8f);
                w.sendEntityStatus(p, (byte) 2);   // анимация получения урона
                if (d != null) {
                    p.takeKnockback(0.9, d.getX() - p.getX(), d.getZ() - p.getZ());
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(p));
                }
            }
            case 55 -> say(p, "Запомните это. Предложение было честным.");
            case 95 -> {
                leave(d);
                applyOnce(server, p, sc, false);
                return true;
            }
            default -> {}
        }
        return false;
    }

    private static void applyOnce(MinecraftServer server, ServerPlayerEntity p, Scene sc, boolean develop) {
        if (sc.applied) return;
        sc.applied = true;
        BodyTradeMod.applyMode(server, p, develop);
        BodyState.of(server).setStory(p.getUuid(), DONE);
        DOCTORS.remove(p.getUuid());
    }

    /** Игрок вышел посреди сцены: сразу доигрываем её итог, чтобы сюжет не сломался. */
    public static void onDisconnect(MinecraftServer server, ServerPlayerEntity p) {
        Scene sc = SCENES.remove(p.getUuid());
        if (sc == null) {
            // вышел, пока Доктор ждал ответа: убираем Доктора, при следующем бревне он придёт снова
            UUID did = DOCTORS.remove(p.getUuid());
            BodyState st = BodyState.of(server);
            if (st.story(p.getUuid()) == OFFER) st.setStory(p.getUuid(), WAIT_WOOD);
            if (did != null && p.getWorld() instanceof ServerWorld w && w.getEntity(did) != null) w.getEntity(did).discard();
            return;
        }
        try {
            applyOnce(server, p, sc, sc.accept);
            if (p.getWorld() instanceof ServerWorld w && w.getEntity(sc.doctor) instanceof DoctorEntity d) { poof(d); d.discard(); }
        } catch (RuntimeException ignored) {}
    }

    // ------------------------------------------------------------------ вспомогательное
    private static void cleanup(ServerPlayerEntity p) {
        SCENES.remove(p.getUuid());
        UUID did = DOCTORS.remove(p.getUuid());
        if (did != null && p.getWorld() instanceof ServerWorld w && w.getEntity(did) != null) w.getEntity(did).discard();
    }

    private static void leave(DoctorEntity d) {
        if (d == null) return;
        poof(d);
        d.getWorld().playSound(null, d.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.7f, 0.5f);
        d.discard();
    }

    static void poof(Entity e) {
        if (e.getWorld() instanceof ServerWorld w)
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 1.0, e.getZ(), 25, 0.3, 0.7, 0.3, 0.02);
    }

    private static void vomitFx(ServerPlayerEntity p, ServerWorld w) {
        Vec3d look = p.getRotationVec(1f);
        Vec3d mouth = p.getEyePos().add(look.multiply(0.35)).subtract(0, 0.25, 0);
        w.spawnParticles(ParticleTypes.ITEM_SLIME, mouth.x, mouth.y, mouth.z, 6, 0.08, 0.05, 0.08, 0.08);
        w.spawnParticles(ParticleTypes.SNEEZE, mouth.x, mouth.y, mouth.z, 2, 0.1, 0.05, 0.1, 0.03);
    }

    private static void say(ServerPlayerEntity p, String line) {
        p.sendMessage(Text.literal("[Доктор] ").formatted(Formatting.DARK_GREEN).append(Text.literal(line).formatted(Formatting.WHITE)));
    }

    /** Свободное место (2 блока воздуха над твёрдым полом) на расстоянии d блоков от игрока, начиная с направления взгляда. */
    static Vec3d findSpot(ServerPlayerEntity p, int[] dists) {
        if (!(p.getWorld() instanceof ServerWorld w)) return null;
        int[] angles = {0, 40, -40, 80, -80, 120, -120, 180};
        for (int d : dists) {
            for (int a : angles) {
                double rad = Math.toRadians(p.getYaw() + a);
                BlockPos base = BlockPos.ofFloored(p.getX() - Math.sin(rad) * d, p.getY(), p.getZ() + Math.cos(rad) * d);
                for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                    BlockPos pos = base.up(dy);
                    BlockPos below = pos.down();
                    if (w.getBlockState(pos).isAir() && w.getBlockState(pos.up()).isAir()
                            && w.getBlockState(below).isSolidBlock(w, below) && w.getFluidState(pos).isEmpty())
                        return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                }
            }
        }
        return null;
    }
}
