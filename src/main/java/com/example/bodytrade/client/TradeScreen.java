package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Алтарь обмена (багровая тема). */
public class TradeScreen extends FancyScreen {
    private final BlockPos pos;
    private int page = 0;

    public TradeScreen(BlockPos pos) { super(Text.literal("Алтарь обмена"), 0xC0392B, 0xE8C77A); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        fancy.clear();
        panel(320, 280);
        int cx = width / 2;
        btn("Отдать часть тела (случайную)", cx - 130, py + 58, 260, 24, () -> send(BodyTradeMod.TRADE_PACKET, null));
        int per = Math.max(2, Math.min(6, (ph - 112 - 34) / 22));
        List<BodyPart> list = new ArrayList<>(BodyTradeClient.MISSING);
        int pages = Math.max(1, (list.size() + per - 1) / per);
        if (page >= pages) page = pages - 1;
        int y = py + 112;
        for (int i = page * per; i < Math.min(list.size(), (page + 1) * per); i++) {
            BodyPart p = list.get(i);
            btn("Вернуть: " + p.nom + " - " + p.tier.refundCount + " × " + new ItemStack(p.tier.refundItem).getName().getString(),
                    cx - 130, y, 260, 20, () -> send(BodyTradeMod.RETURN_PACKET, p.id));
            y += 22;
        }
        if (pages > 1) {
            int by = py + ph - 28;
            ButtonWidget prev = btn("<", cx - 60, by, 40, 20, () -> { page--; clearAndInit(); });
            ButtonWidget next = btn(">", cx + 20, by, 40, 20, () -> { page++; clearAndInit(); });
            prev.active = page > 0;
            next.active = page < pages - 1;
        }
    }

    private void send(Identifier id, String part) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        if (part != null) buf.writeString(part);
        ClientPlayNetworking.send(id, buf);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Отдай часть тела и получи что-то взамен."), width / 2, py + 30, 0xFFE0D0D0);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Часть и награда выбираются случайно. Вернуть можно, но дороже."), width / 2, py + 42, 0xFF9A8A8A);
        if (!BodyTradeClient.MISSING.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Ваши потери (" + BodyTradeClient.MISSING.size() + "):"), width / 2, py + 96, argb(accent2, 0xFF));
    }
}
