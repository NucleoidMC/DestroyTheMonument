package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.DTM;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TeamData {
    public final Set<BlockPos> monuments;
    private final GameTeam team;
    public float spawnYaw;
    public int monumentStartingCount;
    public Set<BlockBounds> classChange;
    private List<BlockPos> spawn;


    public TeamData(GameTeam team) {
        this.monuments = new HashSet<>();
        this.team = team;

    }

    public BlockPos getRandomSpawnPos() {
        return this.spawn.get(DTM.RANDOM.nextInt(this.spawn.size()));
    }

    public void setTeamRegions(List<BlockPos> spawn, float spawnYaw, Set<BlockBounds> monuments, Set<BlockBounds> classChange) {
        this.spawn = spawn;
        this.spawnYaw = spawnYaw;
        for (BlockBounds bounds : monuments) {
            this.monuments.add(bounds.min());
        }
        this.classChange = classChange;
        this.monumentStartingCount = monuments.size();
    }

    public boolean removeMonument(BlockPos pos) {
        for (BlockPos monument : this.monuments) {
            if (monument.equals(pos)) {
                this.monuments.remove(monument);
                return true;
            }
        }

        return false;
    }

    public boolean isMonument(BlockPos pos) {
        for (BlockPos monument : this.monuments) {
            if (monument.equals(pos)) {
                return true;
            }
        }

        return false;
    }

    public int getMonumentCount() {
        return this.monuments.size();
    }
}
