package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.map.GameMap;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import java.util.*;

public class TeamData {
    public final List<Monument> monuments;
    public final List<Monument> aliveMonuments;
    public final List<Monument> brokenMonuments;
    public final GameTeam team;
    public float spawnYaw;
    public int monumentStartingCount;
    public Set<BlockBounds> classChange;
    private List<BlockPos> spawn;

    public TeamData(GameTeam team) {
        this.monuments = new ArrayList<>();
        this.aliveMonuments = new ArrayList<>();
        this.brokenMonuments = new ArrayList<>();
        this.team = team;
    }

    public BlockPos getRandomSpawnPos() {
        return this.spawn.get(DTM.RANDOM.nextInt(this.spawn.size()));
    }

    public void setTeamRegions(List<BlockPos> spawn, float spawnYaw, List<TemplateRegion> monuments, Set<BlockBounds> classChange, GameMap map) {
        this.spawn = spawn;
        this.spawnYaw = spawnYaw;
        int id = 0;

        for (var region : monuments) {
            var pos = region.getBounds().min();

            var name = "" + id;
            if (region.getData().contains("id", NbtElement.STRING_TYPE)) {
                name = region.getData().getString("id");
            }

            var monument = new Monument(name, this, pos, map);
            id++;
            this.monuments.add(monument);
            this.aliveMonuments.add(monument);
            map.monuments.add(monument);
        }

        this.monuments.sort(Comparator.comparing(a -> a.getName().getString()));

        this.classChange = classChange;
        this.monumentStartingCount = monuments.size();
    }

    public boolean breakMonument(BlockPos pos) {
        for (var monument : this.aliveMonuments) {
            if (monument.pos.equals(pos)) {
                monument.setAlive(false);
                return true;
            }
        }

        return false;
    }

    public boolean isAliveMonument(BlockPos pos) {
        for (var monument : this.aliveMonuments) {
            if (monument.pos.equals(pos)) {
                return true;
            }
        }

        return false;
    }
}
