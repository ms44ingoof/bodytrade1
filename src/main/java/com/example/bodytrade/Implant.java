package com.example.bodytrade;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/** Импланты: в каждом слоте (глаз, рука, тело) работает один. Ставятся за частицы жизни и металл. Порядок констант не менять. */
public enum Implant {
    CYBER_EYE("cyber_eye", "Кибер-глаз", "eye", "Ночное зрение, подсветка врагов сквозь стены и рентген: раз в несколько секунд показывает ближайшие руды рядом. Заменяет встроенный фонарь. Нужен хотя бы один глаз.",
            250, Items.GOLD_INGOT, 16, Items.ENDER_EYE),
    LANTERN("lantern", "Встроенный фонарь", "eye", "В темноте всё видно, а рассудок не падает от мрака. Заменяет кибер-глаз. Нужен хотя бы один глаз.",
            120, Items.GOLD_INGOT, 8, Items.LANTERN),
    BLADE_ARM("blade_arm", "Рука-клинок", "arm", "Удар пустой рукой превращается в удар клинком: +6 урона (раз в полсекунды). Нужна хотя бы одна рука.",
            200, Items.IRON_INGOT, 24, Items.IRON_SWORD),
    TITAN_BONES("titan_bones", "Титановый каркас", "body", "Сопротивление урону I. Заменяет нейрочип.",
            350, Items.IRON_INGOT, 32, Items.IRON_BLOCK),
    NEUROCHIP("neurochip", "Нейрочип", "body", "Рассудок падает вдвое медленнее и сам понемногу восстанавливается. Заменяет титановый каркас.",
            300, Items.DIAMOND, 6, Items.COMPARATOR);

    public final String id, nom, slot, desc;
    public final int particles, metalCount;
    public final Item metal, icon;

    Implant(String id, String nom, String slot, String desc, int particles, Item metal, int metalCount, Item icon) {
        this.id = id; this.nom = nom; this.slot = slot; this.desc = desc; this.particles = particles; this.metal = metal; this.metalCount = metalCount; this.icon = icon;
    }

    public String slotName() { return slot.equals("eye") ? "Слот: Глаз" : slot.equals("arm") ? "Слот: Рука" : "Слот: Тело"; }

    public static Implant byId(String id) {
        for (Implant i : values()) if (i.id.equals(id)) return i;
        return null;
    }
}
