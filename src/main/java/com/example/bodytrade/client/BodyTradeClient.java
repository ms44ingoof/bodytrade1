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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
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
    public static final Map<UUID, Integer> LIMBS = BodyTradeMod.CLIENT_LIMBS;
    /** Маска установленных протезов у всех игроков. */
    public static final Map<UUID, Integer> PROS = BodyTradeMod.CLIENT_PROS;
    /** Режим РазВиВАЙСЯ: текущее задание. */
    public static boolean develop;
    public static int[] taskProg = new int[0];
    public static boolean[] taskDone = new boolean[0];
    /** Уровни апгрейдов (индекс = Upgrade.ordinal()). */
    public static int[] upgrades = new int[0];
    public static boolean drillOn = true;
    /** Рассудок в десятых долях процента (0..1000). */
    public static int sanity = 1000;
    public static final Set<String> IMPLANTS = new HashSet<>();
    public static final Set<String> LORE = new HashSet<>();
    public static KeyBinding DRILL_KEY;

    public static int tasksDoneCount() { int c = 0; for (boolean d : taskDone) if (d) c++; return c; }

    /** Деменция: все названия в игре превращаются в иероглифы. */
    public static boolean dementiaActive() {
        Long t = EFFECTS.get("Деменция");
        return t != null && t > System.currentTimeMillis();
    }

    private static final String GLYPHS = "ЖЩЪЫЭЮЯѦѪѬѮѰѲΞΨΩΣΦΔΛØÞÐßŊŦĦÆŒЉЊЋЏҨӜӠ";

    private static int glyph(int cp, int index, long tick) {
        long h = cp * 31L + index * 17L + tick * 2654435761L;
        h ^= (h >>> 13); h *= 0x9E3779B1L; h ^= (h >>> 15);
        return GLYPHS.charAt((int) ((h & 0x7fffffffL) % GLYPHS.length()));
    }

    /** Деменция: буквы постоянно меняются на нечитаемые (применяется при отрисовке текста). */
    public static String scrambleLive(String s) {
        if (s == null) return null;
        long tick = System.currentTimeMillis() / 90;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7') { sb.append(c); if (i + 1 < s.length()) sb.append(s.charAt(++i)); continue; } // коды форматирования
            sb.append(Character.isLetter(c) ? (char) glyph(c, i, tick) : c);
        }
        return sb.toString();
    }

    public static net.minecraft.text.OrderedText scrambleOrdered(net.minecraft.text.OrderedText t) {
        if (t == null) return null;
        long tick = System.currentTimeMillis() / 90;
        return visitor -> t.accept((index, style, cp) -> visitor.accept(index, style, Character.isLetter(cp) ? glyph(cp, index, tick) : cp));
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
        com.example.bodytrade.UpgradeBlock.CLIENT_OPEN = pos -> MinecraftClient.getInstance().setScreen(new UpgradeScreen(pos));
        com.example.bodytrade.DevBlock.CLIENT_OPEN = pos -> MinecraftClient.getInstance().setScreen(new DevScreen(pos));
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
            boolean dev = buf.readBoolean();
            int[] tp = new int[0]; boolean[] td = new boolean[0];
            if (dev) {
                int taskCount = buf.readVarInt();
                tp = new int[taskCount]; td = new boolean[taskCount];
                for (int i = 0; i < taskCount; i++) { tp[i] = buf.readVarInt(); td[i] = buf.readBoolean(); }
            }
            int nu = buf.readVarInt();
            int[] up = new int[nu];
            for (int i = 0; i < nu; i++) up[i] = buf.readVarInt();
            boolean dOn = buf.readBoolean();
            int implCount = buf.readVarInt();
            Set<String> implSet = new HashSet<>();
            for (int i = 0; i < implCount; i++) implSet.add(buf.readString());
            int loreCount = buf.readVarInt();
            Set<String> loreSet = new HashSet<>();
            for (int i = 0; i < loreCount; i++) loreSet.add(buf.readString());
            final int[] ftp = tp; final boolean[] ftd = td;
            client.execute(() -> {
                develop = dev; taskProg = ftp; taskDone = ftd; upgrades = up; drillOn = dOn;
                IMPLANTS.clear(); IMPLANTS.addAll(implSet); LORE.clear(); LORE.addAll(loreSet);
                MISSING.clear(); MISSING.addAll(set);
                PROSTHETIC.clear(); PROSTHETIC.addAll(pset);
                EFFECTS.clear();
                long now = System.currentTimeMillis();
                eff.forEach((name, sec) -> EFFECTS.put(name, now + sec * 1000L));
                if (client.currentScreen instanceof TradeScreen s) s.refresh();
                else if (client.currentScreen instanceof AdminScreen a) a.refresh();
                else if (client.currentScreen instanceof ShopScreen sh) sh.refresh();
                else if (client.currentScreen instanceof DevScreen d) d.refresh();
                else if (client.currentScreen instanceof UpgradeScreen u) u.refresh();
                else if (client.currentScreen instanceof ImplantScreen im) im.refresh();
                else if (client.currentScreen instanceof TasksScreen ts) ts.refresh();
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

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.SANITY_PACKET, (client, handler, buf, sender) -> {
            int v = buf.readVarInt();
            client.execute(() -> sanity = v);
        });

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.MODE_OPEN_PACKET, (client, handler, buf, sender) ->
                client.execute(() -> client.setScreen(new ModeScreen())));

        ClientPlayNetworking.registerGlobalReceiver(BodyTradeMod.SHOP_PACKET, (client, handler, buf, sender) -> {
            int entityId = buf.readVarInt();
            client.execute(() -> client.setScreen(new ShopScreen(entityId)));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
            MISSING.clear(); PROSTHETIC.clear(); EFFECTS.clear(); LIMBS.clear(); PROS.clear(); develop = false; upgrades = new int[0]; sanity = 1000; IMPLANTS.clear(); LORE.clear();
        });

        // клавиша B - включить/выключить бур
        DRILL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bodytrade.drill", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.bodytrade"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (DRILL_KEY.wasPressed()) {
                if (mc.player == null) break;
                net.minecraft.network.PacketByteBuf kb = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                kb.writeString("drill");
                ClientPlayNetworking.send(BodyTradeMod.TOGGLE_PACKET, kb);
            }
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

        // HUD: потери, болезни, рассудок, задания, виньетки
        HudRenderCallback.EVENT.register((ctx, delta) -> ClientHud.render(ctx));

        // Кнопка в инвентаре
        ScreenEvents.AFTER_INIT.register((client, screen, sw, sh) -> {
            if (screen instanceof InventoryScreen) {
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Болезни и части тела"),
                        b -> client.setScreen(new InfoScreen(screen))).dimensions(sw / 2 - 88 - 116, sh / 2 - 80, 110, 20).build());
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Энциклопедия"),
                        b -> client.setScreen(new EncyclopediaScreen(screen))).dimensions(sw / 2 - 88 - 116, sh / 2 - 32, 110, 20).build());
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("Задания"),
                        b -> client.setScreen(new TasksScreen(screen))).dimensions(sw / 2 - 88 - 116, sh / 2 - 56, 110, 20).build());
            }
        });
    }
}
