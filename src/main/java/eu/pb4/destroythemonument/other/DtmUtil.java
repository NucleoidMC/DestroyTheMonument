package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

public class DtmUtil {
    public static MutableComponent getText(String type, String path, Object... values) {
        return Component.translatable(Util.makeDescriptionId(type, Identifier.fromNamespaceAndPath(DTM.ID, path)), values);
    }

    public static MutableComponent getTeamText(TeamData team) {
        return getText("general", "team", team.getConfig().name()).setStyle(Style.EMPTY.withColor(team.getConfig().chatFormatting()));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DTM.ID, path);
    }

    @Nullable
    public static BaseGameLogic getGame(LivingEntity player) {
        var game = GameSpaceManager.get().byLevel(player.level());

        if (game != null) {
            return game.getAttachment(DTM.GAME_LOGIC);
        }

        return null;
    }
}
