package com.leclowndu93150.hungertweaks.voicechat;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.OpenALSoundEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class StadiumMicClient {

    private static final Set<UUID> activeMics = ConcurrentHashMap.newKeySet();

    public static void registerEvents(EventRegistration registration) {
        registration.registerEvent(OpenALSoundEvent.Post.class, StadiumMicClient::onOpenALSoundPost);
    }

    private static void onOpenALSoundPost(OpenALSoundEvent.Post event) {
        if (!StadiumMicConfig.get().bypassOcclusion) {
            return;
        }
        if (!StadiumMicPlugin.CATEGORY_ID.equals(event.getCategory())) {
            return;
        }
        AL10.alSourcei(event.getSource(), EXTEfx.AL_DIRECT_FILTER, EXTEfx.AL_FILTER_NULL);
    }

    public static void setActiveMics(Set<UUID> mics) {
        activeMics.clear();
        activeMics.addAll(mics);
    }

    public static Set<UUID> getActiveMics() {
        return Collections.unmodifiableSet(activeMics);
    }
}
