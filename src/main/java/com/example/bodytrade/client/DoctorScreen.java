package com.example.bodytrade.client;

import com.example.bodytrade.Story;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** Диалог с Доктором: выпить зелье или отказаться. Закрыть без выбора нельзя. */
public class DoctorScreen extends FancyScreen {
    private static final String TEXT =
            "Здравствуйте. Я давно наблюдаю за вами. Вы только что взяли в руки первое дерево - с этого начинается любая история. "
            + "У меня есть для вас предложение: зелье перерождения. Выпьете - ваше тело изменится, и пути назад не будет. "
            + "Откажетесь - я не стану настаивать. Почти.";

    public DoctorScreen() { super(Text.literal("Доктор"), 0x55E08A, 0xE8C77A); }

    @Override
    protected void init() {
        fancy.clear();
        panel(Math.min(360, width - 20), 200);
        int bw = (pw - 52) / 2, by = py + ph - 44;
        btn("Выпить зелье", px + 20, by, bw, 26, () -> pick("accept"));
        btn("Отказаться", px + pw - 20 - bw, by, bw, 26, () -> pick("refuse"));
    }

    private void pick(String choice) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(choice);
        ClientPlayNetworking.send(Story.CHOICE_PACKET, buf);
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        int y = py + 36;
        for (OrderedText line : textRenderer.wrapLines(Text.literal(TEXT), pw - 32)) {
            ctx.drawTextWithShadow(textRenderer, line, px + 16, y, 0xFFD8D8E4);
            y += 11;
        }
    }
}
