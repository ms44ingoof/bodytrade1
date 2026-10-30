package com.example.bodytrade;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Потокобезопасный снимок игроков для Simple Voice Chat: голосовой сервер работает в своём потоке и не должен
 * читать данные мира напрямую. Обновляется главным потоком раз в секунду.
 */
public final class VoiceState {
    private VoiceState() {}

    public record Snap(UUID uuid, String dim, double x, double y, double z, boolean mouthMissing, int earLevel) {}

    private static final Map<UUID, Snap> MAP = new ConcurrentHashMap<>();

    public static void update(ServerPlayerEntity p, boolean mouthMissing, int earLevel) {
        MAP.put(p.getUuid(), new Snap(p.getUuid(), p.getWorld().getRegistryKey().getValue().toString(), p.getX(), p.getY(), p.getZ(), mouthMissing, earLevel));
    }

    public static Snap get(UUID id) { return MAP.get(id); }
    public static Collection<Snap> all() { return MAP.values(); }
    public static void remove(UUID id) { MAP.remove(id); }
    public static void clear() { MAP.clear(); }
}
