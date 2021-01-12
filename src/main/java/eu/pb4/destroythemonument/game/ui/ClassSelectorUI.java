package eu.pb4.destroythemonument.game.ui;

import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/*
 * Based on Shops from Plasmid
 */

public final class ClassSelectorUI implements NamedScreenHandlerFactory {
    private final Text title;
    private final Consumer<ClassSelectorBuilder> builder;

    ClassSelectorUI(Text title, Consumer<ClassSelectorBuilder> builder) {
        this.title = title;
        this.builder = builder;
    }

    public static ClassSelectorUI create(Text title, Consumer<ClassSelectorBuilder> builder) {
        return new ClassSelectorUI(title, builder);
    }

    public Text getDisplayName() {
        return this.title;
    }

    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        final ServerPlayerEntity serverPlayer = (ServerPlayerEntity)player;
        ClassSelectorInventory inventory = new ClassSelectorInventory(serverPlayer, this.builder);
        return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X1, syncId, playerInventory, inventory, 1) {
            public ItemStack transferSlot(PlayerEntity player, int invSlot) {
                this.resendInventory();
                return ItemStack.EMPTY;
            }

            public ItemStack onSlotClick(int slot, int data, SlotActionType action, PlayerEntity player) {
                if (action != SlotActionType.SWAP && action != SlotActionType.THROW && action != SlotActionType.CLONE) {
                    return super.onSlotClick(slot, data, action, player);
                } else {
                    this.resendInventory();
                    return ItemStack.EMPTY;
                }
            }

            private void resendInventory() {
                serverPlayer.onHandlerRegistered(this, this.getStacks());
            }
        };
    }
}

