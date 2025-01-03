package eu.pb4.destroythemonument;

import eu.pb4.destroythemonument.blocks.DtmBlocks;
import eu.pb4.destroythemonument.entities.DtmEntities;
import eu.pb4.destroythemonument.game.logic.BaseGameLogic;
import eu.pb4.destroythemonument.game.playerclass.ClassRegistry;
import eu.pb4.destroythemonument.items.DtmItems;
import eu.pb4.destroythemonument.other.DtmUtil;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.WaitingLobby;

import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import static eu.pb4.destroythemonument.other.DtmUtil.id;

public class DTM implements ModInitializer {
    public static final String ID = "destroy_the_monument";
    public static final Logger LOGGER = LogManager.getLogger(ID);
    public static final Random RANDOM = new Random();
    public static final TagKey<Block> SPAWNABLE_TAG = TagKey.of(RegistryKeys.BLOCK, id("spawnable"));
    public static final TagKey<Block> BUILDING_BLOCKS = TagKey.of(RegistryKeys.BLOCK, id("building_blocks"));

    private static final Hash.Strategy<Object> IDENTITY_HASH = new Hash.Strategy<Object>() {
        @Override
        public int hashCode(Object o) {
            return System.identityHashCode(o);
        }

        @Override
        public boolean equals(Object o, Object k1) {
            return o == k1;
        }
    };
    public static final Set<Block> CONCRETE = new ObjectOpenCustomHashSet<>(IDENTITY_HASH);
    public static final Set<Block> STAINED_GLASS = new ObjectOpenCustomHashSet<>(IDENTITY_HASH);
    public static final Set<Block> STAINED_GLASS_PANES = new ObjectOpenCustomHashSet<>(IDENTITY_HASH);


    public static final GameType<GameConfig> TYPE = GameType.register(
            Identifier.of(ID, ID),
            GameConfig.CODEC,
            WaitingLobby::open
    );

    public static final GameAttachment<BaseGameLogic> GAME_LOGIC = GameAttachment.create(id("game_logic"));

    @Override
    public void onInitialize() {
        DtmItems.registerItems();
        DtmBlocks.register();
        DtmEntities.register();
        ClassRegistry.register();
        ServerLifecycleEvents.SERVER_STARTING.register((s) -> {
            Registries.BLOCK.forEach(x -> {
                if (Registries.BLOCK.getId(x).getPath().endsWith("_concrete")) {
                    CONCRETE.add(x);
                } else if (Registries.BLOCK.getId(x).getPath().endsWith("_stained_glass")) {
                    STAINED_GLASS.add(x);
                } else if (Registries.BLOCK.getId(x).getPath().endsWith("_stained_glass_panes")) {
                    STAINED_GLASS_PANES.add(x);
                }
            });
        });
    }
}
