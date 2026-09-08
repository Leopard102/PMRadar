package com.leopard.pmradar;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.multiblock.wsr88d.WSR88DCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class RadarTowerScanner {
    private RadarTowerScanner() {
    }

    public static boolean isTowerCore(BlockState state) {
        return state.getBlock() instanceof WSR88DCore;
    }

    public static TowerState stateAt(Level level, BlockPos corePos) {
        try {
            BlockState coreState = level.getBlockState(corePos);
            if (!(coreState.getBlock() instanceof WSR88DCore core)) {
                return TowerState.ABSENT;
            }

            int shellBlocks = 0;
            int missingShellBlocks = 0;
            boolean unknown = false;
            Block radome = ModBlocks.RADOME.get();
            for (Map.Entry<BlockPos, Block> entry : core.getStructure().entrySet()) {
                if (BlockPos.ZERO.equals(entry.getKey()) || entry.getValue() != radome) {
                    continue;
                }

                BlockPos shellPos = corePos.offset(entry.getKey());
                if (!hasChunkAt(level, shellPos)) {
                    unknown = true;
                    continue;
                }

                if (level.getBlockState(shellPos).is(radome)) {
                    shellBlocks++;
                } else {
                    missingShellBlocks++;
                }
            }

            if (shellBlocks <= 0) {
                return unknown ? TowerState.UNKNOWN : TowerState.BROKEN;
            }

            if (missingShellBlocks <= 0 && !unknown) {
                return TowerState.WORKING;
            }

            return TowerState.BROKEN;
        } catch (RuntimeException exception) {
            return TowerState.UNKNOWN;
        }
    }

    private static boolean hasChunkAt(Level level, BlockPos pos) {
        return level.hasChunk(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
    }

    public enum TowerState {
        ABSENT,
        WORKING,
        BROKEN,
        UNKNOWN;

        public boolean visible() {
            return this == WORKING || this == BROKEN;
        }

        public boolean operational() {
            return this == WORKING;
        }
    }
}
