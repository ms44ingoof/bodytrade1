package com.example.bodytrade.client;

import com.example.bodytrade.DoctorEntity;
import com.example.bodytrade.Story;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Диалог с Доктором в стиле «визуальной новеллы»: персонаж на фоне мира, имя и должность с орнаментом,
 * реплика внизу с печатающимся текстом, варианты ответа справа. Закрыть без выбора нельзя.
 */
public class DoctorScreen extends Screen {
    private static final String NAME = "Доктор";
    private static final String ROLE = "Врач-экспериментатор";
    private static final String[] PAGES = {
            "Здравствуйте. Я давно наблюдаю за вами... Вы только что взяли в руки первое дерево. С этого начинается любая история.",
            "У меня есть для вас одно предложение: зелье перерождения. Выпьете - ваше тело изменится навсегда. Пути назад не будет.",
            "Откажетесь - я не стану настаивать. Почти. Так что вы решаете?"
    };
    private static final String[] CHOICES = {"Давайте сюда ваше зелье.", "Нет, спасибо. Я пас."};
    private static final String[] CHOICE_IDS = {"accept", "refuse"};

    private static final int GOLD = 0xFFFFC832, GOLD_DIM = 0xFFE0B040;
    private static final float TEXT_SCALE = 1.2f;
    private static final long MS_PER_CHAR = 28;

    private int page = 0;
    private long pageStart = Util.getMeasuringTimeMs();
    private List<OrderedText> lines = new ArrayList<>();
    private int[] lineLen = new int[0];
    private int totalChars = 0;
    private final int[][] choiceRects = new int[CHOICES.length][4];
    private boolean sent = false;

    public DoctorScreen() { super(Text.literal(NAME)); }

    @Override
    protected void init() { wrapPage(); }

    private void wrapPage() {
        int maxW = (int) (Math.min(width * 0.78f, 640f) / TEXT_SCALE);
        lines = textRenderer.wrapLines(Text.literal(PAGES[page]), maxW);
        lineLen = new int[lines.size()];
        totalChars = 0;
        for (int i = 0; i < lines.size(); i++) {
            int[] c = {0};
            lines.get(i).accept((idx, style, cp) -> { c[0]++; return true; });
            lineLen[i] = c[0];
            totalChars += c[0];
        }
    }

    private int revealed() { return (int) Math.min(totalChars, (Util.getMeasuringTimeMs() - pageStart) / MS_PER_CHAR); }
    private boolean typed() { return revealed() >= totalChars; }
    private boolean lastPage() { return page == PAGES.length - 1; }
    private boolean choicesVisible() { return lastPage() && typed(); }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    /** Игра не ставится на паузу: на сервере всё равно живёт мир, Доктор стоит рядом. */
    @Override
    public boolean shouldPause() { return false; }

    // ------------------------------------------------------------ ввод
    private void click() {
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void advance() {
        if (!typed()) { pageStart = Util.getMeasuringTimeMs() - totalChars * MS_PER_CHAR; return; }   // допечатать сразу
        if (!lastPage()) { page++; pageStart = Util.getMeasuringTimeMs(); wrapPage(); click(); }
    }

    private void pick(int i) {
        if (sent) return;
        sent = true;
        click();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(CHOICE_IDS[i]);
        ClientPlayNetworking.send(Story.CHOICE_PACKET, buf);
        close();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (choicesVisible()) {
            for (int i = 0; i < choiceRects.length; i++) {
                int[] r = choiceRects[i];
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) { pick(i); return true; }
            }
        }
        if (button == 0) advance();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (choicesVisible()) {
            if (key == GLFW.GLFW_KEY_1 || key == GLFW.GLFW_KEY_KP_1) { pick(0); return true; }
            if (key == GLFW.GLFW_KEY_2 || key == GLFW.GLFW_KEY_KP_2) { pick(1); return true; }
        }
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { advance(); return true; }
        return true;   // Esc и остальные клавиши не закрывают диалог
    }

    // ------------------------------------------------------------ рисование
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // лёгкое затемнение мира
        ctx.fill(0, 0, width, height, 0x38000000);
        drawDoctor(ctx, mx, my);

        // нижняя тёмная полоса под текст
        int bandH = Math.min(116, (int) (height * 0.44f));
        ctx.fillGradient(0, height - bandH - 24, width, height - bandH, 0x00000000, 0x90000000);
        ctx.fill(0, height - bandH, width, height, 0x90000000);
        ctx.fillGradient(0, height - bandH, width, height, 0x00000000, 0x60000000);

        int cx = width / 2;
        int nameY = height - bandH + 12;

        // имя
        float ns = 1.7f;
        int nw = (int) (textRenderer.getWidth(NAME) * ns);
        drawScaled(ctx, NAME, cx - nw / 2, nameY, ns, GOLD);

        // должность с золотыми линиями по бокам
        int subY = nameY + 22;
        int sw = textRenderer.getWidth(ROLE);
        ctx.drawTextWithShadow(textRenderer, ROLE, cx - sw / 2, subY, GOLD_DIM);
        int ly = subY + 4;
        int lx0 = (int) (width * 0.05f), lx1 = cx - sw / 2 - 10, rx0 = cx + sw / 2 + 10, rx1 = (int) (width * 0.95f);
        ornamentLine(ctx, lx0, lx1, ly, true);
        ornamentLine(ctx, rx0, rx1, ly, false);

