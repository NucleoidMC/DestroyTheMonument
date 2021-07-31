package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.map.Map;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.TeamManager;


public class Teams {
    private final Map map;
    public final TeamManager manager;
    public Object2ObjectMap<GameTeam, TeamData> teamData = new Object2ObjectOpenHashMap<>();
    public Object2ObjectMap<String, GameTeam> teams = new Object2ObjectOpenHashMap<>();

    public Teams(TeamManager manager, GameSpace gameSpace, Map map, GameConfig config) {
        this.manager = manager;
        this.map = map;

        for (GameTeam team : config.teams) {
            this.teams.put(team.key(), team);
            this.createTeam(team);
        }
    }

    public GameTeam getSmallestTeam() {
        GameTeam smallest = null;
        int count = 9999;

        for (GameTeam team : this.teams.values()) {
            int size = this.manager.playersIn(team).size();
            if (size <= count && this.teamData.get(team).getMonumentCount() > 0) {
                smallest = team;
                count = size;
            }
        }

        return smallest;
    }

    private void createTeam(GameTeam team) {
        this.manager.addTeam(team);
        this.manager.setCollisionRule(team, AbstractTeam.CollisionRule.PUSH_OWN_TEAM);
        TeamData teamData = new TeamData(team);
        this.map.setTeamRegions(team, teamData);
        this.teamData.put(team, teamData);
    }

    public void addPlayer(ServerPlayerEntity player, GameTeam team) {
        this.manager.addPlayerTo(player, team);
    }

    public void removePlayer(ServerPlayerEntity player) {
        this.manager.removePlayer(player);
    }
}
