package com.leclowndu93150.hungertweaks.hunter;

import com.leclowndu93150.hungertweaks.mixin.MobAccessor;
import com.leclowndu93150.hungertweaks.network.HunterSpawnPositionsSyncPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class HunterSpawnManager extends SavedData {

    public static final Codec<HunterSpawnManager> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.listOf().fieldOf("hunter_spawn_positions").forGetter(m -> m.spawnPositions)
            ).apply(instance, HunterSpawnManager::new)
    );

    public static final SavedDataType<HunterSpawnManager> TYPE = new SavedDataType<>(
            "hungertweaks_hunter_spawns",
            HunterSpawnManager::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final List<BlockPos> spawnPositions;
    private final Set<UUID> activeHunters = new HashSet<>();

    public HunterSpawnManager() {
        this(new ArrayList<>());
    }

    private HunterSpawnManager(List<BlockPos> positions) {
        this.spawnPositions = new ArrayList<>(positions);
    }

    public static HunterSpawnManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void addSpawnPosition(BlockPos pos) {
        spawnPositions.add(pos);
        setDirty();
    }

    public boolean removeNearestSpawnPosition(BlockPos pos) {
        if (spawnPositions.isEmpty()) return false;
        BlockPos nearest = spawnPositions.stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(pos)))
                .orElse(null);
        if (nearest != null && nearest.distSqr(pos) < 100) {
            spawnPositions.remove(nearest);
            setDirty();
            return true;
        }
        return false;
    }

    public void clearPositions() {
        spawnPositions.clear();
        setDirty();
    }

    public List<BlockPos> getSpawnPositions() {
        return Collections.unmodifiableList(spawnPositions);
    }

    public List<Mob> spawnHunters(MinecraftServer server, EntityType<?> entityType, ServerPlayer target, int count) {
        if (spawnPositions.isEmpty()) return Collections.emptyList();

        ServerLevel level = server.overworld();
        List<Mob> spawned = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            BlockPos pos = spawnPositions.get(i % spawnPositions.size());
            Vec3 spawnPos = Vec3.atBottomCenterOf(pos.above());

            Entity entity = entityType.spawn(level, pos.above(), EntitySpawnReason.COMMAND);
            if (entity instanceof Mob mob) {
                mob.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                ((MobAccessor) mob).getGoalSelector().addGoal(0, new HuntPlayerGoal(mob, target));
                mob.setPersistenceRequired();
                activeHunters.add(mob.getUUID());
                spawned.add(mob);
            }
        }
        return spawned;
    }

    public List<Mob> spawnHuntersToClosest(MinecraftServer server, EntityType<?> entityType, int count) {
        if (spawnPositions.isEmpty()) return Collections.emptyList();

        ServerLevel level = server.overworld();
        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.isAlive() && !p.isSpectator() && !p.isCreative())
                .toList();
        List<Mob> spawned = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            BlockPos pos = spawnPositions.get(i % spawnPositions.size());
            Vec3 spawnPos = Vec3.atBottomCenterOf(pos.above());

            ServerPlayer closest = players.stream()
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(spawnPos)))
                    .orElse(null);

            Entity entity = entityType.spawn(level, pos.above(), EntitySpawnReason.COMMAND);
            if (entity instanceof Mob mob) {
                mob.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                ((MobAccessor) mob).getGoalSelector().addGoal(0, new HuntPlayerGoal(mob, closest));
                mob.setPersistenceRequired();
                activeHunters.add(mob.getUUID());
                spawned.add(mob);
            }
        }
        return spawned;
    }

    public void killAllHunters(MinecraftServer server) {
        ServerLevel level = server.overworld();
        for (UUID uuid : activeHunters) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                entity.discard();
            }
        }
        activeHunters.clear();
    }

    public int getActiveHunterCount() {
        return activeHunters.size();
    }

    public void syncPositionsToAll(MinecraftServer server) {
        HunterSpawnPositionsSyncPayload payload = new HunterSpawnPositionsSyncPayload(new ArrayList<>(spawnPositions));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public void syncPositionsToPlayer(ServerPlayer player) {
        ServerPlayNetworking.send(player, new HunterSpawnPositionsSyncPayload(new ArrayList<>(spawnPositions)));
    }
}
