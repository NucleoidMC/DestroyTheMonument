package eu.pb4.destroythemonument.game.data;

import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.map.GameMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.TemplateRegion;

public class Monument {
    public final String id;
    @Nullable
    public TeamData teamData;
    public final BlockPos pos;
    private final GameMap map;
    private final Component name;
    private boolean alive = true;

    public Monument(String id, TeamData team, BlockPos pos, GameMap map, Component name) {
        this.id = id;
        this.teamData = team;
        this.pos = pos;
        this.map = map;
        this.name = name;
    }

    public static Monument createFrom(GameConfig config, GameMap map, TemplateRegion region, String defaultId, String idPrefix, @Nullable TeamData teamData) {
        var pos = region.getBounds().min();

        var name = defaultId;
        if (region.getData().contains("id")) {
            name = idPrefix + region.getData().getStringOr("id", "");
        }

        Component nameText = null;
        if (region.getData().contains("lang")) {
            nameText = Component.translatable(region.getData().getStringOr("lang", ""));
        } else if (config.monumentRemaps().isPresent()) {
            var key = config.monumentRemaps().get().get(name);

            if (key != null) {
                nameText = Component.translatable(key);
            }
        }

        if (nameText == null) {
            nameText = Component.translatable(Util.makeDescriptionId("monument", config.map().id()) + "." + name);
        }
        return new Monument(name, teamData, pos, map, nameText);
    }


    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(boolean value) {
        if (value && this.map.world != null) {
            this.map.world.setBlockAndUpdate(this.pos, map.config.monument());
            this.teamData.aliveMonuments.add(this);
            this.teamData.brokenMonuments.remove(this);
        } else {
            this.teamData.aliveMonuments.remove(this);
            this.teamData.brokenMonuments.add(this);
        }
        this.alive = value;
    }

    public Component getName() {
        return this.name;
    }
}
