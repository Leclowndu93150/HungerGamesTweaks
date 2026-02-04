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

    public float distance = 10000.0f;
    public boolean bypassOcclusion = true;

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
