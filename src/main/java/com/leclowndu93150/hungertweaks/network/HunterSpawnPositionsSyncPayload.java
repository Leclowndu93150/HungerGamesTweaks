package com.leclowndu93150.hungertweaks.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record HunterSpawnPositionsSyncPayload(List<BlockPos> positions) implements CustomPacketPayload {

    public static final Type<HunterSpawnPositionsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("hungertweaks", "hunter_spawn_positions_sync"));

    public static final StreamCodec<ByteBuf, HunterSpawnPositionsSyncPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(HunterSpawnPositionsSyncPayload::new, HunterSpawnPositionsSyncPayload::positions);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
