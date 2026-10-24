package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;

/** Ценность награды: из пула своего уровня выпадает случайный предмет в случайном количестве. */
public enum Tier {
    COMMON("Обычная", Items.IRON_INGOT, 6,
            new Reward(Items.COAL, 2, 4), new Reward(Items.COPPER_INGOT, 2, 5), new Reward(Items.LEATHER, 1, 3),
            new Reward(Items.BONE, 2, 5), new Reward(Items.IRON_NUGGET, 3, 8), new Reward(Items.FLINT, 2, 4)),
    UNCOMMON("Средняя", Items.GOLD_INGOT, 8,
            new Reward(Items.IRON_INGOT, 1, 4), new Reward(Items.GOLD_INGOT, 1, 3), new Reward(Items.REDSTONE, 4, 9),
            new Reward(Items.LAPIS_LAZULI, 4, 9), new Reward(Items.QUARTZ, 2, 4)),
    VALUABLE("Ценная", Items.DIAMOND, 4,
            new Reward(Items.EMERALD, 1, 3), new Reward(Items.GOLD_INGOT, 3, 5), new Reward(Items.DIAMOND, 1, 2),
            new Reward(Items.EXPERIENCE_BOTTLE, 3, 6), new Reward(Items.ENDER_PEARL, 1, 2)),
    RARE("Редкая", Items.DIAMOND, 8,
            new Reward(Items.DIAMOND, 2, 4), new Reward(Items.NETHERITE_SCRAP, 1, 2), new Reward(Items.ENDER_PEARL, 2, 4),
            new Reward(Items.GOLDEN_APPLE, 1, 1));

    public record Reward(Item item, int min, int max) {}

    public final String title;
    public final Item refundItem;
    public final int refundCount;
    public final Reward[] pool;

    Tier(String title, Item refundItem, int refundCount, Reward... pool) {
        this.title = title; this.refundItem = refundItem; this.refundCount = refundCount; this.pool = pool;
    }

    public ItemStack roll(Random r) {
        Reward w = pool[r.nextInt(pool.length)];
        return new ItemStack(w.item(), w.min() + r.nextInt(w.max() - w.min() + 1));
    }
}
