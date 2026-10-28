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

/** Импланты: в каждом слоте (глаз, рука, тело, ядро) работает один. Цена - частицы жизни и флешки (у ядра ещё алмазы и звёзды Незера). */
public class ImplantScreen extends FancyScreen {
    private static final int CARD_H = 48, GAP = 4;
    private final Screen parent;

    public ImplantScreen(Screen parent) { super(Text.literal("Импланты"), 0x00C2B8, 0xFFD166); this.parent = parent; }

    public void refresh() { clearAndInit(); }

    private int cardY(int i) { return py + 40 + i * (CARD_H + GAP); }

    @Override
    protected void init() {
        fancy.clear();
        panel(460, 40 + Implant.values().length * (CARD_H + GAP) + 40);
        int right = px + pw - 12;
        for (int i = 0; i < Implant.values().length; i++) {
            Implant im = Implant.values()[i];
            boolean have = BodyTradeClient.IMPLANTS.contains(im.id);
            boolean slotBusy = false;
            for (Implant o : Implant.values()) if (o != im && o.slot.equals(im.slot) && BodyTradeClient.IMPLANTS.contains(o.id)) slotBusy = true;
            ButtonWidget b = btn(have ? "Установлен" : (slotBusy ? "Заменить" : "Установить"), right - 98, cardY(i) + 5, 92, 18, () -> buy(im));
            b.active = !have;
        }
        btn("Рецепты флешек", px + pw - 128, py + 5, 120, 16, () -> client.setScreen(new RecipesScreen(this)));
        if (BodyTradeClient.IMPLANTS.contains("cyber_eye"))
            btn("Подсветка: " + (BodyTradeClient.espOn ? "ВКЛ" : "ВЫКЛ") + "  [V]", px + 12, py + ph - 28, 170, 20, this::toggleEsp);
        btn("Назад", width / 2 - 50 + (BodyTradeClient.IMPLANTS.contains("cyber_eye") ? 90 : 0), py + ph - 28, 100, 20, this::close);
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
        ctx.drawTextWithShadow(textRenderer, "Частиц жизни: " + have + "   |   один имплант на слот, флешки делаются из нейрита", px + 12, py + 27, 0xFF77FF88);
        for (int i = 0; i < Implant.values().length; i++) {
            Implant im = Implant.values()[i];
            int x = px + 10, y = cardY(i), w = pw - 20;
            boolean installed = BodyTradeClient.IMPLANTS.contains(im.id);
            ctx.fillGradient(x, y, x + w, y + CARD_H, installed ? 0xFF14302C : 0xFF181B27, installed ? 0xFF0C1C1A : 0xFF0E1018);
            int border = installed ? argb(accent, 0xFF) : 0xFF2A2E40;
            ctx.fill(x, y, x + w, y + 1, border); ctx.fill(x, y + CARD_H - 1, x + w, y + CARD_H, border);
            ctx.fill(x, y, x + 1, y + CARD_H, border); ctx.fill(x + w - 1, y, x + w, y + CARD_H, border);
            ctx.fill(x + 6, y + 10, x + 30, y + 34, 0xFF07080D);
            ctx.drawItem(new ItemStack(im.icon), x + 10, y + 14);
            ctx.drawTextWithShadow(textRenderer, im.nom, x + 38, y + 4, argb(accent2, 0xFF));
            ctx.drawTextWithShadow(textRenderer, im.slotName(), x + 38 + textRenderer.getWidth(im.nom) + 8, y + 4, 0xFF8A90A0);
            int ty = y + 14, lines = 0;
            for (OrderedText l : textRenderer.wrapLines(Text.literal(im.desc), w - 38 - 110)) {
                if (lines++ >= 2) break;
                ctx.drawTextWithShadow(textRenderer, l, x + 38, ty, 0xFFD6D6E4);
                ty += 9;
            }
            // цена: частицы жизни + предметы
            int cx = x + 38, cy = y + CARD_H - 12;
            if (im.particles > 0) {
                small(ctx, new ItemStack(BodyTradeMod.LIFE_PARTICLE), cx, cy - 2);
                ctx.drawTextWithShadow(textRenderer, "" + im.particles, cx + 13, cy, 0xFFF0E6B0);
                cx += 13 + textRenderer.getWidth("" + im.particles) + 10;
            }
            for (ItemStack k : im.cost()) {
                small(ctx, k, cx, cy - 2);
                String t = "×" + k.getCount();
                ctx.drawTextWithShadow(textRenderer, t, cx + 13, cy, 0xFFF0E6B0);
                cx += 13 + textRenderer.getWidth(t) + 10;
            }
        }
    }
}
