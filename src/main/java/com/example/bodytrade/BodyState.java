package com.example.bodytrade;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.*;

/** Сохраняемое состояние: отсутствующие части тела, активные болезни, кому выдан алтарь. */
public class BodyState extends PersistentState {
    private final Map<UUID, EnumSet<BodyPart>> missing = new HashMap<>();
    private final Map<UUID, EnumMap<SideEffect, Long>> effects = new HashMap<>();
    private final Map<UUID, EnumSet<BodyPart>> pros = new HashMap<>();
    private final Set<UUID> gaveBlock = new HashSet<>();
    private final Set<UUID> develop = new HashSet<>();
    private final Map<UUID, int[]> tasks = new HashMap<>(); // [номер задания, стартовый прогресс]

    public static BodyState of(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(BodyState::fromNbt, BodyState::new, "bodytrade");
    }

    public Set<BodyPart> get(UUID id) { return missing.computeIfAbsent(id, k -> EnumSet.noneOf(BodyPart.class)); }
    public void add(UUID id, BodyPart p) { get(id).add(p); markDirty(); }
    public void remove(UUID id, BodyPart p) { get(id).remove(p); markDirty(); }
    public void clear(UUID id) { get(id).clear(); pros(id).clear(); effects(id).clear(); markDirty(); }
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
    public boolean isDevelop(UUID id) { return develop.contains(id); }
    public void setDevelop(UUID id, boolean v) { if (v) develop.add(id); else develop.remove(id); markDirty(); }
    public int taskIndex(UUID id) { int[] t = tasks.get(id); return t == null ? 0 : t[0]; }
    public int taskBase(UUID id) { int[] t = tasks.get(id); return t == null ? 0 : t[1]; }
    public void setTask(UUID id, int idx, int base) { tasks.put(id, new int[]{idx, base}); markDirty(); }
    public boolean markGaveBlock(UUID id) { if (gaveBlock.add(id)) { markDirty(); return true; } return false; }

    /** Болезни: эффект -> игровое время (тики), когда он закончится. */
    public Map<SideEffect, Long> effects(UUID id) { return effects.computeIfAbsent(id, k -> new EnumMap<>(SideEffect.class)); }
    public void setEffect(UUID id, SideEffect e, long endTime) { effects(id).put(e, endTime); markDirty(); }
    public void prune(UUID id, long now) { if (effects(id).values().removeIf(t -> t <= now)) markDirty(); }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();
        missing.forEach((id, set) -> {
            NbtList l = new NbtList();
            for (BodyPart p : set) l.add(NbtString.of(p.id));
            players.put(id.toString(), l);
        });
        nbt.put("players", players);
        NbtCompound pr = new NbtCompound();
        pros.forEach((id, set) -> {
            NbtList l = new NbtList();
            for (BodyPart p : set) l.add(NbtString.of(p.id));
            pr.put(id.toString(), l);
        });
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
        NbtCompound tk = new NbtCompound();
        tasks.forEach((id, t) -> { NbtCompound c = new NbtCompound(); c.putInt("i", t[0]); c.putInt("b", t[1]); tk.put(id.toString(), c); });
        nbt.put("tasks", tk);
        return nbt;
    }

    public static BodyState fromNbt(NbtCompound nbt) {
        BodyState s = new BodyState();
        NbtCompound players = nbt.getCompound("players");
        for (String key : players.getKeys()) {
            EnumSet<BodyPart> set = EnumSet.noneOf(BodyPart.class);
            NbtList l = players.getList(key, NbtElement.STRING_TYPE);
            for (int i = 0; i < l.size(); i++) { BodyPart p = BodyPart.byId(l.getString(i)); if (p != null) set.add(p); }
            s.missing.put(UUID.fromString(key), set);
        }
        NbtCompound pr = nbt.getCompound("pros");
        for (String key : pr.getKeys()) {
            EnumSet<BodyPart> set = EnumSet.noneOf(BodyPart.class);
            NbtList l = pr.getList(key, NbtElement.STRING_TYPE);
            for (int i = 0; i < l.size(); i++) { BodyPart p = BodyPart.byId(l.getString(i)); if (p != null) set.add(p); }
            s.pros.put(UUID.fromString(key), set);
        }
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
        NbtCompound tk = nbt.getCompound("tasks");
        for (String key : tk.getKeys()) { NbtCompound c = tk.getCompound(key); s.tasks.put(UUID.fromString(key), new int[]{c.getInt("i"), c.getInt("b")}); }
        return s;
    }
}
