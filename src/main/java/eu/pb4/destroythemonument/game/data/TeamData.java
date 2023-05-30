package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.Teams;
import eu.pb4.destroythemonument.game.map.GameMap;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;

import java.util.*;

public class TeamData {
    public final List<Monument> monuments;
    public final List<Monument> aliveMonuments;
    public final List<Monument> brokenMonuments;
    public final GameTeamKey team;
    private final Teams teams;
    public float spawnYaw;
    public int monumentStartingCount;
    public Set<BlockBounds> classChange;
    private List<BlockPos> spawn;

    public TeamData(GameTeamKey team, Teams teams) {
        this.monuments = new ArrayList<>();
        this.aliveMonuments = new ArrayList<>();
        this.brokenMonuments = new ArrayList<>();
        this.team = team;
        this.teams = teams;
    }

    public GameTeamConfig getConfig() {
        return this.teams.getConfig(this.team);
    }

    public BlockPos getRandomSpawnPos() {
        return this.spawn.get(DTM.RANDOM.nextInt(this.spawn.size()));
    }

    public void setTeamRegions(List<BlockPos> spawn, float spawnYaw, List<TemplateRegion> monuments, Set<BlockBounds> classChange, GameMap map, GameConfig config) {
        this.spawn = spawn;
        this.spawnYaw = spawnYaw;
        int id = 0;

        for (var region : monuments) {
            var monument = Monument.createFrom(config, map, region,  this.team.id() + "." + id, this.team.id() + ".", this);
            id++;
            this.monuments.add(monument);
            this.aliveMonuments.add(monument);
            map.addMonument(monument);
        }

        this.monuments.sort(Comparator.comparing(a -> a.getName().getString()));

        this.classChange = classChange;
        this.monumentStartingCount = monuments.size();
    }

    public boolean isAliveMonument(BlockPos pos) {
        for (var monument : this.aliveMonuments) {
            if (monument.pos.equals(pos)) {
                return true;
            }
        }

        return false;
    }

    public void removeMonument(Monument monument) {
        this.monuments.remove(monument);
        this.aliveMonuments.remove(monument);
        this.brokenMonuments.remove(monument);
    }

    public void addMonument(Monument monument) {
        this.monuments.add(monument);
        if (monument.isAlive()) {
            this.aliveMonuments.add(monument);
        } else {
            this.brokenMonuments.add(monument);
        }
    }
}
