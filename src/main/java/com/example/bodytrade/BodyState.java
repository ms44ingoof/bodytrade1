package com.example.bodytrade;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.*;

/** Сохраняемое состояние: части тела, протезы, болезни, режим развития, задания, апгрейды, выдача алтаря. */
public class BodyState extends PersistentState {
    private final Map<UUID, EnumSet<BodyPart>> missing = new HashMap<>();
    private final Map<UUID, EnumMap<SideEffect, Long>> effects = new HashMap<>();
    private final Map<UUID, EnumSet<BodyPart>> pros = new HashMap<>();
    private final Set<UUID> gaveBlock = new HashSet<>();
    private final Set<UUID> develop = new HashSet<>();
    private final Set<UUID> drillOff = new HashSet<>();
    private final Set<UUID> espOff = new HashSet<>();
    private final Set<UUID> gaveAltar = new HashSet<>();
    private final Map<UUID, Integer> decay = new HashMap<>();           // тики до следующего "распада" (режим Перерождение)
    private final Map<UUID, Integer> sanity = new HashMap<>();          // рассудок в десятых долях процента (0..1000)
    private final Map<UUID, Map<String, Integer>> implants = new HashMap<>(); // импланты: id -> уровень
    private final Map<UUID, Set<String>> lore = new HashMap<>();         // открытые записи энциклопедии
    private final Map<UUID, int[]> taskBase = new HashMap<>();   // стартовое значение счётчика каждого задания (-1 - ещё не задано)
    private final Map<UUID, int[]> taskDone = new HashMap<>();   // 1 - задание выполнено
    private final Map<UUID, int[]> upgrades = new HashMap<>();   // уровни апгрейдов (индекс = Upgrade.ordinal())
    private final Map<UUID, Integer> story = new HashMap<>();    // этап сюжета (см. Story): нет записи = сюжета нет

    public static BodyState of(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(BodyState::fromNbt, BodyState::new, "bodytrade");
    }

    public Set<BodyPart> get(UUID id) { return missing.computeIfAbsent(id, k -> EnumSet.noneOf(BodyPart.class)); }
    public void add(UUID id, BodyPart p) { get(id).add(p); markDirty(); }
    public void remove(UUID id, BodyPart p) { get(id).remove(p); markDirty(); }
    public void clear(UUID id) { get(id).clear(); pros(id).clear(); effects(id).clear(); markDirty(); }
    public int story(UUID id) { return story.getOrDefault(id, Story.NONE); }
    public void setStory(UUID id, int v) { story.put(id, v); markDirty(); }
    public boolean markGaveBlock(UUID id) { if (gaveBlock.add(id)) { markDirty(); return true; } return false; }

    public int sanity(UUID id) { return sanity.getOrDefault(id, 1000); }
    public void setSanity(UUID id, int v) { sanity.put(id, Math.max(0, Math.min(1000, v))); markDirty(); }
    public Map<String, Integer> implantMap(UUID id) { return implants.computeIfAbsent(id, k -> new HashMap<>()); }
    public int implantLevel(UUID id, String key) { return implantMap(id).getOrDefault(key, 0); }
    public void setImplantLevel(UUID id, String key, int lv) { if (lv <= 0) implantMap(id).remove(key); else implantMap(id).put(key, lv); markDirty(); }
    public boolean hasImplant(UUID id, String key) { return implantLevel(id, key) > 0; }
    public Set<String> lore(UUID id) { return lore.computeIfAbsent(id, k -> new HashSet<>()); }
    /** true, если запись открыта впервые. */
    public boolean unlock(UUID id, String key) { if (lore(id).add(key)) { markDirty(); return true; } return false; }
    public static final int DECAY_TICKS = 15 * 60 * 20;
    public int decay(UUID id) { return decay.getOrDefault(id, DECAY_TICKS); }
    /** Без markDirty: вызывается каждый тик, сохраняется раз в несколько секунд. */
    public void setDecay(UUID id, int ticks) { decay.put(id, ticks); }
    public boolean isEspOn(UUID id) { return !espOff.contains(id); }
    public void setEspOn(UUID id, boolean on) { if (on) espOff.remove(id); else espOff.add(id); markDirty(); }
    /** true, если алтарь обмена выдаётся этому игроку впервые. */
    public boolean markGaveAltar(UUID id) { if (gaveAltar.add(id)) { markDirty(); return true; } return false; }
    public boolean isDrillOn(UUID id) { return !drillOff.contains(id); }
    public void setDrillOn(UUID id, boolean on) { if (on) drillOff.remove(id); else drillOff.add(id); markDirty(); }
    public boolean isDevelop(UUID id) { return develop.contains(id); }
    public void setDevelop(UUID id, boolean v) { if (v) develop.add(id); else develop.remove(id); markDirty(); }

