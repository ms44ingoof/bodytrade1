package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.TradeBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;

public class BodyTradeClient implements ClientModInitializer {
    public static final Set<BodyPart> MISSING = EnumSet.noneOf(BodyPart.class);
    public static final Map<String, Long> EFFECTS = new LinkedHashMap<>();

    @Override
    public void onInitializeClient() {
        TradeBlock.CLIENT_OPEN = pos -> MinecraftClient.getInstance().setScreen(new TradeScreen(pos));

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.SYNC_PACKET, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            Set<BodyPart> set = EnumSet.noneOf(BodyPart.class);
            for (int i = 0; i < n; i++) { BodyPart p = BodyPart.byId(buf.readString()); if (p != null) set.add(p); }
            int k = buf.readVarInt();
            Map<String, Integer> eff = new LinkedHashMap<>();
            for (int i = 0; i < k; i++) eff.put(buf.readString(), buf.readVarInt());
            client.execute(() -> {
                MISSING.clear(); MISSING.addAll(set);
                long now = System.currentTimeMillis();
                eff.forEach((name, sec) -> EFFECTS.put(name, now + sec * 1000L));
                if (client.currentScreen instanceof TradeScreen s) s.refresh();
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { MISSING.clear(); EFFECTS.clear(); });

        // Меню слева сверху + затемнение половины экрана при потере глаза
        HudRenderCallback.EVENT.register((ctx, delta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden || mc.player == null) return;
            int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
            boolean le = MISSING.contains(BodyPart.LEFT_EYE), re = MISSING.contains(BodyPart.RIGHT_EYE);
            if (le && !re) ctx.fill(0, 0, w / 2, h, 0xE6000000);
            if (re && !le) ctx.fill(w / 2, 0, w, h, 0xE6000000);

            long now = System.currentTimeMillis();
            EFFECTS.values().removeIf(t -> t < now);
            int y = 5;
            if (!MISSING.isEmpty()) {
                ctx.drawTextWithShadow(mc.textRenderer, "Отсутствует:", 5, y, 0xFF5555); y += 10;
                for (BodyPart p : MISSING) { ctx.drawTextWithShadow(mc.textRenderer, "- " + p.nom, 5, y, 0xFFFFFF); y += 10; }
            }
            if (!EFFECTS.isEmpty()) {
                y += 3;
                ctx.drawTextWithShadow(mc.textRenderer, "Побочные эффекты:", 5, y, 0xFFFF55); y += 10;
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
