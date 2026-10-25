package com.example.bodytrade;

import net.minecraft.util.math.random.Random;

import java.util.*;

/** Все части тела. Шанс - диапазон в %, на каждой сделке берётся случайное значение из диапазона. */
public enum BodyPart {
    // голова и органы чувств
    LEFT_EAR("left_ear", "Левое ухо", "левого уха", 12, 18, Tier.COMMON,
            "Вы лишились левого уха: звуки слева теперь не слышны."),
    RIGHT_EAR("right_ear", "Правое ухо", "правого уха", 12, 18, Tier.COMMON,
            "Вы лишились правого уха: звуки справа теперь не слышны."),
    LEFT_EYE("left_eye", "Левый глаз", "левого глаза", 8, 12, Tier.VALUABLE,
            "Левый глаз потерян: левая половина экрана полностью чёрная."),
    RIGHT_EYE("right_eye", "Правый глаз", "правого глаза", 8, 12, Tier.VALUABLE,
            "Правый глаз потерян: правая половина экрана полностью чёрная."),
    NOSE("nose", "Полость носа", "полости носа", 8, 12, Tier.COMMON,
            "Без носа вы быстрее задыхаетесь под водой. Если нет и рта, дышать вы не сможете вовсе."),
    TEETH("teeth", "Зубы", "зубов", 15, 20, Tier.COMMON,
            "Без зубов вы можете есть только мягкую пищу: супы, ягоды, мёд, дыню."),
    MOUTH("mouth", "Рот", "рта", 6, 9, Tier.UNCOMMON,
            "Без рта вы не можете есть, пить и говорить в чате."),
    TONGUE("tongue", "Язык", "языка", 8, 12, Tier.COMMON,
            "Без языка вы не можете говорить, а еда насыщает хуже: вкус пропал."),
    // пищеварение
    THROAT("throat", "Глотка", "глотки", 6, 9, Tier.UNCOMMON,
            "Без глотки вы не можете глотать: ни еда, ни зелья вам недоступны."),
    ESOPHAGUS("esophagus", "Пищевод", "пищевода", 6, 9, Tier.UNCOMMON,
            "Без пищевода еда не доходит до желудка: после еды вас тошнит, а сытость пропадает."),
    STOMACH("stomach", "Желудок", "желудка", 8, 12, Tier.VALUABLE,
            "Без желудка еда насыщает вдвое хуже."),
    SMALL_INTESTINE("small_intestine", "Тонкая кишка", "тонкой кишки", 8, 12, Tier.VALUABLE,
            "Без тонкой кишки питательные вещества почти не усваиваются: еда насыщает на треть."),
    LARGE_INTESTINE("large_intestine", "Толстая кишка", "толстой кишки", 8, 12, Tier.UNCOMMON,
            "Без толстой кишки после еды вас часто ждёт расстройство."),
    LIVER("liver", "Печень", "печени", 8, 12, Tier.VALUABLE,
            "Без печени вас периодически мутит, а силы на исходе."),
    GALLBLADDER("gallbladder", "Жёлчный пузырь", "жёлчного пузыря", 12, 18, Tier.COMMON,
            "Без жёлчного пузыря любая еда вызывает тошноту."),
    PANCREAS("pancreas", "Поджелудочная железа", "поджелудочной железы", 8, 12, Tier.VALUABLE,
            "Без поджелудочной железы после еды резко скачет сахар: слабость и вялость."),
    // дыхание
    LARYNX("larynx", "Гортань", "гортани", 6, 9, Tier.UNCOMMON,
            "Без гортани вы потеряли голос: писать в чат нельзя."),
    TRACHEA("trachea", "Трахея", "трахеи", 5, 8, Tier.UNCOMMON,
            "Без трахеи вы хрипите и задыхаетесь: бег невозможен, вы медленнее."),
    BRONCHI("bronchi", "Бронхи", "бронхов", 6, 9, Tier.UNCOMMON,
            "Без бронхов вы быстро выдыхаетесь: после бега вас бросает в слабость."),
    LEFT_LUNG("left_lung", "Левое лёгкое", "левого лёгкого", 8, 12, Tier.VALUABLE,
            "Без лёгкого вы задыхаетесь: бег невозможен. Без обоих лёгких вы почти беспомощны."),
    RIGHT_LUNG("right_lung", "Правое лёгкое", "правого лёгкого", 8, 12, Tier.VALUABLE,
            "Без лёгкого вы задыхаетесь: бег невозможен. Без обоих лёгких вы почти беспомощны."),
    DIAPHRAGM("diaphragm", "Диафрагма", "диафрагмы", 6, 9, Tier.UNCOMMON,
            "Без диафрагмы вас мучает икота: вы периодически подпрыгиваете."),
    // прочие органы
    SPLEEN("spleen", "Селезёнка", "селезёнки", 10, 14, Tier.COMMON,
            "Без селезёнки иммунитет ослаб: время от времени вы подхватываете инфекцию."),
    LEFT_KIDNEY("left_kidney", "Левая почка", "левой почки", 12, 16, Tier.VALUABLE,
            "Без почки вы быстрее проголодаетесь. Без обеих почек организм отравляется."),
    RIGHT_KIDNEY("right_kidney", "Правая почка", "правой почки", 12, 16, Tier.VALUABLE,
            "Без почки вы быстрее проголодаетесь. Без обеих почек организм отравляется."),
    THYROID("thyroid", "Щитовидная железа", "щитовидной железы", 10, 14, Tier.UNCOMMON,
            "Без щитовидной железы обмен веществ замедлен: вы вялый и медленный."),
    APPENDIX("appendix", "Аппендикс", "аппендикса", 15, 20, Tier.COMMON,
            "Аппендикс был не нужен: ничего не изменилось."),
    // конечности
    LEFT_ARM("left_arm", "Левая рука", "левой руки", 25, 30, Tier.UNCOMMON,
            "Левая рука исчезла: вы не можете держать предметы во второй руке."),
    RIGHT_ARM("right_arm", "Правая рука", "правой руки", 25, 30, Tier.UNCOMMON,
            "Правая рука исчезла: вы копаете и бьёте заметно хуже. Без обеих рук предметы выпадают."),
    LEFT_LEG("left_leg", "Левая нога", "левой ноги", 22, 28, Tier.UNCOMMON,
            "Левая нога исчезла: вы хромаете, а падение с высоты 1.5+ блока наносит урон."),
    RIGHT_LEG("right_leg", "Правая нога", "правой ноги", 22, 28, Tier.UNCOMMON,
            "Правая нога исчезла: вы хромаете, а падение с высоты 1.5+ блока наносит урон."),
    LEFT_FOOT("left_foot", "Левая ступня", "левой ступни", 18, 24, Tier.COMMON,
            "Без ступни вы скользите и плохо останавливаетесь, а падения опаснее."),
    RIGHT_FOOT("right_foot", "Правая ступня", "правой ступни", 18, 24, Tier.COMMON,
            "Без ступни вы скользите и плохо останавливаетесь, а падения опаснее."),
    // мозг
    LEFT_HEMISPHERE("left_hemisphere", "Левое полушарие мозга", "левого полушария мозга", 4, 8, Tier.RARE,
            "Левое полушарие изъято: управление влево-вправо перепутано (A и D поменялись местами)."),
    RIGHT_HEMISPHERE("right_hemisphere", "Правое полушарие мозга", "правого полушария мозга", 4, 8, Tier.RARE,
            "Правое полушарие изъято: управление вперёд-назад перепутано (W и S поменялись местами).");

