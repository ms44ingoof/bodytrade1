package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TradeScreen extends Screen {
    private final BlockPos pos;

    public TradeScreen(BlockPos pos) { super(Text.literal("Алтарь обмена")); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Отдать часть тела (случайную)"), b -> send(BodyTradeMod.TRADE_PACKET, null))
                .dimensions(cx - 120, 70, 240, 20).build());
        int y = 115;
        for (BodyPart p : BodyTradeClient.MISSING) {
            Text label = Text.literal("Вернуть: " + p.nom + " - " + p.refundCount + " × ").append(new ItemStack(p.reward).getName());
            addDrawableChild(ButtonWidget.builder(label, b -> send(BodyTradeMod.RETURN_PACKET, p.id))
                    .dimensions(cx - 120, y, 240, 20).build());
            y += 22;
        }
    }

    private void send(Identifier id, String part) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        if (part != null) buf.writeString(part);
        ClientPlayNetworking.send(id, buf);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Отдай любую часть тела и получи что-то взамен."), width / 2, 38, 0xCCCCCC);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Часть выбирается случайно. Вернуть её можно, но дороже."), width / 2, 50, 0xAAAAAA);
        if (!BodyTradeClient.MISSING.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Ваши потери:"), width / 2, 100, 0xFF5555);
    }
}
