package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.map.GameMap;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.TemplateRegion;

public class Monument {
    public final String id;
    @Nullable
    public TeamData teamData;
    public final BlockPos pos;
    private final GameMap map;
    private final Text name;
    private boolean alive = true;

    public Monument(String id, TeamData team, BlockPos pos, GameMap map, Text name) {
        this.id = id;
        this.teamData = team;
        this.pos = pos;
        this.map = map;
        this.name = name;
    }

    public static Monument createFrom(GameConfig config, GameMap map, TemplateRegion region, String defaultId, String idPrefix, @Nullable TeamData teamData) {
        var pos = region.getBounds().min();

        var name = defaultId;
        if (region.getData().contains("id", NbtElement.STRING_TYPE)) {
            name = idPrefix + region.getData().getString("id");
        }

        Text nameText = null;
        if (region.getData().contains("lang", NbtElement.STRING_TYPE)) {
            nameText = Text.translatable(region.getData().getString("lang"));
        } else if (config.monumentRemaps().isPresent()) {
            var key = config.monumentRemaps().get().get(name);

            if (key != null) {
                nameText = Text.translatable(key);
            }
        }

        if (nameText == null) {
            nameText = Text.translatable(Util.createTranslationKey("monument", config.map().id()) + "." + name);
        }
        return new Monument(name, teamData, pos, map, nameText);
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
        return this.name;
    }
}
