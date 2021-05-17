package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.DTM;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class DtmUtil {
    public static final Style PREFIX_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x858585)).withItalic(false);

    public static MutableText getText(String type, String path, Object... values) {
        return new TranslatableText(Util.createTranslationKey(type, new Identifier(DTM.ID, path)), values);
    }

    public static MutableText getTeamText(GameTeam team) {
        return getText("general", "team", team.getDisplay()).formatted(team.getFormatting());
    }

    public static MutableText getFormatted(String prefix, Text message) {
        return new LiteralText(prefix + " ").setStyle(PREFIX_STYLE).append(message);
    }


    public static Identifier id(String path) {
        return new Identifier(DTM.ID, path);
    }
}
