package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.Upgrade;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.Map;

/**
 * Блок апгрейда: дерево улучшений. Слева "Организм", от него расходятся ветки; каждая следующая ступень
 * открывается после максимума предыдущей. Поле двигается мышью (зажать и тянуть) и колёсиком.
 */
public class UpgradeScreen extends FancyScreen {
    private static final int COLW = 158, ROWH = 40, R = 13, LABEL_W = 106, NODE_H = 26, PAD = 28, ROWS = 11;
    private static final int GOLD = 0xFFFFD166, CYAN = 0xFF3DD6FF, VIOLET = 0xFFB07CFF, GRAY = 0xFF3A3F4A;

    private final BlockPos pos;
    private final Map<Upgrade, ItemStack> icons = new EnumMap<>(Upgrade.class);
    private final ItemStack hubIcon = new ItemStack(Items.PLAYER_HEAD);
    private Upgrade selected = null;
    private double panX = PAD, panY = PAD;
    private boolean dragging = false;
    private int vx, vy, vw, vh; // область дерева

    public UpgradeScreen(BlockPos pos) {
        super(Text.literal("Блок апгрейда - дерево улучшений"), 0x9B59F6, 0xFFD166);
        this.pos = pos;
    }

    public void refresh() { clearAndInit(); }

    private int level(Upgrade u) { return u.ordinal() < BodyTradeClient.upgrades.length ? BodyTradeClient.upgrades[u.ordinal()] : 0; }

    private boolean locked(Upgrade u) {
        if (u.requiresId == null) return false;
        Upgrade r = Upgrade.byId(u.requiresId);
        return r != null && level(r) < u.requiresLevel;
    }

    private ItemStack icon(Upgrade u) { return icons.computeIfAbsent(u, k -> new ItemStack(k.icon)); }

    @Override
    protected void init() {
        fancy.clear();
        panel(width, height);
        vx = px + 8; vy = py + 36; vw = pw - 16; vh = ph - 36 - 100;
        clampPan();
        int bx = px + pw - 8 - 200, by = py + ph - 96;
        Upgrade s = selected;
        String label;
        boolean canBuy = false;
        if (s == null) label = "Выберите улучшение";
        else if (level(s) >= s.max()) label = "Максимальный уровень";
        else if (locked(s)) label = "Закрыто";
        else { label = "Улучшить - " + s.costs[level(s)] + " ч. жизни"; canBuy = true; }
        btn("Импланты", px + pw - 108, py + 5, 100, 16, () -> client.setScreen(new ImplantScreen(pos, this)));
        ButtonWidget buy = btn(label, bx, by, 200, 22, this::buySelected);
        buy.active = canBuy;
        if (level(Upgrade.DRILL) > 0) btn("Бур: " + (BodyTradeClient.drillOn ? "ВКЛ" : "ВЫКЛ") + "  [B]", bx, by + 26, 200, 20, this::toggleDrill);
        btn("Закрыть", bx, by + 50, 200, 20, this::close);
    }

    private void clampPan() {
        double treeW = 3 * COLW + R + LABEL_W + PAD;
        double treeH = (ROWS - 1) * ROWH + R + PAD;
        panX = Math.max(Math.min(PAD, vw - treeW), Math.min(PAD, panX));
        panY = Math.max(Math.min(PAD, vh - treeH), Math.min(PAD, panY));
    }

    private void buySelected() {
        if (selected == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(selected.id);
        ClientPlayNetworking.send(BodyTradeMod.UPG_BUY_PACKET, buf);
    }

    private void toggleDrill() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("drill");
        ClientPlayNetworking.send(BodyTradeMod.TOGGLE_PACKET, buf);
    }

    // ------------------------------------------------------------------ координаты
    private int cx(int col) { return vx + (int) panX + col * COLW; }
    private int cy(int row) { return vy + (int) panY + row * ROWH; }
    private int hubCy() { return cy((ROWS - 1) / 2); }

