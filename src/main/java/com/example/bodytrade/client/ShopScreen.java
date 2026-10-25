package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/** Магазин протезиста: протезы за алмазы. */
public class ShopScreen extends Screen {
    private static final BodyPart[] GOODS = {BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
    private final int entityId;

    public ShopScreen(int entityId) { super(Text.literal("Протезист")); this.entityId = entityId; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        int cx = width / 2, y = 70;
        for (BodyPart p : GOODS) {
            boolean missing = BodyTradeClient.MISSING.contains(p);
            boolean has = BodyTradeClient.PROSTHETIC.contains(p);
            String label = "Протез: " + p.nom + " - " + p.prosthesisPrice() + " алм." + (has ? " (установлен)" : (missing ? "" : " (не нужен)"));
            ButtonWidget b = ButtonWidget.builder(Text.literal(label), btn -> buy(p)).dimensions(cx - 120, y, 240, 20).build();
            b.active = missing && !has;
            addDrawableChild(b);
            y += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close()).dimensions(cx - 50, y + 10, 100, 20).build());
    }

    private void buy(BodyPart p) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        buf.writeString(p.id);
        ClientPlayNetworking.send(BodyTradeMod.BUY_PACKET, buf);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        int have = client != null && client.player != null ? client.player.getInventory().count(Items.DIAMOND) : 0;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Протез заменяет потерянную руку или ногу."), width / 2, 34, 0xCCCCCC);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Работает, но чуть хуже родной конечности."), width / 2, 46, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("У вас алмазов: " + have), width / 2, 58, 0x77DDFF);
    }
}
