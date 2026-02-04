package com.leclowndu93150.hungertweaks.voicechat;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StadiumMicPlugin implements VoicechatPlugin {

    public static final String CATEGORY_ID = "stadium_mic";

    private final Map<UUID, EntityAudioChannel> channels = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "hungertweaks";
    }

    @Override
    public void initialize(VoicechatApi api) {
        channels.clear();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onPlayerDisconnected);

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            StadiumMicClient.registerEvents(registration);
        }
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VolumeCategory category = event.getVoicechat().volumeCategoryBuilder()
                .setId(CATEGORY_ID)
                .setName("Stadium Mic")
                .setDescription("Volume of stadium mic announcements")
                .build();
        event.getVoicechat().registerVolumeCategory(category);
    }

    private void onMicPacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) {
            return;
        }

        UUID senderUuid = sender.getPlayer().getUuid();
        if (!StadiumMicManager.get().isActive(senderUuid)) {
            return;
        }

        event.cancel();

        EntityAudioChannel channel = channels.get(senderUuid);
        if (channel == null) {
            channel = event.getVoicechat().createEntityAudioChannel(UUID.randomUUID(), sender.getPlayer());
            if (channel == null) {
                return;
            }
            channel.setDistance(StadiumMicConfig.get().distance);
            channel.setCategory(CATEGORY_ID);
            channels.put(senderUuid, channel);
        }

        channel.send(event.getPacket());
    }

    private void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        UUID uuid = event.getPlayerUuid();
        StadiumMicManager.get().deactivate(uuid);
        EntityAudioChannel channel = channels.remove(uuid);
        if (channel != null) {
            channel.flush();
        }
    }

    public void removeChannel(UUID playerUuid) {
        EntityAudioChannel channel = channels.remove(playerUuid);
        if (channel != null) {
            channel.flush();
        }
    }
}
