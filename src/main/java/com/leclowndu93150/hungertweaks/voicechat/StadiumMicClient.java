package com.leclowndu93150.hungertweaks.voicechat;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import de.maxhenkel.voicechat.api.events.CreateOpenALContextEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.OpenALSoundEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.EXTEfx.*;

@Environment(EnvType.CLIENT)
public class StadiumMicClient {

    private static final Set<UUID> activeMics = ConcurrentHashMap.newKeySet();

    private static boolean initialized = false;
    private static int effectSlot = 0;
    private static int effect = 0;
    private static int directFilter = 0;

    public static void registerEvents(EventRegistration registration) {
        registration.registerEvent(OpenALSoundEvent.Post.class, StadiumMicClient::onOpenALSoundPost);
        registration.registerEvent(CreateOpenALContextEvent.class, StadiumMicClient::onContextCreated);
    }

    private static void onContextCreated(CreateOpenALContextEvent event) {
        initialized = false;
        effectSlot = 0;
        effect = 0;
        directFilter = 0;
    }

    private static void initEfx() {
        if (initialized) return;
        initialized = true;

        StadiumMicConfig config = StadiumMicConfig.get();

        effectSlot = alGenAuxiliaryEffectSlots();
        effect = alGenEffects();

        alEffecti(effect, AL_EFFECT_TYPE, AL_EFFECT_EAXREVERB);
        alEffectf(effect, AL_EAXREVERB_DENSITY, config.reverbDensity);
        alEffectf(effect, AL_EAXREVERB_DIFFUSION, config.reverbDiffusion);
        alEffectf(effect, AL_EAXREVERB_GAIN, config.reverbGain);
        alEffectf(effect, AL_EAXREVERB_GAINHF, 0.5623f);
        alEffectf(effect, AL_EAXREVERB_GAINLF, 0.5012f);
        alEffectf(effect, AL_EAXREVERB_DECAY_TIME, config.reverbDecay);
        alEffectf(effect, AL_EAXREVERB_DECAY_HFRATIO, 0.88f);
        alEffectf(effect, AL_EAXREVERB_DECAY_LFRATIO, 0.68f);
        alEffectf(effect, AL_EAXREVERB_REFLECTIONS_GAIN, config.reverbReflectionsGain);
        alEffectf(effect, AL_EAXREVERB_REFLECTIONS_DELAY, 0.23f);
        alEffectf(effect, AL_EAXREVERB_LATE_REVERB_GAIN, config.reverbLateGain);
        alEffectf(effect, AL_EAXREVERB_LATE_REVERB_DELAY, 0.063f);
        alEffectf(effect, AL_EAXREVERB_ECHO_TIME, 0.25f);
        alEffectf(effect, AL_EAXREVERB_ECHO_DEPTH, 0.2f);
        alEffectf(effect, AL_EAXREVERB_AIR_ABSORPTION_GAINHF, 0.9943f);
        alEffectf(effect, AL_EAXREVERB_HFREFERENCE, 5000.0f);
        alEffectf(effect, AL_EAXREVERB_LFREFERENCE, 250.0f);
        alAuxiliaryEffectSloti(effectSlot, AL_EFFECTSLOT_EFFECT, effect);

        directFilter = alGenFilters();
        alFilteri(directFilter, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
        alFilterf(directFilter, AL_LOWPASS_GAIN, config.directGain);
        alFilterf(directFilter, AL_LOWPASS_GAINHF, config.directHfGain);
    }

    private static void onOpenALSoundPost(OpenALSoundEvent.Post event) {
        if (!StadiumMicPlugin.CATEGORY_ID.equals(event.getCategory())) {
            return;
        }

        initEfx();

        StadiumMicConfig config = StadiumMicConfig.get();
        int source = event.getSource();

        alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);
        alSource3f(source, AL_POSITION, config.sourceX, config.sourceY, config.sourceZ);
        alSourcef(source, AL_ROLLOFF_FACTOR, 0.0f);

        alSourcei(source, AL_DIRECT_FILTER, directFilter);
        alSource3i(source, AL_AUXILIARY_SEND_FILTER, effectSlot, 0, AL_FILTER_NULL);
    }

    public static void setActiveMics(Set<UUID> mics) {
        activeMics.clear();
        activeMics.addAll(mics);
    }

    public static Set<UUID> getActiveMics() {
        return Collections.unmodifiableSet(activeMics);
    }
}
