package com.zergatul.cheatutils.modules.esp.blocks;

import com.zergatul.cheatutils.collections.ImmutableList;
import com.zergatul.cheatutils.configs.BlockEspConfig;
import com.zergatul.cheatutils.configs.BlocksConfig;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.modules.esp.EspGlobal;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

import java.util.ArrayList;
import java.util.List;

public class BlockEspCommand extends CommandBase {

    @Override
    public String getName() {
        return "cu_blockesp";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cu_blockesp <on|off|add|remove|clear|range|tracers|boxes|overlay|list|status|rescan>";
    }

    @Override
    public void execute(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args) {
        BlocksConfig blocks = ConfigStore.instance.getConfig().blocks;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            int index = 1;
            for (BlockEspConfig config : blocks.getBlockConfigs()) {
                StringBuilder names = new StringBuilder();
                for (Block block : config.blocks) {
                    if (names.length() > 0) names.append(", ");
                    ResourceLocation id = Block.REGISTRY.getNameForObject(block);
                    names.append(id == null ? "unknown" : id.toString());
                }
                sendChat(sender, index++ + ": " + names + " | " + (config.enabled ? "ON" : "OFF")
                        + " | range=" + config.maxDistance
                        + " | boxes=" + config.drawBoundingBox
                        + " | tracers=" + config.drawTracers
                        + " | overlay=" + config.drawOverlay);
            }
            if (index == 1) {
                sendChat(sender, "No Block ESP entries. Use /cu_blockesp add <minecraft:block_name>.");
            }
            return;
        }

        String action = args[0].toLowerCase();

        if (action.equals("on") || action.equals("off")) {
            boolean enabled = action.equals("on");
            EspGlobal.enabled = enabled;
            for (BlockEspConfig config : blocks.getBlockConfigs()) config.enabled = enabled;
            ConfigStore.instance.requestWrite();
            sendChat(sender, "Block ESP " + (enabled ? "enabled" : "disabled") + ".");
            return;
        }

        if (action.equals("add")) {
            if (args.length < 2) {
                sendChat(sender, "Usage: /cu_blockesp add <minecraft:block_name>");
                return;
            }
            Block block = findBlock(args[1]);
            if (block == null) {
                sendChat(sender, "Unknown block: " + args[1]);
                return;
            }

            BlockEspConfig existing = blocks.find(block);
            if (existing != null) {
                existing.enabled = true;
                ConfigStore.instance.requestWrite();
                sendChat(sender, "Already configured: " + args[1] + " (enabled it).");
                return;
            }

            BlockEspConfig config = BlockEspConfig.createDefault(ImmutableList.from(block));
            config.enabled = true;
            blocks.add(config);
            ConfigStore.instance.requestWrite();
            sendChat(sender, "Added and enabled " + args[1] + ".");
            return;
        }

        if (action.equals("remove")) {
            if (args.length < 2) {
                sendChat(sender, "Usage: /cu_blockesp remove <minecraft:block_name>");
                return;
            }
            Block block = findBlock(args[1]);
            if (block == null) {
                sendChat(sender, "Unknown block: " + args[1]);
                return;
            }
            BlockEspConfig config = blocks.find(block);
            if (config == null) {
                sendChat(sender, "Not configured: " + args[1]);
                return;
            }
            blocks.remove(config);
            ConfigStore.instance.requestWrite();
            sendChat(sender, "Removed " + args[1] + ".");
            return;
        }

        if (action.equals("clear")) {
            List<BlockEspConfig> copy = new ArrayList<>();
            for (BlockEspConfig config : blocks.getBlockConfigs()) copy.add(config);
            for (BlockEspConfig config : copy) blocks.remove(config);
            ConfigStore.instance.requestWrite();
            sendChat(sender, "Cleared Block ESP.");
            return;
        }

        if (action.equals("rescan")) {
            BlockFinder.instance.rescan();
            sendChat(sender, "Block ESP rescan requested.");
            return;
        }

        if (action.equals("status")) {
            int total = 0;
            int enabled = 0;
            for (BlockEspConfig config : blocks.getBlockConfigs()) {
                Set<BlockPos> found = BlockFinder.instance.blocks.get(config);
                int count = found == null ? 0 : found.size();
                total += count;
                if (config.enabled) enabled++;
                sendChat(sender, "ESP #" + enabled + ": found=" + count + " enabled=" + config.enabled
                        + " boxes=" + config.drawBoundingBox + " tracers=" + config.drawTracers
                        + " overlay=" + config.drawOverlay + " range=" + config.maxDistance);
            }
            sendChat(sender, "Global ESP=" + EspGlobal.enabled + " | configs=" + blocks.getBlockConfigs().size()
                    + " | enabled configs=" + enabled + " | found positions=" + total);
            return;
        }

        if (action.equals("range")) {
            if (args.length < 2) {
                sendChat(sender, "Usage: /cu_blockesp range <1-1000>");
                return;
            }
            double range;
            try {
                range = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sendChat(sender, "Range must be a number.");
                return;
            }
            if (range < 1 || range > 1000) {
                sendChat(sender, "Range must be between 1 and 1000.");
                return;
            }
            for (BlockEspConfig config : blocks.getBlockConfigs()) config.maxDistance = range;
            ConfigStore.instance.requestWrite();
            sendChat(sender, "Block ESP range set to " + range + ".");
            return;
        }

        if (action.equals("tracers") || action.equals("boxes") || action.equals("overlay")) {
            if (args.length < 2) {
                sendChat(sender, "Usage: /cu_blockesp " + action + " <on|off>");
                return;
            }
            boolean value = parseBoolean(args[1]);
            for (BlockEspConfig config : blocks.getBlockConfigs()) {
                if (action.equals("tracers")) config.drawTracers = value;
                if (action.equals("boxes")) config.drawBoundingBox = value;
                if (action.equals("overlay")) config.drawOverlay = value;
            }
            ConfigStore.instance.requestWrite();
            sendChat(sender, action + " " + (value ? "enabled" : "disabled") + ".");
            return;
        }

        sendChat(sender, getUsage(sender));
    }

    private Block findBlock(String id) {
        return Block.REGISTRY.getObject(new ResourceLocation(id));
    }

    public static boolean parseBoolean(String value) {
        return value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes");
    }

    private void sendChat(ICommandSender sender, String message) {
        sender.sendMessage(new net.minecraft.util.text.TextComponentString("[CheatUtils] " + message));
    }

    @Override
    public boolean checkPermission(net.minecraft.server.MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public List<String> getTabCompletions(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            result.add("on");
            result.add("off");
            result.add("add");
            result.add("remove");
            result.add("clear");
            result.add("range");
            result.add("tracers");
            result.add("boxes");
            result.add("overlay");
            result.add("list");
            result.add("status");
            result.add("rescan");
            return getListOfStringsMatchingLastWord(args, result);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("tracers") || args[0].equalsIgnoreCase("boxes") || args[0].equalsIgnoreCase("overlay"))) {
            List<String> result = new ArrayList<>();
            result.add("on");
            result.add("off");
            return getListOfStringsMatchingLastWord(args, result);
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getAliases() {
        return new ArrayList<>();
    }
}
