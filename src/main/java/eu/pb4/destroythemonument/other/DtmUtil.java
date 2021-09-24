package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.data.TeamData;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

public class DtmUtil {
    public static MutableText getText(String type, String path, Object... values) {
        return new TranslatableText(Util.createTranslationKey(type, new Identifier(DTM.ID, path)), values);
    }

    public static MutableText getTeamText(TeamData team) {
        return getText("general", "team", team.getConfig().name()).setStyle(Style.EMPTY.withColor(team.getConfig().chatFormatting()));
    }

    public static Identifier id(String path) {
        return new Identifier(DTM.ID, path);
    }
}
