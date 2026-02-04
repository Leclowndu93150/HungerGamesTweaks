package com.leclowndu93150.hungertweaks;

import com.leclowndu93150.hungertweaks.network.FreezePositionsSyncPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class HungerGamesManager extends SavedData {

    public static final Codec<HungerGamesManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.listOf().fieldOf("freeze_positions").forGetter(m -> m.freezePositions)
            ).apply(instance, HungerGamesManager::new)
    );

    public static final SavedDataType<HungerGamesManager> TYPE = new SavedDataType<>(
            "hungertweaks_hunger_games",
            HungerGamesManager::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final List<BlockPos> freezePositions;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, Vec3> frozenPositionMap = new HashMap<>();
    private int countdownTicks = -1;

    public HungerGamesManager() {
        this(new ArrayList<>());
    }

    private HungerGamesManager(List<BlockPos> positions) {
        this.freezePositions = new ArrayList<>(positions);
    }

    public static HungerGamesManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void addFreezePosition(BlockPos pos) {
        freezePositions.add(pos);
        setDirty();
    }

    public boolean removeNearestFreezePosition(BlockPos pos) {
        if (freezePositions.isEmpty()) return false;
        BlockPos nearest = freezePositions.stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(pos)))
                .orElse(null);
        if (nearest != null && nearest.distSqr(pos) < 100) {
            freezePositions.remove(nearest);
            setDirty();
            return true;
        }
        return false;
    }

    public void clearPositions() {
        freezePositions.clear();
        setDirty();
    }

    public List<BlockPos> getFreezePositions() {
        return Collections.unmodifiableList(freezePositions);
    }

    public void freezeAllNonOps(MinecraftServer server) {
        frozenPlayers.clear();
        frozenPositionMap.clear();

        List<ServerPlayer> targets = server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.hasPermissions(2))
                .toList();

        for (int i = 0; i < targets.size(); i++) {
            ServerPlayer player = targets.get(i);
            frozenPlayers.add(player.getUUID());

            if (!freezePositions.isEmpty()) {
                BlockPos pos = freezePositions.get(i % freezePositions.size());
                Vec3 exactPos = Vec3.atBottomCenterOf(pos.above());
                frozenPositionMap.put(player.getUUID(), exactPos);
                player.teleportTo((ServerLevel) player.level(), exactPos.x, exactPos.y, exactPos.z, Set.of(), player.getYRot(), player.getXRot(), false);
            } else {
                frozenPositionMap.put(player.getUUID(), player.position());
            }

            player.sendSystemMessage(Component.literal("You have been frozen!").withStyle(ChatFormatting.RED));
        }
    }

    public void freezePlayer(ServerPlayer player) {
        frozenPlayers.add(player.getUUID());
        frozenPositionMap.put(player.getUUID(), player.position());
        player.sendSystemMessage(Component.literal("You have been frozen!").withStyle(ChatFormatting.RED));
    }

    public void unfreezeAll() {
        frozenPlayers.clear();
        frozenPositionMap.clear();
        countdownTicks = -1;
    }

    public boolean isPlayerFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public Vec3 getFrozenPosition(UUID uuid) {
        return frozenPositionMap.get(uuid);
    }

    public void startCountdown(int seconds) {
        countdownTicks = seconds * 20;
    }

    public void tick(MinecraftServer server) {
        for (UUID uuid : frozenPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                Vec3 pos = frozenPositionMap.get(uuid);
                if (pos != null) {
                    double dx = player.getX() - pos.x;
                    double dy = player.getY() - pos.y;
                    double dz = player.getZ() - pos.z;
                    if (dx * dx + dy * dy + dz * dz > 0.01) {
                        player.teleportTo((ServerLevel) player.level(), pos.x, pos.y, pos.z, Set.of(), player.getYRot(), player.getXRot(), false);
                    }
                }
            }
        }

        if (countdownTicks < 0) return;

        if (countdownTicks > 0 && countdownTicks % 20 == 0) {
            int secondsLeft = countdownTicks / 20;
            ChatFormatting color;
            if (secondsLeft >= 4) {
                color = ChatFormatting.GREEN;
            } else if (secondsLeft >= 2) {
                color = ChatFormatting.YELLOW;
            } else {
                color = ChatFormatting.RED;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal(String.valueOf(secondsLeft)).withStyle(color, ChatFormatting.BOLD)
                ));
                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1.0F, 1.0F);
            }
        }

        if (countdownTicks == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("GO!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                ));
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0F, 1.0F);
            }
            unfreezeAll();
            return;
        }

        countdownTicks--;
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (frozenPlayers.contains(player.getUUID())) {
            Vec3 pos = frozenPositionMap.get(player.getUUID());
            if (pos != null) {
                player.teleportTo((ServerLevel) player.level(), pos.x, pos.y, pos.z, Set.of(), player.getYRot(), player.getXRot(), false);
            }
        }
    }

    public void syncPositionsToAll(MinecraftServer server) {
        FreezePositionsSyncPayload payload = new FreezePositionsSyncPayload(new ArrayList<>(freezePositions));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public void syncPositionsToPlayer(ServerPlayer player) {
        ServerPlayNetworking.send(player, new FreezePositionsSyncPayload(new ArrayList<>(freezePositions)));
    }
}
