package com.example.bodytrade.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Общий "красивый" каркас для меню мода: тёмный фон с цветным свечением, рамка с орнаментом и свои кнопки
 * (обычные кнопки Minecraft остаются только для обработки кликов, а рисуются поверх нашим стилем).
 */
public abstract class FancyScreen extends Screen {
    protected final int accent, accent2;
    protected final List<ButtonWidget> fancy = new ArrayList<>();
    protected boolean leftAlign = false;
    protected int px, py, pw, ph;

    protected FancyScreen(Text title, int accent, int accent2) {
        super(title);
        this.accent = accent & 0xFFFFFF;
        this.accent2 = accent2 & 0xFFFFFF;
    }

    protected static int argb(int rgb, int a) { return (a << 24) | (rgb & 0xFFFFFF); }

    /** Размер и положение панели (по центру экрана, не больше экрана). */
    protected void panel(int w, int h) {
        pw = Math.min(w, width - 12);
        ph = Math.min(h, height - 12);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    /** Создаёт кнопку, которая будет нарисована в стиле мода. */
    protected ButtonWidget btn(String label, int x, int y, int w, int h, Runnable action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> action.run()).dimensions(x, y, w, h).build();
        fancy.add(b);
        return addDrawableChild(b);
    }

    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        drawBackdrop(ctx);
        drawPanel(ctx);
        super.render(ctx, mx, my, delta); // кнопки Minecraft (будут закрашены нашими)
        for (ButtonWidget b : fancy) drawFancyButton(ctx, b, mx, my);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, py + 9, 0xFFFFFFFF);
        drawContent(ctx, mx, my, delta);
    }

    protected void drawBackdrop(DrawContext c) {
        c.fillGradient(0, 0, width, height, 0xE0050509, 0xF2000000);
        c.fillGradient(0, 0, width, height / 2, argb(accent, 0x38), 0x00000000);
        c.fillGradient(0, height / 2, width, height, 0x00000000, argb(accent, 0x22));
    }

    protected void drawPanel(DrawContext c) {
        c.fill(px + 5, py + 5, px + pw + 5, py + ph + 5, 0x90000000);                  // тень
        c.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, argb(accent, 0xFF));          // внешняя рамка
        c.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF0A0A12);
        c.fillGradient(px, py, px + pw, py + ph, 0xFF1D1C2D, 0xFF0C0B15);              // фон
        c.fillGradient(px, py, px + pw, py + 24, argb(accent, 0xA0), 0x00000000);      // шапка
        c.fill(px + 14, py + 23, px + pw - 14, py + 24, argb(accent2, 0xFF));          // линия под заголовком
        c.fill(px + 4, py + ph - 5, px + pw - 4, py + ph - 4, argb(accent, 0x70));     // нижняя линия
        // уголки-орнаменты
        int s = 5;
        int[][] corners = {{px + 3, py + 3}, {px + pw - 3 - s, py + 3}, {px + 3, py + ph - 3 - s}, {px + pw - 3 - s, py + ph - 3 - s}};
        for (int[] cr : corners) {
            c.fill(cr[0], cr[1], cr[0] + s, cr[1] + s, argb(accent2, 0xFF));
            c.fill(cr[0] + 1, cr[1] + 1, cr[0] + s - 1, cr[1] + s - 1, 0xFF0A0A12);
        }
        // ромбики у заголовка
        c.fill(px + 8, py + 10, px + 12, py + 14, argb(accent2, 0xFF));
        c.fill(px + pw - 12, py + 10, px + pw - 8, py + 14, argb(accent2, 0xFF));
    }

    protected void drawFancyButton(DrawContext c, ButtonWidget b, int mx, int my) {
        int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
        boolean on = b.active;
        boolean hover = on && mx >= x && mx < x + w && my >= y && my < y + h;
        int top = !on ? 0xFF18181F : hover ? argb(blend(accent, 0xFF3A3A55, 0.35f), 0xFF) : 0xFF292939;
        int bot = !on ? 0xFF101016 : hover ? argb(blend(accent, 0xFF202034, 0.30f), 0xFF) : 0xFF181823;
        c.fillGradient(x, y, x + w, y + h, top, bot);
        int border = !on ? 0xFF2B2B36 : hover ? argb(accent2, 0xFF) : argb(accent, 0xFF);
        c.fill(x, y, x + w, y + 1, border);
        c.fill(x, y + h - 1, x + w, y + h, border);
        c.fill(x, y, x + 1, y + h, border);
        c.fill(x + w - 1, y, x + w, y + h, border);
        c.fill(x + 1, y + 1, x + 4, y + h - 1, !on ? 0xFF30303C : hover ? argb(accent2, 0xFF) : argb(accent, 0xFF));
        if (hover) c.fill(x + 4, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
        int color = !on ? 0xFF777788 : hover ? 0xFFFFFFFF : 0xFFE6E6F2;
        if (leftAlign) c.drawTextWithShadow(textRenderer, b.getMessage(), x + 9, y + (h - 8) / 2, color);
        else c.drawCenteredTextWithShadow(textRenderer, b.getMessage(), x + w / 2 + 2, y + (h - 8) / 2, color);
    }

    private static int blend(int rgb, int other, float t) {
        int r = (int) (((rgb >> 16) & 255) * t + ((other >> 16) & 255) * (1 - t));
        int g = (int) (((rgb >> 8) & 255) * t + ((other >> 8) & 255) * (1 - t));
        int b = (int) ((rgb & 255) * t + (other & 255) * (1 - t));
        return (r << 16) | (g << 8) | b;
    }
}
