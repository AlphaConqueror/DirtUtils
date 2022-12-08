package gg.dirtcraft.dirtutils.commands.core.result;

import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class CommandReplyResult implements CommandResult {

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
        public boolean isExecutable() {
            return true;
        }

        @Override
        public String getReply() {
            return this.reply;
        }
    }

    /**
     * The command result representing syntax errors occurring during the execution of a {@link DirtCommandBase}.
     */
    public static final class SyntaxError extends CommandReplyResult {

        private final DirtCommandBase command;
        private final CommandSender sender;

        public SyntaxError(final DirtCommandBase command, final CommandSender sender) {
            this.command = command;
            this.sender = sender;
        }

        @Override
        public boolean isExecutable() {
            return false;
        }

        @Override
        public String getReply() {
            return this.command.getCommandUsage();
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
        public boolean isExecutable() {
            return false;
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

            return String.format("The command '%s' can not be executed %s.", this.command.getPrimaryAlias(), s);
        }
    }
}
