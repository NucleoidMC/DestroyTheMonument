package eu.pb4.destroythemonument.map;

import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.HashSet;
import java.util.Set;

public class TeamRegions {
    private final GameTeam team;
    private final BlockBounds spawn;
    public final Set<BlockPos> monuments;
    public final int monumentStartingCount;
    public final BlockBounds classChange;


    public TeamRegions(GameTeam team, BlockBounds spawn, Set<BlockBounds> monuments, BlockBounds classChange) {
        this.spawn = spawn;
        this.monuments = new HashSet<>();
        for (BlockBounds bounds : monuments) {
            this.monuments.add(bounds.getMin());
        }
        this.team = team;
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

    public BlockBounds getSpawn() {
        return this.spawn;
    }
}
