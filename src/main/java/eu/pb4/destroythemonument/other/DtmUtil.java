package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.DTM;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class DtmUtil {
    public static MutableText getText(String type, String path, Object... values) {
        return new TranslatableText(Util.createTranslationKey(type, new Identifier(DTM.ID, path)), values);
    }

    public static MutableText getTeamText(GameTeam team) {
        return getText("general", "team", team.getDisplay()).formatted(team.getFormatting());
    }

    public static Identifier id(String path) {
        return new Identifier(DTM.ID, path);
    }
}
