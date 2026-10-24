package com.example.bodytrade;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.*;

/** Сохраняемое состояние: отсутствующие части тела, активные болезни, кому выдан алтарь. */
public class BodyState extends PersistentState {
    private final Map<UUID, EnumSet<BodyPart>> missing = new HashMap<>();
    private final Map<UUID, EnumMap<SideEffect, Long>> effects = new HashMap<>();
    private final Set<UUID> gaveBlock = new HashSet<>();

    public static BodyState of(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(BodyState::fromNbt, BodyState::new, "bodytrade");
    }

    public Set<BodyPart> get(UUID id) { return missing.computeIfAbsent(id, k -> EnumSet.noneOf(BodyPart.class)); }
    public void add(UUID id, BodyPart p) { get(id).add(p); markDirty(); }
    public void remove(UUID id, BodyPart p) { get(id).remove(p); markDirty(); }
    public void clear(UUID id) { get(id).clear(); effects(id).clear(); markDirty(); }
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
        NbtCompound fx = nbt.getCompound("effects");
        for (String key : fx.getKeys()) {
            NbtCompound c = fx.getCompound(key);
            EnumMap<SideEffect, Long> map = new EnumMap<>(SideEffect.class);
            for (SideEffect e : SideEffect.values()) if (c.contains(e.name())) map.put(e, c.getLong(e.name()));
            s.effects.put(UUID.fromString(key), map);
        }
        NbtList g = nbt.getList("gave", NbtElement.STRING_TYPE);
        for (int i = 0; i < g.size(); i++) s.gaveBlock.add(UUID.fromString(g.getString(i)));
        return s;
    }
}