    public final String id, nom, gen, consequence;
    public final int minChance, maxChance;
    public final Tier tier;

    BodyPart(String id, String nom, String gen, int min, int max, Tier tier, String consequence) {
        this.id = id; this.nom = nom; this.gen = gen; this.minChance = min; this.maxChance = max;
        this.tier = tier; this.consequence = consequence;
    }

    /** Цена протеза в алмазах (0 - протеза для этой части нет). */
    public int prosthesisPrice() {
        return switch (this) { case LEFT_ARM, RIGHT_ARM -> 3; case LEFT_LEG, RIGHT_LEG -> 4; default -> 0; };
    }

    public static BodyPart byId(String id) {
        for (BodyPart p : values()) if (p.id.equals(id)) return p;
        return null;
    }

    /** Битовая маска пропавших конечностей для отрисовки: 1 лев.рука, 2 прав.рука, 4 лев.нога, 8 прав.нога, 16/32 ступни. */
    public static int limbMask(Set<BodyPart> m) {
        int x = 0;
        if (m.contains(LEFT_ARM)) x |= 1;
        if (m.contains(RIGHT_ARM)) x |= 2;
        if (m.contains(LEFT_LEG)) x |= 4;
        if (m.contains(RIGHT_LEG)) x |= 8;
        if (m.contains(LEFT_FOOT)) x |= 16;
        if (m.contains(RIGHT_FOOT)) x |= 32;
        return x;
    }

    /** Случайный выбор среди ещё не отданных частей: одну и ту же часть дважды не заберут. */
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
