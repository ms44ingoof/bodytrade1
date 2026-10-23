package com.example.bodytrade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BodyTradeMod implements ModInitializer {
    public static final String ID = "bodytrade";
    public static final Identifier TRADE_PACKET = new Identifier(ID, "trade");
    public static final Identifier RETURN_PACKET = new Identifier(ID, "return");
    public static final Identifier SYNC_PACKET = new Identifier(ID, "sync");

    /** Шанс побочного эффекта после каждой сделки. */
    public static final double SIDE_EFFECT_CHANCE = 0.4;
    /** Восстанавливать все части тела после смерти. */
    public static final boolean RESTORE_ON_DEATH = true;

    public static Block TRADE_BLOCK;
    private static final Map<UUID, Float> LAST_FALL = new HashMap<>();

    @Override
    public void onInitialize() {
        TRADE_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "trade_altar"),
                  new TradeBlock(FabricBlockSettings.create().strength(3.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "trade_altar"), new BlockItem(TRADE_BLOCK, new FabricItemSettings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> e.add(TRADE_BLOCK));

        // Первый вход в мир: выдаём блок-алтарь + синхронизируем состояние
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (BodyState.of(server).markGaveBlock(p.getUuid())) {
                p.getInventory().offerOrDrop(new ItemStack(TRADE_BLOCK));
                p.sendMessage(Text.literal("Вам выдан Алтарь обмена. Поставьте его и нажмите ПКМ.").formatted(Formatting.GRAY));
            }
            sync(p, List.of());
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            if (!alive && RESTORE_ON_DEATH) {
                BodyState.of(newP.getServer()).clear(newP.getUuid());
                sync(newP, List.of());
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

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) tickPlayer(server, p);
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
        p.getInventory().offerOrDrop(new ItemStack(part.reward, part.rewardCount));
        title(p, Text.literal("Вы лишились " + part.gen).formatted(Formatting.DARK_RED),
                Text.literal("Получено: " + part.rewardCount + " × ").append(new ItemStack(part.reward).getName()).formatted(Formatting.GRAY));
        p.sendMessage(Text.literal(part.consequence).formatted(part.fatal ? Formatting.DARK_RED : Formatting.RED));

        if (part.fatal) { // мозг - мгновенная смерть (награда выпадет вместе с инвентарём)
            p.kill();
            return;
        }
        st.add(p.getUuid(), part);

        List<String[]> effects = new ArrayList<>();
        if (p.getRandom().nextDouble() < SIDE_EFFECT_CHANCE) {
            SideEffect se = SideEffect.values()[p.getRandom().nextInt(SideEffect.values().length)];
            int sec = se.apply(p);
            p.sendMessage(Text.literal(se.name + ": " + se.message).formatted(Formatting.YELLOW));
            effects.add(new String[]{se.name, String.valueOf(sec)});
        }
        sync(p, effects);
    }

    private static void handleReturn(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, BodyPart part) {
        if (part == null || !nearAltar(p, pos)) return;
        BodyState st = BodyState.of(server);
        if (!st.get(p.getUuid()).contains(part)) return;
        var inv = p.getInventory();
        if (inv.count(part.reward) < part.refundCount) {
            p.sendMessage(Text.literal("Не хватает: нужно " + part.refundCount + " × ").append(new ItemStack(part.reward).getName()).formatted(Formatting.RED));
            return;
        }
        int left = part.refundCount;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(part.reward)) { int t = Math.min(left, s.getCount()); s.decrement(t); left -= t; }
        }
        st.remove(p.getUuid(), part);
        title(p, Text.literal("Вы вернули " + part.nom.toLowerCase()).formatted(Formatting.GREEN), Text.literal("Последствия сняты").formatted(Formatting.GRAY));
        p.sendMessage(Text.literal("Часть тела возвращена: " + part.nom + ". Связанные последствия исчезли.").formatted(Formatting.GREEN));
        sync(p, List.of());
    }

    private static void title(ServerPlayerEntity p, Text title, Text sub) {
        var h = p.networkHandler;
        h.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
        h.sendPacket(new SubtitleS2CPacket(sub));
        h.sendPacket(new TitleS2CPacket(title));
    }

    public static void sync(ServerPlayerEntity p, List<String[]> effects) {
        Set<BodyPart> m = BodyState.of(p.getServer()).get(p.getUuid());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(m.size());
        for (BodyPart bp : m) buf.writeString(bp.id);
        buf.writeVarInt(effects.size());
        for (String[] e : effects) { buf.writeString(e[0]); buf.writeVarInt(Integer.parseInt(e[1])); }
        ServerPlayNetworking.send(p, SYNC_PACKET, buf);
    }

    // ---------------------------------------------------------------- последствия
    private static void eff(ServerPlayerEntity p, StatusEffect e, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false, true));
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayerEntity p) {
        Set<BodyPart> m = BodyState.of(server).get(p.getUuid());
        if (m.isEmpty()) { LAST_FALL.remove(p.getUuid()); return; }
        int legs = (m.contains(BodyPart.LEFT_LEG) ? 1 : 0) + (m.contains(BodyPart.RIGHT_LEG) ? 1 : 0);
        boolean la = m.contains(BodyPart.LEFT_ARM), ra = m.contains(BodyPart.RIGHT_ARM);
        int eyes = (m.contains(BodyPart.LEFT_EYE) ? 1 : 0) + (m.contains(BodyPart.RIGHT_EYE) ? 1 : 0);

        // Хромота: урон при падении с 1.5+ блока (ванильный урон начинается только с 3)
        if (legs > 0) {
            float last = LAST_FALL.getOrDefault(p.getUuid(), 0f);
            if (p.isOnGround()) {
                if (last >= 1.4f && last < 3.2f && !p.getAbilities().creativeMode)
                    p.damage(p.getDamageSources().fall(), legs == 2 ? 3f : 2f);
                LAST_FALL.put(p.getUuid(), 0f);
            } else {
                LAST_FALL.put(p.getUuid(), Math.max(last, p.fallDistance));
            }
        }

        if (m.contains(BodyPart.LUNG)) p.setSprinting(false);
        if (la && !p.getOffHandStack().isEmpty()) {
            p.dropItem(p.getOffHandStack().copy(), false);
            p.getInventory().offHand.set(0, ItemStack.EMPTY);
        }
        if (la && ra && !p.getMainHandStack().isEmpty()) {
            p.dropItem(p.getMainHandStack().copy(), false);
            p.getInventory().setStack(p.getInventory().selectedSlot, ItemStack.EMPTY);
        }

        if (p.age % 20 != 0) return;
        if (legs == 1) eff(p, StatusEffects.SLOWNESS, 0);
        if (legs == 2) { eff(p, StatusEffects.SLOWNESS, 3); eff(p, StatusEffects.JUMP_BOOST, 128); }
        if (ra) { eff(p, StatusEffects.MINING_FATIGUE, 2); eff(p, StatusEffects.WEAKNESS, 1); }
        if (eyes == 2) eff(p, StatusEffects.BLINDNESS, 0);
        if (m.contains(BodyPart.KIDNEY)) eff(p, StatusEffects.HUNGER, 0);
        if (m.contains(BodyPart.LUNG)) { eff(p, StatusEffects.WEAKNESS, 0); }
        if (m.contains(BodyPart.LIVER) && p.age % 600 == 0)
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120));
    }
}
