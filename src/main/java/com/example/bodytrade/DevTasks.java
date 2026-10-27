package com.example.bodytrade;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Режим "РазВиВАЙСЯ": задания (выполняются в любом порядке) и цены конечностей в частицах жизни. */
public final class DevTasks {
    private DevTasks() {}

    /** div - во сколько раз делить счётчик (для расстояний: см -> блоки). */
    public record Task(String text, int need, int reward, int div, List<Stat<?>> stats) {}

    /** Цены конечностей в частицах жизни (порядок = порядок в меню). */
    public static final Map<BodyPart, Integer> PRICES = new LinkedHashMap<>();
    static {
        PRICES.put(BodyPart.RIGHT_ARM, 1);
        PRICES.put(BodyPart.LEFT_LEG, 15);
        PRICES.put(BodyPart.RIGHT_LEG, 35);
        PRICES.put(BodyPart.LEFT_ARM, 50);
    }

    private static List<Task> TASKS;

    private static List<Stat<?>> mined(Block... blocks) {
        List<Stat<?>> l = new ArrayList<>();
        for (Block b : blocks) l.add(Stats.MINED.getOrCreateStat(b));
        return l;
    }
    private static List<Stat<?>> crafted(Item... items) {
        List<Stat<?>> l = new ArrayList<>();
        for (Item i : items) l.add(Stats.CRAFTED.getOrCreateStat(i));
        return l;
    }
    private static List<Stat<?>> used(Item... items) {
        List<Stat<?>> l = new ArrayList<>();
        for (Item i : items) l.add(Stats.USED.getOrCreateStat(i));
        return l;
    }
    private static List<Stat<?>> killed(EntityType<?>... types) {
        List<Stat<?>> l = new ArrayList<>();
        for (EntityType<?> t : types) l.add(Stats.KILLED.getOrCreateStat(t));
        return l;
    }
    private static List<Stat<?>> custom(Identifier... ids) {
        List<Stat<?>> l = new ArrayList<>();
        for (Identifier i : ids) l.add(Stats.CUSTOM.getOrCreateStat(i));
        return l;
    }

    private static void a(List<Task> t, String text, int need, int reward, List<Stat<?>> stats) { t.add(new Task(text, need, reward, 1, stats)); }
    /** Задания, у которых есть повторяющиеся ступени II и III (в конце списка). */
    private static final List<Task> TIERABLE = new ArrayList<>();
    /** Множители ступеней: нужное количество и награда. */
    private static final int II_NEED = 8, II_REWARD = 10, III_NEED = 30, III_REWARD = 22;

    private static void at(List<Task> t, String text, int need, int reward, List<Stat<?>> stats) { Task k = new Task(text, need, reward, 1, stats); t.add(k); TIERABLE.add(k); }
    private static void dt(List<Task> t, String text, int need, int reward, int div, List<Stat<?>> stats) { Task k = new Task(text, need, reward, div, stats); t.add(k); TIERABLE.add(k); }

    private static void d(List<Task> t, String text, int need, int reward, int div, List<Stat<?>> stats) { t.add(new Task(text, need, reward, div, stats)); }

