package com.example.bodytrade.client;

import com.example.bodytrade.BodyTradeMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** Выбор сложности при первом входе в мир: две большие карточки. */
public class ModeScreen extends FancyScreen {
    private int[] r0 = new int[4], r1 = new int[4]; // x, y, w, h карточек

    public ModeScreen() { super(Text.literal("Выберите сложность"), 0xE8C77A, 0x55E08A); }

    @Override
    protected void init() {
        fancy.clear();
        boolean wide = width >= 520;
        panel(wide ? 480 : Math.min(320, width - 20), wide ? 230 : 300);
        int cw = wide ? 210 : pw - 40, ch = wide ? 130 : 100;
        int x0 = wide ? px + 20 : px + 20, y0 = py + 40;
        int x1 = wide ? px + pw - 20 - cw : px + 20, y1 = wide ? py + 40 : py + 40 + ch + 12;
        r0 = new int[]{x0, y0, cw, ch};
        r1 = new int[]{x1, y1, cw, ch};
        btn("", x0, y0, cw, ch, () -> pick("normal"));
        btn("", x1, y1, cw, ch, () -> pick("develop"));
    }

    private void pick(String mode) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(mode);
        ClientPlayNetworking.send(BodyTradeMod.MODE_PICK_PACKET, buf);
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private void card(DrawContext ctx, int[] r, String name, String desc, int color) {
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name), r[0] + r[2] / 2, r[1] + 10, color);
        ctx.fill(r[0] + 14, r[1] + 22, r[0] + r[2] - 14, r[1] + 23, argb(color, 0xB0));
        int y = r[1] + 30;
        for (OrderedText line : textRenderer.wrapLines(Text.literal(desc), r[2] - 26)) {
            ctx.drawTextWithShadow(textRenderer, line, r[0] + 13, y, 0xFFC8C8D6);
            y += 10;
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        card(ctx, r0, "Обычная", "Игра как есть. Части тела вы отдаёте на алтаре обмена сами, когда захотите, и получаете случайные предметы.", 0xFFE8C77A);
        card(ctx, r1, "Перерождение", "Вы начинаете без рук и ног и ползёте. Выполняйте задания, копите частицы жизни, покупайте конечности и улучшайте организм.", 0xFF55E08A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Сменить позже: /bodymode normal  или  /bodymode develop"), width / 2, py + ph - 16, 0xFF77778A);
    }
}
