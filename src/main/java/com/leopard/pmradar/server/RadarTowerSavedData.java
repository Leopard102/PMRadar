package com.leopard.pmradar.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class RadarTowerSavedData extends SavedData {
    private static final String DATA_NAME = "pmradar_radar_towers";
    private static final String TOWERS_TAG = "towers";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";
    private static final String CODE_TAG = "code";
    private static final String OPERATIONAL_TAG = "operational";
    private static final String RANGE_UPGRADED_TAG = "range_upgraded";

    private static final Factory<RadarTowerSavedData> FACTORY = new Factory<>(
            RadarTowerSavedData::new,
            RadarTowerSavedData::load
    );

    private final Map<BlockPos, TowerRecord> towers = new HashMap<>();

    private RadarTowerSavedData() {
    }

    public static RadarTowerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static RadarTowerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RadarTowerSavedData data = new RadarTowerSavedData();
        ListTag towers = tag.getList(TOWERS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < towers.size(); index++) {
            CompoundTag tower = towers.getCompound(index);
            String stationCode = tower.getString(CODE_TAG).trim();
            if (stationCode.isEmpty()) {
                continue;
            }

            BlockPos pos = new BlockPos(
                    tower.getInt(X_TAG),
                    tower.getInt(Y_TAG),
                    tower.getInt(Z_TAG)
            );
            data.towers.put(pos, new TowerRecord(
                    stationCode,
                    tower.getBoolean(OPERATIONAL_TAG),
                    tower.getBoolean(RANGE_UPGRADED_TAG)
            ));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag towersTag = new ListTag();
        for (Map.Entry<BlockPos, TowerRecord> entry : towers.entrySet()) {
            BlockPos pos = entry.getKey();
            TowerRecord record = entry.getValue();
            CompoundTag tower = new CompoundTag();
            tower.putInt(X_TAG, pos.getX());
            tower.putInt(Y_TAG, pos.getY());
            tower.putInt(Z_TAG, pos.getZ());
            tower.putString(CODE_TAG, record.stationCode());
            tower.putBoolean(OPERATIONAL_TAG, record.operational());
            tower.putBoolean(RANGE_UPGRADED_TAG, record.rangeUpgraded());
            towersTag.add(tower);
        }
        tag.put(TOWERS_TAG, towersTag);
        return tag;
    }

    public Map<BlockPos, TowerRecord> towers() {
        return Map.copyOf(towers);
    }

    public TowerRecord get(BlockPos pos) {
        return towers.get(pos);
    }

    public void put(BlockPos pos, TowerRecord record) {
        BlockPos immutable = pos.immutable();
        if (!record.equals(towers.put(immutable, record))) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (towers.remove(pos) != null) {
            setDirty();
        }
    }

    public record TowerRecord(String stationCode, boolean operational, boolean rangeUpgraded) {
    }
}
