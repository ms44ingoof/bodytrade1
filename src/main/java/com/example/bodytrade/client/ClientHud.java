package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.DevTasks;
import com.example.bodytrade.SideEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Красивый HUD: панели потерь, болезней, рассудка и заданий + виньетки при низком здоровье и рассудке. */
public final class ClientHud {
    private ClientHud() {}

    private static final Map<Item, ItemStack> ICONS = new HashMap<>();
    private static ItemStack icon(Item i) { return ICONS.computeIfAbsent(i, ItemStack::new); }

    private static int argb(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

        // полная темнота на стороне потерянного глаза
        boolean le = BodyTradeClient.MISSING.contains(BodyPart.LEFT_EYE), re = BodyTradeClient.MISSING.contains(BodyPart.RIGHT_EYE);
        if (le && !re) ctx.fill(0, 0, w / 2, h, 0xFF000000);
        if (re && !le) ctx.fill(w / 2, 0, w, h, 0xFF000000);

        vignettes(ctx, mc, w, h);

        long now = System.currentTimeMillis();
        BodyTradeClient.EFFECTS.values().removeIf(t -> t < now);
        int x = 6, y = 6;
        y = timerPanel(ctx, tr, x, y);
        y = lossesPanel(ctx, tr, x, y);
        y = diseasesPanel(ctx, tr, x, y, now);
        y = sanityPanel(ctx, tr, x, y);
        tasksPanel(ctx, tr, x, y);
    }

    // ------------------------------------------------------------------ виньетки
    private static void vignettes(DrawContext c, MinecraftClient mc, int w, int h) {
        var p = mc.player;
        float hp = p.getHealth() / Math.max(1f, p.getMaxHealth());
        long t = System.currentTimeMillis();
        if (hp <= 0.35f) { // потеря крови: красные пульсирующие края
            float k = (0.35f - hp) / 0.35f;
            double pulse = 0.5 + 0.5 * Math.sin(t / (hp <= 0.15f ? 180.0 : 320.0));
            int a = (int) (Math.min(1f, 0.35f + k * 0.65f) * (0.45 + 0.45 * pulse) * 200);
            edges(c, w, h, 0xCC1010, a, 48);
        }
        int s = BodyTradeClient.sanity;
        if (s < 400) { // рассудок: тёмно-фиолетовое мерцание
            float k = (400 - s) / 400f;
            int a = (int) (k * (0.5 + 0.3 * Math.sin(t / 260.0)) * 230);
            edges(c, w, h, 0x1A0030, a, 70);
        }
    }

    private static void edges(DrawContext c, int w, int h, int rgb, int alpha, int band) {
        for (int i = 0; i < band; i++) {
            float f = 1f - (float) i / band;
            int a = (int) (alpha * f * f);
            if (a <= 0) continue;
            int col = (a << 24) | rgb;
            c.fill(0, i, w, i + 1, col);
            c.fill(0, h - 1 - i, w, h - i, col);
            c.fill(i, 0, i + 1, h, col);
            c.fill(w - 1 - i, 0, w - i, h, col);
        }
    }

    // ------------------------------------------------------------------ панели
    private static void box(DrawContext c, int x, int y, int w, int h, int accent) {
        c.fillGradient(x, y, x + w, y + h, 0xB00B0D14, 0xB0050609);
        c.fill(x, y, x + 2, y + h, argb(accent, 0xFF));
        c.fill(x, y, x + w, y + 1, argb(accent, 0x70));
    }

    private static void header(DrawContext c, TextRenderer tr, int x, int y, int w, String title, String sub, int accent) {
        c.drawTextWithShadow(tr, title, x + 7, y + 3, argb(accent, 0xFF));
        int tw = tr.getWidth(title);
        if (sub != null) c.drawTextWithShadow(tr, " · " + sub, x + 7 + tw, y + 3, 0xFF8A90A0);
        c.fill(x + 6, y + 13, x + w - 4, y + 14, argb(accent, 0x90));
    }

    /** Значок предмета в 0.65 размера + количество справа. */
    private static void cost(DrawContext c, TextRenderer tr, int rightX, int y, Item item, int count) {
        String txt = "×" + count;
        int tx = rightX - tr.getWidth(txt);
        c.drawTextWithShadow(tr, txt, tx, y, 0xFFF0E6B0);
        var m = c.getMatrices();
        m.push();
        m.translate(tx - 12, y - 2, 0);
        m.scale(0.65f, 0.65f, 1f);
        c.drawItem(icon(item), 0, 0);
        m.pop();
    }

