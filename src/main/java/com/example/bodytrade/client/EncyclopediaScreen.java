package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.Lore;
import com.example.bodytrade.SideEffect;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Энциклопедия: записи открываются, когда вы впервые теряете часть тела или заболеваете. */
public class EncyclopediaScreen extends FancyScreen {
    private record Entry(String key, String name, boolean header, BodyPart part, SideEffect fx) {}

    private static final int LH = 11, LIST_W = 190;
    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private int selected = -1;
    private int scroll = 0;

    public EncyclopediaScreen(Screen parent) { super(Text.literal("Энциклопедия"), 0x4DA3FF, 0xFFD166); this.parent = parent; }

    private boolean open(Entry e) { return !e.header() && BodyTradeClient.LORE.contains(e.key()); }

    @Override
    protected void init() {
        fancy.clear();
        entries.clear();
        panel(520, 1000);
        entries.add(new Entry("", "ЧАСТИ ТЕЛА", true, null, null));
        for (BodyPart p : BodyPart.values()) entries.add(new Entry("part:" + p.id, p.nom, false, p, null));
        entries.add(new Entry("", "БОЛЕЗНИ", true, null, null));
        for (SideEffect s : SideEffect.values()) entries.add(new Entry("fx:" + s.name(), s.title, false, null, s));
        btn("Назад", width / 2 - 50, py + ph - 27, 100, 20, this::close);
    }

    private int top() { return py + 36; }
    private int bottom() { return py + ph - 32; }
    private int listX() { return px + 10; }
    private int maxScroll() { return Math.max(0, entries.size() * LH - (bottom() - top())); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= listX() && mx < listX() + LIST_W && my >= top() && my < bottom()) {
            int idx = (int) ((my - top() + scroll) / LH);
            if (idx >= 0 && idx < entries.size() && !entries.get(idx).header()) { selected = idx; return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 22));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }

    private void paragraph(DrawContext ctx, String text, int x, int y, int w, int color, int[] yOut) {
        for (OrderedText l : textRenderer.wrapLines(Text.literal(text), w)) {
            ctx.drawTextWithShadow(textRenderer, l, x, y, color);
            y += 10;
        }
        yOut[0] = y;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int opened = 0, total = 0;
        for (Entry e : entries) if (!e.header()) { total++; if (open(e)) opened++; }
        ctx.drawTextWithShadow(textRenderer, "Открыто записей: " + opened + " из " + total, px + 12, py + 25, 0xFF8A90A0);

        // список слева
        ctx.fill(listX() - 2, top() - 2, listX() + LIST_W + 2, bottom() + 2, 0xFF0A0B11);
        ctx.enableScissor(listX(), top(), listX() + LIST_W, bottom());
        int y = top() - scroll;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (y > top() - LH && y < bottom()) {
                if (e.header()) {
                    ctx.drawTextWithShadow(textRenderer, e.name(), listX() + 4, y + 1, argb(accent2, 0xFF));
                } else {
                    boolean o = open(e);
                    if (i == selected) ctx.fill(listX(), y - 1, listX() + LIST_W, y + LH - 1, argb(accent, 0x55));
                    ctx.drawTextWithShadow(textRenderer, (o ? e.name() : "??? (закрыто)"), listX() + 10, y + 1, o ? 0xFFF2F2F8 : 0xFF5C6275);
                }
            }
            y += LH;
        }
        ctx.disableScissor();

        // запись справа
        int rx = listX() + LIST_W + 16, rw = px + pw - 12 - rx;
        int ry = top();
        if (selected < 0) {
            paragraph(ctx, "Выберите запись слева. Новые записи открываются, когда вы впервые теряете часть тела или заболеваете.", rx, ry, rw, 0xFF9A9AAA, new int[1]);
            return;
        }
        Entry e = entries.get(selected);
        if (!open(e)) {
            ctx.drawTextWithShadow(textRenderer, "Запись закрыта", rx, ry, 0xFF5C6275);
            paragraph(ctx, e.part() != null ? "Откроется, когда вы впервые потеряете эту часть тела." : "Откроется, когда вы впервые заболеете этой болезнью.",
                    rx, ry + 14, rw, 0xFF8A90A0, new int[1]);
            return;
        }
        String[] lore = e.part() != null ? Lore.part(e.part()) : Lore.effect(e.fx());
        ctx.drawTextWithShadow(textRenderer, e.name(), rx, ry, argb(accent2, 0xFF));
        ctx.fill(rx, ry + 11, rx + rw, ry + 12, argb(accent, 0xA0));
        int[] yo = new int[1];
        ctx.drawTextWithShadow(textRenderer, "История", rx, ry + 17, 0xFF77DDFF);
        paragraph(ctx, lore[0], rx, ry + 28, rw, 0xFFE6E6F2, yo);
        ctx.drawTextWithShadow(textRenderer, "Совет", rx, yo[0] + 6, 0xFF77FF88);
        paragraph(ctx, lore[1], rx, yo[0] + 17, rw, 0xFFE6E6F2, yo);
        String eff = e.part() != null ? e.part().consequence : e.fx().message;
        ctx.drawTextWithShadow(textRenderer, "Последствия", rx, yo[0] + 6, 0xFFFF9A6B);
        paragraph(ctx, eff, rx, yo[0] + 17, rw, 0xFFE6E6F2, yo);
        if (e.part() != null) {
            ctx.drawTextWithShadow(textRenderer, "Шанс при обмене: " + e.part().minChance + "-" + e.part().maxChance + "%  |  ценность награды: " + e.part().tier.title,
                    rx, yo[0] + 8, 0xFF8A90A0);
        } else {
            ctx.drawTextWithShadow(textRenderer, "Длительность: " + e.fx().seconds + " с" + (e.fx().isRare() ? "  |  РЕДКАЯ" : ""), rx, yo[0] + 8, 0xFF8A90A0);
        }
    }
}
