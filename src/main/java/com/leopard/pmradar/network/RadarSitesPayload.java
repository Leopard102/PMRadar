package com.leopard.pmradar.network;

import com.leopard.pmradar.PMRadar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RadarSitesPayload(ResourceLocation dimension, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<RadarSitesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PMRadar.MODID, "radar_sites")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarSitesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RadarSitesPayload decode(RegistryFriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = buffer.readVarInt();
            List<Entry> entries = new ArrayList<>(Math.min(count, 256));
            for (int i = 0; i < count; i++) {
                entries.add(new Entry(buffer.readBlockPos(), buffer.readByte()));
            }

            return new RadarSitesPayload(dimension, List.copyOf(entries));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RadarSitesPayload payload) {
            buffer.writeResourceLocation(payload.dimension());
            buffer.writeVarInt(payload.entries().size());
            for (Entry entry : payload.entries()) {
                buffer.writeBlockPos(entry.pos());
                buffer.writeByte(entry.state());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(BlockPos pos, byte state) {
        public static final byte REMOVED = 0;
        public static final byte WORKING = 1;
        public static final byte BROKEN = 2;

        public static Entry removed(BlockPos pos) {
            return new Entry(pos.immutable(), REMOVED);
        }

        public static Entry visible(BlockPos pos, boolean operational) {
            return new Entry(pos.immutable(), operational ? WORKING : BROKEN);
        }

        public boolean visible() {
            return state != REMOVED;
        }

        public boolean operational() {
            return state == WORKING;
        }
    }
}
