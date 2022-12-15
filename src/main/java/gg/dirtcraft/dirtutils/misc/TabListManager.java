package gg.dirtcraft.dirtutils.misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

public class TabListManager implements Listener {

    @EventHandler
    private void onJoin(final PlayerJoinEvent event) {
        event.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        this.setScoreboard(event.getPlayer());

        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (p != event.getPlayer()) {
                this.addPlayerToTeam(p.getScoreboard(), event.getPlayer());
            }
        }
    }

    @EventHandler
    private void onKick(final PlayerKickEvent event) {
        this.removePlayerFromScoreboards(event.getPlayer());
        this.removeScoreboard(event.getPlayer());
    }

    @EventHandler
    private void onQuit(final PlayerQuitEvent event) {
        this.removePlayerFromScoreboards(event.getPlayer());
        this.removeScoreboard(event.getPlayer());
    }

    private void setScoreboard(final Player player) {
        this.registerTeams(player);

        for (final Player p : Bukkit.getOnlinePlayers()) {
            this.addPlayerToTeam(player.getScoreboard(), p);
        }
    }

    private void removeScoreboard(final Player player) {
        player.getScoreboard().clearSlot(DisplaySlot.PLAYER_LIST);
        this.removePlayerFromScoreboards(player);
    }

    private void registerTeams(final Player player) {
        final Scoreboard board = player.getScoreboard();

        for (final ScoreboardTeam team : ScoreboardTeam.values()) {
            if (board.getTeam(team.team) == null) {
                board.registerNewTeam(team.getTeam());
            }

            board.getTeam(team.team).setPrefix(team.getPrefix());
        }
    }

    private void addPlayerToTeam(final Scoreboard board, final Player player) {
        board.getTeam(this.getTeam(player).getTeam()).addPlayer(player);
    }

    private void removePlayerFromScoreboards(final Player player) {
        for (final Player all : Bukkit.getOnlinePlayers()) {
            if (all != player) {
                all.getScoreboard().getTeam(this.getTeam(player).getTeam()).removePlayer(player);
            }
        }
    }

    private ScoreboardTeam getTeam(final Player player) {
        final ScoreboardTeam team;

        team = ScoreboardTeam.DEFAULT;

        return team;
    }

    private enum ScoreboardTeam {
        DEFAULT("00100Default", ChatColor.RED.toString());

        private final String team,
                prefix;

        ScoreboardTeam(final String team, final String prefix) {
            this.team = team;
            this.prefix = prefix;
        }

        private String getTeam() {
            return this.team;
        }

        private String getPrefix() {
            return this.prefix;
        }
    }
}
