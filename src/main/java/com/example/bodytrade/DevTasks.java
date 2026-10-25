package com.example.bodytrade;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Режим "РазВиВАЙСЯ": цепочка заданий (награда - частицы жизни) и цены конечностей. */
public final class DevTasks {
    private DevTasks() {}

    public record Task(String text, int need, int reward, List<Stat<?>> stats) {}

    /** Цены конечностей в частицах жизни (порядок = порядок в меню). */
    public static final Map<BodyPart, Integer> PRICES = new LinkedHashMap<>();
    static {
        PRICES.put(BodyPart.RIGHT_ARM, 1);
        PRICES.put(BodyPart.LEFT_LEG, 3);
        PRICES.put(BodyPart.RIGHT_LEG, 6);
        PRICES.put(BodyPart.LEFT_ARM, 10);
    }

    private static List<Task> TASKS;

    private static List<Stat<?>> mined(Block... blocks) {
        List<Stat<?>> l = new ArrayList<>();
        for (Block b : blocks) l.add(Stats.MINED.getOrCreateStat(b));
        return l;
    }
    private static List<Stat<?>> crafted(Item item) { List<Stat<?>> l = new ArrayList<>(); l.add(Stats.CRAFTED.getOrCreateStat(item)); return l; }
    private static List<Stat<?>> killed(EntityType<?>... types) {
        List<Stat<?>> l = new ArrayList<>();
        for (EntityType<?> t : types) l.add(Stats.KILLED.getOrCreateStat(t));
        return l;
    }

    public static List<Task> tasks() {
        if (TASKS == null) {
            List<Task> t = new ArrayList<>();
            t.add(new Task("Добудь землю", 3, 1, mined(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT)));
            t.add(new Task("Добудь дерево", 5, 1, mined(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG,
                    Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG)));
            t.add(new Task("Сделай деревянную кирку", 1, 2, crafted(Items.WOODEN_PICKAXE)));
            t.add(new Task("Добудь камень", 8, 2, mined(Blocks.STONE)));
            t.add(new Task("Сделай каменную кирку", 1, 3, crafted(Items.STONE_PICKAXE)));
            t.add(new Task("Добудь железную руду", 5, 4, mined(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE)));
            t.add(new Task("Сделай железную кирку", 1, 5, crafted(Items.IRON_PICKAXE)));
            t.add(new Task("Убей 10 монстров", 10, 6, killed(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER)));
            t.add(new Task("Добудь алмазную руду", 1, 8, mined(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE)));
            t.add(new Task("Сделай алмазную кирку", 1, 10, crafted(Items.DIAMOND_PICKAXE)));
            TASKS = t;
        }
        return TASKS;
    }

    public static int progress(ServerPlayerEntity p, Task t) {
        int s = 0;
        for (Stat<?> st : t.stats()) s += p.getStatHandler().getStat(st);
        return s;
    }
}
