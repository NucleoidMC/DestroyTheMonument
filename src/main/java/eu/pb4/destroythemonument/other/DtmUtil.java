package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.data.TeamData;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

public class DtmUtil {
    public static MutableText getText(String type, String path, Object... values) {
        return Text.translatable(Util.createTranslationKey(type, Identifier.of(DTM.ID, path)), values);
    }

    public static MutableText getTeamText(TeamData team) {
        return getText("general", "team", team.getConfig().name()).setStyle(Style.EMPTY.withColor(team.getConfig().chatFormatting()));
    }

    public static Identifier id(String path) {
        return Identifier.of(DTM.ID, path);
    }

    @Nullable
    public static BaseGameLogic getGame(LivingEntity player) {
        var game = GameSpaceManager.get().byWorld(player.getWorld());

        if (game != null) {
            return game.getAttachment(DTM.GAME_LOGIC);
        }

        return null;
    }
}
