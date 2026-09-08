package com.leopard.pmradar.network;

import com.leopard.pmradar.PMRadar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RadarDebrisPayload(ResourceLocation dimension, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<RadarDebrisPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PMRadar.MODID, "radar_debris")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RadarDebrisPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RadarDebrisPayload decode(RegistryFriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = buffer.readVarInt();
            List<Entry> entries = new ArrayList<>(Math.min(count, 256));
            for (int i = 0; i < count; i++) {
                entries.add(new Entry(
                        buffer.readLong(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readFloat(),
                        buffer.readVarInt()
                ));
            }

            return new RadarDebrisPayload(dimension, List.copyOf(entries));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RadarDebrisPayload payload) {
            buffer.writeResourceLocation(payload.dimension());
            buffer.writeVarInt(payload.entries().size());
            for (Entry entry : payload.entries()) {
                buffer.writeLong(entry.stormId());
                buffer.writeDouble(entry.x());
                buffer.writeDouble(entry.z());
                buffer.writeDouble(entry.radius());
                buffer.writeFloat(entry.strength());
                buffer.writeVarInt(entry.count());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(long stormId, double x, double z, double radius, float strength, int count) {
    }
}
