package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.DevTasks;
import com.example.bodytrade.Tier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * «Возврат» (режим Перерождение): список потерь карточками с ценой и кнопкой «Вернуть».
 * Конечности стоят частиц жизни (как в блоке развития), остальное - предметов по ценности части.
 */
public class ReturnScreen extends FancyScreen {
    private static final int ROW_H = 38, ROW_GAP = 4;
    private final Screen parent;
    private int first = 0;                       // первая видимая карточка
    private int visibleRows = 4;
    private final List<BodyPart> rows = new ArrayList<>();

    public ReturnScreen(Screen parent) { super(Text.literal("Возврат"), 0xE05555, 0xF7E26B); this.parent = parent; }

    public void refresh() { clearAndInit(); }

    @Override
    public void close() { client.setScreen(parent); }

    private static Item priceItem(BodyPart p) { return DevTasks.PRICES.containsKey(p) ? BodyTradeMod.LIFE_PARTICLE : p.tier.refundItem; }
    private static int priceCount(BodyPart p) { return DevTasks.PRICES.containsKey(p) ? DevTasks.PRICES.get(p) : p.tier.refundCount; }
    private int have(Item it) { return client != null && client.player != null ? client.player.getInventory().count(it) : 0; }

    private int listTop() { return py + 62; }

    @Override
    protected void init() {
        fancy.clear();
        panel(400, 300);
        rows.clear();
        rows.addAll(BodyTradeClient.MISSING);
        visibleRows = Math.max(1, (ph - 62 - 40) / (ROW_H + ROW_GAP));
        first = Math.max(0, Math.min(first, Math.max(0, rows.size() - visibleRows)));
        for (int i = first; i < Math.min(rows.size(), first + visibleRows); i++) {
            BodyPart p = rows.get(i);
            int y = listTop() + (i - first) * (ROW_H + ROW_GAP);
            ButtonWidgetHolder.add(this, btn("Вернуть", px + pw - 16 - 74, y + (ROW_H - 20) / 2, 74, 20, () -> send(p)),
                    have(priceItem(p)) >= priceCount(p));
        }
        btn("Закрыть", px + pw / 2 - 45, py + ph - 28, 90, 20, this::close);
    }

    private void send(BodyPart p) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(p.id);
        ClientPlayNetworking.send(BodyTradeMod.INV_RETURN_PACKET, buf);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        int max = Math.max(0, rows.size() - visibleRows);
        int nf = (int) Math.max(0, Math.min(max, first - Math.signum(amount)));
        if (nf != first) { first = nf; clearAndInit(); }
        return true;
    }

    private static int tierColor(Tier t) {
        return switch (t) { case COMMON -> 0xB0B4C0; case UNCOMMON -> 0xF0C050; case VALUABLE -> 0x55D8F0; case RARE -> 0xC070FF; };
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        // шапка: сколько частиц жизни и алмазов у вас
        int lp = have(BodyTradeMod.LIFE_PARTICLE);
        String s1 = "Частицы жизни: " + lp;
        ctx.drawTextWithShadow(textRenderer, s1, px + 18, py + 32, 0xFF77FF88);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Верните то, что потеряли"), px + pw / 2 + 40, py + 32, 0xFFB8B8C8);
        ctx.fill(px + 14, py + 50, px + pw - 14, py + 51, argb(accent, 0x90));

        if (rows.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Всё на месте"), px + pw / 2, py + ph / 2 - 14, 0xFF77FF88);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("У вас нет потерь, возвращать нечего."), px + pw / 2, py + ph / 2 - 2, 0xFF9A9AAA);
            return;
        }
        for (int i = first; i < Math.min(rows.size(), first + visibleRows); i++) {
            BodyPart p = rows.get(i);
            int y = listTop() + (i - first) * (ROW_H + ROW_GAP);
            int x0 = px + 14, x1 = px + pw - 14;
            boolean hover = mx >= x0 && mx < x1 && my >= y && my < y + ROW_H;
            ctx.fillGradient(x0, y, x1, y + ROW_H, hover ? 0xFF272638 : 0xFF1B1B29, hover ? 0xFF181826 : 0xFF11111B);
            ctx.fill(x0, y, x0 + 3, y + ROW_H, 0xFF000000 | tierColor(p.tier));
            ctx.fill(x0 + 3, y, x1, y + 1, 0x30FFFFFF);
            boolean pros = BodyTradeClient.PROSTHETIC.contains(p);
            ctx.drawTextWithShadow(textRenderer, p.nom, x0 + 10, y + 5, pros ? 0xFF77DDFF : 0xFFFFFFFF);
            int nw = textRenderer.getWidth(p.nom);
            ctx.drawTextWithShadow(textRenderer, (pros ? "протез · " : "") + p.tier.title, x0 + 16 + nw, y + 5, 0xFF000000 | tierColor(p.tier));
            // последствие потери (одна-две строки серым)
            int textW = (x1 - x0) - 10 - 92 - 8;
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(p.consequence), textW);
            for (int k = 0; k < Math.min(2, lines.size()); k++)
                ctx.drawTextWithShadow(textRenderer, lines.get(k), x0 + 10, y + 16 + k * 10, 0xFF8A8AA0);
            // цена: значок предмета и количество (красным, если не хватает)
            Item it = priceItem(p);
            int need = priceCount(p), got = have(it);
            String price = "×" + need + "  (" + got + ")";
            int priceX = x1 - 92 - textRenderer.getWidth(price) - 12;
            ctx.drawItem(new ItemStack(it), priceX - 18, y + (ROW_H - 16) / 2);
            ctx.drawTextWithShadow(textRenderer, price, priceX, y + (ROW_H - 8) / 2, got >= need ? 0xFF77FF88 : 0xFFFF7777);
        }
        if (rows.size() > visibleRows) {
            // полоса прокрутки
            int top = listTop(), h = visibleRows * (ROW_H + ROW_GAP) - ROW_GAP;
            int sx = px + pw - 10;
            ctx.fill(sx, top, sx + 3, top + h, 0xFF1B1B29);
            int knob = Math.max(12, h * visibleRows / rows.size());
            int ky = top + (h - knob) * first / Math.max(1, rows.size() - visibleRows);
            ctx.fill(sx, ky, sx + 3, ky + knob, argb(accent2, 0xFF));
        }
    }

    /** Помогает выключить кнопку «Вернуть», если не хватает предметов. */
    private static final class ButtonWidgetHolder {
        static void add(ReturnScreen s, net.minecraft.client.gui.widget.ButtonWidget b, boolean enough) { b.active = enough; }
    }
}
