package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/**
 * Импланты: в каждом слоте (глаз, ухо, рука, тело, ядро) работает один. У некоторых есть уровни (кибер-глаз - 3, механическое ухо - 4).
 * Каждый уровень стоит частицы жизни и флешку импланта. Порядок констант не менять.
 */
public enum Implant {
    CYBER_EYE("cyber_eye", "Кибер-глаз", "eye", "Подсвечивает руды и врагов цветным контуром сквозь стены. Уровни: 1 - на 4 блока, 2 - на 8 блоков, 3 - на 16 блоков. "
            + "Каждый уровень требует флешку. Включается и выключается клавишей V. Заменяет встроенный фонарь. Нужен хотя бы один глаз.",
            new int[]{250, 400, 650}, Items.ENDER_EYE),
    LANTERN("lantern", "Встроенный фонарь", "eye", "В темноте всё видно, а рассудок не падает от мрака. Заменяет кибер-глаз. Нужен хотя бы один глаз.",
            new int[]{120}, Items.LANTERN),
    BLADE_ARM("blade_arm", "Рука-клинок", "arm", "Удар пустой рукой превращается в удар клинком: +6 урона (раз в полсекунды). Нужна хотя бы одна рука.",
            new int[]{200}, Items.IRON_SWORD),
    TITAN_BONES("titan_bones", "Титановый каркас", "body", "Сопротивление урону I. Заменяет нейрочип.",
            new int[]{350}, Items.IRON_BLOCK),
    NEUROCHIP("neurochip", "Нейрочип", "body", "Рассудок падает вдвое медленнее и сам понемногу восстанавливается. Заменяет титановый каркас.",
            new int[]{300}, Items.COMPARATOR),
    CHRONO_CORE("chrono_core", "Хроно-стабилизатор", "core", "Навсегда отключает таймер распада режима Перерождение: конечности и здоровье больше не отнимаются по расписанию.",
            new int[]{0}, Items.CLOCK),
    MECH_EAR("mech_ear", "Механическое ухо", "ear", "Связь через мод Simple Voice Chat: вы слышите других игроков с таким же ухом. Уровни: 1 - в Верхнем мире на 200 блоков, "
            + "2 - на 400 блоков, 3 - по всему Верхнему миру, 4 - в любых измерениях (Нижний мир и Энд). Каждый уровень требует флешку.",
            new int[]{200, 350, 600, 1000}, Items.NOTE_BLOCK);

    public final String id, nom, slot, desc;
    /** Цена в частицах жизни для каждого уровня (длина массива = максимальный уровень). */
    public final int[] particles;
    public final Item icon;

    Implant(String id, String nom, String slot, String desc, int[] particles, Item icon) {
        this.id = id; this.nom = nom; this.slot = slot; this.desc = desc; this.particles = particles; this.icon = icon;
    }

    public int maxLevel() { return particles.length; }

    public String slotName() {
        return switch (slot) { case "eye" -> "Слот: Глаз"; case "arm" -> "Слот: Рука"; case "body" -> "Слот: Тело"; case "ear" -> "Слот: Ухо"; default -> "Слот: Ядро"; };
    }

    /** Предметы, которые тратятся на каждый уровень (вместе с частицами жизни). */
    public List<ItemStack> cost() {
        switch (this) {
            case CYBER_EYE: return List.of(new ItemStack(BodyTradeMod.FLASH_CYBER_EYE));
            case LANTERN: return List.of(new ItemStack(BodyTradeMod.FLASH_LANTERN));
            case BLADE_ARM: return List.of(new ItemStack(BodyTradeMod.FLASH_BLADE_ARM));
            case TITAN_BONES: return List.of(new ItemStack(BodyTradeMod.FLASH_TITAN_BONES));
            case NEUROCHIP: return List.of(new ItemStack(BodyTradeMod.FLASH_NEUROCHIP));
            case MECH_EAR: return List.of(new ItemStack(BodyTradeMod.FLASH_MECH_EAR));
            default: return List.of(new ItemStack(BodyTradeMod.FLASH_CHRONO), new ItemStack(BodyTradeMod.FLASH_STASIS),
                    new ItemStack(Items.DIAMOND, 128), new ItemStack(Items.NETHER_STAR, 4));
        }
    }

    public static Implant byId(String id) {
        for (Implant i : values()) if (i.id.equals(id)) return i;
        return null;
    }
}
