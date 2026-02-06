package com.leclowndu93150.hungertweaks;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import com.leclowndu93150.hungertweaks.hunter.HunterSpawnManager;
import com.leclowndu93150.hungertweaks.network.StadiumMicConfigSyncPayload;
import com.leclowndu93150.hungertweaks.network.StadiumMicSyncPayload;
import com.leclowndu93150.hungertweaks.voicechat.StadiumMicManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public class HungerGamesCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("hungergames")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("freeze").executes(ctx -> {
                    HungerGamesManager manager = HungerGamesManager.get(ctx.getSource().getServer());
                    manager.freezeAllNonOps(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("All non-op players have been frozen!").withStyle(ChatFormatting.AQUA), true);
                    return 1;
                }))
                .then(Commands.literal("start")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(ctx -> {
                                    int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                    HungerGamesManager manager = HungerGamesManager.get(ctx.getSource().getServer());
                                    manager.startCountdown(seconds);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Countdown started: " + seconds + " seconds!").withStyle(ChatFormatting.GOLD), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("unfreeze").executes(ctx -> {
                    HungerGamesManager manager = HungerGamesManager.get(ctx.getSource().getServer());
                    manager.unfreezeAll();
                    ctx.getSource().sendSuccess(() -> Component.literal("All players have been unfrozen!").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
                .then(FabricLoader.getInstance().isDevelopmentEnvironment()
                        ? Commands.literal("debug").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            HungerGamesManager manager = HungerGamesManager.get(ctx.getSource().getServer());
                            if (manager.isPlayerFrozen(player.getUUID())) {
                                manager.unfreezeAll();
                                ctx.getSource().sendSuccess(() -> Component.literal("Unfroze yourself").withStyle(ChatFormatting.GREEN), false);
                            } else {
                                manager.freezePlayer(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Froze yourself").withStyle(ChatFormatting.RED), false);
                            }
                            return 1;
                        })
                        : Commands.literal("debug").requires(source -> false).executes(ctx -> 0)
                )
                .then(Commands.literal("clear").executes(ctx -> {
                    HungerGamesManager manager = HungerGamesManager.get(ctx.getSource().getServer());
                    manager.clearPositions();
                    manager.syncPositionsToAll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("All freeze positions cleared!").withStyle(ChatFormatting.RED), true);
                    return 1;
                }))
                .then(Commands.literal("micsync").executes(ctx -> {
                    StadiumMicConfigSyncPayload payload = StadiumMicConfigSyncPayload.fromConfig(StadiumMicConfig.get());
                    for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        ServerPlayNetworking.send(p, payload);
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Stadium mic config synced to all players").withStyle(ChatFormatting.GREEN), true);
                    return 1;
                }))
                .then(Commands.literal("mic")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return toggleMic(ctx.getSource(), player);
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    return toggleMic(ctx.getSource(), target);
                                })
                        )
                )
                .then(Commands.literal("hunter")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("entity", ResourceArgument.resource(registryAccess, Registries.ENTITY_TYPE))
                                        .executes(ctx -> {
                                            EntityType<?> type = ResourceArgument.getEntityType(ctx, "entity").value();
                                            return spawnHunters(ctx.getSource(), type, 1, null);
                                        })
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                                .executes(ctx -> {
                                                    EntityType<?> type = ResourceArgument.getEntityType(ctx, "entity").value();
                                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                                    return spawnHunters(ctx.getSource(), type, count, null);
                                                })
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(ctx -> {
                                                            EntityType<?> type = ResourceArgument.getEntityType(ctx, "entity").value();
                                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                            return spawnHunters(ctx.getSource(), type, count, target);
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("kill")
                                .executes(ctx -> {
                                    HunterSpawnManager manager = HunterSpawnManager.get(ctx.getSource().getServer());
                                    int count = manager.getActiveHunterCount();
                                    manager.killAllHunters(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Killed " + count + " hunters").withStyle(ChatFormatting.RED), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("clear")
                                .executes(ctx -> {
                                    HunterSpawnManager manager = HunterSpawnManager.get(ctx.getSource().getServer());
                                    manager.clearPositions();
                                    manager.syncPositionsToAll(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared all hunter spawn positions").withStyle(ChatFormatting.RED), true);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static int spawnHunters(CommandSourceStack source, EntityType<?> type, int count, ServerPlayer target) {
        HunterSpawnManager manager = HunterSpawnManager.get(source.getServer());

        if (manager.getSpawnPositions().isEmpty()) {
            source.sendFailure(Component.literal("No hunter spawn positions set! Use the Hunter Spawn Wand first."));
            return 0;
        }

        java.util.List<Mob> spawned;
        if (target != null) {
            spawned = manager.spawnHunters(source.getServer(), type, target, count);
            source.sendSuccess(() -> Component.literal("Spawned " + spawned.size() + " hunters targeting " + target.getName().getString()).withStyle(ChatFormatting.GREEN), true);
        } else {
            spawned = manager.spawnHuntersToClosest(source.getServer(), type, count);
            source.sendSuccess(() -> Component.literal("Spawned " + spawned.size() + " hunters targeting closest player").withStyle(ChatFormatting.GREEN), true);
        }
        return spawned.size();
    }

    private static int toggleMic(CommandSourceStack source, ServerPlayer target) {
        boolean nowActive = StadiumMicManager.get().toggle(target.getUUID());
        StadiumMicSyncPayload payload = new StadiumMicSyncPayload(StadiumMicManager.get().getActiveMics());
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, payload);
        }
        String name = target.getName().getString();
        if (nowActive) {
            source.sendSuccess(() -> Component.literal("Stadium mic ON for " + name).withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendSuccess(() -> Component.literal("Stadium mic OFF for " + name).withStyle(ChatFormatting.RED), true);
        }
        return 1;
    }
}
