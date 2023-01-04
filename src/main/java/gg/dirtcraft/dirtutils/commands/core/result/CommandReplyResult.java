package gg.dirtcraft.dirtutils.commands.core.result;

import gg.dirtcraft.dirtutils.Cli;
import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class CommandReplyResult implements ICommandResult {

    public abstract String getReply();

    /**
     * The command result representing a {@link DirtCommandBase} that can be executed.
     */
    public static class Success extends CommandReplyResult {

        private final String reply;

        public Success(final String reply) {
            this.reply = reply;
        }

        @Override
        public String getReply() {
            return Cli.PREFIX + this.reply;
        }
    }

    /**
     * The command result representing syntax errors occurring during the execution of a {@link DirtCommandBase}.
     */
    public static final class SyntaxError extends CommandReplyResult {

        private final DirtCommandBase command;

        public SyntaxError(final DirtCommandBase command) {
            this.command = command;
        }

        @Override
        public String getReply() {
            return String.format("%s%sUsage: %s%s", Cli.PREFIX, ChatColor.DARK_AQUA, ChatColor.AQUA,
                    this.command.getCommandUsage());
        }
    }

    /**
     * The command result representing an illegal executor of a {@link DirtCommandBase}.
     */
    public static final class IllegalExecutor extends CommandReplyResult {

        private final DirtCommandBase command;
        private final CommandSender sender;


        public IllegalExecutor(final DirtCommandBase command, final CommandSender sender) {
            this.command = command;
            this.sender = sender;
        }

        @Override
        public String getReply() {
            final String s;

            if (this.sender instanceof Player) {
                s = "as a player";
            } else if (this.sender instanceof BlockCommandSender) {
                s = "from a command block";
            } else {
                s = "from the console";
            }

            return String.format("%s%sThe command %s%s%s can not be executed %s.",
                    Cli.PREFIX, this.sender instanceof Player ? ChatColor.DARK_AQUA : "", ChatColor.AQUA,
                    this.command.getPrimaryAlias(), ChatColor.DARK_AQUA, s);
        }
    }

    /**
     * The command result representing an illegal executor of a {@link DirtCommandBase}.
     */
    public static final class InsufficientPermission extends CommandReplyResult {

        private final String permission;

        public InsufficientPermission(final String permission) {
            this.permission = permission;
        }

        @Override
        public String getReply() {
            return String.format("%s%sYou are missing the permission: %s%s", Cli.PREFIX, ChatColor.DARK_AQUA,
                    ChatColor.AQUA, this.permission);
        }
    }
}
