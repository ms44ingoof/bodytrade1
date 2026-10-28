package com.example.bodytrade;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Рецепты предметов мода (для экрана "Рецепты"). Должны совпадать с data/bodytrade/recipes. */
public final class FlashRecipes {
    private FlashRecipes() {}

    public record Entry(String name, ItemStack result, List<ItemStack> ingredients, String note) {}

    private static ItemStack s(net.minecraft.item.Item i, int n) { return new ItemStack(i, n); }

    public static List<Entry> all() {
        List<Entry> l = new ArrayList<>();
        ItemStack prism = s(BodyTradeMod.SPECTRAL_PRISM, 1);
        l.add(new Entry("Спектральная призма", prism, List.of(s(BodyTradeMod.NEURITE, 1), s(BodyTradeMod.SELENITE, 1), s(Items.DIAMOND, 1),
                s(Items.AMETHYST_SHARD, 1), s(Items.GLASS, 1)), "Нейрит и селенит добываются из редких руд под землёй (в новых чанках)."));
        l.add(new Entry("Флешка «Кибер-глаз»", s(BodyTradeMod.FLASH_CYBER_EYE, 1), List.of(prism, s(Items.REDSTONE, 2), s(Items.IRON_INGOT, 1), s(Items.ENDER_EYE, 1)), ""));
        l.add(new Entry("Флешка «Фонарь»", s(BodyTradeMod.FLASH_LANTERN, 1), List.of(prism, s(Items.REDSTONE, 2), s(Items.IRON_INGOT, 1), s(Items.GLOWSTONE_DUST, 1)), ""));
        l.add(new Entry("Флешка «Клинок»", s(BodyTradeMod.FLASH_BLADE_ARM, 1), List.of(prism, s(Items.REDSTONE, 2), s(Items.NETHERITE_SWORD, 1)), "Незеритовый меч тратится."));
        l.add(new Entry("Флешка «Титан»", s(BodyTradeMod.FLASH_TITAN_BONES, 1), List.of(prism, s(Items.REDSTONE, 2), s(Items.IRON_INGOT, 1), s(Items.IRON_BLOCK, 1)), ""));
        l.add(new Entry("Флешка «Нейрочип»", s(BodyTradeMod.FLASH_NEUROCHIP, 1), List.of(prism, s(Items.REDSTONE, 2), s(Items.IRON_INGOT, 1), s(Items.COMPARATOR, 1)), ""));
        l.add(new Entry("Флешка «Хроно-ядро»", s(BodyTradeMod.FLASH_CHRONO, 1), List.of(s(BodyTradeMod.SPECTRAL_PRISM, 2), s(Items.REDSTONE_BLOCK, 1), s(Items.GOLD_INGOT, 2), s(Items.CLOCK, 1)), "Для Хроно-стабилизатора."));
        l.add(new Entry("Флешка «Стазис»", s(BodyTradeMod.FLASH_STASIS, 1), List.of(s(BodyTradeMod.SPECTRAL_PRISM, 2), s(Items.REDSTONE_BLOCK, 1), s(Items.DIAMOND, 1), s(Items.ENDER_PEARL, 2)), "Для Хроно-стабилизатора."));
        l.add(new Entry("Модуль апгрейда", s(BodyTradeMod.UPGRADE_DEVICE, 1), List.of(s(Items.IRON_INGOT, 4), s(Items.REDSTONE, 4), s(Items.DIAMOND, 1)), "Железо по углам, редстоун по бокам, алмаз в центре."));
        return l;
    }
}
