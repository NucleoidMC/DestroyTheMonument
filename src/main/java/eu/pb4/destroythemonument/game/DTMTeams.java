package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.map.DTMMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.apache.commons.lang3.RandomStringUtils;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.GameTeam;


public class DTMTeams implements AutoCloseable {
    private final DTMMap map;

    public Object2ObjectMap<GameTeam, Team> scoreboardTeams = new Object2ObjectOpenHashMap<>();
    public Object2ObjectMap<String, GameTeam> teams = new Object2ObjectOpenHashMap<>();

    final Scoreboard scoreboard;


    public DTMTeams(GameSpace gameSpace, DTMMap map, DTMConfig config) {
        this.scoreboard = gameSpace.getServer().getScoreboard();
        this.map = map;

        for (GameTeam team : config.teams) {
            this.scoreboardTeams.put(team, this.createTeam(team));
            this.teams.put(team.getKey(), team);

        }
    }

    public GameTeam getSmallestTeam() {
        GameTeam smallest = null;
        int count = 9999;

        for (GameTeam team : this.teams.values()) {
            if (this.scoreboardTeams.get(team).getPlayerList().size() <= count) {
                smallest = team;
                count = this.scoreboardTeams.get(team).getPlayerList().size();
            }
        }


        return smallest;

    }

    private Team createTeam(GameTeam team) {
        Team scoreboardTeam = this.scoreboard.addTeam(RandomStringUtils.randomAlphanumeric(16));
        scoreboardTeam.setDisplayName(new LiteralText(team.getDisplay()).formatted(team.getFormatting()));
        scoreboardTeam.setColor(team.getFormatting());
        scoreboardTeam.setFriendlyFireAllowed(false);
        scoreboardTeam.setCollisionRule(AbstractTeam.CollisionRule.NEVER);

        this.map.addTeamRegions(team);

        return scoreboardTeam;
    }

    public void addPlayer(ServerPlayerEntity player, GameTeam team) {
        Team scoreboardTeam = this.getScoreboardTeam(team);
        this.scoreboard.addPlayerToTeam(player.getEntityName(), scoreboardTeam);
    }

    public void removePlayer(ServerPlayerEntity player, GameTeam team) {
        Team scoreboardTeam = this.getScoreboardTeam(team);
        this.scoreboard.removePlayerFromTeam(player.getEntityName(), scoreboardTeam);
    }

    private Team getScoreboardTeam(GameTeam team) {
        return this.scoreboardTeams.get(team);
    }

    @Override
    public void close() {
        for (Team team : this.scoreboardTeams.values()) {
            this.scoreboard.removeTeam(team);
        }
    }
}
