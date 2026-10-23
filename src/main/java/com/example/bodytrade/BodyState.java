package com.example.bodytrade;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.*;

/** Сохраняемое состояние: какие части тела отсутствуют у каждого игрока + кому уже выдан алтарь. */
public class BodyState extends PersistentState {
    private final Map<UUID, EnumSet<BodyPart>> missing = new HashMap<>();
    private final Set<UUID> gaveBlock = new HashSet<>();

    public static BodyState of(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(BodyState::fromNbt, BodyState::new, "bodytrade");
    }

    public Set<BodyPart> get(UUID id) { return missing.computeIfAbsent(id, k -> EnumSet.noneOf(BodyPart.class)); }
    public void add(UUID id, BodyPart p) { get(id).add(p); markDirty(); }
    public void remove(UUID id, BodyPart p) { get(id).remove(p); markDirty(); }
    public void clear(UUID id) { get(id).clear(); markDirty(); }
    public boolean markGaveBlock(UUID id) { if (gaveBlock.add(id)) { markDirty(); return true; } return false; }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();
        missing.forEach((id, set) -> {
            NbtList l = new NbtList();
            for (BodyPart p : set) l.add(NbtString.of(p.id));
            players.put(id.toString(), l);
        });
        nbt.put("players", players);
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
        NbtList g = nbt.getList("gave", NbtElement.STRING_TYPE);
        for (int i = 0; i < g.size(); i++) s.gaveBlock.add(UUID.fromString(g.getString(i)));
        return s;
    }
}
