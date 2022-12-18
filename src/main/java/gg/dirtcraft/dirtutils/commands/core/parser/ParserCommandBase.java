package gg.dirtcraft.dirtutils.commands.core.parser;

import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.ICommandResult;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

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
        return this.executePlayerCommand(sender, this.parse(args));
    }

    protected ICommandResult executePlayerCommand(final Player sender, final Map<Long, Object> args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    @Override
    protected final ICommandResult executeConsoleCommand(final ConsoleCommandSender sender, final String[] args) {
        return this.executeConsoleCommand(sender, this.parse(args));
    }

    protected ICommandResult executeConsoleCommand(final ConsoleCommandSender sender, final Map<Long, Object> args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    @Override
    protected final ICommandResult executeCommandBlockCommand(final BlockCommandSender sender, final String[] args) {
        return this.executeCommandBlockCommand(sender, this.parse(args));
    }

    protected ICommandResult executeCommandBlockCommand(final BlockCommandSender sender, final Map<Long, Object> args) {
        return new CommandReplyResult.IllegalExecutor(this, sender);
    }

    private Map<Long, Object> parse(final String[] args) {
        final Map<Long, Object> parsedArgs = new HashMap<>();
        final List<String> argList = new ArrayList<>(Arrays.asList(args));

        this.expectedArgs.forEach(argContainer -> parsedArgs.put(argContainer.getId(), argContainer.parse(argList)));

        return parsedArgs;
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
