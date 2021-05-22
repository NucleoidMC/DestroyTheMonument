package eu.pb4.destroythemonument.other;

import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class FormattingUtil {
    public static final Style PREFIX_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x858585));
    public static final Style GENERAL_STYLE = Style.EMPTY.withColor(Formatting.WHITE);
    public static final Style WIN_STYLE = Style.EMPTY.withColor(Formatting.GOLD);
    public static final Style DEATH_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xbfbfbf));

    public static final String GENERAL_PREFIX = "»";
    public static final String DEATH_PREFIX = "☠";
    public static final String PICKAXE_PREFIX = "⛏";
    public static final String HEALTH_PREFIX = "✚";
    public static final String SUN_PREFIX = "☀";
    public static final String UMBRELLA_PREFIX = "☂";
    public static final String CLOUD_PREFIX = "☁";
    public static final String MUSIC_PREFIX = "♫";
    public static final String HEART_PREFIX = "♥";
    public static final String STAR_PREFIX = "★";

    public static final String CHECKMARK = "✔";
    public static final String HEALTH = "✚";


    public static MutableText format(String prefix, Style style, Text message) {
        return new LiteralText(prefix + " ").setStyle(PREFIX_STYLE).append(message.shallowCopy().fillStyle(style));
    }

    public static MutableText format(String prefix, Text message) {
        return new LiteralText(prefix + " ").setStyle(PREFIX_STYLE).append(message.shallowCopy());
    }
}
