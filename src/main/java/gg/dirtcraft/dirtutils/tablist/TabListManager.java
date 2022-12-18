package gg.dirtcraft.dirtutils.tablist;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TabListManager implements Listener {

    // TODO: Set log level via config.

    private static final Logger LOGGER = LoggerFactory.getLogger(TabListManager.class);
    private static final int MAX_TEAM_NAME_LENGTH = 16;
    private static final int MAX_PREFIX_LENGTH = 15;
    private static final int MAX_WEIGHT_LENGTH = 5;
    private final Map<UUID, ScoreboardTeam> teams = new HashMap<>();
    private final Lock teamsLock = new ReentrantLock();

    @EventHandler
    private void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        this.teamsLock.lock();

        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        this.addPlayerTeam(player);
        this.registerTeams(player);
        this.addPlayerToScoreboards(player);

        this.teamsLock.unlock();
    }

    @EventHandler
    private void onKick(final PlayerKickEvent event) {
        this.teamsLock.lock();
        this.removePlayerFromScoreboards(event.getPlayer());
        this.teamsLock.unlock();
    }

    @EventHandler
    private void onQuit(final PlayerQuitEvent event) {
        this.teamsLock.lock();
        this.removePlayerFromScoreboards(event.getPlayer());
        this.teamsLock.unlock();
    }

    private void registerTeams(final Player player) {
        final Scoreboard board = player.getScoreboard();

        for (final UUID uuid : this.teams.keySet()) {
            final ScoreboardTeam scoreboardTeam = this.teams.get(uuid);
            final Player target = Bukkit.getPlayer(uuid);

            if (scoreboardTeam == null) {
                LOGGER.warn("Could not add player '{}' to a team.", player.getName());
                return;
            }

            Team team = board.getTeam(scoreboardTeam.getName());

            if (team == null) {
                team = board.registerNewTeam(scoreboardTeam.getName());
            }

            team.addPlayer(target);

            if (scoreboardTeam.getPrefix() != null) {
                team.setPrefix(scoreboardTeam.getPrefix());
            }
        }
    }

    private ScoreboardTeam generateTeam(final Player player) {
        final LuckPerms api = LuckPermsProvider.get();
        final User user = api.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            LOGGER.warn("Could not find user '{}' via LuckPerms API", player.getName());
            return null;
        }

        final Group primaryGroup = api.getGroupManager().getGroup(user.getPrimaryGroup());

        if (primaryGroup == null) {
            LOGGER.warn("Could not find primary group of user '{}' via LuckPerms API", player.getName());
            return null;
        }

        final int weight = primaryGroup.getWeight().orElse(0);
        final String teamName = this.generateTeamName(player, weight);

        if (teamName == null) {
            return null;
        }

        String prefix = primaryGroup.getCachedData().getMetaData().getPrefix();

        if (prefix != null) {
            prefix = prefix.substring(0, Math.min(prefix.length(), MAX_PREFIX_LENGTH))
                    .replaceAll("&", String.valueOf('\u00a7')) + ' ';
        }

        return new ScoreboardTeam(teamName, prefix);
    }

    /**
     * Generates a team name using the weight of the players primary permission group and the first part of the
     * players name.
     *
     * @param player The player.
     * @param weight The weight of the primary group.
     * @return The generated team name.
     */
    private String generateTeamName(final Player player, final int weight) {
        String name = this.addZeros(String.valueOf(weight), MAX_WEIGHT_LENGTH) + player.getName().substring(0,
                Math.min(player.getName().length(), MAX_TEAM_NAME_LENGTH - MAX_WEIGHT_LENGTH - 1));
        final String finalName = name;

        if (this.teams.values().stream().anyMatch(team -> team.getName().equals(finalName))) {
            for (int i = 0; i < 10; i++) {
                final String altName = name + i;

                if (this.teams.values().stream().noneMatch(team -> team.getName().equals(altName))) {
                    name = altName;
                    break;
                }
            }

            if (finalName.equals(name)) {
                LOGGER.warn("Could not assign player to a group.");
                name = null;
            }
        }

        return name;
    }

    private String addZeros(final String str, final int length) {
        final StringBuilder s = new StringBuilder();

        for (int i = 0; i < (length - str.length()); i++) {
            s.append('0');
        }

        s.append(str);

        return s.toString();
    }

    private void addPlayerTeam(final Player player) {
        final ScoreboardTeam scoreboardTeam = this.generateTeam(player);

        if (scoreboardTeam == null) {
            LOGGER.warn("Could not add player '{}' to a team.", player.getName());
            return;
        }

        this.teams.put(player.getUniqueId(), scoreboardTeam);
    }

    private void addPlayerToScoreboards(final Player player) {
        final ScoreboardTeam scoreboardTeam = this.teams.get(player.getUniqueId());

        if (scoreboardTeam == null) {
            LOGGER.warn("Could not add player '{}' to a team.", player.getName());
            return;
        }


        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                final Scoreboard scoreboard = p.getScoreboard();
                Team team = scoreboard.getTeam(scoreboardTeam.getName());

                if (team == null) {
                    team = scoreboard.registerNewTeam(scoreboardTeam.getName());
                }

                team.addPlayer(player);

                if (scoreboardTeam.getPrefix() != null) {
                    team.setPrefix(scoreboardTeam.getPrefix());
                }
            }
        }
    }

    private void removePlayerFromScoreboards(final Player player) {
        final ScoreboardTeam scoreboardTeam = this.teams.get(player.getUniqueId());

        if (scoreboardTeam == null) {
            return;
        }

        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                p.getScoreboard().getTeam(scoreboardTeam.getName()).unregister();
            }
        }

        this.teams.remove(player.getUniqueId());
    }
}