    private static int[] sized(Map<UUID, int[]> map, UUID id, int n, int fill) {
        int[] a = map.get(id);
        if (a == null || a.length != n) {
            int[] b = new int[n];
            Arrays.fill(b, fill);
            if (a != null) System.arraycopy(a, 0, b, 0, Math.min(a.length, n));
            map.put(id, b);
            a = b;
        }
        return a;
    }
    /** Стартовые значения счётчиков заданий (-1 - не задано). Массив можно менять, потом вызвать markDirty(). */
    public int[] taskBase(UUID id, int n) { return sized(taskBase, id, n, -1); }
    public int[] taskDone(UUID id, int n) { return sized(taskDone, id, n, 0); }
    public int[] upgrades(UUID id, int n) { return sized(upgrades, id, n, 0); }

    /** Установленные протезы (подмножество отсутствующих частей). */
    public Set<BodyPart> pros(UUID id) { return pros.computeIfAbsent(id, k -> EnumSet.noneOf(BodyPart.class)); }
    public void addPros(UUID id, BodyPart p) { pros(id).add(p); markDirty(); }
    public void removePros(UUID id, BodyPart p) { pros(id).remove(p); markDirty(); }
    /** Части, отсутствие которых реально влияет на игрока (без тех, что заменены протезом). */
    public Set<BodyPart> functional(UUID id) {
        EnumSet<BodyPart> s = EnumSet.noneOf(BodyPart.class);
        s.addAll(get(id)); s.removeAll(pros(id));
        return s;
    }

    /** Болезни: эффект -> игровое время (тики), когда он закончится. */
    public Map<SideEffect, Long> effects(UUID id) { return effects.computeIfAbsent(id, k -> new EnumMap<>(SideEffect.class)); }
    public void setEffect(UUID id, SideEffect e, long endTime) { effects(id).put(e, endTime); markDirty(); }
    public void prune(UUID id, long now) { if (effects(id).values().removeIf(t -> t <= now)) markDirty(); }

    private static NbtList idList(Set<BodyPart> set) {
        NbtList l = new NbtList();
        for (BodyPart p : set) l.add(NbtString.of(p.id));
        return l;
    }
    private static EnumSet<BodyPart> readSet(NbtList l) {
        EnumSet<BodyPart> set = EnumSet.noneOf(BodyPart.class);
        for (int i = 0; i < l.size(); i++) { BodyPart p = BodyPart.byId(l.getString(i)); if (p != null) set.add(p); }
        return set;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();
        missing.forEach((id, set) -> players.put(id.toString(), idList(set)));
        nbt.put("players", players);
        NbtCompound pr = new NbtCompound();
        pros.forEach((id, set) -> pr.put(id.toString(), idList(set)));
        nbt.put("pros", pr);
        NbtCompound fx = new NbtCompound();
        effects.forEach((id, map) -> {
            NbtCompound c = new NbtCompound();
            map.forEach((e, t) -> c.putLong(e.name(), t));
            fx.put(id.toString(), c);
        });
        nbt.put("effects", fx);
        NbtList g = new NbtList();
        for (UUID id : gaveBlock) g.add(NbtString.of(id.toString()));
        nbt.put("gave", g);
        NbtList dv = new NbtList();
        for (UUID id : develop) dv.add(NbtString.of(id.toString()));
        nbt.put("develop", dv);
        NbtList dof = new NbtList();
        for (UUID id : drillOff) dof.add(NbtString.of(id.toString()));
        nbt.put("drillOff", dof);
        NbtCompound tb = new NbtCompound(), td = new NbtCompound(), up = new NbtCompound();
        taskBase.forEach((id, a) -> tb.putIntArray(id.toString(), a));
        taskDone.forEach((id, a) -> td.putIntArray(id.toString(), a));
        upgrades.forEach((id, a) -> up.putIntArray(id.toString(), a));
        nbt.put("taskBase", tb);
        nbt.put("taskDone", td);
        nbt.put("upgrades", up);
        NbtCompound sn = new NbtCompound(), im = new NbtCompound(), lr = new NbtCompound();
        sanity.forEach((id, v) -> sn.putInt(id.toString(), v));
        implants.forEach((id, map) -> { NbtCompound c = new NbtCompound(); map.forEach((k, v) -> c.putInt(k, v)); im.put(id.toString(), c); });
        lore.forEach((id, set) -> { NbtList l = new NbtList(); for (String k : set) l.add(NbtString.of(k)); lr.put(id.toString(), l); });
        NbtCompound dc = new NbtCompound();
        decay.forEach((id, v) -> dc.putInt(id.toString(), v));
        nbt.put("decay", dc);
        NbtList eo = new NbtList();
        for (UUID id : espOff) eo.add(NbtString.of(id.toString()));
        nbt.put("espOff", eo);
        NbtList ga = new NbtList();
        for (UUID id : gaveAltar) ga.add(NbtString.of(id.toString()));
        nbt.put("gaveAltar", ga);
        NbtCompound stc = new NbtCompound();
        story.forEach((id, v) -> stc.putInt(id.toString(), v));
        nbt.put("story", stc);
        nbt.put("sanity", sn);
        nbt.put("implants", im);
        nbt.put("lore", lr);
        return nbt;
    }

