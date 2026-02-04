package com.leclowndu93150.hungertweaks.network;

import net.minecraft.core.BlockPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record FreezePositionsSyncPayload(List<BlockPos> positions) implements CustomPacketPayload {

    public static final Type<FreezePositionsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("hungertweaks", "freeze_positions_sync"));

    public static final StreamCodec<ByteBuf, FreezePositionsSyncPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(FreezePositionsSyncPayload::new, FreezePositionsSyncPayload::positions);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
