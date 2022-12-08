package gg.dirtcraft.dirtutils.commands.core;

import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.CommandResult;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ParserCommandBase extends DirtCommandBase {

    public ParserCommandBase(final JavaPlugin javaPlugin, final String primaryAlias) {
        super(javaPlugin, primaryAlias);
    }

    @Override
    public CommandResult executeCommand(final CommandSender sender, final String[] args) {
        if (args.length < this.getNeededArgsAmount() || args.length > this.getExpectedArgs().size()) {
            return new CommandReplyResult.SyntaxError(this, sender);
        }

        return super.executeCommand(sender, args);
    }

    private long getNeededArgsAmount() {
        return this.getExpectedArgs().stream().filter(argContainer -> !argContainer.isOptional()).count();
    }
}
