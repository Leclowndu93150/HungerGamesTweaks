package com.leclowndu93150.hungertweaks.client;

import com.leclowndu93150.hungertweaks.network.FreezePositionsSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class HungertweaksClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FreezePositionsSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> FreezePositionRenderer.setPositions(payload.positions()));
        });

        FreezePositionRenderer.register();
    }
}
