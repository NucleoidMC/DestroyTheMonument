package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.sgui.api.elements.AnimatedGuiElement;
import eu.pb4.sgui.api.elements.AnimatedGuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;

public class ClassPreviewUI extends SimpleGui {
    private BaseGameLogic game;
    private Kit kit;

    public ClassPreviewUI(ServerPlayerEntity player, BaseGameLogic game, Kit kit) {
        super(ScreenHandlerType.GENERIC_9X3, player, false);
        this.game = game;
        this.kit = kit;
        this.setTitle(DtmUtil.getText("ui", "class_preview", DtmUtil.getText("class", kit.name)));
    }

    @Override
    public void onUpdate(boolean firstUpdate) {
        int pos = 0;

        for (ItemStack itemStack : this.kit.items) {
            this.setSlot(pos++, itemStack.copy());
        }

        for (Kit.RestockableItem item : this.kit.restockableItems) {
            ItemStack stackA = ItemStack.EMPTY;

            if (item.startingCount > 0) {
                stackA = item.itemStack.copy();
                stackA.setCount(item.startingCount);
            }

            ItemStack stackB = item.itemStack.copy();
            stackB.setCount(item.maxCount);
            this.setSlot(pos++, new AnimatedGuiElement(new ItemStack[]{ stackA, stackB }, 20,false, (x,y,z) -> {}));
        }

        pos = 0;

        for (ItemStack itemStack : this.kit.armor) {
            this.setSlot(9 + pos, itemStack.copy());
            pos++;
        }

        this.setSlot(this.size - 1, new GuiElementBuilder(Items.BARRIER)
                .setName(DtmUtil.getText("ui", "return_selector").setStyle(Style.EMPTY.withItalic(false)))
                .setCallback((x, y, z) -> {
                    this.close();
                })
        );

        super.onUpdate(firstUpdate);
    }

    @Override
    public void onClose() {
        this.player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 0.5f, 1);
        ClassSelectorUI.openSelector(player, this.game);
        super.onClose();
    }
}
