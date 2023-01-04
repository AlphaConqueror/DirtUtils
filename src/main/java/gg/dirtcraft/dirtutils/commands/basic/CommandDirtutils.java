package gg.dirtcraft.dirtutils.commands.basic;

import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.ICommandResult;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CommandDirtutils extends DirtCommandBase {

    // TODO: Add info sub-command to show version.
    // TODO: Add help sub-command to show all available sub-commands.

    private final List<DirtCommandBase> commands;

    public CommandDirtutils(final JavaPlugin javaPlugin, final List<DirtCommandBase> commands) {
        super(javaPlugin, "dirtutils");

        this.commands = commands;

        // sort list by primary alias
        this.commands.sort(Comparator.comparing(DirtCommandBase::getPrimaryAlias));
    }

    @Override
    public String getCommandUsage() {
        return "/dirtutils";
    }

    @Override
    public ICommandResult executeCommand(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            return new CommandReplyResult.Success(String.format("Using %s%s v%s%s.",
                    ChatColor.AQUA, this.getJavaPlugin().getDescription().getName(),
                    this.getJavaPlugin().getDescription().getVersion(), ChatColor.DARK_AQUA));
        }

        final Optional<DirtCommandBase> command =
                this.commands.stream().filter(dirtCommandBase -> dirtCommandBase.getPrimaryAlias().equalsIgnoreCase(args[0])).findFirst();

        if (command.isPresent()) {
            return command.get().executeCommand(sender, args);
        }

        return new CommandReplyResult.SyntaxError(this);
    }
}
