package com.example.bodytrade.client;

import com.example.bodytrade.AdminBlock;
import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.TradeBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BodyTradeClient implements ClientModInitializer {
    public static final Set<BodyPart> MISSING = BodyTradeMod.CLIENT_MISSING;
    /** Части, заменённые протезом. */
    public static final Set<BodyPart> PROSTHETIC = EnumSet.noneOf(BodyPart.class);
    /** Название болезни -> время окончания (мс). */
    public static final Map<String, Long> EFFECTS = new LinkedHashMap<>();
    /** Маска пропавших конечностей у всех игроков (для отрисовки модели). */
    public static final Map<UUID, Integer> LIMBS = new HashMap<>();
    /** Маска установленных протезов у всех игроков. */
    public static final Map<UUID, Integer> PROS = new HashMap<>();

    /** Деменция: все названия в игре превращаются в иероглифы. */
    public static boolean dementiaActive() {
        Long t = EFFECTS.get("Деменция");
        return t != null && t > System.currentTimeMillis();
    }

    public static String scramble(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '%') { // не трогаем подстановки вида %s, %1$s, %d
                int j = i + 1;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '$')) j++;
                if (j < s.length()) j++;
                sb.append(s, i, j);
                i = j;
                continue;
            }
            if (Character.isLetter(c)) {
                int h = Math.abs((c * 31 + 7) * 2654435);
                sb.append((char) (0x4E00 + h % 0x1F00));
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BodyTradeMod.SMILER, ctx ->
                new BillboardRenderer(ctx, new Identifier(BodyTradeMod.ID, "textures/entity/smiler.png"), 1.15f, 2.0f));
        EntityRendererRegistry.register(BodyTradeMod.SCREAMER, ctx ->
                new BillboardRenderer(ctx, new Identifier(BodyTradeMod.ID, "textures/entity/screamer.png"), 2.3f, 2.2f));
        EntityModelLayerRegistry.registerModelLayer(ProsthetistModel.LAYER, ProsthetistModel::getTexturedModelData);
        EntityRendererRegistry.register(BodyTradeMod.PROSTHETIST, ProsthetistRenderer::new);

        // металлические протезы на модели игрока
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer pr) helper.register(new ProsthesisFeature(pr, ctx.getModelLoader()));
        });

        AdminBlock.CLIENT_OPEN = pos -> MinecraftClient.getInstance().setScreen(new AdminScreen(pos));
        TradeBlock.CLIENT_OPEN = pos -> MinecraftClient.getInstance().setScreen(new TradeScreen(pos));

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.SYNC_PACKET, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            Set<BodyPart> set = EnumSet.noneOf(BodyPart.class);
            for (int i = 0; i < n; i++) { BodyPart p = BodyPart.byId(buf.readString()); if (p != null) set.add(p); }
            int np = buf.readVarInt();
            Set<BodyPart> pset = EnumSet.noneOf(BodyPart.class);
            for (int i = 0; i < np; i++) { BodyPart p = BodyPart.byId(buf.readString()); if (p != null) pset.add(p); }
            int k = buf.readVarInt();
            Map<String, Integer> eff = new LinkedHashMap<>();
            for (int i = 0; i < k; i++) eff.put(buf.readString(), buf.readVarInt());
            client.execute(() -> {
                MISSING.clear(); MISSING.addAll(set);
                PROSTHETIC.clear(); PROSTHETIC.addAll(pset);
                EFFECTS.clear();
                long now = System.currentTimeMillis();
                eff.forEach((name, sec) -> EFFECTS.put(name, now + sec * 1000L));
                if (client.currentScreen instanceof TradeScreen s) s.refresh();
                else if (client.currentScreen instanceof AdminScreen a) a.refresh();
                else if (client.currentScreen instanceof ShopScreen sh) sh.refresh();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.LIMBS_PACKET, (client, handler, buf, sender) -> {
            UUID who = buf.readUuid();
            int mask = buf.readVarInt();
            int pmask = buf.readVarInt();
            client.execute(() -> {
                if (mask == 0) LIMBS.remove(who); else LIMBS.put(who, mask);
                if (pmask == 0) PROS.remove(who); else PROS.put(who, pmask);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.SHOP_PACKET, (client, handler, buf, sender) -> {
            int entityId = buf.readVarInt();
            client.execute(() -> client.setScreen(new ShopScreen(entityId)));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
            MISSING.clear(); PROSTHETIC.clear(); EFFECTS.clear(); LIMBS.clear(); PROS.clear();
        });

        // Без ступней - скольжение, как по льду
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            var p = mc.player;
            if (p == null) return;
            int feet = (MISSING.contains(BodyPart.LEFT_FOOT) ? 1 : 0) + (MISSING.contains(BodyPart.RIGHT_FOOT) ? 1 : 0);
            if (feet == 0 || !p.isOnGround()) return;
            if (p.input.movementForward != 0 || p.input.movementSideways != 0) return;
            Vec3d v = p.getVelocity();
            double s = Math.hypot(v.x, v.z);
            if (s < 0.003 || s > 0.6) return;
            double k = feet == 2 ? 1.6 : 1.3;
            p.setVelocity(v.x * k, v.y, v.z * k);
        });

        // Меню слева сверху + полная темнота на стороне потерянного глаза
        HudRenderCallback.EVENT.register((ctx, delta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.player == null) return;
            int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
            boolean le = MISSING.contains(BodyPart.LEFT_EYE), re = MISSING.contains(BodyPart.RIGHT_EYE);
            if (le && !re) ctx.fill(0, 0, w / 2, h, 0xFF000000);
            if (re && !le) ctx.fill(w / 2, 0, w, h, 0xFF000000);

            long now = System.currentTimeMillis();
            EFFECTS.values().removeIf(t -> t < now);
            int y = 5, shown = 0;
            if (!MISSING.isEmpty()) {
                ctx.drawTextWithShadow(mc.textRenderer, "Отсутствует:", 5, y, 0xFF5555); y += 10;
                for (BodyPart p : MISSING) {
                    if (shown >= 14) { ctx.drawTextWithShadow(mc.textRenderer, "...и ещё " + (MISSING.size() - shown), 5, y, 0xAAAAAA); y += 10; break; }
                    String label = "- " + p.nom + (PROSTHETIC.contains(p) ? " (протез)" : "");
                    ctx.drawTextWithShadow(mc.textRenderer, label, 5, y, PROSTHETIC.contains(p) ? 0x77DDFF : 0xFFFFFF); y += 10; shown++;
                }
            }
            if (!EFFECTS.isEmpty()) {
                y += 3;
                ctx.drawTextWithShadow(mc.textRenderer, "Болезни и эффекты:", 5, y, 0xFFFF55); y += 10;
                for (var e : EFFECTS.entrySet()) {
                    long sec = (e.getValue() - now) / 1000;
                    ctx.drawTextWithShadow(mc.textRenderer, "- " + e.getKey() + " (" + sec + " с)", 5, y, 0xFFFFFF); y += 10;
                }
            }
        });

        // Кнопка в инвентаре
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof InventoryScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Болезни и части тела"),
                        b -> client.setScreen(new InfoScreen(screen))).dimensions(sw / 2 + 92, sh / 2 - 80, 110, 20).build());
            }
        });
    }
}
