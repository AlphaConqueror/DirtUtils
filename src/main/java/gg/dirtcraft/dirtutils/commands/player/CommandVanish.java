package gg.dirtcraft.dirtutils.commands.player;

import gg.dirtcraft.dirtutils.commands.core.ParserCommandBase;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.CommandResult;
import gg.dirtcraft.dirtutils.commands.util.ArgType;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CommandVanish extends ParserCommandBase {

    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Lock vanishLock = new ReentrantLock();

    public CommandVanish(final JavaPlugin javaPlugin) {
        super(javaPlugin, "vanish");

        this.addExpectedArg(ArgType.WORD, "player", true);

        this.registerEvents();
    }

    @Override
    protected CommandResult executePlayerCommand(final Player sender, final String[] args) {
        this.vanishToggle(sender);
        return new CommandReplyResult.Success(this.isVanished(sender) ? "Successfully vanished!" : "Successfully unvanished!");
    }

    private void vanishToggle(final Player player) {
        try {
            this.vanish(player, !this.isVanished(player));
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isVanished(final Player player) {
        this.vanishLock.lock();
        final boolean isVanished = this.vanishedPlayers.stream().anyMatch(uuid -> uuid.equals(player.getUniqueId()));
        this.vanishLock.unlock();

        return isVanished;
    }

    private void vanish(final Player player, final boolean state) throws NoSuchFieldException, IllegalAccessException {
        assert state != this.isVanished(player) : "Tried to un-/vanish player twice!";

        final Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());

        // remove vanished players from the list of all players that should not be able to see the player
        this.vanishLock.lock();
        this.vanishedPlayers.forEach(uuid -> players.removeIf(p -> p.getUniqueId().equals(uuid)));
        this.vanishLock.unlock();

        if (state) {
            // add player to the list of all vanished players
            this.vanishLock.lock();
            this.vanishedPlayers.add(player.getUniqueId());
            this.vanishLock.unlock();

            // hide player from all players that should not be able to see the player
            for (final Player p : players) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                p.hidePlayer(player);
            }

            // show vanished players to player
            for (final UUID uuid : this.vanishedPlayers) {
                if (uuid.equals(player.getUniqueId())) {
                    continue;
                }

                this.showPlayer(player, Bukkit.getPlayer(uuid));
            }
        } else {
            // remove player from the list of all vanished players
            this.vanishLock.lock();
            this.vanishedPlayers.removeIf(uuid -> uuid.equals(player.getUniqueId()));
            this.vanishLock.unlock();

            // show player to all online players
            for (final Player p : players) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                this.showPlayer(p, player);
            }

            // hide vanished players from player
            for (final UUID uuid : this.vanishedPlayers) {
                if (uuid.equals(player.getUniqueId())) {
                    continue;
                }

                player.hidePlayer(Bukkit.getPlayer(uuid));
            }
        }
    }

    private void showPlayer(final Player player, final Player target) {
        player.showPlayer(target);

        final CraftPlayer craftPlayer = (CraftPlayer) player;
        System.out.println(player);

        final EntityPlayer entityPlayer = craftPlayer.getHandle();
        System.out.println(entityPlayer);

        final PlayerConnection playerConnection = entityPlayer.playerConnection;
        System.out.println(playerConnection);

        final CraftPlayer craftTarget = (CraftPlayer) target;
        System.out.println(craftTarget);

        final EntityPlayer entityTarget = craftTarget.getHandle();
        System.out.println(entityTarget);

        final WorldServer worldServer = ((CraftWorld) target.getWorld()).getHandle();
        final EntityTracker tracker = worldServer.getTracker();
        final EntityTrackerEntry entry = (EntityTrackerEntry) tracker.trackedEntities.get(target.getEntityId());

        entry.updatePlayer(entityPlayer);
    }

    @EventHandler
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();

        if (!this.isVanished(player)) {
            return;
        }

        final Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());

        System.out.println("Changed World Event");

        // remove vanished players from the list of all players that should not be able to see the player
        this.vanishLock.lock();
        this.vanishedPlayers.forEach(uuid -> players.removeIf(p -> p.getUniqueId().equals(uuid)));
        this.vanishLock.unlock();

        // hide player from all players that should not be able to see the player
        for (final Player p : players) {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            p.hidePlayer(player);
        }

        // show vanished players to player
        for (final UUID uuid : this.vanishedPlayers) {
            if (uuid.equals(player.getUniqueId())) {
                continue;
            }

            player.hidePlayer(Bukkit.getPlayer(uuid));

            System.out.println("Showing player " + Bukkit.getPlayer(uuid).getDisplayName() + " to " + player.getDisplayName());
            this.showPlayer(player, Bukkit.getPlayer(uuid));
        }
    }
}
