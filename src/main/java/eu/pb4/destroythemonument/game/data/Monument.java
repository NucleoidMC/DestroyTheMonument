package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.game.map.GameMap;
import eu.pb4.destroythemonument.other.DtmUtil;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

public class Monument {
    public final String id;
    public final TeamData teamData;
    public final BlockPos pos;
    private final GameMap map;
    private boolean alive = true;

    public Monument(String id, TeamData team, BlockPos pos, GameMap map) {
        this.id = id;
        this.teamData = team;
        this.pos = pos;
        this.map = map;
    }


    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(boolean value) {
        if (value && this.map.world != null) {
            this.map.world.setBlockState(this.pos, map.config.monument());
            this.teamData.aliveMonuments.add(this);
            this.teamData.brokenMonuments.remove(this);
        } else {
            this.teamData.aliveMonuments.remove(this);
            this.teamData.brokenMonuments.add(this);
        }
        this.alive = value;
    }

    public Text getName() {
        return new TranslatableText(Util.createTranslationKey("monument", map.config.id()) + "." + this.teamData.team.key() + "." + id);
    }
}
