package eu.pb4.destroythemonument.game.ui;

import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.function.Consumer;

public final class ClassSelectorEntry {
    private final ItemStackBuilder icon;
    private Consumer<ServerPlayerEntity> useAction;

    private ClassSelectorEntry(ItemStack icon) {
        this.icon = ItemStackBuilder.of(icon);
    }

    public static ClassSelectorEntry ofIcon(ItemStack icon) {
        return new ClassSelectorEntry(icon);
    }

    public static ClassSelectorEntry ofIcon(ItemConvertible icon) {
        return new ClassSelectorEntry(new ItemStack(icon));
    }

    public static ClassSelectorEntry buyItem(ItemStack stack) {
        ItemStack icon = stack.copy();

        MutableText count = new LiteralText(stack.getCount() + "x ");
        Text name = icon.getName().shallowCopy().formatted(Formatting.BOLD);
        icon.setCustomName(count.append(name));

        return new ClassSelectorEntry(stack).onUse(player -> {
            player.inventory.offerOrDrop(player.world, stack);
        });
    }


    public ClassSelectorEntry withName(Text name) {
        this.icon.setName(name);
        return this;
    }

    public ClassSelectorEntry addLore(Text lore) {
        this.icon.addLore(lore);
        return this;
    }

    public ClassSelectorEntry onUse(Consumer<ServerPlayerEntity> action) {
        this.useAction = action;
        return this;
    }

    ItemStack createIcon(ServerPlayerEntity player) {
        ItemStack icon = this.icon.build().copy();
        icon.getTag().putByte("HideFlags", (byte) 127);

        Style style = Style.EMPTY.withItalic(false).withColor(Formatting.GOLD);

        Text name = icon.getName().shallowCopy().setStyle(style);
        icon.setCustomName(name);

        return icon;
    }

    void onClick(ServerPlayerEntity player) {
        SoundEvent sound;
        if (this.useAction != null) {
            this.useAction.accept(player);
        }
        sound = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;

        player.playSound(sound, SoundCategory.MASTER, 1.0F, 1.0F);
    }
}