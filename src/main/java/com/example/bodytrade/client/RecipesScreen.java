package com.example.bodytrade.client;

import com.example.bodytrade.FlashRecipes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;

/** Рецепты призмы, флешек и модуля апгрейда. Прокрутка колесом мыши. */
public class RecipesScreen extends FancyScreen {
    private static final int ROW_H = 50;
    private final Screen parent;
    private int scroll = 0;
    private List<FlashRecipes.Entry> entries = List.of();

    public RecipesScreen(Screen parent) { super(Text.literal("Рецепты"), 0xC04DFF, 0xFFD166); this.parent = parent; }

    @Override
    protected void init() {
        fancy.clear();
        entries = FlashRecipes.all();
        panel(470, 1000);
        btn("Назад", width / 2 - 50, py + ph - 27, 100, 20, this::close);
    }

    private int top() { return py + 30; }
    private int bottom() { return py + ph - 32; }
    private int maxScroll() { return Math.max(0, entries.size() * ROW_H - (bottom() - top())); }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 24));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        ctx.enableScissor(px + 4, top(), px + pw - 4, bottom());
        int y = top() - scroll;
        for (FlashRecipes.Entry e : entries) {
            if (y > top() - ROW_H && y < bottom()) {
                int x = px + 10, w = pw - 20;
                ctx.fillGradient(x, y, x + w, y + ROW_H - 4, 0xFF181B27, 0xFF0E1018);
                int border = argb(accent, 0xA0);
                ctx.fill(x, y, x + w, y + 1, border); ctx.fill(x, y + ROW_H - 5, x + w, y + ROW_H - 4, border);
                ctx.fill(x + 6, y + 8, x + 30, y + 32, 0xFF07080D);
                ctx.drawItem(e.result(), x + 10, y + 12);
                ctx.drawTextWithShadow(textRenderer, e.name(), x + 38, y + 4, argb(accent2, 0xFF));
                int ix = x + 38;
                for (ItemStack k : e.ingredients()) {
                    ctx.drawItem(k, ix, y + 15);
                    String t = "×" + k.getCount();
                    ctx.drawTextWithShadow(textRenderer, t, ix + 17, y + 20, 0xFFF0E6B0);
                    ix += 17 + textRenderer.getWidth(t) + 9;
                }
                if (!e.note().isEmpty()) {
                    for (OrderedText l : textRenderer.wrapLines(Text.literal(e.note()), w - 46)) {
                        ctx.drawTextWithShadow(textRenderer, l, x + 38, y + 34, 0xFF8A90A0);
                        break;
                    }
                }
            }
            y += ROW_H;
        }
        ctx.disableScissor();
        if (maxScroll() > 0) ctx.drawTextWithShadow(textRenderer, "колесо мыши - прокрутка", px + pw - 16 - textRenderer.getWidth("колесо мыши - прокрутка"), py + ph - 14, 0xFF77778A);
    }
}
