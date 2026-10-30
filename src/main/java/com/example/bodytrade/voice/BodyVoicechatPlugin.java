package com.example.bodytrade.voice;

import com.example.bodytrade.VoiceState;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

import java.util.UUID;

/**
 * Связь с Simple Voice Chat (подключается только если этот мод установлен):
 * - без рта игрок не может говорить;
 * - механическое ухо позволяет слышать других игроков с ухом на большом расстоянии (по уровням).
 * Плагин работает в потоке голосового сервера, поэтому читает только потокобезопасный снимок VoiceState.
 */
public class BodyVoicechatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() { return "bodytrade"; }

    @Override
    public void initialize(VoicechatApi api) {}

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        VoicechatServerApi api = event.getVoicechat();
        if (sender == null || api == null) return;
        UUID id = sender.getPlayer().getUuid();
        VoiceState.Snap me = VoiceState.get(id);
        if (me == null) return;

        // Нет рта - нет голоса
        if (me.mouthMissing()) { event.cancel(); return; }

        double normal = api.getVoiceChatDistance();
        for (VoiceState.Snap listener : VoiceState.all()) {
            if (listener.uuid().equals(id) || listener.earLevel() <= 0) continue;
            if (!reaches(me, listener, normal)) continue;
            VoicechatConnection receiver = api.getConnectionOf(listener.uuid());
            if (receiver == null) continue;
            api.sendStaticSoundPacketTo(receiver, event.getPacket().staticSoundPacketBuilder().build());
        }
    }

    /**
     * Слышит ли слушатель говорящего за пределами обычной дальности голосового чата.
     * Уровни уха: 1 - 200 блоков, 2 - 400 блоков (оба только в Верхнем мире), 3 - весь Верхний мир, 4 - любые измерения.
     */
    private static boolean reaches(VoiceState.Snap me, VoiceState.Snap l, double normal) {
        boolean sameDim = me.dim().equals(l.dim());
        double d = Double.MAX_VALUE;
        if (sameDim) {
            double dx = me.x() - l.x(), dy = me.y() - l.y(), dz = me.z() - l.z();
            d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d <= normal) return false; // в обычной дальности Simple Voice Chat всё доставит сам
        }
        int lv = l.earLevel();
        if (lv >= 4) return true;
        if (!sameDim || !"minecraft:overworld".equals(me.dim())) return false;
        if (lv == 3) return true;
        return d <= (lv == 1 ? 200 : 400);
    }
}