        // реплика с печатающимся текстом
        int textY = subY + 17;
        int left = revealed();
        int maxW = (int) (Math.min(width * 0.78f, 640f));
        for (int i = 0; i < lines.size(); i++) {
            OrderedText full = lines.get(i);
            int fullW = (int) (textRenderer.getWidth(full) * TEXT_SCALE);
            final int n = Math.max(0, Math.min(lineLen[i], left));
            left -= lineLen[i];
            OrderedText part = visitor -> {
                int[] k = {0};
                return full.accept((idx, style, cp) -> k[0]++ < n && visitor.accept(idx, style, cp));
            };
            ctx.getMatrices().push();
            ctx.getMatrices().translate(cx - fullW / 2f, textY + i * 13.5f, 0);
            ctx.getMatrices().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            ctx.drawTextWithShadow(textRenderer, part, 0, 0, 0xFFFFFFFF);
            ctx.getMatrices().pop();
        }

        if (choicesVisible()) drawChoices(ctx, mx, my);
        else if (typed()) {
            // мигающий значок «дальше»
            if ((Util.getMeasuringTimeMs() / 450) % 2 == 0) { int ty = height - 14; for (int k = 0; k < 4; k++) ctx.fill(cx - 4 + k, ty + k, cx + 5 - k, ty + k + 1, GOLD); }
        }
    }

    private void drawScaled(DrawContext ctx, String s, int x, int y, float scale, int color) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawTextWithShadow(textRenderer, s, 0, 0, color);
        ctx.getMatrices().pop();
    }

    /** Тонкая золотая линия с ромбиком на внешнем конце и более яркой частью у надписи. */
    private static void ornamentLine(DrawContext c, int x0, int x1, int y, boolean leftSide) {
        if (x1 - x0 < 12) return;
        c.fill(x0, y, x1, y + 1, 0x70C8962A);
        int bright = Math.min(60, (x1 - x0) / 2);
        if (leftSide) c.fill(x1 - bright, y, x1, y + 1, 0xFFE8B84A); else c.fill(x0, y, x0 + bright, y + 1, 0xFFE8B84A);
        int dx = leftSide ? x0 : x1;
        c.fill(dx - 2, y, dx + 3, y + 1, GOLD);
        c.fill(dx - 1, y - 1, dx + 2, y + 2, GOLD);
        c.fill(dx, y - 2, dx + 1, y + 3, GOLD);
    }

    /** Рисует самого Доктора (живую сущность рядом с игроком) по пояс, как портрет. */
    private void drawDoctor(DrawContext ctx, int mx, int my) {
        MinecraftClient mc = client;
        if (mc == null || mc.world == null || mc.player == null) return;
        DoctorEntity best = null;
        double bd = Double.MAX_VALUE;
        for (DoctorEntity d : mc.world.getEntitiesByClass(DoctorEntity.class, mc.player.getBoundingBox().expand(32), e -> true)) {
            double dd = d.squaredDistanceTo(mc.player);
            if (dd < bd) { bd = dd; best = d; }
        }
        if (best == null) return;
        int size = (int) (height * 0.60f);
        int x = (int) (width * 0.45f);
        int feetY = (int) (height * 0.10f + size * 1.8f);
        boolean nameShown = best.isCustomNameVisible();
        best.setCustomNameVisible(false);   // табличка с именем над головой в портрете не нужна
        InventoryScreen.drawEntity(ctx, x, feetY, size, (x - mx) * 0.25f, (feetY * 0.45f - my) * 0.25f, best);
        best.setCustomNameVisible(nameShown);
    }

    private void drawChoices(DrawContext ctx, int mx, int my) {
        int cw = (int) Math.min(270, width * 0.34f), ch = 26, gap = 8;
        int x = width - cw - 18;
        int total = CHOICES.length * ch + (CHOICES.length - 1) * gap;
        int y0 = (int) (height * 0.50f) - total / 2;
        for (int i = 0; i < CHOICES.length; i++) {
            int y = y0 + i * (ch + gap);
            choiceRects[i] = new int[]{x, y, cw, ch};
            boolean hover = mx >= x && mx < x + cw && my >= y && my < y + ch;
            int bg = hover ? 0xFFECE5D8 : 0xF0222838, border = hover ? 0xFFFFFFFF : 0xFF4C566B;
            int fg = hover ? 0xFF2A2A33 : 0xFFFFFFFF;
            rrect(ctx, x - 1, y - 1, cw + 2, ch + 2, 13, border);
            rrect(ctx, x, y, cw, ch, 12, bg);
            // значок реплики (облачко с тремя точками)
            int bx = x + 12, by = y + (ch - 11) / 2;
            int bubble = hover ? 0xFF2A2A33 : 0xFFF2F2F2, dots = hover ? 0xFFECE5D8 : 0xFF222838;
            rrect(ctx, bx, by, 15, 10, 4, bubble);
            ctx.fill(bx + 3, by + 10, bx + 6, by + 12, bubble);
            for (int k = 0; k < 3; k++) ctx.fill(bx + 3 + k * 4, by + 4, bx + 5 + k * 4, by + 6, dots);
            String label = textRenderer.trimToWidth(CHOICES[i], cw - 48);
            if (hover) ctx.drawText(textRenderer, label, x + 34, y + (ch - 8) / 2, fg, false);
            else ctx.drawTextWithShadow(textRenderer, label, x + 34, y + (ch - 8) / 2, fg);
        }
    }

    /** Скруглённый прямоугольник из горизонтальных полосок. */
    private static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int k = i < r ? i : (i >= h - r ? h - 1 - i : r);
            int inset = k >= r ? 0 : r - (int) Math.round(Math.sqrt(Math.max(0, r * r - (r - k - 0.5) * (r - k - 0.5))));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }
}
