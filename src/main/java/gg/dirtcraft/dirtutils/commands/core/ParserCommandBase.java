package gg.dirtcraft.dirtutils.commands.core;

import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.ICommandResult;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ParserCommandBase extends DirtCommandBase {

    private final List<ArgContainer<?>> expectedArgs = new ArrayList<>();

    public ParserCommandBase(final JavaPlugin javaPlugin, final String primaryAlias) {
        super(javaPlugin, primaryAlias);
    }

    @Override
    public String getCommandUsage() {
        final StringBuilder s = new StringBuilder("/");

        s.append(this.getPrimaryAlias());
        this.expectedArgs.forEach(argContainer -> s.append(' ').append(argContainer.toString()));

        return s.toString();
    }

    @Override
    public final ICommandResult executeCommand(final CommandSender sender, final String[] args) {
        if (args.length < this.getNeededArgsAmount() || args.length > this.expectedArgs.size()) {
            return new CommandReplyResult.SyntaxError(this);
        }

        return super.executeCommand(sender, args);
    }

    private long getNeededArgsAmount() {
        return this.expectedArgs.stream().filter(argContainer -> !argContainer.isOptional()).count();
    }

    @Override
    protected final ICommandResult executePlayerCommand(final Player sender, final String[] args) {
        this.parse(args);
        return this.executePlayerCommand(sender);
    }

    protected ICommandResult executePlayerCommand(final Player sender) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    @Override
    protected final ICommandResult executeConsoleCommand(final ConsoleCommandSender sender, final String[] args) {
        return super.executeConsoleCommand(sender, args);
    }

    protected ICommandResult executeConsoleCommand(final Player sender) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    @Override
    protected final ICommandResult executeCommandBlockCommand(final BlockCommandSender sender, final String[] args) {
        return super.executeCommandBlockCommand(sender, args);
    }

    protected ICommandResult executeCommandBlockCommand(final Player sender) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    private List<ArgContainer<?>> parse(final String[] args) {
        final List<String> argList = new ArrayList<>(Arrays.asList(args));

        this.expectedArgs.forEach(argContainer -> argContainer.computeParsedArg(argList));

        return this.expectedArgs;
    }

    // add options in front to parse them first
    protected void addExpectedArg(final ArgContainer<?> argContainer) {
        if (argContainer instanceof ArgContainer.OptionArgContainer) {
            this.expectedArgs.add(0, argContainer);
        } else {
            this.expectedArgs.add(argContainer);
        }
    }
}
