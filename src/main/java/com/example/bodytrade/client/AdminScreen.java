package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.SideEffect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class AdminScreen extends Screen {
    private static final int ROWS = 6, PER_PAGE = ROWS * 2;
    private final BlockPos pos;
    private int mode = 0; // 0 - части тела, 1 - болезни
    private int page = 0;

    public AdminScreen(BlockPos pos) { super(Text.literal("Админ-алтарь")); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    private void send(String action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(action);
        ClientPlayNetworking.send(BodyTradeMod.ADMIN_PACKET, buf);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Части тела"), b -> { mode = 0; page = 0; clearAndInit(); })
                .dimensions(cx - 155, 24, 150, 20).build()).active = mode != 0;
        addDrawableChild(ButtonWidget.builder(Text.literal("Болезни"), b -> { mode = 1; page = 0; clearAndInit(); })
                .dimensions(cx + 5, 24, 150, 20).build()).active = mode != 1;

        long now = System.currentTimeMillis();
        int total = mode == 0 ? BodyPart.values().length : SideEffect.values().length;
        int pages = Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int first = page * PER_PAGE;
        for (int i = first; i < Math.min(total, first + PER_PAGE); i++) {
            int idx = i - first;
            int x = cx - 155 + (idx % 2) * 160;
            int y = 52 + (idx / 2) * 22;
            if (mode == 0) {
                BodyPart p = BodyPart.values()[i];
                boolean gone = BodyTradeClient.MISSING.contains(p);
                addDrawableChild(ButtonWidget.builder(Text.literal((gone ? "[-] " : "[+] ") + p.nom), b -> send("part:" + p.id))
                        .dimensions(x, y, 150, 20).build());
            } else {
                SideEffect s = SideEffect.values()[i];
                Long end = BodyTradeClient.EFFECTS.get(s.title);
                boolean active = end != null && end > now;
                addDrawableChild(ButtonWidget.builder(Text.literal(s.title + (active ? " [снять]" : " [выдать]")), b -> send("effect:" + s.name()))
                        .dimensions(x, y, 150, 20).build());
            }
        }
        int py = 52 + ROWS * 22 + 4;
        if (pages > 1) {
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); }).dimensions(cx - 60, py, 40, 20).build()).active = page > 0;
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); }).dimensions(cx + 20, py, 40, 20).build()).active = page < pages - 1;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Вернуть все части"), b -> send("parts_clear"))
                .dimensions(cx - 155, py + 26, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Снять все болезни"), b -> send("effects_clear"))
                .dimensions(cx + 5, py + 26, 150, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
    }
}
