package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.example.bodytrade.SideEffect;
import com.example.bodytrade.Tier;
import com.example.bodytrade.Upgrade;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Справочник: части тела, награды, протезы, апгрейды, болезни. Прокрутка колесом мыши. */
public class InfoScreen extends FancyScreen {
    private record Line(OrderedText text, int color) {}

    private final Screen parent;
    private final List<Line> lines = new ArrayList<>();
    private int scroll = 0;
    private static final int LH = 10;

    public InfoScreen(Screen parent) { super(Text.literal("Болезни и части тела"), 0xC0392B, 0xE8C77A); this.parent = parent; }

    private void add(String s, int color, int indent) {
        for (OrderedText t : textRenderer.wrapLines(Text.literal(s), pw - 36 - indent)) lines.add(new Line(t, color));
    }

    @Override
    protected void init() {
        fancy.clear();
        lines.clear();
        panel(480, 1000);
        int H = 0xFFFFD866, W = 0xFFE6E6F0, G = 0xFF9A9AAA, R = 0xFFFF6B6B;
        add("ЧАСТИ ТЕЛА (шанс в %, относительный; красным - уже отсутствует)", H, 0);
        for (BodyPart p : BodyPart.values()) {
            boolean gone = BodyTradeClient.MISSING.contains(p);
            add(p.nom + ": " + p.minChance + "-" + p.maxChance + "% | награда: " + p.tier.title + " | вернуть: "
                    + p.tier.refundCount + "× " + new ItemStack(p.tier.refundItem).getName().getString(), gone ? R : W, 0);
            add("   " + p.consequence, G, 10);
        }
        add(" ", W, 0);
        add("НАГРАДЫ: за любую часть вы получаете СЛУЧАЙНЫЙ предмет из всей игры. Количество зависит от ценности:", H, 0);
        for (Tier t : Tier.values()) add(t.title + ": от 1 до " + t.maxCount + " шт.", W, 0);
        add(" ", W, 0);
        add("ПРОТЕЗЫ: у протезиста (его можно скрафтить) за алмазы покупаются протезы рук (3) и ног (4). "
                + "Протез заменяет потерянную конечность: она снова работает, но чуть хуже.", H, 0);
        add(" ", W, 0);
        add("РЕЖИМ «РазВиВАЙСЯ»: вы начинаете без рук и ног и ползёте. Выполняйте задания в любом порядке (кнопка «Задания»), получайте частицы жизни "
                + "и покупайте конечности в Блоке развития: правая рука 1, левая нога 15, правая нога 35, левая рука 50. "
                + "Сменить сложность: /bodymode normal или /bodymode develop.", H, 0);
        add(" ", W, 0);
        add("РАССУДОК: отдельная шкала. Падает в темноте, рядом с монстрами и при болезнях. Растёт у горящего костра, во сне и в компании других игроков. "
                + "При низком рассудке слышатся шёпоты и ложные звуки, темнеет, появляются чудовища, а на нуле наступает психоз.", H, 0);
        add(" ", W, 0);
        add("ИМПЛАНТЫ (Блок апгрейда, кнопка «Импланты»): кибер-глаз, встроенный фонарь, рука-клинок, титановый каркас, нейрочип. Цена - частицы жизни и металл, один имплант на слот.", H, 0);
        add(" ", W, 0);
        add("АПГРЕЙДЫ (Блок апгрейда, за частицы жизни):", H, 0);
        for (Upgrade u : Upgrade.values()) {
            StringBuilder cost = new StringBuilder();
            for (int c : u.costs) cost.append(cost.length() > 0 ? " / " : "").append(c);
            add((u.isBranch() ? "[ветка] " : "") + u.nom + " (" + cost + " ч.): " + u.desc, u.isBranch() ? 0xFF77DDFF : W, 0);
        }
        add(" ", W, 0);
        add("БОЛЕЗНИ И ПОБОЧНЫЕ ЭФФЕКТЫ (после каждого обмена шанс " + (int) (BodyTradeMod.SIDE_EFFECT_CHANCE * 100)
                + "%, конкретная болезнь выбирается случайно; редкие выпадают отдельно)", H, 0);
        for (SideEffect s : SideEffect.values()) {
            String rare = s.isRare() ? " [РЕДКАЯ, шанс " + String.format(Locale.ROOT, "%.1f", s.rareChance * 100) + "% за обмен]" : "";
            add(s.title + " (" + s.seconds + " с)" + rare + ": " + s.message, s.isRare() ? R : W, 0);
        }
        btn("Назад", width / 2 - 50, py + ph - 27, 100, 20, this::close);
    }

    private int top() { return py + 30; }
    private int bottom() { return py + ph - 32; }
    private int maxScroll() { return Math.max(0, lines.size() * LH - (bottom() - top())); }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 20));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        ctx.enableScissor(px + 4, top(), px + pw - 4, bottom());
        int y = top() - scroll;
        for (Line l : lines) {
            if (y > top() - LH && y < bottom()) ctx.drawTextWithShadow(textRenderer, l.text(), px + 16, y, l.color());
            y += LH;
        }
        ctx.disableScissor();
        if (maxScroll() > 0) ctx.drawTextWithShadow(textRenderer, "колесо мыши - прокрутка", px + pw - 16 - textRenderer.getWidth("колесо мыши - прокрутка"), py + ph - 14, 0xFF77778A);
    }
}
