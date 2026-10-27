package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/** Импланты: в каждом слоте (глаз, рука, тело, ядро) работает один. Цена - частицы жизни и флешки (а для ядра ещё алмазы и звёзды Незера). Порядок констант не менять. */
public enum Implant {
    CYBER_EYE("cyber_eye", "Кибер-глаз", "eye", "Подсвечивает руды сквозь стены цветным контуром и выделяет врагов сквозь стены. Заменяет встроенный фонарь. Нужен хотя бы один глаз.",
            250, Items.ENDER_EYE),
    LANTERN("lantern", "Встроенный фонарь", "eye", "В темноте всё видно, а рассудок не падает от мрака. Заменяет кибер-глаз. Нужен хотя бы один глаз.",
            120, Items.LANTERN),
    BLADE_ARM("blade_arm", "Рука-клинок", "arm", "Удар пустой рукой превращается в удар клинком: +6 урона (раз в полсекунды). Нужна хотя бы одна рука.",
            200, Items.IRON_SWORD),
    TITAN_BONES("titan_bones", "Титановый каркас", "body", "Сопротивление урону I. Заменяет нейрочип.",
            350, Items.IRON_BLOCK),
    NEUROCHIP("neurochip", "Нейрочип", "body", "Рассудок падает вдвое медленнее и сам понемногу восстанавливается. Заменяет титановый каркас.",
            300, Items.COMPARATOR),
    CHRONO_CORE("chrono_core", "Хроно-стабилизатор", "core", "Навсегда отключает таймер распада режима РазВиВАЙСЯ: конечности и здоровье больше не отнимаются по расписанию.",
            0, Items.CLOCK);

    public final String id, nom, slot, desc;
    public final int particles;
    public final Item icon;

    Implant(String id, String nom, String slot, String desc, int particles, Item icon) {
        this.id = id; this.nom = nom; this.slot = slot; this.desc = desc; this.particles = particles; this.icon = icon;
    }

    public String slotName() {
        return switch (slot) { case "eye" -> "Слот: Глаз"; case "arm" -> "Слот: Рука"; case "body" -> "Слот: Тело"; default -> "Слот: Ядро"; };
    }

    /** Предметы, которые тратятся при установке (вместе с частицами жизни). */
    public List<ItemStack> cost() {
        switch (this) {
            case CYBER_EYE: return List.of(new ItemStack(BodyTradeMod.FLASH_CYBER_EYE));
            case LANTERN: return List.of(new ItemStack(BodyTradeMod.FLASH_LANTERN));
            case BLADE_ARM: return List.of(new ItemStack(BodyTradeMod.FLASH_BLADE_ARM));
            case TITAN_BONES: return List.of(new ItemStack(BodyTradeMod.FLASH_TITAN_BONES));
            case NEUROCHIP: return List.of(new ItemStack(BodyTradeMod.FLASH_NEUROCHIP));
            default: return List.of(new ItemStack(BodyTradeMod.FLASH_CHRONO), new ItemStack(BodyTradeMod.FLASH_STASIS),
                    new ItemStack(Items.DIAMOND, 128), new ItemStack(Items.NETHER_STAR, 4));
        }
    }

    public static Implant byId(String id) {
        for (Implant i : values()) if (i.id.equals(id)) return i;
        return null;
    }
}
