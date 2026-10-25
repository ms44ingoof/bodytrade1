package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/** Магазин протезиста (стальная тема): протезы за алмазы. */
public class ShopScreen extends FancyScreen {
    private static final BodyPart[] GOODS = {BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
    private final int entityId;

    public ShopScreen(int entityId) { super(Text.literal("Протезист"), 0x8FA3B8, 0xF1C40F); this.entityId = entityId; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        fancy.clear();
        panel(300, 230);
        int cx = width / 2, y = py + 78;
        for (BodyPart p : GOODS) {
            boolean missing = BodyTradeClient.MISSING.contains(p);
            boolean has = BodyTradeClient.PROSTHETIC.contains(p);
            ButtonWidget b = btn("Протез: " + p.nom + " - " + p.prosthesisPrice() + " алм." + (has ? " (установлен)" : (missing ? "" : " (не нужен)")),
                    cx - 120, y, 240, 21, () -> buy(p));
            b.active = missing && !has;
            y += 24;
        }
        btn("Закрыть", cx - 50, y + 8, 100, 20, this::close);
    }

    private void buy(BodyPart p) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        buf.writeString(p.id);
        ClientPlayNetworking.send(BodyTradeMod.BUY_PACKET, buf);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int have = client != null && client.player != null ? client.player.getInventory().count(Items.DIAMOND) : 0;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Протез заменяет потерянную руку или ногу."), width / 2, py + 32, 0xFFE0E0EA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Работает, но чуть хуже родной конечности."), width / 2, py + 44, 0xFF9A9AAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("У вас алмазов: " + have), width / 2, py + 60, 0xFF77DDFF);
    }
}
