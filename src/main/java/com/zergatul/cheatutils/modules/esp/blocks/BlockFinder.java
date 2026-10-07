package com.zergatul.cheatutils.modules.esp.blocks;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.SnapshotChunk;
import com.zergatul.cheatutils.configs.BlockEspConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.common.events.BlockUpdateEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockFinder {

    public static final BlockFinder instance = new BlockFinder();

    public final Map<BlockEspConfig, Set<BlockPos>> blocks = new ConcurrentHashMap<>();

    private BlockFinder() {
        Events.ChunkLoaded.add(this::onChunkLoaded);
        Events.ChunkUnloaded.add(this::onChunkUnloaded);
        Events.BlockUpdated.add(this::onBlockUpdated);
    }

    public void addConfig(BlockEspConfig config) {
        // The config is already installed in BlocksConfig when this is called.
        // Publish its result set immediately so the scan request cannot race ahead
        // of the config becoming visible to the renderer.
        blocks.put(config, ConcurrentHashMap.newKeySet());
        BlockEventsProcessor.instance.requestScan(config);
    }

    public void applyConfigs(ImmutableList<BlockEspConfig> configs) {
        blocks.clear();
        for (BlockEspConfig config : configs) {
            blocks.put(config, ConcurrentHashMap.newKeySet());
        }
        BlockEventsProcessor.instance.requestFullScan();
    }

    public void removeConfig(BlockEspConfig config) {
        blocks.remove(config);
    }

    public void clearPositions() {
        for (Set<BlockPos> set : blocks.values()) {
            set.clear();
        }
    }

    public void rescan() {
        clearPositions();
        BlockEventsProcessor.instance.requestFullScan();
    }

    private void onChunkLoaded(SnapshotChunk chunk) {
        scanChunkForAllBlocks(chunk);
    }

    private void onChunkUnloaded(ChunkPos pos) {
        final int cx = pos.x;
        final int cz = pos.z;
        for (Set<BlockPos> set : blocks.values()) {
            set.removeIf(p -> (p.getX() >> 4) == cx && (p.getZ() >> 4) == cz);
        }
    }

    private void onBlockUpdated(BlockUpdateEvent event) {
        BlockPos pos = event.pos();
        for (Set<BlockPos> set : blocks.values()) {
            set.remove(pos);
        }

        List<BlockEspConfig> configs = ConfigStore.instance.getConfig().blocks
                .getConfigsMap()
                .get(event.state().getBlock());
        if (configs == null) {
            return;
        }

        for (BlockEspConfig config : configs) {
            Set<BlockPos> set = blocks.get(config);
            if (set != null) {
                set.add(pos);
            }
        }
    }

    public void scanChunkForBlock(SnapshotChunk chunk, BlockEspConfig config) {
        Set<BlockPos> set = blocks.get(config);
        if (set == null) {
            return;
        }

        Set<Block> blockTypes = new HashSet<>();
        for (Block block : config.blocks) {
            blockTypes.add(block);
        }

        int xc = chunk.getPos().x << 4;
        int zc = chunk.getPos().z << 4;
        for (int x = 0; x < 16; x++) {
            int xw = xc | x;
            for (int z = 0; z < 16; z++) {
                int zw = zc | z;
                for (int y = 0; y < 256; y++) {
                    IBlockState state = chunk.getBlockState(x, y, z);
                    if (blockTypes.contains(state.getBlock())) {
                        set.add(new BlockPos(xw, y, zw));
                    }
                }
            }
        }
    }

    private void checkBlock(int x, int y, int z, IBlockState state, Map<Block, BlockEspConfig> map) {
        if (state.getMaterial() == Material.AIR) {
            return;
        }

        BlockEspConfig config = map.get(state.getBlock());
        if (config != null) {
            Set<BlockPos> set = blocks.get(config);
            if (set != null) {
                BlockPos pos = new BlockPos(x, y, z);
                set.add(pos);
            }
        }
    }
    private void scanChunkForAllBlocks(SnapshotChunk chunk) {
        int xc = chunk.getPos().x << 4;
        int zc = chunk.getPos().z << 4;
        Map<Block, List<BlockEspConfig>> map = ConfigStore.instance.getConfig().blocks.getConfigsMap();

        for (int x = 0; x < 16; x++) {
            int xw = xc | x;
            for (int z = 0; z < 16; z++) {
                int zw = zc | z;
                for (int y = 0; y < 256; y++) {
                    IBlockState state = chunk.getBlockState(x, y, z);
                    if (state.getMaterial() == Material.AIR) {
                        continue;
                    }

                    List<BlockEspConfig> configs = map.get(state.getBlock());
                    if (configs == null) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(xw, y, zw);
                    for (BlockEspConfig config : configs) {
                        Set<BlockPos> set = blocks.get(config);
                        if (set != null) {
                            set.add(pos);
                        }
                    }
                }
            }
        }
    }

}