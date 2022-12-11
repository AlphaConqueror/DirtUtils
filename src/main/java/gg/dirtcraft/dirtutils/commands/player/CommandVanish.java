package gg.dirtcraft.dirtutils.commands.player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import gg.dirtcraft.dirtutils.commands.core.ArgContainer;
import gg.dirtcraft.dirtutils.commands.core.ChestData;
import gg.dirtcraft.dirtutils.commands.core.ParserCommandBase;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.ICommandResult;
import gg.dirtcraft.dirtutils.utils.PlayerUtils;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTracker;
import net.minecraft.server.v1_7_R4.EntityTrackerEntry;
import net.minecraft.server.v1_7_R4.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CommandVanish extends ParserCommandBase {

    // TODO: Add locks before/after iterating

    private static final String PERM_VANISH_SELF = "dirtutils.vanish";
    private static final String PERM_VANISH_OTHERS = "dirtutils.vanish.others";
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Lock vanishLock = new ReentrantLock();
    private final Set<UUID> iOptionList = new HashSet<>();
    private final Lock iOptionLock = new ReentrantLock();
    private final ArgContainer.PlayerArgContainer playerArgContainer = new ArgContainer.PlayerArgContainer("player", true);
    private final ArgContainer.OptionArgContainer optionArgContainer = new ArgContainer.OptionArgContainer("-i");

    private final List<ChestData> chestInteractions = new ArrayList<>();
    private final Lock chestInteractionLock = new ReentrantLock();

    public CommandVanish(final JavaPlugin javaPlugin) {
        super(javaPlugin, "vanish");

        this.addExpectedArg(this.playerArgContainer);
        this.addExpectedArg(this.optionArgContainer);

        this.registerEvents();

        this.setupAnimationListener(javaPlugin);
        this.setupSoundListener(javaPlugin);
    }

    public void setupAnimationListener(final JavaPlugin javaPlugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(javaPlugin, ListenerPriority.HIGH, PacketType.Play.Server.BLOCK_ACTION) {
                    @Override
                    public void onPacketSending(final PacketEvent e) {
                        if (e.getPacketType() == PacketType.Play.Server.BLOCK_ACTION) {
                            final Player listener = e.getPlayer();
                            final Location loc = new Location(listener.getWorld(),
                                    (double) e.getPacket().getIntegers().read(0),
                                    (double) e.getPacket().getIntegers().read(1),
                                    (double) e.getPacket().getIntegers().read(2));
                            final Block block = listener.getWorld().getBlockAt(loc);

                            if (block.getState() instanceof InventoryHolder) {
                                CommandVanish.this.chestInteractionLock.lock();
                                final ChestData chestData = CommandVanish.this.getMostRecentChestData(loc);

                                if (chestData != null) {
                                    e.setCancelled(true);
                                }
                                CommandVanish.this.chestInteractionLock.unlock();
                            }
                        }
                    }
                });
    }

    public void setupSoundListener(final JavaPlugin javaPlugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(javaPlugin, ListenerPriority.HIGH, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(final PacketEvent e) {
                        if (e.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                            if (!(e.getPacket().getStrings().read(0).equalsIgnoreCase("random.chestopen")
                                    || e.getPacket().getStrings().read(0).equalsIgnoreCase("random.chestclosed"))) {
                                return;
                            }

                            final Player listener = e.getPlayer();
                            // divide the location by 8, since it's a bit
                            // obfuscated
                            final Location loc = new Location(listener.getWorld(),
                                    e.getPacket().getIntegers().read(0) / 8.0,
                                    e.getPacket().getIntegers().read(1) / 8.0,
                                    e.getPacket().getIntegers().read(2) / 8.0);
                            final Block block = listener.getWorld().getBlockAt(loc);

                            if (block.getState() instanceof InventoryHolder) {
                                CommandVanish.this.chestInteractionLock.lock();
                                final ChestData chestData = CommandVanish.this.getMostRecentChestData(loc);

                                if (chestData != null) {
                                    e.setCancelled(true);
                                }
                                CommandVanish.this.chestInteractionLock.unlock();
                            }
                        }
                    }
                });
    }

    private ChestData getMostRecentChestData(final Location loc) {
        ChestData chestData = null;

        for (final ChestData cd : new ArrayList<>(CommandVanish.this.chestInteractions)) {
            // remove invalid data
            if (!cd.isValid()) {
                this.chestInteractions.remove(cd);
                continue;
            }

            if (cd.isVerified() && cd.getLocation().distance(loc) < 2 && (chestData == null || cd.getMillis() < chestData.getMillis())) {
                chestData = cd;
            }
        }

        return chestData;
    }

    @Override
    protected ICommandResult executePlayerCommand(final Player sender) {
        final boolean iOption = this.optionArgContainer.getResult();
        final Player otherPlayer = this.playerArgContainer.getResult();
        final String reply;

        System.out.println("Is iOption on? = " + iOption);

        if (otherPlayer == null) {
            this.checkPermission(sender, PERM_VANISH_SELF);
            this.vanishToggle(sender);

            if (this.isVanished(sender)) {
                reply = String.format("%sVanish %senabled%s!", ChatColor.DARK_AQUA, ChatColor.GREEN, ChatColor.DARK_AQUA);
            } else {
                reply = String.format("%sVanish %sdisabled%s!", ChatColor.DARK_AQUA, ChatColor.RED, ChatColor.DARK_AQUA);
            }
        } else {
            this.checkPermission(sender, PERM_VANISH_OTHERS);
            this.vanishToggle(otherPlayer);

            final String replyOther;

            if (this.isVanished(otherPlayer)) {
                reply = String.format("%sVanish %senabled%s for %s%s%s!", ChatColor.DARK_AQUA, ChatColor.GREEN, ChatColor.DARK_AQUA, ChatColor.AQUA, otherPlayer.getDisplayName(), ChatColor.DARK_AQUA);
                replyOther = String.format("%sVanish %senabled%s!", ChatColor.DARK_AQUA, ChatColor.GREEN, ChatColor.DARK_AQUA);
            } else {
                reply = String.format("%sVanish %sdisabled%s for %s%s%s!", ChatColor.DARK_AQUA, ChatColor.RED, ChatColor.DARK_AQUA, ChatColor.AQUA, otherPlayer.getDisplayName(), ChatColor.DARK_AQUA);
                replyOther = String.format("%sVanish %sdisabled%s!", ChatColor.DARK_AQUA, ChatColor.RED, ChatColor.DARK_AQUA);
            }

            PlayerUtils.sendMessageWithPrefix(otherPlayer, replyOther);
        }

        return new CommandReplyResult.Success(reply);
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
        final boolean isVanished = this.vanishedPlayers.contains(player.getUniqueId());
        this.vanishLock.unlock();

        return isVanished;
    }

    private boolean hasIOptionEnabled(final Player player) {
        this.iOptionLock.lock();
        final boolean hasIOptionEnabled = this.iOptionList.contains(player.getUniqueId());
        this.iOptionLock.unlock();

        return hasIOptionEnabled;
    }

    private void enableIOption(final Player player) {
        this.iOptionLock.lock();
        this.iOptionList.add(player.getUniqueId());
        this.iOptionLock.unlock();
    }

    private void disableIOption(final Player player) {
        this.iOptionLock.lock();
        this.iOptionList.remove(player.getUniqueId());
        this.iOptionLock.unlock();
    }

    /**
     * Enables or disabled vanishes status of a  player.
     *
     * @param player The player that should be vanished.
     * @param state  If vanish should be enabled or not.
     */
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

            // remove player from tab list for unvanished player
            //this.removeFromTabList(player, players);

            // remove player as target from all creatures in a 70 block radius
            final List<Entity> nearbyEntities = player.getNearbyEntities(70, 70, 70);
            for (final Entity entity : nearbyEntities) {
                if (entity instanceof Creature) {
                    final Creature creature = (Creature) entity;

                    creature.setTarget(null);
                }
            }

            if (this.optionArgContainer.getResult()) {
                this.enableIOption(player);
            } else {
                // remove collision
                player.spigot().setCollidesWithEntities(false);
            }
        } else {
            // remove player from the list of all vanished players
            this.vanishLock.lock();
            this.vanishedPlayers.remove(player.getUniqueId());
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

            if (this.hasIOptionEnabled(player)) {
                this.disableIOption(player);
            } else {
                // add collision back
                player.spigot().setCollidesWithEntities(true);
            }
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

        final PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);

        packetContainer.getModifier().writeDefaults();
        // string that should be displayed in the tab list
        packetContainer.getStrings().write(0, target.getName());
        // true = add; false = remove
        packetContainer.getBooleans().write(0, true);
        // ping
        packetContainer.getIntegers().write(0, ((CraftPlayer) target).getHandle().ping);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetContainer);
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e);
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

        if (this.hasIOptionEnabled(player)) {
            this.disableIOption(player);
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && this.isVanished((Player) event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(final PlayerDropItemEvent event) {
        if (this.hasIOptionEnabled(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock().getState() instanceof InventoryHolder
                && this.isVanished(event.getPlayer())) {
            this.chestInteractionLock.lock();
            this.chestInteractions.add(new ChestData(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation()));
            this.chestInteractionLock.unlock();
        }
    }

    @EventHandler
    public void onInventoryOpen(final InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !this.isVanished((Player) event.getPlayer())) {
            return;
        }

        this.chestInteractionLock.lock();

        final Optional<ChestData> chestData = this.chestInteractions.stream().filter(cd ->
                cd.getUuid().equals(event.getPlayer().getUniqueId()) && !cd.isVerified() && System.currentTimeMillis() - cd.getMillis() < 200).findFirst();

        assert chestData.isPresent();
        chestData.get().verify();

        this.chestInteractionLock.unlock();
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player) || event.getInventory().getHolder() instanceof Player || !this.isVanished((Player) event.getPlayer())) {
            return;
        }

        this.chestInteractionLock.lock();

        final Optional<ChestData> chestData = this.chestInteractions.stream().filter(cd ->
                cd.getUuid().equals(event.getPlayer().getUniqueId())).findFirst();

        assert chestData.isPresent();
        chestData.get().updateMillis();

        this.chestInteractionLock.unlock();
    }
}
