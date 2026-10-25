package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.DevTasks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/** Блок развития: покупка конечностей за частицы жизни. */
public class DevScreen extends Screen {
    private final BlockPos pos;

    public DevScreen(BlockPos pos) { super(Text.literal("Блок развития")); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        int cx = width / 2, y = 78;
        for (Map.Entry<BodyPart, Integer> e : DevTasks.PRICES.entrySet()) {
            BodyPart p = e.getKey();
            boolean missing = BodyTradeClient.MISSING.contains(p);
            String label = p.nom + " - " + e.getValue() + " ч. жизни" + (missing ? "" : " (на месте)");
            ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> buy(p)).dimensions(cx - 110, y, 220, 20).build();
            b.active = missing;
            addDrawableChild(b);
            y += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Список заданий"), b -> client.setScreen(new TasksScreen(this)))
                .dimensions(cx - 110, y + 6, 220, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(cx - 50, y + 32, 100, 20).build());
    }

    private void buy(BodyPart p) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(p.id);
        ClientPlayNetworking.send(BodyTradeMod.DEV_BUY_PACKET, buf);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        int have = client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Частиц жизни у вас: " + have), width / 2, 32, 0x77FF77);
        if (BodyTradeClient.develop)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Заданий выполнено: " + BodyTradeClient.tasksDoneCount() + "/" + DevTasks.tasks().size()), width / 2, 46, 0xFFFFFF);
        else
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Вы на обычной сложности (режим: /bodymode develop)."), width / 2, 46, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Купить конечность:"), width / 2, 64, 0xFFFF55);
    }
}
