package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.map.GameMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.game.common.team.TeamManager;

import java.util.Iterator;
import java.util.Objects;


public class Teams implements Iterable<TeamData> {
    private final GameMap map;
    private TeamManager manager;
    private Object2ObjectMap<GameTeamKey, GameTeamConfig> teamConfigs = new Object2ObjectOpenHashMap<>();
    private Object2ObjectMap<GameTeamKey, TeamData> teamData = new Object2ObjectOpenHashMap<>();

    public Teams(GameMap map, GameConfig config) {
        this.map = map;

        for (var entry : config.teams()) {
            this.createTeam(entry.key(), entry.config(), config);
        }
    }

    public TeamData getData(GameTeamKey team) {
        return this.teamData.get(team);
    }

    public GameTeamKey getSmallestTeam() {
        GameTeamKey smallest = null;
        int count = 9999;

        for (GameTeamKey team : this.teamData.keySet()) {
            int size = this.manager.playersIn(team).size();
            if (size <= count && this.teamData.get(team).aliveMonuments.size() > 0) {
                smallest = team;
                count = size;
            }
        }

        return smallest;
    }

    private void createTeam(GameTeamKey team, GameTeamConfig teamConfig, GameConfig config) {
        if (this.manager != null) {
            throw new RuntimeException("Can't add new teams after initialization!");
        }

        this.teamConfigs.put(team, GameTeamConfig.builder(teamConfig)
                .setFriendlyFire(false)
                .setCollision(AbstractTeam.CollisionRule.PUSH_OWN_TEAM).build());
        TeamData teamData = new TeamData(team, this);
        this.map.setTeamRegions(team, teamData, config);
        this.teamData.put(team, teamData);
    }

    public void addPlayer(ServerPlayerEntity player, GameTeamKey team) {
        this.manager.addPlayerTo(player, team);
    }

    public void removePlayer(ServerPlayerEntity player) {
        this.manager.removePlayer(player);
    }

    public void applyTo(GameActivity game) {
        this.manager = TeamManager.addTo(game);

        for (var entry : this.teamConfigs.entrySet()) {
            this.manager.addTeam(entry.getKey(), entry.getValue());
        }
    }

    public TeamManager getManager() {
        return Objects.requireNonNull(this.manager);
    }

    public GameTeamConfig getConfig(GameTeamKey team) {
        return this.teamConfigs.get(team);
    }

    @NotNull
    @Override
    public Iterator<TeamData> iterator() {
        return this.teamData.values().iterator();
    }
}
