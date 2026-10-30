package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Рецепты предметов мода (для экрана "Рецепты"). Все рецепты - с фигурным расположением на верстаке; сгенерировано из data/bodytrade/recipes. */
public final class FlashRecipes {
    private FlashRecipes() {}

    public record Entry(String name, ItemStack result, List<ItemStack> ingredients, String note) {}

    private static ItemStack s(Item i, int n) { return new ItemStack(i, n); }

    public static List<Entry> all() {
        List<Entry> l = new ArrayList<>();
        l.add(new Entry("Спектральная призма", s(BodyTradeMod.SPECTRAL_PRISM, 1), List.of(s(BodyTradeMod.NEURITE, 2), s(Items.DIAMOND_BLOCK, 1), s(BodyTradeMod.SELENITE, 2), s(Items.AMETHYST_BLOCK, 1), s(Items.TINTED_GLASS, 2), s(Items.ENDER_EYE, 1)), "Нейрит и селенит - редкие руды (в новых чанках под землёй)."));
        l.add(new Entry("Флешка «Кибер-глаз»", s(BodyTradeMod.FLASH_CYBER_EYE, 1), List.of(s(Items.DIAMOND, 2), s(Items.ENDER_EYE, 2), s(Items.REDSTONE_BLOCK, 2), s(BodyTradeMod.SPECTRAL_PRISM, 1), s(Items.GOLD_INGOT, 2)), "Нужна на каждый из 3 уровней кибер-глаза."));
        l.add(new Entry("Флешка «Фонарь»", s(BodyTradeMod.FLASH_LANTERN, 1), List.of(s(Items.GOLD_INGOT, 4), s(Items.SEA_LANTERN, 1), s(Items.REDSTONE_BLOCK, 2), s(BodyTradeMod.SPECTRAL_PRISM, 1), s(Items.GLOWSTONE, 1)), ""));
        l.add(new Entry("Флешка «Клинок»", s(BodyTradeMod.FLASH_BLADE_ARM, 1), List.of(s(Items.IRON_BLOCK, 4), s(Items.NETHERITE_SWORD, 1), s(Items.REDSTONE_BLOCK, 2), s(BodyTradeMod.SPECTRAL_PRISM, 1), s(Items.DIAMOND_BLOCK, 1)), "Незеритовый меч тратится."));
        l.add(new Entry("Флешка «Титан»", s(BodyTradeMod.FLASH_TITAN_BONES, 1), List.of(s(Items.IRON_BLOCK, 4), s(Items.BONE_BLOCK, 2), s(Items.REDSTONE_BLOCK, 2), s(BodyTradeMod.SPECTRAL_PRISM, 1)), ""));
        l.add(new Entry("Флешка «Нейрочип»", s(BodyTradeMod.FLASH_NEUROCHIP, 1), List.of(s(Items.COMPARATOR, 4), s(Items.REDSTONE_BLOCK, 2), s(Items.QUARTZ_BLOCK, 2), s(BodyTradeMod.SPECTRAL_PRISM, 1)), ""));
        l.add(new Entry("Флешка «Хроно-ядро»", s(BodyTradeMod.FLASH_CHRONO, 1), List.of(s(BodyTradeMod.SPECTRAL_PRISM, 2), s(Items.GOLD_BLOCK, 3), s(Items.REDSTONE_BLOCK, 2), s(Items.CLOCK, 1), s(Items.DIAMOND_BLOCK, 1)), "Для Хроно-стабилизатора."));
        l.add(new Entry("Флешка «Стазис»", s(BodyTradeMod.FLASH_STASIS, 1), List.of(s(BodyTradeMod.SPECTRAL_PRISM, 2), s(Items.ENDER_EYE, 1), s(Items.OBSIDIAN, 2), s(Items.AMETHYST_BLOCK, 1), s(Items.DIAMOND, 2), s(Items.ENDER_PEARL, 1)), "Для Хроно-стабилизатора."));
        l.add(new Entry("Флешка «Механическое ухо»", s(BodyTradeMod.FLASH_MECH_EAR, 1), List.of(s(Items.NOTE_BLOCK, 2), s(Items.JUKEBOX, 1), s(Items.REDSTONE_BLOCK, 2), s(BodyTradeMod.SPECTRAL_PRISM, 1), s(Items.AMETHYST_BLOCK, 2), s(Items.ENDER_EYE, 1)), "Нужна на каждый из 4 уровней механического уха."));
        l.add(new Entry("Модуль апгрейда", s(BodyTradeMod.UPGRADE_DEVICE, 1), List.of(s(Items.IRON_INGOT, 4), s(Items.REDSTONE, 4), s(Items.DIAMOND, 1)), "Не выдаётся: его нужно скрафтить."));
        l.add(new Entry("Синтезатор жизни", s(BodyTradeMod.LIFE_CONVERTER.asItem(), 1), List.of(s(Items.NETHERITE_INGOT, 4), s(Items.NETHER_STAR, 1), s(Items.DIAMOND_BLOCK, 2), s(Items.BEACON, 1), s(BodyTradeMod.SPECTRAL_PRISM, 1)), "Очень дорогой блок: обменивает алмазы на частицы жизни 1:1."));
        return l;
    }
}