    public static BodyState fromNbt(NbtCompound nbt) {
        BodyState s = new BodyState();
        NbtCompound players = nbt.getCompound("players");
        for (String key : players.getKeys()) s.missing.put(UUID.fromString(key), readSet(players.getList(key, NbtElement.STRING_TYPE)));
        NbtCompound pr = nbt.getCompound("pros");
        for (String key : pr.getKeys()) s.pros.put(UUID.fromString(key), readSet(pr.getList(key, NbtElement.STRING_TYPE)));
        NbtCompound fx = nbt.getCompound("effects");
        for (String key : fx.getKeys()) {
            NbtCompound c = fx.getCompound(key);
            EnumMap<SideEffect, Long> map = new EnumMap<>(SideEffect.class);
            for (SideEffect e : SideEffect.values()) if (c.contains(e.name())) map.put(e, c.getLong(e.name()));
            s.effects.put(UUID.fromString(key), map);
        }
        NbtList g = nbt.getList("gave", NbtElement.STRING_TYPE);
        for (int i = 0; i < g.size(); i++) s.gaveBlock.add(UUID.fromString(g.getString(i)));
        NbtList dv = nbt.getList("develop", NbtElement.STRING_TYPE);
        for (int i = 0; i < dv.size(); i++) s.develop.add(UUID.fromString(dv.getString(i)));
        NbtList dof = nbt.getList("drillOff", NbtElement.STRING_TYPE);
        for (int i = 0; i < dof.size(); i++) s.drillOff.add(UUID.fromString(dof.getString(i)));
        NbtCompound tb = nbt.getCompound("taskBase"), td = nbt.getCompound("taskDone"), up = nbt.getCompound("upgrades");
        for (String key : tb.getKeys()) s.taskBase.put(UUID.fromString(key), tb.getIntArray(key));
        for (String key : td.getKeys()) s.taskDone.put(UUID.fromString(key), td.getIntArray(key));
        for (String key : up.getKeys()) s.upgrades.put(UUID.fromString(key), up.getIntArray(key));
        NbtCompound sn = nbt.getCompound("sanity"), im = nbt.getCompound("implants"), lr = nbt.getCompound("lore");
        for (String key : sn.getKeys()) s.sanity.put(UUID.fromString(key), sn.getInt(key));
        NbtList eo = nbt.getList("espOff", NbtElement.STRING_TYPE);
        for (int i = 0; i < eo.size(); i++) s.espOff.add(UUID.fromString(eo.getString(i)));
        NbtList ga = nbt.getList("gaveAltar", NbtElement.STRING_TYPE);
        for (int i = 0; i < ga.size(); i++) s.gaveAltar.add(UUID.fromString(ga.getString(i)));
        NbtCompound dc = nbt.getCompound("decay");
        for (String key : dc.getKeys()) s.decay.put(UUID.fromString(key), dc.getInt(key));
        for (String key : im.getKeys()) {
            Map<String, Integer> map = new HashMap<>();
            NbtElement el = im.get(key);
            if (el instanceof NbtList l) { for (int i = 0; i < l.size(); i++) map.put(l.getString(i), 1); }   // старый формат: просто список
            else if (el instanceof NbtCompound c) { for (String k : c.getKeys()) map.put(k, c.getInt(k)); }
            s.implants.put(UUID.fromString(key), map);
        }
        for (String key : lr.getKeys()) { Set<String> set = new HashSet<>(); NbtList l = lr.getList(key, NbtElement.STRING_TYPE); for (int i = 0; i < l.size(); i++) set.add(l.getString(i)); s.lore.put(UUID.fromString(key), set); }
        NbtCompound stc = nbt.getCompound("story");
        for (String key : stc.getKeys()) s.story.put(UUID.fromString(key), stc.getInt(key));
        return s;
    }
}