    /** Новые задания добавляйте только в КОНЕЦ списка, чтобы не сбить прогресс в сохранённых мирах. */
    public static List<Task> tasks() {
        if (TASKS == null) {
            List<Task> t = new ArrayList<>();
            // --- ресурсы
            at(t, "Добудь землю", 3, 1, mined(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT));
            at(t, "Добудь дерево", 5, 1, mined(Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG,
                    Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG));
            at(t, "Добудь песок", 5, 1, mined(Blocks.SAND, Blocks.RED_SAND));
            at(t, "Добудь гравий", 5, 1, mined(Blocks.GRAVEL));
            at(t, "Добудь камень", 8, 2, mined(Blocks.STONE));
            at(t, "Добудь уголь", 5, 2, mined(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE));
            at(t, "Добудь медную руду", 5, 3, mined(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE));
            at(t, "Добудь железную руду", 5, 4, mined(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE));
            at(t, "Добудь красную пыль", 8, 4, mined(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE));
            at(t, "Добудь лазурит", 8, 4, mined(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE));
            at(t, "Добудь золотую руду", 3, 5, mined(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE));
            at(t, "Добудь изумрудную руду", 1, 6, mined(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE));
            at(t, "Добудь алмазную руду", 1, 8, mined(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE));
            at(t, "Добудь обсидиан", 4, 6, mined(Blocks.OBSIDIAN));
            at(t, "Добудь адский камень", 10, 3, mined(Blocks.NETHERRACK));
            at(t, "Добудь песок душ", 5, 4, mined(Blocks.SOUL_SAND));
            at(t, "Добудь кварцевую руду", 5, 6, mined(Blocks.NETHER_QUARTZ_ORE));
            at(t, "Добудь древние обломки", 1, 15, mined(Blocks.ANCIENT_DEBRIS));
            // --- крафт
            a(t, "Сделай верстак", 1, 1, crafted(Items.CRAFTING_TABLE));
            at(t, "Сделай палки", 4, 1, crafted(Items.STICK));
            a(t, "Сделай сундук", 1, 1, crafted(Items.CHEST));
            a(t, "Сделай деревянный меч", 1, 1, crafted(Items.WOODEN_SWORD));
            a(t, "Сделай деревянную кирку", 1, 2, crafted(Items.WOODEN_PICKAXE));
            a(t, "Сделай печку", 1, 2, crafted(Items.FURNACE));
            a(t, "Сделай каменный меч", 1, 2, crafted(Items.STONE_SWORD));
            a(t, "Сделай каменную кирку", 1, 3, crafted(Items.STONE_PICKAXE));
            at(t, "Выплавь железо", 5, 3, crafted(Items.IRON_INGOT));
            a(t, "Сделай ведро", 1, 3, crafted(Items.BUCKET));
            a(t, "Сделай щит", 1, 3, crafted(Items.SHIELD));
            a(t, "Сделай железную кирку", 1, 5, crafted(Items.IRON_PICKAXE));
            a(t, "Сделай железный меч", 1, 4, crafted(Items.IRON_SWORD));
            a(t, "Сделай железный нагрудник", 1, 5, crafted(Items.IRON_CHESTPLATE));
            a(t, "Сделай алмазную кирку", 1, 10, crafted(Items.DIAMOND_PICKAXE));
            a(t, "Сделай алмазный меч", 1, 8, crafted(Items.DIAMOND_SWORD));
            a(t, "Сделай алмазный нагрудник", 1, 12, crafted(Items.DIAMOND_CHESTPLATE));
            a(t, "Сделай лук", 1, 3, crafted(Items.BOW));
            at(t, "Сделай стрелы", 8, 2, crafted(Items.ARROW));
            a(t, "Сделай арбалет", 1, 5, crafted(Items.CROSSBOW));
            a(t, "Сделай удочку", 1, 2, crafted(Items.FISHING_ROD));
            a(t, "Сделай ножницы", 1, 2, crafted(Items.SHEARS));
            a(t, "Сделай компас", 1, 4, crafted(Items.COMPASS));
            a(t, "Сделай часы", 1, 6, crafted(Items.CLOCK));
            a(t, "Сделай карту", 1, 4, crafted(Items.MAP));
            a(t, "Сделай огниво", 1, 3, crafted(Items.FLINT_AND_STEEL));
            a(t, "Сделай поршень", 1, 4, crafted(Items.PISTON));
            a(t, "Сделай воронку", 1, 5, crafted(Items.HOPPER));
            a(t, "Сделай динамит", 1, 4, crafted(Items.TNT));
            a(t, "Сделай книжную полку", 1, 4, crafted(Items.BOOKSHELF));
            a(t, "Сделай стол зачарований", 1, 10, crafted(Items.ENCHANTING_TABLE));
            at(t, "Сделай хлеб", 1, 2, crafted(Items.BREAD));
            a(t, "Сделай торт", 1, 4, crafted(Items.CAKE));
            a(t, "Сделай золотое яблоко", 1, 8, crafted(Items.GOLDEN_APPLE));
            a(t, "Сделай незеритовый слиток", 1, 25, crafted(Items.NETHERITE_INGOT));
            a(t, "Сделай маяк", 1, 35, crafted(Items.BEACON));
            // --- ферма и жизнь
            at(t, "Собери пшеницу", 5, 2, mined(Blocks.WHEAT));
            at(t, "Собери морковь", 5, 2, mined(Blocks.CARROTS));
            at(t, "Собери картофель", 5, 2, mined(Blocks.POTATOES));
            a(t, "Посади саженец", 1, 1, used(Items.OAK_SAPLING, Items.BIRCH_SAPLING, Items.SPRUCE_SAPLING, Items.JUNGLE_SAPLING,
                    Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING));
            a(t, "Съешь хлеб", 3, 2, used(Items.BREAD));
            at(t, "Разведи животных", 3, 4, custom(Stats.ANIMALS_BRED));
            at(t, "Поймай рыбу", 3, 5, custom(Stats.FISH_CAUGHT));
            at(t, "Поторгуй с жителем", 3, 5, custom(Stats.TRADED_WITH_VILLAGER));
            at(t, "Поспи в кровати", 1, 3, custom(Stats.SLEEP_IN_BED));
            at(t, "Открой сундуки", 10, 2, custom(Stats.OPEN_CHEST));
            a(t, "Зачаруй предмет", 1, 6, custom(Stats.ENCHANT_ITEM));
            a(t, "Используй наковальню", 1, 4, custom(Stats.INTERACT_WITH_ANVIL));
            a(t, "Используй варочную стойку", 1, 5, custom(Stats.INTERACT_WITH_BREWINGSTAND));
            // --- бои
            at(t, "Убей зомби", 5, 3, killed(EntityType.ZOMBIE));
            at(t, "Убей скелетов", 5, 4, killed(EntityType.SKELETON));
            at(t, "Убей пауков", 3, 3, killed(EntityType.SPIDER));
            at(t, "Убей криперов", 3, 5, killed(EntityType.CREEPER));
            at(t, "Убей эндермена", 1, 8, killed(EntityType.ENDERMAN));
            at(t, "Убей ведьму", 1, 10, killed(EntityType.WITCH));
            at(t, "Убей разбойников", 5, 6, killed(EntityType.PILLAGER));
            at(t, "Убей зомби-свинолюдей", 3, 5, killed(EntityType.ZOMBIFIED_PIGLIN));
            at(t, "Убей скелета-иссушителя", 1, 10, killed(EntityType.WITHER_SKELETON));
            at(t, "Убей ифритов", 3, 8, killed(EntityType.BLAZE));
            at(t, "Убей гаста", 1, 15, killed(EntityType.GHAST));
            a(t, "Убей древнего стража", 1, 20, killed(EntityType.ELDER_GUARDIAN));
            a(t, "Убей хранителя", 1, 30, killed(EntityType.WARDEN));
            a(t, "Убей иссушителя", 1, 40, killed(EntityType.WITHER));
            a(t, "Убей дракона Края", 1, 50, killed(EntityType.ENDER_DRAGON));
            // --- путешествия (счётчики в см делятся на 100 = блоки)
            dt(t, "Пройди 500 блоков", 500, 5, 100, custom(Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM));
            d(t, "Пройди 3000 блоков", 3000, 15, 100, custom(Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM));
            dt(t, "Проплыви 100 блоков", 100, 3, 100, custom(Stats.SWIM_ONE_CM));
            at(t, "Прыгни 100 раз", 100, 2, custom(Stats.JUMP));
            dt(t, "Проедь верхом 300 блоков", 300, 4, 100, custom(Stats.HORSE_ONE_CM));
            dt(t, "Проплыви на лодке 200 блоков", 200, 3, 100, custom(Stats.BOAT_ONE_CM));
            dt(t, "Проедь в вагонетке 200 блоков", 200, 3, 100, custom(Stats.MINECART_ONE_CM));
            dt(t, "Пролети на элитрах 500 блоков", 500, 10, 100, custom(Stats.AVIATE_ONE_CM));
            // ступени II и III повторяемых заданий (новые задания - только в конец списка)
            for (Task b : TIERABLE) t.add(new Task(b.text() + " II", b.need() * II_NEED, b.reward() * II_REWARD, b.div(), b.stats()));
            for (Task b : TIERABLE) t.add(new Task(b.text() + " III", b.need() * III_NEED, b.reward() * III_REWARD, b.div(), b.stats()));
            TASKS = t;
        }
        return TASKS;
    }

    /** Сырой счётчик задания (не вычитая стартовое значение). */
    public static int progress(ServerPlayerEntity p, Task t) {
        int s = 0;
        for (Stat<?> st : t.stats()) s += p.getStatHandler().getStat(st);
        return s;
    }
}
