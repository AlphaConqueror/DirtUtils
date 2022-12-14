package gg.dirtcraft.dirtutils.commands.core;

import gg.dirtcraft.dirtutils.commands.core.exceptions.CommandPermissionException;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.ICommandResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class DirtCommandBase implements CommandExecutor, Listener {

    private final JavaPlugin javaPlugin;
    private final PluginCommand command;
    private final String primaryAlias;

    private final Set<DirtCommandBase> subCommands = new HashSet<>();

    protected DirtCommandBase(final JavaPlugin javaPlugin, final String primaryAlias) {
        this.javaPlugin = javaPlugin;
        this.command = javaPlugin.getCommand(primaryAlias);
        this.primaryAlias = primaryAlias;
    }

    public void register() {
        this.command.setUsage(this.getCommandUsage());

        this.command.setExecutor(this);
    }

    public String getPrimaryAlias() {
        return this.primaryAlias;
    }

    public abstract String getCommandUsage();

    public void onDisable() {}

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        final ICommandResult commandResult = this.executeCommand(sender, args);

        if (commandResult instanceof CommandReplyResult) {
            final CommandReplyResult commandReplyResult = (CommandReplyResult) commandResult;

            sender.sendMessage(commandReplyResult.getReply());
        }

        return true;
    }

    public ICommandResult executeCommand(final CommandSender sender, final String[] args) {
        // handle sub-command
        if (args.length > 0) {
            final Optional<DirtCommandBase> subCommand =
                    this.subCommands.stream().filter(dcb -> dcb.getPrimaryAlias().equals(args[0])).findFirst();

            if (subCommand.isPresent()) {
                return subCommand.get().executeCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        try {
            if (sender instanceof Player) {
                return this.executePlayerCommand((Player) sender, args);
            } else if (sender instanceof BlockCommandSender) {
                return this.executeCommandBlockCommand((BlockCommandSender) sender, args);
            } else {
                return this.executeConsoleCommand((ConsoleCommandSender) sender, args);
            }
        } catch (final CommandPermissionException e) {
            return new CommandReplyResult.InsufficientPermission(e.getMessage());
        }
    }

    protected ICommandResult executePlayerCommand(final Player sender, final String[] args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    protected ICommandResult executeConsoleCommand(final ConsoleCommandSender sender, final String[] args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    protected ICommandResult executeCommandBlockCommand(final BlockCommandSender sender, final String[] args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    // add support for * permissions
    public void checkPermission(final Player player, final String permission) throws CommandPermissionException {
        final OfflinePlayer offlinePlayer = Bukkit.getPlayer(player.getUniqueId());

        if (offlinePlayer == null || !offlinePlayer.isOnline() || !offlinePlayer.getPlayer().hasPermission(permission)) {
            throw new CommandPermissionException(permission);
        }
    }

    protected void registerEvents() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this.javaPlugin);
    }

    protected JavaPlugin getJavaPlugin() {
        return this.javaPlugin;
    }

    protected void addSubCommand(final DirtCommandBase dirtCommandBase) {
        assert this.subCommands.stream().noneMatch(dcb -> dcb.getPrimaryAlias().equals(dirtCommandBase.getPrimaryAlias()))
                : String.format("Sub-command list already contains sub-command with primary alias '%s'.",
                dirtCommandBase.getPrimaryAlias());

        this.subCommands.add(dirtCommandBase);
    }
}