    /** Таймер распада: раз в 15 минут отнимается конечность или выдаётся болезнь (режим Перерождение). */
    private static int timerPanel(DrawContext c, TextRenderer tr, int x, int y) {
        int d = BodyTradeClient.decay;
        if (d == -1) return y;
        boolean off = d == -2;
        long t = System.currentTimeMillis();
        boolean blink = !off && d < 30 && (t / 300) % 2 == 0;
        int accent = off ? 0x55E08A : (d < 60 ? 0xFF4D4D : 0xFFB84D);
        int w = 170, h = 32;
        box(c, x, y, w, h, accent);
        header(c, tr, x, y, w, "ТАЙМЕР РАСПАДА", off ? "отключён" : null, accent);
        if (off) {
            c.drawTextWithShadow(tr, "Хроно-стабилизатор активен", x + 7, y + 19, 0xFF77FF88);
            return y + h + 4;
        }
        String time = String.format("%d:%02d", d / 60, d % 60);
        c.drawTextWithShadow(tr, time, x + w - 6 - tr.getWidth(time), y + 3, blink ? 0xFFFFFFFF : argb(accent, 0xFF));
        int bw = w - 14, by = y + 19;
        c.fill(x + 7, by, x + 7 + bw, by + 6, 0xFF1B1D26);
        float f = Math.max(0f, Math.min(1f, d / 900f));
        c.fill(x + 7, by, x + 7 + (int) (bw * f), by + 6, argb(accent, 0xFF));
        c.fill(x + 7, by, x + 7 + (int) (bw * f), by + 1, 0x40FFFFFF);
        return y + h + 4;
    }

    private static int lossesPanel(DrawContext c, TextRenderer tr, int x, int y) {
        if (BodyTradeClient.MISSING.isEmpty()) return y;
        List<BodyPart> list = new ArrayList<>(BodyTradeClient.MISSING);
        int rows = Math.min(list.size(), 12);
        String title = "ВАШИ ПОТЕРИ", sub = "цена возврата";
        int w = tr.getWidth(title + " · " + sub) + 16;
        for (int i = 0; i < rows; i++) w = Math.max(w, tr.getWidth(label(list.get(i))) + 62);
        int h = 18 + rows * 11 + (list.size() > rows ? 11 : 0) + 3;
        box(c, x, y, w, h, 0xE05555);
        header(c, tr, x, y, w, title, sub, 0xFF6B6B);
        int ry = y + 17;
        for (int i = 0; i < rows; i++) {
            BodyPart p = list.get(i);
            boolean pros = BodyTradeClient.PROSTHETIC.contains(p);
            c.drawTextWithShadow(tr, (i == 0 ? "> " : "  ") + label(p), x + 6, ry, pros ? 0xFF77DDFF : 0xFFF2F2F8);
            if (BodyTradeClient.develop && DevTasks.PRICES.containsKey(p)) cost(c, tr, x + w - 6, ry, BodyTradeMod.LIFE_PARTICLE, DevTasks.PRICES.get(p));
            else cost(c, tr, x + w - 6, ry, p.tier.refundItem, p.tier.refundCount);
            ry += 11;
        }
        if (list.size() > rows) c.drawTextWithShadow(tr, "  ...и ещё " + (list.size() - rows), x + 6, ry, 0xFF8A90A0);
        return y + h + 4;
    }

    private static String label(BodyPart p) { return p.nom + (BodyTradeClient.PROSTHETIC.contains(p) ? " [протез]" : ""); }

