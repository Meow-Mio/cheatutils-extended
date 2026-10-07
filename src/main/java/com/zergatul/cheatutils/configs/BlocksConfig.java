package com.zergatul.cheatutils.configs;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.configs.adapters.GsonSkip;
import com.zergatul.cheatutils.modules.esp.blocks.BlockFinder;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BlocksConfig implements ModuleStateProvider, Sanitizable {

    private ImmutableList<BlockEspConfig> configs = new ImmutableList<>();

    @GsonSkip
    private volatile Map<Block, BlockEspConfig> map;

    @GsonSkip
    private volatile Map<Block, List<BlockEspConfig>> configsMap;

    public ImmutableList<BlockEspConfig> getBlockConfigs() {
        return configs;
    }

    public Map<Block, BlockEspConfig> getMap() {
        return map;
    }

    public Map<Block, List<BlockEspConfig>> getConfigsMap() {
        return configsMap;
    }

    @Override
    public boolean isEnabled() {
        return configs.stream().anyMatch(c -> c.enabled);
    }

    public void apply() {
        refreshMap();
        BlockFinder.instance.applyConfigs(configs);
    }

    public BlockEspConfig find(Block block) {
        return map.get(block);
    }

    public BlockEspConfig findExact(ImmutableList<Block> blocks) {
        for (BlockEspConfig config: configs) {
            if (config.blocks.equals(blocks)) {
                return config;
            }
        }

        return null;
    }

    public void add(BlockEspConfig config) {
        updateBlockConfigs(configs.add(config));
        BlockFinder.instance.addConfig(config);
    }

    public void remove(BlockEspConfig config) {
        updateBlockConfigs(configs.remove(config));
        BlockFinder.instance.removeConfig(config);
    }

    public void updateBlockConfigs(ImmutableList<BlockEspConfig> configs) {
        Map<Block, BlockEspConfig> map = rebuildMap(configs);
        Map<Block, List<BlockEspConfig>> configsMap = rebuildConfigsMap(configs);
        this.configs = configs;
        this.map = map;
        this.configsMap = configsMap;
    }

    public void refreshMap() {
        map = rebuildMap(configs);
        configsMap = rebuildConfigsMap(configs);
    }

    private Map<Block, BlockEspConfig> rebuildMap(ImmutableList<BlockEspConfig> configs) {
        Map<Block, BlockEspConfig> map = new HashMap<>();
        for (BlockEspConfig config: configs) {
            for (Block block: config.blocks) {
                map.put(block, config);
            }
        }
        return map;
    }

    private Map<Block, List<BlockEspConfig>> rebuildConfigsMap(ImmutableList<BlockEspConfig> configs) {
        Map<Block, List<BlockEspConfig>> map = new HashMap<>();
        for (BlockEspConfig config : configs) {
            for (Block block : config.blocks) {
                map.computeIfAbsent(block, b -> new ArrayList<>()).add(config);
            }
        }
        return map;
    }

    @Override
    public void sanitize() {
        for (BlockEspConfig config : configs) {
            config.blocks = config.blocks.removeIf(b -> b == Blocks.AIR);
        }
        configs = configs.removeIf(c -> c.blocks.isEmpty());
    }
}