    private Upgrade nodeAt(int mx, int my) {
        for (Upgrade u : Upgrade.values()) {
            int x = cx(u.col), y = cy(u.row);
            int dx = mx - x, dy = my - y;
            boolean inDisc = dx * dx + dy * dy <= (R + 2) * (R + 2);
            int lx = x + R + 3, ly = y - NODE_H / 2;
            boolean inLabel = mx >= lx && mx < lx + LABEL_W && my >= ly && my < ly + NODE_H;
            if (inDisc || inLabel) return u;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= vx && mx < vx + vw && my >= vy && my < vy + vh) {
            Upgrade hit = nodeAt((int) mx, (int) my);
            if (hit != null) { selected = hit; clearAndInit(); return true; }
            dragging = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) { panX += dx; panY += dy; clampPan(); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        panY += amount * 34;
        clampPan();
        return true;
    }

    // ------------------------------------------------------------------ рисование
    private static void disc(DrawContext c, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, color);
        }
    }

    private static void hline(DrawContext c, int x1, int x2, int y, int color) { c.fill(Math.min(x1, x2), y - 1, Math.max(x1, x2) + 1, y + 1, color); }
    private static void vline(DrawContext c, int x, int y1, int y2, int color) { c.fill(x - 1, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, color); }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int have = client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0;
        ctx.drawTextWithShadow(textRenderer, "Частиц жизни: " + have, px + 12, py + 9, 0xFF77FF88);

        // область дерева
        ctx.fill(vx, vy, vx + vw, vy + vh, 0xFF07080D);
        ctx.enableScissor(vx, vy, vx + vw, vy + vh);

        // точечная сетка
        int gx0 = vx + (((int) panX) % 24 + 24) % 24, gy0 = vy + (((int) panY) % 24 + 24) % 24;
        for (int x = gx0; x < vx + vw; x += 24) for (int y = gy0; y < vy + vh; y += 24) ctx.fill(x, y, x + 1, y + 1, 0x30FFFFFF);

        Upgrade[] all = Upgrade.values();
        int hx = cx(0), hy = hubCy();

        // связи
        for (Upgrade u : all) {
            int childX = cx(u.col) - R, childY = cy(u.row);
            int parentRight, parentY;
            boolean active;
            Upgrade par = u.requiresId != null ? Upgrade.byId(u.requiresId) : null;
            if (par == null) { parentRight = hx + R + 3; parentY = hy; active = true; }
            else { parentRight = cx(par.col) + R + 3 + LABEL_W; parentY = cy(par.row); active = level(par) >= u.requiresLevel; }
            int midX = parentRight + (childX - parentRight) / 2;
            int col = active ? argb(0x3DD6FF, 0xE0) : 0xFF28303C;
            hline(ctx, parentRight, midX, parentY, col);
            vline(ctx, midX, parentY, childY, col);
            hline(ctx, midX, childX, childY, col);
        }

        // центральный узел "Организм"
        disc(ctx, hx, hy, R + 5, argb(accent2, 0xFF));
        disc(ctx, hx, hy, R + 3, 0xFF12141C);
        ctx.drawItem(hubIcon, hx - 8, hy - 8);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Организм"), hx, hy + R + 8, 0xFFE6E6F2);

        // заголовки ступеней
        String[] heads = {"", "Основа", "Ступень II", "Ступень III"};
        for (int c = 1; c <= 3; c++) ctx.drawTextWithShadow(textRenderer, heads[c], cx(c) - R, vy + 3, 0xFF6F7690);

        // узлы
        for (Upgrade u : all) drawNode(ctx, u, mx, my);

        ctx.disableScissor();
        // рамка области
        ctx.fill(vx - 1, vy - 1, vx + vw + 1, vy, argb(accent, 0xFF));
        ctx.fill(vx - 1, vy + vh, vx + vw + 1, vy + vh + 1, argb(accent, 0xFF));
        ctx.fill(vx - 1, vy, vx, vy + vh, argb(accent, 0xFF));
        ctx.fill(vx + vw, vy, vx + vw + 1, vy + vh, argb(accent, 0xFF));

        drawDetails(ctx);
    }

    private void drawNode(DrawContext ctx, Upgrade u, int mx, int my) {
        int x = cx(u.col), y = cy(u.row);
        if (x < vx - R - LABEL_W - 10 || x > vx + vw + R + 10 || y < vy - 20 || y > vy + vh + 20) return;
        int lv = level(u), max = u.max();
        boolean lock = locked(u), maxed = lv >= max;
        boolean hover = mx >= vx && mx < vx + vw && my >= vy && my < vy + vh && nodeAt(mx, my) == u;
        boolean sel = u == selected;
        int ring = lock ? GRAY : maxed ? GOLD : lv > 0 ? CYAN : VIOLET;
        if (hover || sel) ring = sel ? 0xFFFFFFFF : brighten(ring);

        disc(ctx, x, y, R, ring);
        disc(ctx, x, y, R - 2, lock ? 0xFF14161B : 0xFF0E111A);
        ctx.drawItem(icon(u), x - 8, y - 8);
        if (lock) disc(ctx, x, y, R - 2, 0xB0000000);

        int lx = x + R + 3, ly = y - NODE_H / 2;
        ctx.fill(lx - 1, ly - 1, lx + LABEL_W + 1, ly + NODE_H + 1, ring);
        ctx.fillGradient(lx, ly, lx + LABEL_W, ly + NODE_H, lock ? 0xFF15171D : 0xFF222737, lock ? 0xFF0E1015 : 0xFF141824);
        String name = textRenderer.trimToWidth(u.nom, LABEL_W - 8);
        ctx.drawTextWithShadow(textRenderer, name, lx + 4, ly + 3, lock ? 0xFF6B7280 : 0xFFF0F0FA);
        // ячейки уровней
        for (int k = 0; k < max; k++) {
            int px0 = lx + 4 + k * 8, py0 = ly + NODE_H - 8;
            ctx.fill(px0, py0, px0 + 6, py0 + 4, 0xFF05060A);
            ctx.fill(px0 + 1, py0 + 1, px0 + 5, py0 + 3, k < lv ? GOLD : (lock ? 0xFF22242B : 0xFF343A4C));
        }
        String right = lock ? "закрыто" : maxed ? "МАКС" : u.costs[lv] + " ч.";
        int rc = lock ? 0xFFB05A5A : maxed ? 0xFF77FF88 : 0xFFE8D28A;
        ctx.drawTextWithShadow(textRenderer, right, lx + LABEL_W - 4 - textRenderer.getWidth(right), ly + NODE_H - 10, rc);
    }

    private static int brighten(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 60), g = Math.min(255, ((c >> 8) & 255) + 60), b = Math.min(255, (c & 255) + 60);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void drawDetails(DrawContext ctx) {
        int dx = px + 14, dy = py + ph - 94, dw = pw - 240;
        if (selected == null) {
            for (OrderedText l : textRenderer.wrapLines(Text.literal("Выберите узел дерева, чтобы прочитать описание. Поле двигается мышью (зажмите и тяните) и колёсиком. "
                    + "Каждая следующая ступень ветки открывается после максимума предыдущей и даёт более сильный эффект."), dw)) {
                ctx.drawTextWithShadow(textRenderer, l, dx, dy, 0xFF9A9AAA);
                dy += 10;
            }
            return;
        }
        Upgrade u = selected;
        int lv = level(u);
        ctx.drawTextWithShadow(textRenderer, u.nom + "  -  уровень " + lv + " из " + u.max(), dx, dy, GOLD);
        dy += 12;
        int lines = 0;
        for (OrderedText l : textRenderer.wrapLines(Text.literal(u.desc), dw)) {
            if (lines++ >= 5) break;
            ctx.drawTextWithShadow(textRenderer, l, dx, dy, 0xFFE6E6F2);
            dy += 10;
        }
        if (locked(u)) {
            Upgrade r = Upgrade.byId(u.requiresId);
            ctx.drawTextWithShadow(textRenderer, "Откроется: «" + r.nom + "» до " + u.requiresLevel + " ур.", dx, py + ph - 20, 0xFFFF6B6B);
        } else if (lv < u.max()) {
            ctx.drawTextWithShadow(textRenderer, "Следующий уровень: " + u.costs[lv] + " ч. жизни", dx, py + ph - 20, 0xFFE8D28A);
        } else {
            ctx.drawTextWithShadow(textRenderer, "Максимальный уровень достигнут", dx, py + ph - 20, 0xFF77FF88);
        }
    }
}
