package com.leclowndu93150.hungertweaks;

import com.leclowndu93150.hungertweaks.network.FreezePositionsSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM, FREEZE_WAND_KEY, FREEZE_WAND);

        PayloadTypeRegistry.playS2C().register(FreezePositionsSyncPayload.TYPE, FreezePositionsSyncPayload.STREAM_CODEC);

        CommandRegistrationCallback.EVENT.register(HungerGamesCommand::register);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            HungerGamesManager manager = HungerGamesManager.get(server);
            manager.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            HungerGamesManager manager = HungerGamesManager.get(server);
            manager.onPlayerJoin(handler.player);
            manager.syncPositionsToPlayer(handler.player);
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
        });
    }
}
