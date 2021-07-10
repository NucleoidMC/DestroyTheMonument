package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.map.Map;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.apache.commons.lang3.RandomStringUtils;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;


public class Teams implements AutoCloseable {
    private final Map map;

    public Object2ObjectMap<GameTeam, Team> scoreboardTeams = new Object2ObjectOpenHashMap<>();
    public Object2ObjectMap<GameTeam, TeamData> teamData = new Object2ObjectOpenHashMap<>();

    public Object2ObjectMap<String, GameTeam> teams = new Object2ObjectOpenHashMap<>();

    final Scoreboard scoreboard;


    public Teams(GameSpace gameSpace, Map map, GameConfig config) {
        this.scoreboard = gameSpace.getServer().getScoreboard();
        this.map = map;

        for (GameTeam team : config.teams) {
            this.scoreboardTeams.put(team, this.createTeam(team));
            this.teams.put(team.key(), team);

        }
    }

    public GameTeam getSmallestTeam() {
        GameTeam smallest = null;
        int count = 9999;

        for (GameTeam team : this.teams.values()) {
            if (this.scoreboardTeams.get(team).getPlayerList().size() <= count && this.teamData.get(team).getMonumentCount() > 0) {
                smallest = team;
                count = this.scoreboardTeams.get(team).getPlayerList().size();
            }
        }


        return smallest;

    }

    private Team createTeam(GameTeam team) {
        Team scoreboardTeam = this.scoreboard.addTeam(RandomStringUtils.randomAlphanumeric(16));
        scoreboardTeam.setDisplayName(new LiteralText(team.display()).formatted(team.formatting()));
        scoreboardTeam.setColor(team.formatting());
        scoreboardTeam.setFriendlyFireAllowed(false);
        scoreboardTeam.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
        TeamData teamData = new TeamData(team);
        this.map.setTeamRegions(team, teamData);
        this.teamData.put(team, teamData);

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
