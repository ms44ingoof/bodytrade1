package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.SideEffect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Админ-алтарь (синяя тема): выдать/снять любую часть тела или болезнь. */
public class AdminScreen extends FancyScreen {
    private static final int ROWS = 6, PER_PAGE = ROWS * 2;
    private final BlockPos pos;
    private int mode = 0; // 0 - части тела, 1 - болезни
    private int page = 0;

    public AdminScreen(BlockPos pos) { super(Text.literal("Админ-алтарь"), 0x3A86FF, 0xFFD166); this.pos = pos; }

    public void refresh() { clearAndInit(); }

    private void send(String action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(action);
        ClientPlayNetworking.send(BodyTradeMod.ADMIN_PACKET, buf);
    }

    @Override
    protected void init() {
        fancy.clear();
        panel(350, 262);
        int cx = width / 2;
        ButtonWidget t0 = btn("Части тела", cx - 162, py + 32, 160, 20, () -> { mode = 0; page = 0; clearAndInit(); });
        ButtonWidget t1 = btn("Болезни", cx + 2, py + 32, 160, 20, () -> { mode = 1; page = 0; clearAndInit(); });
        t0.active = mode != 0;
        t1.active = mode != 1;

        long now = System.currentTimeMillis();
        int total = mode == 0 ? BodyPart.values().length : SideEffect.values().length;
        int pages = Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int first = page * PER_PAGE;
        for (int i = first; i < Math.min(total, first + PER_PAGE); i++) {
            int idx = i - first;
            int x = cx - 162 + (idx % 2) * 164, y = py + 58 + (idx / 2) * 22;
            if (mode == 0) {
                BodyPart p = BodyPart.values()[i];
                boolean gone = BodyTradeClient.MISSING.contains(p);
                btn((gone ? "[-] " : "[+] ") + p.nom, x, y, 160, 20, () -> send("part:" + p.id));
            } else {
                SideEffect s = SideEffect.values()[i];
                Long end = BodyTradeClient.EFFECTS.get(s.title);
                boolean active = end != null && end > now;
                btn(s.title + (active ? " [снять]" : " [выдать]"), x, y, 160, 20, () -> send("effect:" + s.name()));
            }
        }
        int py2 = py + 58 + ROWS * 22 + 2;
        if (pages > 1) {
            ButtonWidget prev = btn("<", cx - 60, py2, 40, 20, () -> { page--; clearAndInit(); });
            ButtonWidget next = btn(">", cx + 20, py2, 40, 20, () -> { page++; clearAndInit(); });
            prev.active = page > 0;
            next.active = page < pages - 1;
        }
        btn("Вернуть все части", cx - 162, py + ph - 28, 160, 20, () -> send("parts_clear"));
        btn("Снять все болезни", cx + 2, py + ph - 28, 160, 20, () -> send("effects_clear"));
    }
}
