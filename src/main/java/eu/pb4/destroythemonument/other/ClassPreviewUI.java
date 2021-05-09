package eu.pb4.destroythemonument.other;

import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.kit.Kit;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
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

        this.setSlot(pos++, this.kit.foodItem.copy());
        this.setSlot(pos++, new ItemStack(Items.OAK_PLANKS, this.kit.numberOfPlanks));
        this.setSlot(pos++, this.kit.additionalItem.copy());

        pos = 0;

        for (ItemStack itemStack : this.kit.armor) {
            this.setSlot(9 + pos, itemStack.copy());
            pos++;
        }

        this.setSlot(this.size - 1, new GuiElementBuilder(Items.BARRIER)
                .setName(DtmUtil.getText("ui", "return_selector").setStyle(Style.EMPTY.withItalic(false)))
                .setCallback((x, y, z) -> {
                    this.close();
                    ClassSelectorUI.openSelector(player, this.game);
                })
        );

        super.onUpdate(firstUpdate);
    }
}
