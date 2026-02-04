package com.leclowndu93150.hungertweaks.voicechat;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StadiumMicManager {

    private static final StadiumMicManager INSTANCE = new StadiumMicManager();

    private final Set<UUID> activeMics = ConcurrentHashMap.newKeySet();

    public static StadiumMicManager get() {
        return INSTANCE;
    }

    public boolean toggle(UUID uuid) {
        if (activeMics.contains(uuid)) {
            activeMics.remove(uuid);
            return false;
        } else {
            activeMics.add(uuid);
            return true;
        }
    }

    public void activate(UUID uuid) {
        activeMics.add(uuid);
    }

    public void deactivate(UUID uuid) {
        activeMics.remove(uuid);
    }

    public boolean isActive(UUID uuid) {
        return activeMics.contains(uuid);
    }

    public Set<UUID> getActiveMics() {
        return Collections.unmodifiableSet(activeMics);
    }
}
