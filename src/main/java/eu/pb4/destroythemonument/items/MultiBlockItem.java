package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registry;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class MultiBlockItem extends BlockItem implements PolymerItem {
    public MultiBlockItem(Settings settings) {
        super(Blocks.BIRCH_PLANKS, settings);
    }

    @Override
    public String getTranslationKey() {
        return this.getOrCreateTranslationKey();
    }

    @Override
    protected BlockState getPlacementState(ItemPlacementContext context) {
        if (context.getPlayer() != null) {
            GameSpace gameSpace = GameSpaceManager.get().byPlayer(context.getPlayer());
            if (gameSpace != null) {
                BaseGameLogic logic = DTM.ACTIVE_GAMES.get(gameSpace);

                if (logic != null) {
                    Block block = logic.participants.get(PlayerRef.of(context.getPlayer())).selectedBlock;
                    BlockState state = block.getPlacementState(context);
                    return state != null && this.canPlace(context, state) ? state : null;
                }
            }
        }

        BlockState blockState = this.getBlock().getPlacementState(context);
        return blockState != null && this.canPlace(context, blockState) ? blockState : null;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, ServerPlayerEntity player) {
        return Items.BIRCH_PLANKS;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, ServerPlayerEntity player) {
        Item item = Items.BIRCH_PLANKS;
        GameSpace gameSpace = GameSpaceManager.get().byPlayer(player);
        if (gameSpace != null) {
            BaseGameLogic logic = DTM.ACTIVE_GAMES.get(gameSpace);

            if (logic != null) {
                PlayerData data = logic.participants.get(PlayerRef.of(player));

                if (data != null) {
                    item = data.selectedBlock.asItem();
                }
            }

            if (item instanceof PolymerItem virtualItem && item != this) {
                ItemStack stack = item.getDefaultStack();
                stack.setCount(itemStack.getCount());
                ItemStack out = virtualItem.getPolymerItemStack(stack, context, player);
                out.getOrCreateNbt().putString(PolymerItemUtils.POLYMER_ITEM_ID, Registries.ITEM.getId(itemStack.getItem()).toString());
                return out;
            }
        }

        ItemStack out = new ItemStack(item, itemStack.getCount());

        if (itemStack.getNbt() != null) {
            out.getOrCreateNbt().put(PolymerItemUtils.REAL_TAG, itemStack.getNbt());
        }

        out.getOrCreateNbt().putString(PolymerItemUtils.POLYMER_ITEM_ID, Registries.ITEM.getId(itemStack.getItem()).toString());

        return out;
    }
}
