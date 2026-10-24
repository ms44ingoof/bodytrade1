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

import java.util.ArrayList;
import java.util.List;

public class TradeScreen extends Screen {
    private static final int PER_PAGE = 6;
    private final BlockPos pos;
    private int page = 0;

    public TradeScreen(BlockPos pos) { super(Text.literal("Алтарь обмена")); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Отдать часть тела (случайную)"), b -> send(BodyTradeMod.TRADE_PACKET, null))
                .dimensions(cx - 120, 70, 240, 20).build());
        List<BodyPart> list = new ArrayList<>(BodyTradeClient.MISSING);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int y = 115;
        for (int i = page * PER_PAGE; i < Math.min(list.size(), (page + 1) * PER_PAGE); i++) {
            BodyPart p = list.get(i);
            Text label = Text.literal("Вернуть: " + p.nom + " - " + p.tier.refundCount + " × ").append(new ItemStack(p.tier.refundItem).getName());
            addDrawableChild(ButtonWidget.builder(label, b -> send(BodyTradeMod.RETURN_PACKET, p.id)).dimensions(cx - 120, y, 240, 20).build());
            y += 22;
        }
        if (pages > 1) {
            int by = 115 + PER_PAGE * 22 + 4;
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); }).dimensions(cx - 60, by, 40, 20).build()).active = page > 0;
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); }).dimensions(cx + 20, by, 40, 20).build()).active = page < pages - 1;
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
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Часть и награда выбираются случайно. Вернуть часть можно, но дороже."), width / 2, 50, 0xAAAAAA);
        if (!BodyTradeClient.MISSING.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Ваши потери (" + BodyTradeClient.MISSING.size() + "):"), width / 2, 100, 0xFF5555);
    }
}
