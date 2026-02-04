package com.leclowndu93150.hungertweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class StadiumMicConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("hungertweaks-stadium-mic.json");

    private static StadiumMicConfig INSTANCE;

    public String _comment_distance = "Max distance (in blocks) at which players receive the audio from the server";
    public float distance = 10000.0f;

    public String _comment_volume = "Master volume of the stadium mic output (0.0 = silent, 1.0 = normal, 2.0+ = amplified)";
    public float volume = 1.0f;

    public String _comment_source = "World position the sound appears to come from (like a PA speaker in the sky)";
    public float sourceX = 0.0f;
    public float sourceY = 150.0f;
    public float sourceZ = 0.0f;

    public String _comment_directGain = "Volume of the dry/direct signal (0.0 = muted, 1.0 = full). Lower values make reverb more prominent";
    public float directGain = 0.5f;

    public String _comment_directHfGain = "High-frequency rolloff on the direct signal (0.0 = very muffled, 1.0 = crisp). Lower values sound more distant";
    public float directHfGain = 0.7f;

    public String _comment_reverb = "EAX Reverb settings based on the SPORT_STADIUMTANNOY preset. Tweak these to change the echo feel";
    public float reverbDecay = 1.8f;
    public float reverbGain = 0.25f;
    public float reverbDensity = 0.4f;
    public float reverbDiffusion = 0.8f;

    public String _comment_reverbLateGain = "Volume of the late reverb tail (the lingering echo)";
    public float reverbLateGain = 0.35f;

    public String _comment_reverbReflectionsGain = "Volume of early reflections (the initial bounce)";
    public float reverbReflectionsGain = 0.15f;

    public static StadiumMicConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, StadiumMicConfig.class);
                return;
            } catch (IOException ignored) {
            }
        }
        INSTANCE = new StadiumMicConfig();
        save();
    }

    public static void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException ignored) {
        }
    }
}
