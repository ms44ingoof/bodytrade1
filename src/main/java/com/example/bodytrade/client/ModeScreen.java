package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/** Выбор сложности при первом входе в мир. */
public class ModeScreen extends Screen {
    public ModeScreen() { super(Text.literal("Выберите сложность")); }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Обычная"), b -> pick("normal")).dimensions(cx - 110, 80, 220, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("РазВиВАЙСЯ"), b -> pick("develop")).dimensions(cx - 110, 130, 220, 24).build());
    }

    private void pick(String mode) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(mode);
        ClientPlayNetworking.send(BodyTradeMod.MODE_PICK_PACKET, buf);
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 30, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Части тела можно отдавать на алтаре обмена."), width / 2, 50, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Игра как есть: вы сами решаете, чем жертвовать."), width / 2, 108, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Вы начинаете без рук и ног. Выполняйте задания,"), width / 2, 158, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("получайте частицы жизни и покупайте конечности."), width / 2, 170, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Сменить сложность позже: /bodymode normal или /bodymode develop"), width / 2, 200, 0x777777);
    }
}
