package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.DevTasks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/** Блок развития (зелёная тема): покупка конечностей за частицы жизни. */
public class DevScreen extends FancyScreen {
    private final BlockPos pos;

    public DevScreen(BlockPos pos) { super(Text.literal("Блок развития"), 0x2ECC71, 0xF7E26B); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        fancy.clear();
        panel(300, 236);
        int cx = width / 2, y = py + 82;
        for (Map.Entry<BodyPart, Integer> e : DevTasks.PRICES.entrySet()) {
            BodyPart p = e.getKey();
            boolean missing = BodyTradeClient.MISSING.contains(p);
            ButtonWidget b = btn(p.nom + " - " + e.getValue() + " ч. жизни" + (missing ? "" : " (на месте)"), cx - 115, y, 230, 20, () -> buy(p));
            b.active = missing;
            y += 23;
        }
        btn("Список заданий", cx - 115, y + 4, 230, 20, () -> client.setScreen(new TasksScreen(this)));
        btn("Закрыть", cx - 50, y + 30, 100, 20, this::close);
    }

    private void buy(BodyPart p) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(p.id);
        ClientPlayNetworking.send(BodyTradeMod.DEV_BUY_PACKET, buf);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int have = client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Частиц жизни у вас: " + have), width / 2, py + 32, 0xFF77FF88);
        if (BodyTradeClient.develop)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Заданий выполнено: " + BodyTradeClient.tasksDoneCount() + "/" + DevTasks.tasks().size()), width / 2, py + 46, 0xFFE8E8F0);
        else
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Обычная сложность (режим: /bodymode develop)"), width / 2, py + 46, 0xFF9A9AAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Купить конечность:"), width / 2, py + 66, argb(accent2, 0xFF));
    }
}
