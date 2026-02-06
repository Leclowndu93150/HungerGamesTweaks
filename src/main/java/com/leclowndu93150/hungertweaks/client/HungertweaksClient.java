package com.leclowndu93150.hungertweaks.client;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import com.leclowndu93150.hungertweaks.network.FreezePositionsSyncPayload;
import com.leclowndu93150.hungertweaks.network.HunterSpawnPositionsSyncPayload;
import com.leclowndu93150.hungertweaks.network.StadiumMicConfigSyncPayload;
import com.leclowndu93150.hungertweaks.network.StadiumMicSyncPayload;
import com.leclowndu93150.hungertweaks.voicechat.StadiumMicClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HungertweaksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FreezePositionsSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> FreezePositionRenderer.setPositions(payload.positions()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HunterSpawnPositionsSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> HunterSpawnPositionRenderer.setPositions(payload.positions()));
        });

        ClientPlayNetworking.registerGlobalReceiver(StadiumMicSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> StadiumMicClient.setActiveMics(payload.activePlayers()));
        });

        ClientPlayNetworking.registerGlobalReceiver(StadiumMicConfigSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                payload.applyToConfig(StadiumMicConfig.get());
                StadiumMicConfig.save();
                StadiumMicClient.resetEfx();
            });
        });

        FreezePositionRenderer.register();
        HunterSpawnPositionRenderer.register();
    }
}
