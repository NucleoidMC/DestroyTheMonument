package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.DTM;
import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.data.PlayerData;
import eu.pb4.polymer.item.ItemHelper;
import eu.pb4.polymer.item.VirtualItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class MultiBlockItem extends BlockItem implements VirtualItem {
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
    public Item getVirtualItem() {
        return Items.BIRCH_PLANKS;
    }

    @Override
    public ItemStack getVirtualItemStack(ItemStack itemStack, ServerPlayerEntity player) {
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

            if (item instanceof VirtualItem virtualItem && item != this) {
                ItemStack stack = item.getDefaultStack();
                stack.setCount(itemStack.getCount());
                ItemStack out = virtualItem.getVirtualItemStack(stack, player);
                out.getOrCreateNbt().putString(ItemHelper.VIRTUAL_ITEM_ID, Registry.ITEM.getId(itemStack.getItem()).toString());
                return out;
            }
        }

        ItemStack out = new ItemStack(item, itemStack.getCount());

        if (itemStack.getNbt() != null) {
            out.getOrCreateNbt().put(ItemHelper.REAL_TAG, itemStack.getNbt());
        }

        out.getOrCreateNbt().putString(ItemHelper.VIRTUAL_ITEM_ID, Registry.ITEM.getId(itemStack.getItem()).toString());

        return out;
    }
}
