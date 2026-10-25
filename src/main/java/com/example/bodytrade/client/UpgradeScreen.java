package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.Upgrade;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Блок апгрейда: прокачка органов и конечностей за частицы жизни. Описание - при наведении на кнопку. */
public class UpgradeScreen extends Screen {
    private final BlockPos pos;
    private final List<int[]> rects = new ArrayList<>(); // x, y, w, h, индекс апгрейда

    public UpgradeScreen(BlockPos pos) { super(Text.literal("Блок апгрейда")); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    private int level(Upgrade u) { return u.ordinal() < BodyTradeClient.upgrades.length ? BodyTradeClient.upgrades[u.ordinal()] : 0; }

    @Override
    protected void init() {
        rects.clear();
        int cx = width / 2;
        Upgrade[] all = Upgrade.values();
        for (int i = 0; i < all.length; i++) {
            Upgrade u = all[i];
            int lv = level(u);
            int x = cx - 155 + (i % 2) * 160, y = 50 + (i / 2) * 22;
            String label = u.nom + " [" + lv + "/" + u.max() + "]" + (lv >= u.max() ? " макс." : " - " + u.costs[lv] + " ч.");
            ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> buy(u)).dimensions(x, y, 150, 20).build();
            b.active = lv < u.max();
            addDrawableChild(b);
            rects.add(new int[]{x, y, 150, 20, i});
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(cx - 50, 50 + 6 * 22 + 40, 100, 20).build());
    }

    private void buy(Upgrade u) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(u.id);
        ClientPlayNetworking.send(BodyTradeMod.UPG_BUY_PACKET, buf);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        int have = client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Частиц жизни у вас: " + have), width / 2, 24, 0x77FF77);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Наведите на улучшение, чтобы прочитать описание"), width / 2, 36, 0xAAAAAA);
        for (int[] r : rects) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                Upgrade u = Upgrade.values()[r[4]];
                int y = 50 + 6 * 22 + 2;
                for (OrderedText line : textRenderer.wrapLines(Text.literal(u.desc), Math.min(width - 20, 320))) {
                    ctx.drawCenteredTextWithShadow(textRenderer, line, width / 2, y, 0xFFFFAA);
                    y += 10;
                }
                break;
            }
        }
    }
}
