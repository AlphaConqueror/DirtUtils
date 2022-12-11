package gg.dirtcraft.dirtutils.utils;

import gg.dirtcraft.dirtutils.Cli;
import org.bukkit.entity.Player;

public class PlayerUtils {

    public static void sendMessageWithPrefix(final Player target, final String message) {
        target.sendMessage(Cli.PREFIX + message);
    }
}
