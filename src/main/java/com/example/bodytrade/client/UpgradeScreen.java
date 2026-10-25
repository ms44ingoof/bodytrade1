package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.Upgrade;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Блок апгрейда (фиолетово-золотая тема): уровни показаны ячейками, ветки открываются после 3 уровня. */
public class UpgradeScreen extends FancyScreen {
    private final BlockPos pos;
    private final List<int[]> rects = new ArrayList<>(); // x, y, w, h, индекс апгрейда

    public UpgradeScreen(BlockPos pos) { super(Text.literal("Блок апгрейда"), 0x9B59F6, 0xFFD166); this.pos = pos; this.leftAlign = true; }

    public void refresh() { clearAndInit(); }

    private int level(Upgrade u) { return u.ordinal() < BodyTradeClient.upgrades.length ? BodyTradeClient.upgrades[u.ordinal()] : 0; }

    private boolean locked(Upgrade u) {
        if (u.requiresId == null) return false;
        Upgrade r = Upgrade.byId(u.requiresId);
        return r != null && level(r) < u.requiresLevel;
    }

    @Override
    protected void init() {
        fancy.clear();
        rects.clear();
        panel(380, 296);
        int cx = width / 2;
        Upgrade[] all = Upgrade.values();
        for (int i = 0; i < all.length; i++) {
            Upgrade u = all[i];
            int x = cx - 182 + (i % 2) * 185, y = py + 52 + (i / 2) * 20;
            ButtonWidget b = btn((u.isBranch() ? "> " : "") + u.nom, x, y, 179, 18, () -> buy(u));
            b.active = level(u) < u.max() && !locked(u);
            rects.add(new int[]{x, y, 179, 18, i});
        }
        int by = py + ph - 27;
        if (level(Upgrade.DRILL) > 0) {
            btn("Бур: " + (BodyTradeClient.drillOn ? "ВКЛ" : "ВЫКЛ") + "  [B]", cx - 182, by, 179, 20, this::toggleDrill);
            btn("Закрыть", cx + 3, by, 179, 20, this::close);
        } else {
            btn("Закрыть", cx - 60, by, 120, 20, this::close);
        }
    }

    private void buy(Upgrade u) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(u.id);
        ClientPlayNetworking.send(BodyTradeMod.UPG_BUY_PACKET, buf);
    }

    private void toggleDrill() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("drill");
        ClientPlayNetworking.send(BodyTradeMod.TOGGLE_PACKET, buf);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int have = client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Частиц жизни у вас: " + have), width / 2, py + 30, 0xFF77FF88);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Наведите на улучшение, чтобы прочитать описание. Ветки (>) открываются после 3 уровня."), width / 2, py + 41, 0xFF9A9AAA);
        Upgrade[] all = Upgrade.values();
        int hoverIdx = -1;
        for (int[] r : rects) {
            Upgrade u = all[r[4]];
            int lv = level(u), max = u.max();
            boolean lock = locked(u);
            int pipW = max * 8;
            int pipX = r[0] + r[2] - 6 - pipW;
            for (int k = 0; k < max; k++) {
                int x = pipX + k * 8;
                ctx.fill(x, r[1] + 6, x + 6, r[1] + 12, 0xFF0A0A12);
                ctx.fill(x + 1, r[1] + 7, x + 5, r[1] + 11, k < lv ? argb(accent2, 0xFF) : 0xFF2C2C3A);
            }
            String right;
            int rc;
            if (lock) { Upgrade req = Upgrade.byId(u.requiresId); right = req.nom + " " + u.requiresLevel + " ур."; rc = 0xFFFF6666; }
            else if (lv >= max) { right = "макс."; rc = 0xFF77FF88; }
            else { right = u.costs[lv] + " ч."; rc = 0xFFD8D8E8; }
            ctx.drawTextWithShadow(textRenderer, right, pipX - 6 - textRenderer.getWidth(right), r[1] + 5, rc);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) hoverIdx = r[4];
        }
        if (hoverIdx >= 0) {
            Upgrade u = all[hoverIdx];
            int y = py + 52 + 7 * 20 + 6;
            ctx.fill(px + 10, y - 3, px + pw - 10, y + 33, 0x80000000);
            for (OrderedText line : textRenderer.wrapLines(Text.literal(u.nom + ": " + u.desc), pw - 30)) {
                ctx.drawTextWithShadow(textRenderer, line, px + 15, y, 0xFFFFE9A8);
                y += 10;
            }
        }
    }
}
