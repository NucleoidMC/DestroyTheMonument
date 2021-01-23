package eu.pb4.destroythemonument;

import eu.pb4.destroythemonument.kit.KitsRegistry;
import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.pb4.destroythemonument.game.DTMConfig;
import eu.pb4.destroythemonument.game.DTMWaiting;

public class DTM implements ModInitializer {

    public static final String ID = "destroythemonument";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<DTMConfig> TYPE = GameType.register(
            new Identifier(ID, "destroythemonument"),
            DTMWaiting::open,
            DTMConfig.CODEC
    );

    @Override
    public void onInitialize() {
        KitsRegistry.register();
    }
}
