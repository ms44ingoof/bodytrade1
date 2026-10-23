package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;

import java.util.*;

/** Все части тела. minChance/maxChance - диапазон шанса (в %), на каждой сделке выбирается случайное значение из диапазона. */
public enum BodyPart {
    LEFT_EAR("left_ear", "Левое ухо", "левого уха", 12, 18, Items.COAL, 2, 6,
            "Вы лишились левого уха - теперь вы не слышите звуки слева.", false),
    RIGHT_EAR("right_ear", "Правое ухо", "правого уха", 12, 18, Items.COAL, 2, 6,
            "Вы лишились правого уха - теперь вы не слышите звуки справа.", false),
    LEFT_EYE("left_eye", "Левый глаз", "левого глаза", 8, 12, Items.IRON_INGOT, 3, 9,
            "Левый глаз потерян - левая половина зрения закрыта тьмой.", false),
    RIGHT_EYE("right_eye", "Правый глаз", "правого глаза", 8, 12, Items.IRON_INGOT, 3, 9,
            "Правый глаз потерян - правая половина зрения закрыта тьмой.", false),
    LEFT_ARM("left_arm", "Левая рука", "левой руки", 30, 40, Items.GOLD_INGOT, 2, 8,
            "Без левой руки вы не можете держать предметы во второй руке.", false),
    RIGHT_ARM("right_arm", "Правая рука", "правой руки", 30, 40, Items.GOLD_INGOT, 2, 8,
            "Без правой руки вы копаете и бьёте заметно хуже.", false),
    LEFT_LEG("left_leg", "Левая нога", "левой ноги", 25, 35, Items.EMERALD, 1, 5,
            "У вас сломана левая нога - вы хромаете, а падение с высоты 1.5+ блока наносит урон.", false),
    RIGHT_LEG("right_leg", "Правая нога", "правой ноги", 25, 35, Items.EMERALD, 1, 5,
            "У вас сломана правая нога - вы хромаете, а падение с высоты 1.5+ блока наносит урон.", false),
    KIDNEY("kidney", "Почка", "почки", 15, 20, Items.LAPIS_LAZULI, 8, 20,
            "Без почки вы быстрее проголодаетесь.", false),
    LIVER("liver", "Печень", "печени", 10, 15, Items.REDSTONE, 10, 24,
            "Без печени вас периодически мутит.", false),
    LUNG("lung", "Лёгкое", "лёгкого", 8, 12, Items.COPPER_INGOT, 6, 14,
            "Без лёгкого вы задыхаетесь: бег невозможен, силы на исходе.", false),
    BRAIN("brain", "Мозг", "мозга", 4, 10, Items.DIAMOND, 1, 0,
            "Мозг изъят. Вы мертвы.", true);

    public final String id, nom, gen, consequence;
    public final int minChance, maxChance, rewardCount, refundCount;
    public final Item reward;
    public final boolean fatal;

    BodyPart(String id, String nom, String gen, int min, int max, Item reward, int rewardCount, int refundCount,
             String consequence, boolean fatal) {
        this.id = id; this.nom = nom; this.gen = gen; this.minChance = min; this.maxChance = max;
        this.reward = reward; this.rewardCount = rewardCount; this.refundCount = refundCount;
        this.consequence = consequence; this.fatal = fatal;
    }

    public static BodyPart byId(String id) {
        for (BodyPart p : values()) if (p.id.equals(id)) return p;
        return null;
    }

    /** Случайный выбор среди ещё не отданных частей (исключение: одну и ту же часть дважды не заберут). */
    public static BodyPart roll(Set<BodyPart> missing, Random r) {
        List<BodyPart> av = new ArrayList<>();
        List<Double> w = new ArrayList<>();
        double total = 0;
        for (BodyPart p : values()) {
            if (missing.contains(p)) continue;
            double x = p.minChance + r.nextDouble() * (p.maxChance - p.minChance);
            av.add(p); w.add(x); total += x;
        }
        if (av.isEmpty()) return null;
        double t = r.nextDouble() * total;
        for (int i = 0; i < av.size(); i++) { t -= w.get(i); if (t <= 0) return av.get(i); }
        return av.get(av.size() - 1);
    }
}
