package me.moonful.fsp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FspCommand implements CommandExecutor {

    private final FunSpeedRepairPlugin plugin;

    public FspCommand(FunSpeedRepairPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadValues();
            sender.sendMessage("§aFunSpeedRepair reloaded!");
            return true;
        }

        sender.sendMessage("§eUsage: /fsp reload");
        return true;
    }
}
