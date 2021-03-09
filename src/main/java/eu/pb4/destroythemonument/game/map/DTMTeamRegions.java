package eu.pb4.destroythemonument.game.map;

import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.Set;

public class DTMTeamRegions {
    private final GameTeam team;
    private final BlockBounds spawn;
    public final Set<BlockBounds> monuments;
    public final int monumentStartingCount;
    public final BlockBounds classChange;


    public DTMTeamRegions(GameTeam team, BlockBounds spawn, Set<BlockBounds> monuments, BlockBounds classChange) {
        this.spawn = spawn;
        this.monuments = monuments;
        this.team = team;
        this.classChange = classChange;

        this.monumentStartingCount = monuments.size();
    }


    public boolean removeMonument(BlockPos pos) {
        for (BlockBounds monument : this.monuments) {
            if (monument.contains(pos)) {
                this.monuments.remove(monument);
                return true;
            }
        }

        return false;
    }

    public boolean isMonument(BlockPos pos) {
        for (BlockBounds monument : this.monuments) {
            if (monument.contains(pos)) {
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
