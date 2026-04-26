package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.entities.DtmTntEntity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DtmTntItem extends Item implements PolymerItem {
    public DtmTntItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);
        if (!stack.isEmpty() && !user.getCooldowns().isOnCooldown(stack)) {
            stack.shrink(1);
            user.getCooldowns().addCooldown(stack, 20);
            DtmTntEntity.createThrown(user);
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getItemInHand().isEmpty() && !context.getPlayer().getCooldowns().isOnCooldown(context.getItemInHand())) {
            context.getPlayer().getCooldowns().addCooldown(context.getItemInHand(), 20);
            context.getItemInHand().shrink(1);
            DtmTntEntity.createPlaced(context.getPlayer(), context.getClickedPos().relative(context.getClickedFace()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TNT;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider provider) {
        return null;
    }
}
