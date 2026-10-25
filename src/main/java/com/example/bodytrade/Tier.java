package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.*;

/** Ценность награды. За часть тела выдаётся СЛУЧАЙНЫЙ предмет из всей игры, количество зависит от ценности. */
public enum Tier {
    COMMON("Обычная", Items.IRON_INGOT, 6, 3),
    UNCOMMON("Средняя", Items.GOLD_INGOT, 8, 5),
    VALUABLE("Ценная", Items.DIAMOND, 4, 8),
    RARE("Редкая", Items.DIAMOND, 8, 16);

    public final String title;
    public final Item refundItem;
    public final int refundCount, maxCount;

    Tier(String title, Item refundItem, int refundCount, int maxCount) {
        this.title = title; this.refundItem = refundItem; this.refundCount = refundCount; this.maxCount = maxCount;
    }

    private static List<Item> POOL;

    private static List<Item> pool() {
        if (POOL == null) {
            Set<Item> banned = new HashSet<>(Arrays.asList(Items.AIR, Items.BEDROCK, Items.BARRIER, Items.COMMAND_BLOCK,
                    Items.CHAIN_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK, Items.COMMAND_BLOCK_MINECART, Items.STRUCTURE_BLOCK,
                    Items.STRUCTURE_VOID, Items.JIGSAW, Items.DEBUG_STICK, Items.KNOWLEDGE_BOOK, Items.LIGHT, Items.SPAWNER,
                    Items.REINFORCED_DEEPSLATE, Items.END_PORTAL_FRAME, Items.PETRIFIED_OAK_SLAB, Items.FARMLAND,
                    Items.CHORUS_PLANT, Items.BUDDING_AMETHYST));
            List<Item> list = new ArrayList<>();
            for (Item i : Registries.ITEM) {
                Identifier id = Registries.ITEM.getId(i);
                if (!id.getNamespace().equals("minecraft") || banned.contains(i)) continue;
                list.add(i);
            }
            POOL = list;
        }
        return POOL;
    }

    public ItemStack roll(Random r) {
        List<Item> p = pool();
        Item it = p.get(r.nextInt(p.size()));
        int n = Math.min(1 + r.nextInt(maxCount), it.getMaxCount());
        return new ItemStack(it, Math.max(1, n));
    }
}
