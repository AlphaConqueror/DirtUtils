package gg.dirtcraft.dirtutils.commands.player.vanish;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import gg.dirtcraft.dirtutils.Cli;
import gg.dirtcraft.dirtutils.commands.core.parser.ArgContainer;
import gg.dirtcraft.dirtutils.commands.core.parser.ParserCommandBase;
import gg.dirtcraft.dirtutils.commands.core.result.CommandReplyResult;
import gg.dirtcraft.dirtutils.commands.core.result.ICommandResult;
import gg.dirtcraft.dirtutils.utils.ChestData;
import gg.dirtcraft.dirtutils.utils.PlayerUtils;
import gg.dirtcraft.dirtutils.utils.VanishData;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

    // TODO: test #onDisable
    // TODO: Add console command handling

    // IDEA: cancel sound packets coming from vanished players (difficult)

    private static final String PERM_VANISH_SELF = "dirtutils.vanish";
    private static final String PERM_VANISH_OTHERS = "dirtutils.vanish.others";

    private final Set<VanishData> vanishedPlayers = new HashSet<>();
    private final Lock vanishLock = new ReentrantLock();

    private final ArgContainer.OptionArgContainer iOptionArgContainer = new ArgContainer.OptionArgContainer("-i");
    private final ArgContainer.OptionArgContainer bOptionArgContainer = new ArgContainer.OptionArgContainer("-b");
    private final ArgContainer.PlayerArgContainer playerArgContainer
            = new ArgContainer.PlayerArgContainer("player", true);
    private final ArgContainer.SubCommandArgContainer subCommandArgContainer
            = new ArgContainer.SubCommandArgContainer("info");

    private final List<ChestData> chestInteractions = new ArrayList<>();
    private final Lock chestInteractionLock = new ReentrantLock();

    public CommandVanish(final JavaPlugin javaPlugin) {
        super(javaPlugin, "vanish");

        this.addExpectedArg(this.iOptionArgContainer);
        this.addExpectedArg(this.bOptionArgContainer);
        this.addExpectedArg(this.playerArgContainer);
        this.addExpectedArg(this.subCommandArgContainer);

        this.registerEvents();

        this.setupAnimationListener(javaPlugin);
        this.setupSoundListener(javaPlugin);
    }

    public void setupAnimationListener(final JavaPlugin javaPlugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(javaPlugin, ListenerPriority.HIGH, PacketType.Play.Server.BLOCK_ACTION) {
                    @Override
                    public void onPacketSending(final PacketEvent e) {
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
                });
    }

    public void setupSoundListener(final JavaPlugin javaPlugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(javaPlugin, ListenerPriority.HIGH, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(final PacketEvent e) {
                        final String soundName = e.getPacket().getStrings().read(0);

                        if (!(soundName.equalsIgnoreCase("random.chestopen")
                                || soundName.equalsIgnoreCase("random.chestclosed"))) {
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
                });
    }

    // is guarded by locks
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
    public void onDisable() {
        this.vanishLock.lock();
        // make everyone visible again
        this.vanishedPlayers.forEach(vanishData -> this.vanishToggle(Bukkit.getPlayer(vanishData.getUniqueId()),
                false, false));
        this.vanishLock.unlock();
    }

    @Override
    protected ICommandResult executePlayerCommand(final Player sender, final Map<Long, Object> args) {
        final Player otherPlayer = (Player) args.get(this.playerArgContainer.getId());

        final boolean iOption = (boolean) args.get(this.iOptionArgContainer.getId());
        final boolean bOption = (boolean) args.get(this.bOptionArgContainer.getId());
        final boolean infoSubCommand = (boolean) args.get(this.subCommandArgContainer.getId());
        final String reply;

        this.vanishLock.lock();

        try {
            if (otherPlayer == null || otherPlayer.getUniqueId().equals(sender.getUniqueId())) {
                this.checkPermission(sender, PERM_VANISH_SELF);

                if (infoSubCommand) {
                    return new CommandReplyResult.Success(this.generateVanishInfoSubCommand(sender));
                }

                Optional<VanishData> vanishData = this.getVanishedPlayer(sender);

                if (vanishData.isPresent() && (iOption || bOption)) {
                    if (iOption) {
                        this.iOptionToggle(sender, vanishData.get());
                    }

                    if (bOption) {
                        this.bOptionToggle(vanishData.get());
                    }

                    return new CommandReplyResult.Success(String.format("%sVanish has been %smodified%s!",
                            ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.DARK_AQUA)
                            + this.generateVanishStatusInfo(vanishData.orElse(null)));
                }

                this.vanishToggle(sender, iOption, bOption);

                // update vanish data after toggle
                vanishData = this.getVanishedPlayer(sender);

                reply = String.format("%sVanish has been %s%s!", ChatColor.DARK_AQUA,
                        vanishData.isPresent() ?
                                ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled", ChatColor.DARK_AQUA)
                        + this.generateVanishOptionInfo(vanishData.orElse(null));
            } else {
                this.checkPermission(sender, PERM_VANISH_OTHERS);

                if (infoSubCommand) {
                    return new CommandReplyResult.Success(this.generateVanishInfoSubCommand(otherPlayer));
                }

                Optional<VanishData> vanishData = this.getVanishedPlayer(otherPlayer);

                if (vanishData.isPresent() && (iOption || bOption)) {
                    if (iOption) {
                        this.iOptionToggle(otherPlayer, vanishData.get());
                    }

                    if (bOption) {
                        this.bOptionToggle(vanishData.get());
                    }

                    PlayerUtils.sendMessageWithPrefix(otherPlayer,
                            String.format("%sVanish has been %smodified%s by %s%s%s!",
                                    ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.DARK_AQUA, ChatColor.AQUA,
                                    sender.getDisplayName(), ChatColor.DARK_AQUA)
                                    + this.generateVanishStatusInfo(vanishData.orElse(null)));

                    return new CommandReplyResult.Success(String.format("%sVanish of %s%s%s has been %smodified%s!",
                            ChatColor.DARK_AQUA, ChatColor.AQUA, otherPlayer.getDisplayName(), ChatColor.DARK_AQUA,
                            ChatColor.AQUA, ChatColor.DARK_AQUA)
                            + this.generateVanishStatusInfo(vanishData.orElse(null)));
                }

                this.vanishToggle(otherPlayer, iOption, bOption);

                // update vanish data after toggle
                vanishData = this.getVanishedPlayer(otherPlayer);

                reply = String.format("%sVanish of %s%s%s has been %s%s!", ChatColor.DARK_AQUA, ChatColor.AQUA,
                        otherPlayer.getDisplayName(), ChatColor.DARK_AQUA, vanishData.isPresent() ?
                                ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled", ChatColor.DARK_AQUA)
                        + this.generateVanishOptionInfo(vanishData.orElse(null));

                PlayerUtils.sendMessageWithPrefix(otherPlayer, String.format("%sVanish has been %s%s by %s%s!",
                        ChatColor.DARK_AQUA, vanishData.isPresent() ? ChatColor.GREEN + "enabled" : ChatColor.RED +
                                "disabled", ChatColor.DARK_AQUA, sender.getDisplayName(), ChatColor.DARK_AQUA)
                        + this.generateVanishOptionInfo(vanishData.orElse(null)));
            }

            return new CommandReplyResult.Success(reply);
        } finally {
            this.vanishLock.unlock();
        }
    }

    private void vanishToggle(final Player player, final boolean iOption, final boolean bOption) {
        this.vanish(player, !this.getVanishedPlayer(player).isPresent(), iOption, bOption);
    }

    private void iOptionToggle(final Player player, final VanishData vanishData) {
        vanishData.setIOption(!vanishData.hasIOption());

        if (vanishData.hasIOption()) {
            this.enableIOption(player);
        } else {
            this.disableIOption(player);
        }
    }

    private void bOptionToggle(final VanishData vanishData) {
        vanishData.setBOption(!vanishData.hasBOption());
    }

    /**
     * Checks whether the player is vanished or not.
     *
     * @param player The player in question.
     * @return True, if the player is vanished, false, if otherwise.
     */
    private Optional<VanishData> getVanishedPlayer(final Player player) {
        return this.vanishedPlayers.stream().filter(vanishData -> vanishData.getUniqueId().equals(player.getUniqueId())).findFirst();
    }

    /**
     * Enables or disabled vanishes status of a  player.
     *
     * @param player The player that should be vanished.
     * @param state  If vanish should be enabled or not.
     */
    private void vanish(final Player player, final boolean state, final boolean iOption, final boolean bOption) {
        final Optional<VanishData> vanishedPlayer = this.getVanishedPlayer(player);

        assert state != vanishedPlayer.isPresent() : "Tried to un-/vanish player twice!";

        final Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());

        // remove vanished players from the list of all players that should not be able to see the player
        this.vanishedPlayers.forEach(vanishData -> players.removeIf(p -> p.getUniqueId().equals(vanishData.getUniqueId())));

        if (state) {
            // hide player from all players that should not be able to see the player
            for (final Player p : players) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                p.hidePlayer(player);
            }

            // show vanished players to player
            for (final VanishData vanishData : this.vanishedPlayers) {
                final UUID uuid = vanishData.getUniqueId();

                this.showPlayer(player, Bukkit.getPlayer(uuid));
            }

            // remove player as target from all creatures in a 70 block radius
            final List<Entity> nearbyEntities = player.getNearbyEntities(70, 70, 70);

            for (final Entity entity : nearbyEntities) {
                if (entity instanceof Creature) {
                    final Creature creature = (Creature) entity;

                    creature.setTarget(null);
                }
            }

            // add player to the list of all vanished players
            this.vanishedPlayers.add(new VanishData(player.getUniqueId(), iOption, bOption));

            if (!iOption) {
                // remove collision
                player.spigot().setCollidesWithEntities(false);
            }
        } else {
            final VanishData vanishData =
                    this.vanishedPlayers.stream().filter(vd -> vd.getUniqueId().equals(player.getUniqueId())).findFirst().orElseThrow(AssertionError::new);

            if (!iOption) {
                // add collision back
                player.spigot().setCollidesWithEntities(true);
            }

            // remove player from the list of all vanished players
            this.vanishedPlayers.remove(vanishData);

            // show player to all online players
            for (final Player p : players) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                this.showPlayer(p, player);
            }

            // hide vanished players from player
            for (final VanishData vd : this.vanishedPlayers) {
                final UUID uuid = vd.getUniqueId();

                player.hidePlayer(Bukkit.getPlayer(uuid));
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
        } catch (final InvocationTargetException ignored) {
            // problem during reflection operations
        }
    }

    private String generateVanishInfoSubCommand(final Player player) {
        String message = "";
        final Optional<VanishData> vanishData = this.getVanishedPlayer(player);

        message += String.format("%sVanish information about %s%s%s!", ChatColor.DARK_AQUA,
                ChatColor.AQUA, player.getDisplayName(), ChatColor.DARK_AQUA)
                + this.generateVanishStatusInfo(vanishData.orElse(null));

        return message;
    }

    // only shown when vanish is enabled
    private String generateVanishStatusInfo(final VanishData vanishData) {
        return String.format("\n%s%s%s>%s Status: %s", Cli.PREFIX, ChatColor.AQUA, ChatColor.BOLD,
                ChatColor.DARK_AQUA, vanishData == null ? ChatColor.RED + "disabled" : ChatColor.GREEN + "enabled")
                + this.generateVanishOptionInfo(vanishData);
    }

    private String generateVanishOptionInfo(final VanishData vanishData) {
        String message = "";

        if (vanishData != null) {
            // i option
            message += String.format("\n%s%s%s>%s [%s-i%s]:%s inventory actions", Cli.PREFIX,
                    ChatColor.AQUA, ChatColor.BOLD, ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.DARK_AQUA,
                    vanishData.hasIOption() ? ChatColor.GREEN : ChatColor.RED);
            // b option
            message += String.format("\n%s%s%s>%s [%s-b%s]:%s block actions", Cli.PREFIX,
                    ChatColor.AQUA, ChatColor.BOLD, ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.DARK_AQUA,
                    vanishData.hasBOption() ? ChatColor.GREEN : ChatColor.RED);
        }

        return message;
    }

    private void enableIOption(final Player player) {
        final Optional<VanishData> vanishData = this.getVanishedPlayer(player);

        assert vanishData.isPresent();
        vanishData.get().setIOption(true);

        player.spigot().setCollidesWithEntities(true);
    }

    private void disableIOption(final Player player) {
        final Optional<VanishData> vanishData = this.getVanishedPlayer(player);

        vanishData.ifPresent(data -> data.setIOption(false));

        player.spigot().setCollidesWithEntities(false);
    }

    @EventHandler

    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // hide vanished players from player
        this.vanishLock.lock();
        for (final VanishData vanishData : this.vanishedPlayers) {
            if (vanishData.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            player.hidePlayer(Bukkit.getPlayer(vanishData.getUniqueId()));
        }
        this.vanishLock.unlock();
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        if (this.getVanishedPlayer(player).isPresent()) {
            // remove player from the list of all vanished players
            this.vanishLock.lock();
            this.vanishedPlayers.removeIf(vanishData -> vanishData.getUniqueId().equals(player.getUniqueId()));
            this.vanishLock.unlock();

            // showing player to all players (idk, thank 1.7.10)
            Bukkit.getOnlinePlayers().forEach(p -> this.showPlayer(p, player));
        }
    }

    @EventHandler
    public void onEntityTarget(final EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && this.getVanishedPlayer((Player) event.getTarget()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Optional<VanishData> vanishData = this.getVanishedPlayer(event.getPlayer());

        if (vanishData.isPresent() && !vanishData.get().hasIOption()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        // ignore if player is not vanished
        if (!this.getVanishedPlayer(event.getPlayer()).isPresent()) {
            return;
        }

        if (event.getAction() == Action.PHYSICAL) {
            // disable pressure plates
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock().getState() instanceof InventoryHolder) {
            this.chestInteractionLock.lock();
            this.chestInteractions.add(new ChestData(event.getPlayer().getUniqueId(),
                    event.getClickedBlock().getLocation()));
            this.chestInteractionLock.unlock();
        }
    }

    @EventHandler
    public void onInventoryOpen(final InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !this.getVanishedPlayer((Player) event.getPlayer()).isPresent()) {
            return;
        }

        this.chestInteractionLock.lock();

        final Optional<ChestData> chestData = this.chestInteractions.stream().filter(cd ->
                cd.getUuid().equals(event.getPlayer().getUniqueId()) && !cd.isVerified() && System.currentTimeMillis() - cd.getMillis() < 200).findFirst();

        /**
         * {@link PlayerInteractEvent} is fired before {@link InventoryOpenEvent]
         */
        assert chestData.isPresent();
        chestData.get().verify();

        this.chestInteractionLock.unlock();
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player) || event.getInventory().getHolder() instanceof Player || !this.getVanishedPlayer((Player) event.getPlayer()).isPresent()) {
            return;
        }

        this.chestInteractionLock.lock();

        final Optional<ChestData> chestData = this.chestInteractions.stream().filter(cd ->
                cd.getUuid().equals(event.getPlayer().getUniqueId())).findFirst();

        // check since the player could've been vanished by other means while having an inventory open
        chestData.ifPresent(ChestData::updateMillis);

        this.chestInteractionLock.unlock();
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Optional<VanishData> vanishData = this.getVanishedPlayer(event.getPlayer());

        if (vanishData.isPresent() && !vanishData.get().hasBOption()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Optional<VanishData> vanishData = this.getVanishedPlayer(event.getPlayer());

        if (vanishData.isPresent() && !vanishData.get().hasBOption()) {
            event.setCancelled(true);
        }
    }
}
