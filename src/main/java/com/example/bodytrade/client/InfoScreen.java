package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.SideEffect;
import com.example.bodytrade.Tier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class InfoScreen extends Screen {
    private record Line(OrderedText text, int color) {}

    private final Screen parent;
    private final List<Line> lines = new ArrayList<>();
    private int scroll = 0;
    private static final int TOP = 24, LH = 10;

    public InfoScreen(Screen parent) { super(Text.literal("Болезни и части тела")); this.parent = parent; }

    private void add(String s, int color, int indent) {
        int w = Math.min(width - 40, 420) - indent;
        for (OrderedText t : textRenderer.wrapLines(Text.literal(s), w)) lines.add(new Line(t, color));
    }

    @Override
    protected void init() {
        lines.clear();
        add("ЧАСТИ ТЕЛА (шанс в %, относительный; красным - уже отсутствует)", 0xFFFF55, 0);
        for (BodyPart p : BodyPart.values()) {
            boolean gone = BodyTradeClient.MISSING.contains(p);
            add(p.nom + ": " + p.minChance + "-" + p.maxChance + "% | награда: " + p.tier.title + " | вернуть: "
                    + p.tier.refundCount + "× " + new ItemStack(p.tier.refundItem).getName().getString(), gone ? 0xFF5555 : 0xFFFFFF, 0);
            add("   " + p.consequence, 0xAAAAAA, 10);
        }
        add(" ", 0xFFFFFF, 0);
        add("НАГРАДЫ (предмет и количество выбираются случайно из пула)", 0xFFFF55, 0);
        for (Tier t : Tier.values()) {
            StringBuilder sb = new StringBuilder(t.title + ": ");
            for (int i = 0; i < t.pool.length; i++) {
                Tier.Reward r = t.pool[i];
                if (i > 0) sb.append(", ");
                sb.append(new ItemStack(r.item()).getName().getString()).append(' ').append(r.min());
                if (r.max() != r.min()) sb.append('-').append(r.max());
            }
            add(sb.toString(), 0xFFFFFF, 0);
        }
        add(" ", 0xFFFFFF, 0);
        add("БОЛЕЗНИ И ПОБОЧНЫЕ ЭФФЕКТЫ (после каждого обмена шанс " + (int) (BodyTradeMod.SIDE_EFFECT_CHANCE * 100)
                + "%, конкретная болезнь выбирается случайно)", 0xFFFF55, 0);
        for (SideEffect s : SideEffect.values()) {
            add(s.title + " (" + s.seconds + " с)" + (s.isRare() ? " [РЕДКАЯ, шанс 0.5% за обмен]" : "") + ": " + s.message,
                    s.isRare() ? 0xFF5555 : 0xFFFFFF, 0);
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close()).dimensions(width / 2 - 40, height - 24, 80, 20).build());
    }

    private int maxScroll() { return Math.max(0, lines.size() * LH - (height - TOP - 30)); }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 20));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
        int x = Math.max(10, width / 2 - 210);
        ctx.enableScissor(0, TOP, width, height - 30);
        int y = TOP - scroll;
        for (Line l : lines) {
            if (y > TOP - LH && y < height - 30) ctx.drawTextWithShadow(textRenderer, l.text(), x, y, l.color());
            y += LH;
        }
        ctx.disableScissor();
        super.render(ctx, mx, my, delta);
        if (maxScroll() > 0) ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Колесо мыши - прокрутка"), width / 2 + 150, height - 18, 0x888888);
    }
}
