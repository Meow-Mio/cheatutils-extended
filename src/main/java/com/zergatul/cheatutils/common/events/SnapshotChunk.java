package com.zergatul.cheatutils.common.events;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

public class SnapshotChunk {

    private final ChunkPos pos;
    private final Block[][] sections;

    private SnapshotChunk(ChunkPos pos, Block[][] sections) {
        this.pos = pos;
        this.sections = sections;
    }

    public ChunkPos getPos() {
        return pos;
    }

    public IBlockState getBlockState(int x, int y, int z) {
        Block block = sections[y >> 4][((y & 0x0F) << 8) | (z << 4) | x];
        return block == null ? Blocks.AIR.getDefaultState() : block.getDefaultState();
    }

    /**
     * Make a thread-safe snapshot of the block types in a chunk.
     *
     * VintageFix replaces/optimizes several of vanilla's chunk-access and
     * BlockStateContainer internals. Reading those internals through
     * mixin accessors made the ESP scanner see empty sections in E2E.
     *
     * Chunk#getBlockState is the stable Forge/Minecraft API and is also the
     * path used by VintageFix's optimized chunk access, so capture the block
     * types through that API while we are still on the main client thread.
     *
     * BlockFinder only needs the block type, not block properties, so storing
     * Block references is sufficient and considerably smaller than copying
     * complete IBlockState objects.
     */
    public static SnapshotChunk from(Chunk chunk) {
        Block[][] sections = new Block[16][];
        for (int sectionY = 0; sectionY < 16; sectionY++) {
            Block[] section = new Block[16 * 16 * 16];
            int baseY = sectionY << 4;

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        pos.setPos((chunk.x << 4) + x, baseY + y, (chunk.z << 4) + z);
                        IBlockState state = chunk.getBlockState(pos);
                        section[(y << 8) | (z << 4) | x] =
                                state == null ? Blocks.AIR : state.getBlock();
                    }
                }
            }

            sections[sectionY] = section;
        }

        return new SnapshotChunk(chunk.getPos(), sections);
    }
}
