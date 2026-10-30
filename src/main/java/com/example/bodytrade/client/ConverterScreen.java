package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Синтезатор жизни (бирюзовая тема): алмазы -> частицы жизни по курсу 1:1. */
public class ConverterScreen extends FancyScreen {
    private final BlockPos pos;
    private int lastD = -1, lastP = -1;

    public ConverterScreen(BlockPos pos) { super(Text.literal("Синтезатор жизни"), 0x33E6FF, 0xFFD166); this.pos = pos; }

    private int diamonds() { return client != null && client.player != null ? client.player.getInventory().count(Items.DIAMOND) : 0; }
    private int particles() { return client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0; }

    @Override
    protected void init() {
        fancy.clear();
        panel(320, 220);
        int cx = width / 2, d = diamonds();
        lastD = d; lastP = particles();
        int y = py + 78;
        int[] amounts = {1, 8, 64};
        for (int a : amounts) {
            ButtonWidget b = btn("Обменять " + a + (a == 1 ? " алмаз" : " алмазов"), cx - 100, y, 200, 22, () -> send(a));
            b.active = d >= a;
            y += 26;
        }
        ButtonWidget all = btn("Обменять все (" + d + ")", cx - 100, y, 200, 22, () -> send(0));
        all.active = d > 0;
        btn("Закрыть", cx - 50, py + ph - 28, 100, 20, this::close);
    }

    private void send(int amount) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeVarInt(amount);
        ClientPlayNetworking.send(BodyTradeMod.CONVERT_PACKET, buf);
    }

    @Override
    public void tick() {
        super.tick();
        if (diamonds() != lastD || particles() != lastP) clearAndInit(); // обновить кнопки после обмена
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Курс обмена: 1 алмаз = 1 частица жизни"), width / 2, py + 32, 0xFFE6E6F2);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Алмазов: " + diamonds() + "     Частиц жизни: " + particles()), width / 2, py + 48, 0xFF77FF88);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Выберите, сколько обменять:"), width / 2, py + 64, argb(accent2, 0xFF));
    }
}
