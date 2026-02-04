package com.leclowndu93150.hungertweaks.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record StadiumMicSyncPayload(Set<UUID> activePlayers) implements CustomPacketPayload {

    public static final Type<StadiumMicSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("hungertweaks", "stadium_mic_sync"));

    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, uuid) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    public static final StreamCodec<ByteBuf, StadiumMicSyncPayload> STREAM_CODEC =
            UUID_CODEC.apply(ByteBufCodecs.list())
                    .map(list -> new StadiumMicSyncPayload(new HashSet<>(list)), payload -> List.copyOf(payload.activePlayers()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
