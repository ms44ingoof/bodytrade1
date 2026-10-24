package com.example.bodytrade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BodyTradeMod implements ModInitializer {
    public static final String ID = "bodytrade";
    public static final Identifier TRADE_PACKET = new Identifier(ID, "trade");
    public static final Identifier RETURN_PACKET = new Identifier(ID, "return");
    public static final Identifier SYNC_PACKET = new Identifier(ID, "sync");
    public static final Identifier LIMBS_PACKET = new Identifier(ID, "limbs");
    public static final Identifier ADMIN_PACKET = new Identifier(ID, "admin");

    /** Шанс болезни/побочного эффекта после каждой сделки. */
    public static final double SIDE_EFFECT_CHANCE = 0.4;
    /** После смерти все части тела и болезни сбрасываются. */
    public static final boolean RESTORE_ON_DEATH = true;

    /** Клиентская копия списка потерянных частей (заполняется на клиенте, на сервере пуста). */
    public static final Set<BodyPart> CLIENT_MISSING = EnumSet.noneOf(BodyPart.class);

    private static final Set<Item> SOFT_FOOD = Set.of(Items.MUSHROOM_STEW, Items.BEETROOT_SOUP, Items.RABBIT_STEW,
            Items.SUSPICIOUS_STEW, Items.HONEY_BOTTLE, Items.MELON_SLICE, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.PUMPKIN_PIE);

    public static Block TRADE_BLOCK, ADMIN_BLOCK;
    public static EntityType<HallucinationEntity> SMILER, SCREAMER;
    private static final Map<UUID, Float> LAST_FALL = new HashMap<>();
    private static final Map<UUID, Integer> LAST_FOOD = new HashMap<>();

    @Override
    public void onInitialize() {
        TRADE_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "trade_altar"),
                new TradeBlock(FabricBlockSettings.create().strength(3.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "trade_altar"), new BlockItem(TRADE_BLOCK, new FabricItemSettings()));
        SMILER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "smiler"),
                FabricEntityTypeBuilder.<HallucinationEntity>create(SpawnGroup.MONSTER, HallucinationEntity::new)
                        .dimensions(EntityDimensions.fixed(0.8f, 2.0f)).build());
        SCREAMER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "screamer"),
                FabricEntityTypeBuilder.<HallucinationEntity>create(SpawnGroup.MONSTER, HallucinationEntity::new)
                        .dimensions(EntityDimensions.fixed(1.0f, 2.2f)).build());
        FabricDefaultAttributeRegistry.register(SMILER, HallucinationEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCREAMER, HallucinationEntity.createAttributes());
        ADMIN_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "admin_altar"),
                new AdminBlock(FabricBlockSettings.create().strength(3.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "admin_altar"), new BlockItem(ADMIN_BLOCK, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> { e.add(TRADE_BLOCK); e.add(ADMIN_BLOCK); });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (BodyState.of(server).markGaveBlock(p.getUuid())) {
                p.getInventory().offerOrDrop(new ItemStack(TRADE_BLOCK));
                p.sendMessage(Text.literal("Вам выдан Алтарь обмена. Поставьте его и нажмите ПКМ.").formatted(Formatting.GRAY));
            }
            sync(p);
            for (ServerPlayerEntity q : server.getPlayerManager().getPlayerList())
                sendLimbs(p, q.getUuid(), BodyPart.limbMask(BodyState.of(server).get(q.getUuid())));
            broadcastLimbs(server, p);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            if (!alive && RESTORE_ON_DEATH) {
                BodyState.of(newP.getServer()).clear(newP.getUuid());
                sync(newP);
                broadcastLimbs(newP.getServer(), newP);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TRADE_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> handleTrade(server, player, pos));
        });
        ServerPlayNetworking.registerGlobalReceiver(RETURN_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            BodyPart part = BodyPart.byId(buf.readString());
            server.execute(() -> handleReturn(server, player, pos, part));
        });

        ServerPlayNetworking.registerGlobalReceiver(ADMIN_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            String action = buf.readString();
            server.execute(() -> handleAdmin(server, player, pos, action));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) tickPlayer(server, p);
        });

        // Рот / язык / гортань: нельзя говорить в чате
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            Set<BodyPart> m = BodyState.of(sender.getServer()).get(sender.getUuid());
            if (m.contains(BodyPart.MOUTH) || m.contains(BodyPart.TONGUE) || m.contains(BodyPart.LARYNX)) {
                sender.sendMessage(Text.literal("Вы не можете говорить.").formatted(Formatting.RED), true);
                return false;
            }
            return true;
        });

        // Рот / глотка / зубы: ограничения на еду и питьё
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            boolean food = stack.isFood();
            boolean drink = stack.getUseAction() == UseAction.DRINK;
            if (!food && !drink) return TypedActionResult.pass(stack);
            Set<BodyPart> m = world.isClient ? CLIENT_MISSING : BodyState.of(world.getServer()).get(player.getUuid());
            String why = null;
            if (m.contains(BodyPart.MOUTH)) why = "Без рта вы не можете есть и пить.";
            else if (m.contains(BodyPart.THROAT)) why = "Без глотки вы не можете глотать.";
            else if (food && m.contains(BodyPart.TEETH) && !SOFT_FOOD.contains(stack.getItem())) why = "Без зубов вы можете есть только мягкую пищу.";
            if (why == null) return TypedActionResult.pass(stack);
            if (!world.isClient) player.sendMessage(Text.literal(why).formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        });
    }

    // ---------------------------------------------------------------- обмен
    private static boolean nearAltar(ServerPlayerEntity p, BlockPos pos) {
        return p.getWorld().getBlockState(pos).isOf(TRADE_BLOCK) && p.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 64;
    }

    private static void handleTrade(MinecraftServer server, ServerPlayerEntity p, BlockPos pos) {
        if (!nearAltar(p, pos) || !p.isAlive()) return;
        BodyState st = BodyState.of(server);
        BodyPart part = BodyPart.roll(st.get(p.getUuid()), p.getRandom());
        if (part == null) {
            p.sendMessage(Text.literal("Вам больше нечего отдавать.").formatted(Formatting.GRAY));
            return;
        }
        ItemStack reward = part.tier.roll(p.getRandom());
        Text rewardText = Text.literal("Получено: " + reward.getCount() + " × ").append(reward.getName());
        p.getInventory().offerOrDrop(reward);
        st.add(p.getUuid(), part);

        title(p, Text.literal("Вы лишились " + part.gen).formatted(Formatting.DARK_RED), rewardText.copy().formatted(Formatting.GRAY));
        p.sendMessage(Text.literal(part.consequence).formatted(Formatting.RED));

        SideEffect se = null;
        if (p.getRandom().nextDouble() < SideEffect.RARE_CHANCE) {
            se = SideEffect.PSYCHOSIS; // редкая болезнь: 0.5% за обмен
        } else if (p.getRandom().nextDouble() < SIDE_EFFECT_CHANCE) {
            List<SideEffect> common = new ArrayList<>();
            for (SideEffect x : SideEffect.values()) if (!x.isRare()) common.add(x);
            se = common.get(p.getRandom().nextInt(common.size()));
        }
        if (se != null) {
            st.setEffect(p.getUuid(), se, server.getOverworld().getTime() + se.seconds * 20L);
            p.sendMessage(Text.literal(se.title + ": " + se.message).formatted(Formatting.YELLOW));
        }
        sync(p);
        broadcastLimbs(server, p);
    }

    private static void handleReturn(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, BodyPart part) {
        if (part == null || !nearAltar(p, pos)) return;
        BodyState st = BodyState.of(server);
        if (!st.get(p.getUuid()).contains(part)) return;
        Item item = part.tier.refundItem;
        int need = part.tier.refundCount;
        var inv = p.getInventory();
        if (inv.count(item) < need) {
            p.sendMessage(Text.literal("Не хватает: нужно " + need + " × ").append(new ItemStack(item).getName()).formatted(Formatting.RED));
            return;
        }
        int left = need;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) { int t = Math.min(left, s.getCount()); s.decrement(t); left -= t; }
        }
        st.remove(p.getUuid(), part);
        title(p, Text.literal("Вы вернули: " + part.nom.toLowerCase()).formatted(Formatting.GREEN), Text.literal("Последствия сняты").formatted(Formatting.GRAY));
        p.sendMessage(Text.literal("Часть тела возвращена: " + part.nom + ". Связанные последствия исчезли.").formatted(Formatting.GREEN));
        sync(p);
        broadcastLimbs(server, p);
    }

    /** Админ-блок: выдать/снять любую часть тела или болезнь. Доступно в креативе или операторам (уровень 2). */
    private static void handleAdmin(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, String action) {
        if (!p.getWorld().getBlockState(pos).isOf(ADMIN_BLOCK) || p.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64) return;
        if (!(p.isCreative() || p.hasPermissionLevel(2))) {
            p.sendMessage(Text.literal("Нужны права администратора (креатив или оператор).").formatted(Formatting.RED));
            return;
        }
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        long now = server.getOverworld().getTime();
        if (action.startsWith("part:")) {
            BodyPart part = BodyPart.byId(action.substring(5));
            if (part == null) return;
            if (st.get(id).contains(part)) {
                st.remove(id, part);
                p.sendMessage(Text.literal("[Админ] Возвращено: " + part.nom).formatted(Formatting.GREEN));
            } else {
                st.add(id, part);
                p.sendMessage(Text.literal("[Админ] Отнято: " + part.nom).formatted(Formatting.RED));
                p.sendMessage(Text.literal(part.consequence).formatted(Formatting.RED));
            }
        } else if (action.startsWith("effect:")) {
            SideEffect se;
            try { se = SideEffect.valueOf(action.substring(7)); } catch (IllegalArgumentException ex) { return; }
            if (st.effects(id).containsKey(se) && st.effects(id).get(se) > now) {
                st.effects(id).remove(se);
                st.markDirty();
                p.sendMessage(Text.literal("[Админ] Снята болезнь: " + se.title).formatted(Formatting.GREEN));
            } else {
                st.setEffect(id, se, now + se.seconds * 20L);
                p.sendMessage(Text.literal("[Админ] Выдана болезнь: " + se.title).formatted(Formatting.YELLOW));
                p.sendMessage(Text.literal(se.title + ": " + se.message).formatted(Formatting.YELLOW));
            }
        } else if (action.equals("parts_clear")) {
            st.get(id).clear();
            st.markDirty();
            p.sendMessage(Text.literal("[Админ] Все части тела возвращены.").formatted(Formatting.GREEN));
        } else if (action.equals("effects_clear")) {
            st.effects(id).clear();
            st.markDirty();
            p.sendMessage(Text.literal("[Админ] Все болезни сняты.").formatted(Formatting.GREEN));
        } else {
            return;
        }
        sync(p);
        broadcastLimbs(server, p);
    }

    private static void title(ServerPlayerEntity p, Text title, Text sub) {
        var h = p.networkHandler;
        h.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
        h.sendPacket(new SubtitleS2CPacket(sub));
        h.sendPacket(new TitleS2CPacket(title));
    }

    // ---------------------------------------------------------------- сеть
    public static void sync(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        BodyState st = BodyState.of(s);
        long now = s.getOverworld().getTime();
        Set<BodyPart> m = st.get(p.getUuid());
        List<Map.Entry<SideEffect, Long>> act = new ArrayList<>();
        for (Map.Entry<SideEffect, Long> e : st.effects(p.getUuid()).entrySet()) if (e.getValue() > now) act.add(e);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(m.size());
        for (BodyPart bp : m) buf.writeString(bp.id);
        buf.writeVarInt(act.size());
        for (Map.Entry<SideEffect, Long> e : act) { buf.writeString(e.getKey().title); buf.writeVarInt((int) ((e.getValue() - now) / 20)); }
        ServerPlayNetworking.send(p, SYNC_PACKET, buf);
    }

    private static void sendLimbs(ServerPlayerEntity to, UUID who, int mask) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(who);
        buf.writeVarInt(mask);
        ServerPlayNetworking.send(to, LIMBS_PACKET, buf);
    }

    /** Все игроки узнают, каких конечностей не хватает у p (чтобы скрыть их на модели). */
    private static void broadcastLimbs(MinecraftServer server, ServerPlayerEntity p) {
        int mask = BodyPart.limbMask(BodyState.of(server).get(p.getUuid()));
        for (ServerPlayerEntity q : server.getPlayerManager().getPlayerList()) sendLimbs(q, p.getUuid(), mask);
    }

    // ---------------------------------------------------------------- последствия
    private static void eff(ServerPlayerEntity p, StatusEffect e, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false, true));
    }
    private static void effT(ServerPlayerEntity p, StatusEffect e, int ticks, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, ticks, amp, true, false, true));
    }
    private static void bar(ServerPlayerEntity p, String s) { p.sendMessage(Text.literal(s).formatted(Formatting.RED), true); }

    private static void tickPlayer(MinecraftServer server, ServerPlayerEntity p) {
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        Set<BodyPart> m = st.get(id);
        long now = server.getOverworld().getTime();
        st.prune(id, now);

        // отслеживаем приём пищи
        int food = p.getHungerManager().getFoodLevel();
        Integer prev = LAST_FOOD.get(id);
        if (prev != null && food > prev && !m.isEmpty()) food = onEat(p, m, prev, food);
        LAST_FOOD.put(id, food);

        Map<SideEffect, Long> fx = st.effects(id);
        for (SideEffect e : fx.keySet()) e.tick(p);
        if (m.isEmpty()) { LAST_FALL.remove(id); return; }

        int legs = cnt(m, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        int feet = cnt(m, BodyPart.LEFT_FOOT, BodyPart.RIGHT_FOOT);
        int lungs = cnt(m, BodyPart.LEFT_LUNG, BodyPart.RIGHT_LUNG);
        int kidneys = cnt(m, BodyPart.LEFT_KIDNEY, BodyPart.RIGHT_KIDNEY);
        int eyes = cnt(m, BodyPart.LEFT_EYE, BodyPart.RIGHT_EYE);
        boolean la = m.contains(BodyPart.LEFT_ARM), ra = m.contains(BodyPart.RIGHT_ARM);

        // хромота / ступни: урон при падении с небольшой высоты (ванильный урон начинается с 3 блоков)
        if (legs > 0 || feet > 0) {
            float last = LAST_FALL.getOrDefault(id, 0f);
            if (p.isOnGround()) {
                float threshold = legs > 0 ? 1.4f : 2.2f;
                if (last >= threshold && last < 3.2f && !p.getAbilities().creativeMode)
                    p.damage(p.getDamageSources().fall(), Math.min(5f, 1f + legs + feet));
                LAST_FALL.put(id, 0f);
            } else {
                LAST_FALL.put(id, Math.max(last, p.fallDistance));
            }
        }

        boolean noSprint = lungs > 0 || m.contains(BodyPart.TRACHEA);
        if (noSprint) p.setSprinting(false);

        if (la && !p.getOffHandStack().isEmpty()) {
            p.dropItem(p.getOffHandStack().copy(), false);
            p.getInventory().offHand.set(0, ItemStack.EMPTY);
        }
        if (la && ra && !p.getMainHandStack().isEmpty()) {
            p.dropItem(p.getMainHandStack().copy(), false);
            p.getInventory().setStack(p.getInventory().selectedSlot, ItemStack.EMPTY);
        }

        int a = p.age;
        if (m.contains(BodyPart.DIAPHRAGM) && a % 300 == 0) {
            p.setVelocity(p.getVelocity().add(0, 0.3, 0));
            p.velocityModified = true;
            bar(p, "Ик!");
        }
        if (m.contains(BodyPart.SPLEEN) && a % 2400 == 0 && p.getRandom().nextDouble() < 0.4) {
            effT(p, StatusEffects.POISON, 80, 0);
            bar(p, "Вы подхватили инфекцию.");
        }
        if (kidneys == 2 && a % 600 == 0) effT(p, StatusEffects.POISON, 60, 0);
        if (m.contains(BodyPart.NOSE) && m.contains(BodyPart.MOUTH) && a % 80 == 0 && p.getHealth() > 4f) {
            p.damage(p.getDamageSources().drown(), 1f);
            bar(p, "Нечем дышать!");
        }
        if (m.contains(BodyPart.BRONCHI) && p.isSprinting() && a % 40 == 0) effT(p, StatusEffects.SLOWNESS, 100, 1);
        if (m.contains(BodyPart.LIVER) && a % 600 == 0) effT(p, StatusEffects.NAUSEA, 120, 0);

        if (a % 20 != 0) return;

        if (p.isSubmergedIn(FluidTags.WATER)) {
            int drain = (m.contains(BodyPart.NOSE) ? 20 : 0) + lungs * 20;
            if (drain > 0) p.setAir(p.getAir() - drain);
        }
        if (legs == 1) eff(p, StatusEffects.SLOWNESS, 0);
        if (legs == 2) { eff(p, StatusEffects.SLOWNESS, 3); eff(p, StatusEffects.JUMP_BOOST, 128); }
        if (ra) { eff(p, StatusEffects.MINING_FATIGUE, 2); eff(p, StatusEffects.WEAKNESS, 1); }
        if (eyes == 2) eff(p, StatusEffects.BLINDNESS, 0);
        if (kidneys >= 1) eff(p, StatusEffects.HUNGER, 0);
        if (kidneys == 2) { eff(p, StatusEffects.HUNGER, 1); eff(p, StatusEffects.WEAKNESS, 0); }
        if (lungs >= 1) eff(p, StatusEffects.WEAKNESS, 0);
        if (lungs == 2) { eff(p, StatusEffects.WEAKNESS, 1); eff(p, StatusEffects.SLOWNESS, 1); }
        if (m.contains(BodyPart.TRACHEA)) eff(p, StatusEffects.SLOWNESS, 0);
        if (m.contains(BodyPart.THYROID)) { eff(p, StatusEffects.SLOWNESS, 0); eff(p, StatusEffects.MINING_FATIGUE, 0); }
        if (feet == 2) eff(p, StatusEffects.SLOWNESS, 0);
    }

    private static int cnt(Set<BodyPart> m, BodyPart a, BodyPart b) { return (m.contains(a) ? 1 : 0) + (m.contains(b) ? 1 : 0); }

    /** Последствия еды для пищеварительных органов. Возвращает итоговый уровень сытости. */
    private static int onEat(ServerPlayerEntity p, Set<BodyPart> m, int prev, int food) {
        int gain = food - prev;
        var hm = p.getHungerManager();
        if (m.contains(BodyPart.ESOPHAGUS)) {
            hm.setFoodLevel(prev);
            effT(p, StatusEffects.NAUSEA, 200, 0);
            effT(p, StatusEffects.HUNGER, 200, 0);
            bar(p, "Пища не дошла до желудка...");
            return prev;
        }
        double mult = 1.0;
        if (m.contains(BodyPart.STOMACH)) mult *= 0.5;
        if (m.contains(BodyPart.SMALL_INTESTINE)) mult *= 0.34;
        if (m.contains(BodyPart.TONGUE)) mult *= 0.7;
        int result = food;
        if (mult < 1.0) { result = prev + (int) Math.round(gain * mult); hm.setFoodLevel(result); }
        if (m.contains(BodyPart.LARGE_INTESTINE) && p.getRandom().nextDouble() < 0.5) {
            effT(p, StatusEffects.NAUSEA, 100, 0); effT(p, StatusEffects.HUNGER, 100, 0);
            bar(p, "Желудок бунтует.");
        }
        if (m.contains(BodyPart.GALLBLADDER)) effT(p, StatusEffects.NAUSEA, 100, 0);
        if (m.contains(BodyPart.PANCREAS)) { effT(p, StatusEffects.SLOWNESS, 300, 1); effT(p, StatusEffects.WEAKNESS, 300, 0); }
        return result;
    }
}
