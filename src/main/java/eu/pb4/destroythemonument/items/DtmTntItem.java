package eu.pb4.destroythemonument.items;

import eu.pb4.destroythemonument.entities.blocks.DtmTntEntity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DtmTntItem extends Item implements PolymerItem {
    public DtmTntItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (!stack.isEmpty() && !user.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            stack.decrement(1);
            user.getItemCooldownManager().set(stack.getItem(), 20);
            DtmTntEntity.createThrown(user);
            return TypedActionResult.success(stack);
        }
        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getStack().isEmpty() && !context.getPlayer().getItemCooldownManager().isCoolingDown(context.getStack().getItem())) {
            context.getPlayer().getItemCooldownManager().set(context.getStack().getItem(), 20);
            context.getStack().decrement(1);
            DtmTntEntity.createPlaced(context.getPlayer(), context.getBlockPos().offset(context.getSide()));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.STICK;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, context, player);
        var out2 = new ItemStack(Items.TNT, out.getCount());
        out2.setNbt(out.getNbt());
        return out2;
    }
}
