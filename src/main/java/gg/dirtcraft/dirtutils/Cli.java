package gg.dirtcraft.dirtutils;

import gg.dirtcraft.dirtutils.commands.basic.CommandDirtutils;
import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;
import gg.dirtcraft.dirtutils.commands.player.CommandVanish;
import gg.dirtcraft.dirtutils.tablist.TabListManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Cli extends JavaPlugin {

    // TODO: Read variables from config.
    // TODO: Be able to remove commands via config.

    public static final String PREFIX = String.format("%s[%s%sD%s%sU%s]%s ", ChatColor.GRAY, ChatColor.DARK_RED,
            ChatColor.BOLD, ChatColor.RED, ChatColor.BOLD, ChatColor.GRAY, ChatColor.RESET);
    private final List<DirtCommandBase> commands = new ArrayList<>();

    @Override
    public void onEnable() {
        this.registerCommands();
        this.registerEvents();
    }

    @Override
    public void onDisable() {
        this.commands.forEach(DirtCommandBase::onDisable);
    }

    private void registerCommands() {
        this.commands.add(new CommandVanish(this));

        // keep at the bottom for all commands to be available via dirtutils command
        this.commands.add(new CommandDirtutils(this, this.commands));

        this.commands.forEach(DirtCommandBase::register);
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new TabListManager(), this);
    }
}
