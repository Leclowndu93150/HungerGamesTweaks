package com.leclowndu93150.hungertweaks;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import com.leclowndu93150.hungertweaks.hunter.HunterSpawnManager;
import com.leclowndu93150.hungertweaks.hunter.HunterSpawnWandItem;
import com.leclowndu93150.hungertweaks.network.FreezePositionsSyncPayload;
import com.leclowndu93150.hungertweaks.network.HunterSpawnPositionsSyncPayload;
import com.leclowndu93150.hungertweaks.network.StadiumMicConfigSyncPayload;
import com.leclowndu93150.hungertweaks.network.StadiumMicSyncPayload;
import com.leclowndu93150.hungertweaks.voicechat.StadiumMicManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class Hungertweaks implements ModInitializer {

    public static final ResourceKey<Item> FREEZE_WAND_KEY =
            ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("hungertweaks", "freeze_wand"));

    public static final Item FREEZE_WAND = new FreezeWandItem(
            new Item.Properties().setId(FREEZE_WAND_KEY).stacksTo(1).rarity(Rarity.EPIC)
    );

    public static final ResourceKey<Item> HUNTER_SPAWN_WAND_KEY =
            ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("hungertweaks", "hunter_spawn_wand"));

    public static final Item HUNTER_SPAWN_WAND = new HunterSpawnWandItem(
            new Item.Properties().setId(HUNTER_SPAWN_WAND_KEY).stacksTo(1).rarity(Rarity.EPIC)
    );

    @Override
    public void onInitialize() {
        StadiumMicConfig.load();

        Registry.register(BuiltInRegistries.ITEM, FREEZE_WAND_KEY, FREEZE_WAND);
        Registry.register(BuiltInRegistries.ITEM, HUNTER_SPAWN_WAND_KEY, HUNTER_SPAWN_WAND);

        PayloadTypeRegistry.playS2C().register(FreezePositionsSyncPayload.TYPE, FreezePositionsSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HunterSpawnPositionsSyncPayload.TYPE, HunterSpawnPositionsSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(StadiumMicSyncPayload.TYPE, StadiumMicSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(StadiumMicConfigSyncPayload.TYPE, StadiumMicConfigSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(StadiumMicConfigSyncPayload.TYPE, StadiumMicConfigSyncPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(StadiumMicConfigSyncPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            if (!sender.hasPermissions(2)) {
                return;
            }
            context.server().execute(() -> {
                payload.applyToConfig(StadiumMicConfig.get());
                StadiumMicConfig.save();
                for (ServerPlayer p : context.server().getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(p, payload);
                }
            });
        });

        CommandRegistrationCallback.EVENT.register(HungerGamesCommand::register);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HungerGamesManager.get(server).tick(server);
            HunterSpawnManager.get(server).tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HungerGamesManager manager = HungerGamesManager.get(server);
            manager.onPlayerJoin(handler.player);
            manager.syncPositionsToPlayer(handler.player);
            HunterSpawnManager.get(server).syncPositionsToPlayer(handler.player);
            ServerPlayNetworking.send(handler.player, new StadiumMicSyncPayload(StadiumMicManager.get().getActiveMics()));
            ServerPlayNetworking.send(handler.player, StadiumMicConfigSyncPayload.fromConfig(StadiumMicConfig.get()));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (StadiumMicManager.get().isActive(handler.player.getUUID())) {
                StadiumMicManager.get().deactivate(handler.player.getUUID());
                StadiumMicSyncPayload payload = new StadiumMicSyncPayload(StadiumMicManager.get().getActiveMics());
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    ServerPlayNetworking.send(p, payload);
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide && player.getServer() != null) {
                HungerGamesManager manager = HungerGamesManager.get(player.getServer());
                if (manager.isPlayerFrozen(player.getUUID())) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide && player.getServer() != null) {
                HungerGamesManager manager = HungerGamesManager.get(player.getServer());
                if (manager.isPlayerFrozen(player.getUUID())) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide && player.getServer() != null) {
                HungerGamesManager manager = HungerGamesManager.get(player.getServer());
                if (manager.isPlayerFrozen(player.getUUID())) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(FREEZE_WAND);
            entries.accept(HUNTER_SPAWN_WAND);
        });
    }
}
