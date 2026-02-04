package com.leclowndu93150.hungertweaks;

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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
        );
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
