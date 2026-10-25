package com.example.bodytrade.client;

import com.example.bodytrade.DevTasks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Список всех заданий режима РазВиВАЙСЯ (прокрутка колесом мыши). Невыполненные - сверху, ближайшие к цели - первыми. */
public class TasksScreen extends Screen {
    private static final int TOP = 36, LH = 11;
    private final Screen parent;
    private int scroll = 0;

    public TasksScreen(Screen parent) { super(Text.literal("Задания")); this.parent = parent; }

    public void refresh() { clearAndInit(); }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> close()).dimensions(width / 2 - 40, height - 24, 80, 20).build());
    }

    private List<Integer> order() {
        var tasks = DevTasks.tasks();
        List<Integer> open = new ArrayList<>(), done = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            if (i < BodyTradeClient.taskDone.length && BodyTradeClient.taskDone[i]) done.add(i); else open.add(i);
        }
        open.sort((x, z) -> Double.compare(ratio(z), ratio(x)));
        open.addAll(done);
        return open;
    }

    private double ratio(int i) {
        if (i >= BodyTradeClient.taskProg.length) return 0;
        return (double) BodyTradeClient.taskProg[i] / DevTasks.tasks().get(i).need();
    }

    private int maxScroll() { return Math.max(0, DevTasks.tasks().size() * LH - (height - TOP - 30)); }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = (int) Math.max(0, Math.min(maxScroll(), scroll - amount * 22));
        return true;
    }

    @Override
    public void close() { client.setScreen(parent); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
        if (!BodyTradeClient.develop) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Задания доступны в режиме РазВиВАЙСЯ (/bodymode develop)."), width / 2, 60, 0xAAAAAA);
            super.render(ctx, mx, my, delta);
            return;
        }
        var tasks = DevTasks.tasks();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Выполнено: " + BodyTradeClient.tasksDoneCount() + " из " + tasks.size()
                + ". Задания можно выполнять в любом порядке."), width / 2, 22, 0x77FF77);
        int x = Math.max(10, width / 2 - 200);
        ctx.enableScissor(0, TOP, width, height - 30);
        int y = TOP - scroll;
        for (int i : order()) {
            if (y > TOP - LH && y < height - 30) {
                var t = tasks.get(i);
                boolean done = i < BodyTradeClient.taskDone.length && BodyTradeClient.taskDone[i];
                int prog = i < BodyTradeClient.taskProg.length ? BodyTradeClient.taskProg[i] : 0;
                String s = (done ? "[+] " : "[ ] ") + t.text() + " (" + prog + "/" + t.need() + ")  награда: " + t.reward() + " ч.";
                ctx.drawTextWithShadow(textRenderer, s, x, y, done ? 0x55AA55 : (prog > 0 ? 0xFFFF77 : 0xFFFFFF));
            }
            y += LH;
        }
        ctx.disableScissor();
        super.render(ctx, mx, my, delta);
    }
}
