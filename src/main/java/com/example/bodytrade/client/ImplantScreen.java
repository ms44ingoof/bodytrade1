package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.Implant;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Импланты: в каждом слоте работает один, у кибер-глаза 3 уровня, у механического уха 4. Прокрутка колесом мыши. */
public class ImplantScreen extends FancyScreen {
    private static final int RH = 32;
    private final Screen parent;
    private final List<ButtonWidget> buys = new ArrayList<>();
    private int scroll = 0;
    private int selected = 0;

    public ImplantScreen(Screen parent) { super(Text.literal("Импланты"), 0x00C2B8, 0xFFD166); this.parent = parent; }

    public void refresh() { clearAndInit(); }

    private int level(Implant im) { return BodyTradeClient.IMPLANTS.getOrDefault(im.id, 0); }
    private int listTop() { return py + 38; }
    private int listBottom() { return py + ph - 98; }
    private int maxScroll() { return Math.max(0, Implant.values().length * RH - (listBottom() - listTop())); }

    @Override
    protected void init() {
        fancy.clear();
        buys.clear();
        panel(500, 420);
        Implant[] all = Implant.values();
        for (int i = 0; i < all.length; i++) {
            Implant im = all[i];
            int lv = level(im);
            boolean slotBusy = false;
            for (Implant o : all) if (o != im && o.slot.equals(im.slot) && level(o) > 0) slotBusy = true;
            String label = lv == 0 ? (slotBusy ? "Заменить" : "Установить") : (lv < im.maxLevel() ? "Улучшить" : "Максимум");
            final int idx = i;
            ButtonWidget b = btn(label, px + pw - 108, listTop(), 96, 20, () -> { selected = idx; buy(im); });
            b.active = lv < im.maxLevel();
            buys.add(b);
        }
        placeButtons();
        btn("Рецепты флешек", px + pw - 128, py + 5, 120, 16, () -> client.setScreen(new RecipesScreen(this)));
        boolean eye = level(Implant.CYBER_EYE) > 0;
        if (eye) btn("Подсветка: " + (BodyTradeClient.espOn ? "ВКЛ" : "ВЫКЛ") + "  [V]", px + 12, py + ph - 28, 170, 20, this::toggleEsp);
        btn("Назад", width / 2 - 50 + (eye ? 90 : 0), py + ph - 28, 100, 20, this::close);
    }

    private void placeButtons() {
        for (int i = 0; i < buys.size(); i++) {
            ButtonWidget b = buys.get(i);
            int y = listTop() + i * RH - scroll + 6;
            b.setY(y);
            b.visible = y >= listTop() && y + 20 <= listBottom();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 24));
        placeButtons();
        return true;
    }

    private void buy(Implant im) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(im.id);
        ClientPlayNetworking.send(BodyTradeMod.IMPLANT_BUY_PACKET, buf);
    }

    private void toggleEsp() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("esp");
        ClientPlayNetworking.send(BodyTradeMod.TOGGLE_PACKET, buf);
    }

    @Override
    public void close() { client.setScreen(parent); }

    private void small(DrawContext c, ItemStack stack, int x, int y) {
        var m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(0.7f, 0.7f, 1f);
        c.drawItem(stack, 0, 0);
        m.pop();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int have = client != null && client.player != null ? client.player.getInventory().count(BodyTradeMod.LIFE_PARTICLE) : 0;
        ctx.drawTextWithShadow(textRenderer, "Частиц жизни: " + have, px + 12, py + 27, 0xFF77FF88);
        Implant[] all = Implant.values();
        int hover = -1;
        ctx.enableScissor(px + 4, listTop(), px + pw - 4, listBottom());
        for (int i = 0; i < all.length; i++) {
            Implant im = all[i];
            int x = px + 10, y = listTop() + i * RH - scroll, w = pw - 20;
            if (y + RH < listTop() || y > listBottom()) continue;
            int lv = level(im), max = im.maxLevel();
            boolean installed = lv > 0;
            if (mx >= x && mx < x + w && my >= y && my < y + RH - 2 && my >= listTop() && my < listBottom()) hover = i;
            ctx.fillGradient(x, y, x + w, y + RH - 2, installed ? 0xFF14302C : 0xFF181B27, installed ? 0xFF0C1C1A : 0xFF0E1018);
            int border = installed ? argb(accent, 0xFF) : 0xFF2A2E40;
            ctx.fill(x, y, x + w, y + 1, border); ctx.fill(x, y + RH - 3, x + w, y + RH - 2, border);
            ctx.fill(x, y, x + 1, y + RH - 2, border); ctx.fill(x + w - 1, y, x + w, y + RH - 2, border);
            ctx.fill(x + 5, y + 5, x + 25, y + 25, 0xFF07080D);
            ctx.drawItem(new ItemStack(im.icon), x + 7, y + 7);
            ctx.drawTextWithShadow(textRenderer, im.nom, x + 32, y + 4, argb(accent2, 0xFF));
            ctx.drawTextWithShadow(textRenderer, im.slotName(), x + 32 + textRenderer.getWidth(im.nom) + 8, y + 4, 0xFF8A90A0);
            // ячейки уровней
            int pipX = x + 32 + textRenderer.getWidth(im.nom) + 8 + textRenderer.getWidth(im.slotName()) + 10;
            if (max > 1) for (int k = 0; k < max; k++) {
                ctx.fill(pipX + k * 9, y + 4, pipX + k * 9 + 7, y + 11, 0xFF05060A);
                ctx.fill(pipX + k * 9 + 1, y + 5, pipX + k * 9 + 6, y + 10, k < lv ? argb(accent2, 0xFF) : 0xFF2C2C3A);
            }
            // цена следующего уровня
            int cx = x + 32, cy = y + 17;
            if (lv >= max) {
                ctx.drawTextWithShadow(textRenderer, "Максимальный уровень", cx, cy, 0xFF77FF88);
            } else {
                int part = im.particles[lv];
                if (part > 0) {
                    small(ctx, new ItemStack(BodyTradeMod.LIFE_PARTICLE), cx, cy - 2);
                    ctx.drawTextWithShadow(textRenderer, "" + part, cx + 13, cy, 0xFFF0E6B0);
                    cx += 13 + textRenderer.getWidth("" + part) + 10;
                }
                for (ItemStack k : im.cost()) {
                    small(ctx, k, cx, cy - 2);
                    String t = "×" + k.getCount();
                    ctx.drawTextWithShadow(textRenderer, t, cx + 13, cy, 0xFFF0E6B0);
                    cx += 13 + textRenderer.getWidth(t) + 10;
                }
            }
        }
        ctx.disableScissor();
        // описание наведённого (или выбранного) импланта
        Implant d = all[hover >= 0 ? hover : Math.min(selected, all.length - 1)];
        int dy = listBottom() + 6;
        ctx.fill(px + 10, dy - 2, px + pw - 10, dy + 62, 0x80000000);
        ctx.drawTextWithShadow(textRenderer, d.nom, px + 16, dy + 1, argb(accent2, 0xFF));
        int ly = dy + 12, lines = 0;
        for (OrderedText l : textRenderer.wrapLines(Text.literal(d.desc), pw - 36)) {
            if (lines++ >= 5) break;
            ctx.drawTextWithShadow(textRenderer, l, px + 16, ly, 0xFFD6D6E4);
            ly += 9;
        }
    }
}
