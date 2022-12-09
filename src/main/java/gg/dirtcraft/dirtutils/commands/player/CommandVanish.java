package gg.dirtcraft.dirtutils.commands.player;

import gg.dirtcraft.dirtutils.commands.core.ArgContainer;
import gg.dirtcraft.dirtutils.commands.core.ParserCommandBase;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.CommandResult;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTracker;
import net.minecraft.server.v1_7_R4.EntityTrackerEntry;
import net.minecraft.server.v1_7_R4.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CommandVanish extends ParserCommandBase {

    // TODO: Add option to enable/disable pickup/drop

    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Lock vanishLock = new ReentrantLock();
    private final ArgContainer.PlayerArgContainer playerArgContainer = new ArgContainer.PlayerArgContainer("player", true);
    private final ArgContainer.OptionArgContainer optionArgContainer = new ArgContainer.OptionArgContainer("-i");

    public CommandVanish(final JavaPlugin javaPlugin) {
        super(javaPlugin, "vanish");

        this.addExpectedArg(this.playerArgContainer);
        this.addExpectedArg(this.optionArgContainer);

        this.registerEvents();
    }

    @Override
    protected CommandResult executePlayerCommand(final Player sender) {
        final boolean iOption = this.optionArgContainer.getResult();
        final Player otherPlayer = this.playerArgContainer.getResult();

        System.out.println("has option 'i'?: " + iOption);

        if (otherPlayer != null) {
            this.vanishToggle(otherPlayer);
            return new CommandReplyResult.Success(this.isVanished(otherPlayer)
                    ? "Successfully vanished " + otherPlayer.getDisplayName() + "!"
                    : "Successfully unvanished " + otherPlayer.getDisplayName() + "!");
        } else {
            this.vanishToggle(sender);
            return new CommandReplyResult.Success(this.isVanished(sender) ? "Successfully vanished!" : "Successfully unvanished!");
        }
    }

    private void vanishToggle(final Player player) {
        this.vanish(player, !this.isVanished(player));
    }

    /**
     * Checks whether the player is vanished or not.
     *
     * @param player The player in question.
     * @return True, if the player is vanished, false, if otherwise.
     */
    private boolean isVanished(final Player player) {
        this.vanishLock.lock();
        final boolean isVanished = this.vanishedPlayers.stream().anyMatch(uuid -> uuid.equals(player.getUniqueId()));
        this.vanishLock.unlock();

        return isVanished;
    }

    private void vanish(final Player player, final boolean state) {
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

            // remove collision
            player.spigot().setCollidesWithEntities(false);
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

            // add collision back
            player.spigot().setCollidesWithEntities(true);
        }
    }

    /**
     * Reveals target to player.
     *
     * @param player The player who should be able to see target.
     * @param target The target to be revealed.
     */
    private void showPlayer(final Player player, final Player target) {
        player.showPlayer(target);

        final CraftPlayer craftPlayer = (CraftPlayer) player;
        final EntityPlayer entityPlayer = craftPlayer.getHandle();
        final WorldServer worldServer = ((CraftWorld) target.getWorld()).getHandle();
        final EntityTracker tracker = worldServer.getTracker();
        final EntityTrackerEntry entry = (EntityTrackerEntry) tracker.trackedEntities.get(target.getEntityId());

        // update player in entity tracker entry since Player#showPlayer does not do it properly
        entry.updatePlayer(entityPlayer);
    }

    @EventHandler
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();

        if (!this.isVanished(player)) {
            return;
        }

        final Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());

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

            // reapply for #showPlayer to take effect (idk, thank 1.7.10)
            player.hidePlayer(Bukkit.getPlayer(uuid));
            this.showPlayer(player, Bukkit.getPlayer(uuid));
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // hide vanished players from player
        for (final UUID uuid : this.vanishedPlayers) {
            if (uuid.equals(player.getUniqueId())) {
                continue;
            }

            player.hidePlayer(Bukkit.getPlayer(uuid));
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        if (this.isVanished(player)) {
            // remove player from the list of all vanished players
            this.vanishLock.lock();
            this.vanishedPlayers.removeIf(uuid -> uuid.equals(player.getUniqueId()));
            this.vanishLock.unlock();

            // showing player to all players (idk, thank 1.7.10)
            Bukkit.getOnlinePlayers().forEach(p -> this.showPlayer(p, player));
        }
    }
}
