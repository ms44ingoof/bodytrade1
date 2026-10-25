package com.example.bodytrade.client;

import com.example.bodytrade.DevTasks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Список заданий: от лёгких к сложным (по награде), выполненные - внизу. Прокрутка колесом мыши. */
public class TasksScreen extends FancyScreen {
    private static final int LH = 12;
    private final Screen parent;
    private int scroll = 0;

    public TasksScreen(Screen parent) { super(Text.literal("Задания"), 0x2ECC71, 0xF7E26B); this.parent = parent; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        fancy.clear();
        panel(480, 1000);
        btn("Назад", width / 2 - 50, py + ph - 27, 100, 20, this::close);
    }

    /** Порядок: невыполненные по возрастанию награды (лёгкие первыми), затем выполненные. */
    private List<Integer> order() {
        var tasks = DevTasks.tasks();
        List<Integer> open = new ArrayList<>(), done = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            if (i < BodyTradeClient.taskDone.length && BodyTradeClient.taskDone[i]) done.add(i); else open.add(i);
        }
        open.sort((x, z) -> Integer.compare(tasks.get(x).reward(), tasks.get(z).reward()));
        done.sort((x, z) -> Integer.compare(tasks.get(x).reward(), tasks.get(z).reward()));
        open.addAll(done);
        return open;
    }

    private int top() { return py + 46; }
    private int bottom() { return py + ph - 32; }
    private int maxScroll() { return Math.max(0, DevTasks.tasks().size() * LH - (bottom() - top())); }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 24));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    protected void drawContent(DrawContext ctx, int mx, int my, float delta) {
        if (!BodyTradeClient.develop) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Задания доступны в режиме РазВиВАЙСЯ (/bodymode develop)."), width / 2, py + 60, 0xFF9A9AAA);
            return;
        }
        var tasks = DevTasks.tasks();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Выполнено " + BodyTradeClient.tasksDoneCount() + " из " + tasks.size()
                + ". Порядок любой, лёгкие сверху."), width / 2, py + 30, 0xFF77FF88);
        int x = px + 16;
        ctx.enableScissor(px + 4, top(), px + pw - 4, bottom());
        int y = top() - scroll;
        for (int i : order()) {
            if (y > top() - LH && y < bottom()) {
                var t = tasks.get(i);
                boolean done = i < BodyTradeClient.taskDone.length && BodyTradeClient.taskDone[i];
                int prog = i < BodyTradeClient.taskProg.length ? BodyTradeClient.taskProg[i] : 0;
                String s = (done ? "[+] " : "[ ] ") + t.text() + " (" + prog + "/" + t.need() + ")  +" + t.reward() + " ч.";
                ctx.drawTextWithShadow(textRenderer, s, x, y, done ? 0xFF55AA55 : (prog > 0 ? 0xFFFFFF77 : 0xFFE6E6F0));
            }
            y += LH;
        }
        ctx.disableScissor();
        if (maxScroll() > 0) ctx.drawTextWithShadow(textRenderer, "колесо мыши - прокрутка", px + pw - 16 - textRenderer.getWidth("колесо мыши - прокрутка"), py + ph - 14, 0xFF77778A);
    }
}
