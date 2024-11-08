package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.entities.DtmTntEntity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class DtmTntItem extends Item implements PolymerItem {
    public DtmTntItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (!stack.isEmpty() && !user.getItemCooldownManager().isCoolingDown(stack)) {
            stack.decrement(1);
            user.getItemCooldownManager().set(stack, 20);
            DtmTntEntity.createThrown(user);
            return ActionResult.SUCCESS_SERVER;
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getStack().isEmpty() && !context.getPlayer().getItemCooldownManager().isCoolingDown(context.getStack())) {
            context.getPlayer().getItemCooldownManager().set(context.getStack(), 20);
            context.getStack().decrement(1);
            DtmTntEntity.createPlaced(context.getPlayer(), context.getBlockPos().offset(context.getSide()));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TNT;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }
}
