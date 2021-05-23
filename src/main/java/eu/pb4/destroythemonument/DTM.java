package eu.pb4.destroythemonument;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.kit.KitsRegistry;
import eu.pb4.destroythemonument.items.DtmItems;
import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.WaitingLobby;

import java.util.WeakHashMap;

public class DTM implements ModInitializer {
    public static final String ID = "destroythemonument";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<GameConfig> TYPE = GameType.register(
            new Identifier(ID, "destroythemonument"),
            WaitingLobby::open,
            GameConfig.CODEC
    );

    public static final WeakHashMap<GameSpace, BaseGameLogic> ACTIVE_GAMES = new WeakHashMap<>();

    @Override
    public void onInitialize() {
        DtmItems.registerItems();
        KitsRegistry.register();
    }
}
