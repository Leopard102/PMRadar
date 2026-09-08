package com.leopard.pmradar;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared material exclusions for client and server debris detection. */
public final class RadarDebrisFilter {
    private RadarDebrisFilter() {
    }

    public static boolean isCandidate(BlockState blockState) {
        if (blockState == null || blockState.isAir()) {
            return false;
        }

        return blockState.blocksMotion()
                && !blockState.liquid()
                && blockState.getFluidState().isEmpty()
                && !blockState.is(BlockTags.DIRT)
                && !blockState.is(BlockTags.SAND)
                && !blockState.is(BlockTags.SNOW)
                && !blockState.is(BlockTags.ICE)
                && !blockState.is(BlockTags.LEAVES)
                && !blockState.is(BlockTags.FLOWERS)
                && !blockState.is(BlockTags.CROPS)
                && !blockState.is(BlockTags.SAPLINGS)
                && !blockState.is(Blocks.GRASS_BLOCK)
                && !blockState.is(Blocks.DIRT)
                && !blockState.is(Blocks.COARSE_DIRT)
                && !blockState.is(Blocks.PODZOL)
                && !blockState.is(Blocks.ROOTED_DIRT)
                && !blockState.is(Blocks.MUD)
                && !blockState.is(Blocks.SAND)
                && !blockState.is(Blocks.RED_SAND)
                && !blockState.is(Blocks.GRAVEL)
                && !blockState.is(Blocks.CLAY)
                && !blockState.is(Blocks.STONE)
                && !blockState.is(Blocks.DEEPSLATE)
                && !blockState.is(Blocks.GRANITE)
                && !blockState.is(Blocks.DIORITE)
                && !blockState.is(Blocks.ANDESITE)
                && !blockState.is(Blocks.TUFF)
                && !blockState.is(Blocks.CALCITE)
                && !blockState.is(Blocks.DRIPSTONE_BLOCK)
                && !blockState.is(Blocks.SHORT_GRASS)
                && !blockState.is(Blocks.TALL_GRASS)
                && !blockState.is(Blocks.FERN)
                && !blockState.is(Blocks.LARGE_FERN)
                && !blockState.is(Blocks.SNOW)
                && !blockState.is(Blocks.SNOW_BLOCK);
    }
}