    private static int diseasesPanel(DrawContext c, TextRenderer tr, int x, int y, long now) {
        if (BodyTradeClient.EFFECTS.isEmpty()) return y;
        String title = "ВАШИ БОЛЕЗНИ", sub = "осталось";
        int w = tr.getWidth(title + " · " + sub) + 16;
        for (String n : BodyTradeClient.EFFECTS.keySet()) w = Math.max(w, tr.getWidth("> " + n) + 52);
        int rows = BodyTradeClient.EFFECTS.size();
        int h = 18 + rows * 13 + 3;
        box(c, x, y, w, h, 0xE6C84A);
        header(c, tr, x, y, w, title, sub, 0xFFD966);
        int ry = y + 17;
        boolean first = true;
        for (var e : BodyTradeClient.EFFECTS.entrySet()) {
            long left = Math.max(0, (e.getValue() - now) / 1000);
            c.drawTextWithShadow(tr, (first ? "> " : "  ") + e.getKey(), x + 6, ry, 0xFFF2F2F8);
            String t = left + " с";
            c.drawTextWithShadow(tr, t, x + w - 6 - tr.getWidth(t), ry, 0xFFE8D28A);
            // полоска оставшегося времени
            int total = 60;
            for (SideEffect s : SideEffect.values()) if (s.title.equals(e.getKey())) { total = s.seconds; break; }
            float f = Math.max(0f, Math.min(1f, left / (float) total));
            int bw = w - 14;
            c.fill(x + 7, ry + 9, x + 7 + bw, ry + 11, 0xFF1B1D26);
            int col = f > 0.5f ? 0xFF6BD66B : f > 0.2f ? 0xFFE6C84A : 0xFFE05555;
            c.fill(x + 7, ry + 9, x + 7 + (int) (bw * f), ry + 11, col);
            ry += 13;
            first = false;
        }
        return y + h + 4;
    }

    private static int sanityPanel(DrawContext c, TextRenderer tr, int x, int y) {
        int s = BodyTradeClient.sanity;
        if (s >= 995 && BodyTradeClient.IMPLANTS.isEmpty() && !BodyTradeClient.develop) return y; // не мешаем, пока всё спокойно
        int w = 150, h = 32;
        box(c, x, y, w, h, 0xB07CFF);
        header(c, tr, x, y, w, "РАССУДОК", null, 0xB07CFF);
        String pct = (s / 10) + "%";
        c.drawTextWithShadow(tr, pct, x + w - 6 - tr.getWidth(pct), y + 3, 0xFFE6E6F2);
        int bw = w - 14, by = y + 19;
        c.fill(x + 7, by, x + 7 + bw, by + 6, 0xFF1B1D26);
        float f = s / 1000f;
        int col = f > 0.6f ? 0xFF6BD66B : f > 0.3f ? 0xFFE6C84A : 0xFFE05555;
        c.fill(x + 7, by, x + 7 + (int) (bw * f), by + 6, col);
        c.fill(x + 7, by, x + 7 + (int) (bw * f), by + 1, 0x40FFFFFF);
        return y + h + 4;
    }

    private static void tasksPanel(DrawContext c, TextRenderer tr, int x, int y) {
        if (!BodyTradeClient.develop || BodyTradeClient.taskProg.length == 0) return;
        var tasks = DevTasks.tasks();
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < tasks.size() && i < BodyTradeClient.taskProg.length; i++) if (!BodyTradeClient.taskDone[i]) open.add(i);
        open.sort((a, b) -> Integer.compare(tasks.get(a).reward(), tasks.get(b).reward())); // от лёгких к сложным
        int rows = Math.min(3, open.size());
        String title = "ЗАДАНИЯ", sub = BodyTradeClient.tasksDoneCount() + "/" + tasks.size();
        int w = tr.getWidth(title + " · " + sub) + 16;
        for (int k = 0; k < rows; k++) {
            var t = tasks.get(open.get(k));
            w = Math.max(w, tr.getWidth(t.text() + " (" + t.need() + "/" + t.need() + ")") + 48);
        }
        int h = 18 + Math.max(1, rows) * 11 + 3;
        box(c, x, y, w, h, 0x55E08A);
        header(c, tr, x, y, w, title, sub, 0x55E08A);
        int ry = y + 17;
        if (rows == 0) c.drawTextWithShadow(tr, "Все задания выполнены!", x + 6, ry, 0xFFFFD700);
        for (int k = 0; k < rows; k++) {
            int i = open.get(k);
            var t = tasks.get(i);
            c.drawTextWithShadow(tr, (k == 0 ? "> " : "  ") + t.text() + " (" + BodyTradeClient.taskProg[i] + "/" + t.need() + ")", x + 6, ry, 0xFFF2F2F8);
            cost(c, tr, x + w - 6, ry, BodyTradeMod.LIFE_PARTICLE, t.reward());
            ry += 11;
        }
    }
}
