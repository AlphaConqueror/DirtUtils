package gg.dirtcraft.dirtutils.commands.core;

import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.CommandResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public abstract class DirtCommandBase implements CommandExecutor, Listener {

    private final JavaPlugin javaPlugin;
    private final PluginCommand command;
    private final String primaryAlias;
    private final List<String> defaultAliases = new ArrayList<>();

    protected DirtCommandBase(final JavaPlugin javaPlugin, final String primaryAlias) {
        this.javaPlugin = javaPlugin;
        this.command = javaPlugin.getCommand(primaryAlias);
        this.primaryAlias = primaryAlias;
    }

    public void register() {
        this.command.setAliases(this.computeCommandAliases());
        this.command.setUsage(this.getCommandUsage());

        this.command.setExecutor(this);
    }

    protected PluginCommand getCommand() {
        return this.command;
    }

    public String getPrimaryAlias() {
        return this.primaryAlias;
    }

    private List<String> computeCommandAliases() {
        final List<String> aliases = new ArrayList<>();

        //aliases.add(this.primaryAlias);
        aliases.addAll(this.defaultAliases);

        return aliases;
    }

    public abstract String getCommandUsage();

    protected DirtCommandBase addDefaultAlias(final String alias) {
        this.defaultAliases.add(alias);

        return this;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        final CommandResult commandResult = this.executeCommand(sender, args);

        if (commandResult instanceof CommandReplyResult) {
            final CommandReplyResult commandReplyResult = (CommandReplyResult) commandResult;

            sender.sendMessage(commandReplyResult.getReply());
        }

        return commandResult.isExecutable();
    }

    protected CommandResult executeCommand(final CommandSender sender, final String[] args) {
        if (sender instanceof Player) {
            return this.executePlayerCommand((Player) sender, args);
        } else if (sender instanceof BlockCommandSender) {
            return this.executeCommandBlockCommand((BlockCommandSender) sender, args);
        } else {
            return this.executeConsoleCommand((ConsoleCommandSender) sender, args);
        }
    }

    protected CommandResult executePlayerCommand(final Player sender, final String[] args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    protected CommandResult executeConsoleCommand(final ConsoleCommandSender sender, final String[] args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    protected CommandResult executeCommandBlockCommand(final BlockCommandSender sender, final String[] args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    public boolean hasPermission(final Player player, final String permission) {
        final OfflinePlayer offlinePlayer = Bukkit.getPlayer(player.getUniqueId());

        if (offlinePlayer == null) {
            return false;
        }

        if (!offlinePlayer.isOnline()) {
            return false;
        }

        return offlinePlayer.getPlayer().hasPermission(permission);
    }

    protected void registerEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this.javaPlugin);
    }
}
