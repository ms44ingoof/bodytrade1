package com.example.bodytrade.client;

import com.example.bodytrade.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Кнопки мода в инвентаре. Стоят слева от окна; когда открыта книга рецептов (окно смещается вправо, а книга
 * занимает место слева), кнопки отодвигаются за книгу. Если слева места нет - переходят вправо, а если нет нигде, прячутся.
 */
public final class InventoryButtons {
    private InventoryButtons() {}

    private static final int BW = 112, BH = 20, GAP = 4, INV_W = 176, BOOK_W = 151;

    public static void install(MinecraftClient client, Screen screen) {
        List<ButtonWidget> list = new ArrayList<>();
        add(list, screen, "Болезни и части тела", () -> client.setScreen(new InfoScreen(screen)));
        add(list, screen, "Энциклопедия", () -> client.setScreen(new EncyclopediaScreen(screen)));
        add(list, screen, "Задания", () -> client.setScreen(new TasksScreen(screen)));
        add(list, screen, "Рецепты", () -> client.setScreen(new RecipesScreen(screen)));
        if (BodyTradeClient.develop) add(list, screen, "Возврат", () -> client.setScreen(new ReturnScreen(screen)));
        ScreenEvents.beforeRender(screen).register((s, ctx, mx, my, delta) -> layout(s, list));
        layout(screen, list);
    }

    private static void add(List<ButtonWidget> list, Screen screen, String label, Runnable action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label), bb -> action.run()).dimensions(0, 0, BW, BH).build();
        list.add(b);
        Screens.getButtons(screen).add(b);
    }

    private static void layout(Screen screen, List<ButtonWidget> list) {
        if (!(screen instanceof HandledScreenAccessor acc)) return;
        int x = acc.bodytrade$x(), y = acc.bodytrade$y();
        int closedX = (screen.width - INV_W) / 2;
        boolean bookOpen = x > closedX + 1;                       // окно сдвинуто вправо - значит, слева книга рецептов
        int left = bookOpen ? x - BOOK_W : x;                     // левый край того, что занято слева
        int bx;
        if (left - GAP - BW >= 2) bx = left - GAP - BW;           // слева (за книгой, если она открыта)
        else if (!bookOpen && x + INV_W + GAP + BW <= screen.width - 2) bx = x + INV_W + GAP;   // справа от окна
        else bx = Integer.MIN_VALUE;
        int by = y + 6;
        for (ButtonWidget b : list) {
            b.visible = b.active = bx != Integer.MIN_VALUE;
            if (bx != Integer.MIN_VALUE) { b.setX(bx); b.setY(by); }
            by += BH + GAP;
        }
    }
}
