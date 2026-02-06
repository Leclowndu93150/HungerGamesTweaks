package com.leclowndu93150.hungertweaks.network;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StadiumMicConfigSyncPayload(
        float distance,
        float volume,
        float sourceX,
        float sourceY,
        float sourceZ,
        float directGain,
        float directHfGain,
        float reverbDecay,
        float reverbGain,
        float reverbDensity,
        float reverbDiffusion,
        float reverbLateGain,
        float reverbReflectionsGain
) implements CustomPacketPayload {

    public static final Type<StadiumMicConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("hungertweaks", "stadium_mic_config_sync"));

    public static final StreamCodec<ByteBuf, StadiumMicConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeFloat(payload.distance);
                buf.writeFloat(payload.volume);
                buf.writeFloat(payload.sourceX);
                buf.writeFloat(payload.sourceY);
                buf.writeFloat(payload.sourceZ);
                buf.writeFloat(payload.directGain);
                buf.writeFloat(payload.directHfGain);
                buf.writeFloat(payload.reverbDecay);
                buf.writeFloat(payload.reverbGain);
                buf.writeFloat(payload.reverbDensity);
                buf.writeFloat(payload.reverbDiffusion);
                buf.writeFloat(payload.reverbLateGain);
                buf.writeFloat(payload.reverbReflectionsGain);
            },
            buf -> new StadiumMicConfigSyncPayload(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    public static StadiumMicConfigSyncPayload fromConfig(StadiumMicConfig config) {
        return new StadiumMicConfigSyncPayload(
                config.distance, config.volume,
                config.sourceX, config.sourceY, config.sourceZ,
                config.directGain, config.directHfGain,
                config.reverbDecay, config.reverbGain, config.reverbDensity, config.reverbDiffusion,
                config.reverbLateGain, config.reverbReflectionsGain
        );
    }

    public void applyToConfig(StadiumMicConfig config) {
        config.distance = this.distance;
        config.volume = this.volume;
        config.sourceX = this.sourceX;
        config.sourceY = this.sourceY;
        config.sourceZ = this.sourceZ;
        config.directGain = this.directGain;
        config.directHfGain = this.directHfGain;
        config.reverbDecay = this.reverbDecay;
        config.reverbGain = this.reverbGain;
        config.reverbDensity = this.reverbDensity;
        config.reverbDiffusion = this.reverbDiffusion;
        config.reverbLateGain = this.reverbLateGain;
        config.reverbReflectionsGain = this.reverbReflectionsGain;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
