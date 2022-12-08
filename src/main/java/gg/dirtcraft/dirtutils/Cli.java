package gg.dirtcraft.dirtutils;

import gg.dirtcraft.dirtutils.commands.core.DirtCommandBase;
import gg.dirtcraft.dirtutils.commands.player.CommandVanish;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Cli extends JavaPlugin {

    @Override
    public void onEnable() {
        this.registerCommands();
    }

    private void registerCommands() {
        final List<DirtCommandBase> commands = new ArrayList<>();

        commands.add(new CommandVanish(this));

        commands.forEach(DirtCommandBase::register);
    }
}
