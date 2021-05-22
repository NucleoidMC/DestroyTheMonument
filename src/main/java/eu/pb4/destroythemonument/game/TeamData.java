package eu.pb4.destroythemonument.game;

import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TeamData {
    private final GameTeam team;
    public final Set<BlockPos> monuments;
    public BlockBounds spawn;
    public float spawnYaw;
    public int monumentStartingCount;
    public BlockBounds classChange;


    public TeamData(GameTeam team) {
        this.monuments = new HashSet<>();
        this.team = team;

    }

    public void setTeamRegions(BlockBounds spawn, float spawnYaw, Set<BlockBounds> monuments, BlockBounds classChange) {
        this.spawn = spawn;
        this.spawnYaw = spawnYaw;
        for (BlockBounds bounds : monuments) {
            this.monuments.add(bounds.getMin());
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
