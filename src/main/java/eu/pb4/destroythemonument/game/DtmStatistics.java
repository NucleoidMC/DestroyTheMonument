package eu.pb4.destroythemonument.game;

import eu.pb4.destroythemonument.other.DtmUtil;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;

public class DtmStatistics {
    public static final StatisticKey<Integer> MONUMENTS_DESTROYED = StatisticKey.intKey(DtmUtil.id("monuments_destroyed"));
}
