package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.SideEffect;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class InfoScreen extends Screen {
    private final Screen parent;

    public InfoScreen(Screen parent) { super(Text.literal("Болезни и части тела")); this.parent = parent; }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close()).dimensions(width / 2 - 40, height - 26, 80, 20).build());
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        int x = Math.max(10, width / 2 - 170), y = 8;
        ctx.drawTextWithShadow(textRenderer, "Части тела, которые могут забрать:", x, y, 0xFFFF55); y += 12;
        for (BodyPart p : BodyTradeClient.MISSING.isEmpty() ? java.util.List.of(BodyPart.values()) : java.util.List.of(BodyPart.values())) {
            boolean gone = BodyTradeClient.MISSING.contains(p);
            String reward = p.rewardCount + "× " + new ItemStack(p.reward).getName().getString();
            String line = p.nom + ": " + p.minChance + "-" + p.maxChance + "%  | дают " + reward
                    + (p.fatal ? "  | СМЕРТЬ" : "  | вернуть за " + p.refundCount + "×");
            ctx.drawTextWithShadow(textRenderer, line, x, y, gone ? 0xFF5555 : 0xFFFFFF); y += 10;
        }
        y += 6;
        ctx.drawTextWithShadow(textRenderer, "Побочные эффекты (шанс " + (int) (BodyTradeMod.SIDE_EFFECT_CHANCE * 100) + "% после обмена):", x, y, 0xFFFF55); y += 12;
        for (SideEffect s : SideEffect.values()) {
            ctx.drawTextWithShadow(textRenderer, s.name + " - " + s.message, x, y, 0xFFFFFF); y += 10;
        }
        y += 4;
        ctx.drawTextWithShadow(textRenderer, "Красным отмечено то, что у вас уже отсутствует.", x, y, 0xAAAAAA);
    }
}
