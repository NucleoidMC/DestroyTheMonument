package eu.pb4.destroythemonument.entities.blocks;

import eu.pb4.destroythemonument.other.DtmUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.EntityExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.util.PlayerRef;


public class DtmTntEntity extends Entity implements PolymerEntity {
    public int fuse = 80;
    private boolean hitBlock = false;
    private final boolean hitEntity = true;

    @Nullable
    private GameTeamKey team;
    @Nullable
    public LivingEntity causingEntity;

    public DtmTntEntity(EntityType<DtmTntEntity> entityType, World world) {
        super(entityType, world);
    }

    public static void createThrown(LivingEntity player) {
        var tnt = new DtmTntEntity(DtmEntities.TNT, player.world);
        tnt.causingEntity = player;
        tnt.hitBlock = true;
        tnt.fuse = 40;

        var game = DtmUtil.getGame(player);
        if (game != null && player instanceof ServerPlayerEntity serverPlayerEntity) {
            var pData = game.participants.get(PlayerRef.of(serverPlayerEntity));

            if (pData != null) {
                tnt.team = pData.teamData.team;
            }
        }

        double pitchRad = Math.toRadians(-player.getPitch());
        double yawRad = Math.toRadians(player.getYaw() - 180);

        double horizontal = Math.cos(pitchRad);
        tnt.setVelocity(new Vec3d(
                Math.sin(yawRad) * horizontal,
                Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal
        ).multiply(0.8));
        tnt.setPosition(player.getX() + Math.sin(yawRad) * horizontal * 0.3, player.getEyeY() + Math.sin(pitchRad) * 0.3, player.getZ() + -Math.cos(yawRad) * horizontal * 0.3);

        player.world.spawnEntity(tnt);
    }

    public static boolean createPlaced(LivingEntity player, BlockPos pos) {
        var tnt = new DtmTntEntity(DtmEntities.TNT, player.world);
        tnt.setPosition(Vec3d.ofBottomCenter(pos));
        tnt.causingEntity = player;
        tnt.fuse = 20;

        var game = DtmUtil.getGame(player);
        if (game != null && player instanceof ServerPlayerEntity serverPlayerEntity) {
            var pData = game.participants.get(PlayerRef.of(serverPlayerEntity));

            if (pData != null) {
                tnt.team = pData.teamData.team;
            }
        }

        player.world.spawnEntity(tnt);
        return true;
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        double pitchRad = Math.toRadians(-attacker.getPitch());
        double yawRad = Math.toRadians(attacker.getYaw() - 180);
        double horizontal = Math.cos(pitchRad);

        this.setVelocity(this.getVelocity().add(new Vec3d(
                Math.sin(yawRad) * horizontal,
                Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal
        ).multiply(attacker == this.causingEntity ? 0.12 : 0.15)));

        this.fuse += 5;

        return false;
    }

    protected MoveEffect getMoveEffect() {
        return MoveEffect.NONE;
    }

    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {

    }

    @Override
    protected void initDataTracker() {

    }

    public void tick() {
        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0.0D, -0.04D, 0.0D));
        }

        if (this.hitBlock) {
            var bb = this.getBoundingBox().stretch(this.getVelocity());

            for (var blockPos : BlockPos.iterateOutwards(this.getBlockPos(), 1, 1 ,1)) {
                BlockState blockState = this.world.getBlockState(blockPos);

                if (!blockState.isAir()) {
                    var voxelShape = blockState.getCollisionShape(this.world, blockPos);
                    if (!voxelShape.isEmpty()) {
                        for (var box : voxelShape.getBoundingBoxes()) {
                            if (box.offset(blockPos).intersects(bb)) {
                                this.onBlockHit();
                                return;
                            }
                        }
                    }
                }
            }
        }

        this.move(MovementType.SELF, this.getVelocity());
        this.setVelocity(this.getVelocity().multiply(0.98D));
        if (this.onGround) {
            this.setVelocity(this.getVelocity().multiply(0.7D, -0.5D, 0.7D));
        }

        int i = this.fuse - 1;
        this.fuse = i;

        if (i <= 0) {
            this.explode();
            return;
        } else {
            this.updateWaterState();
        }

        if (this.hitEntity) {
            for (var entity : this.world.getOtherEntities(this, this.getBoundingBox())) {
                this.onEntityHit(entity);
                return;
            }
        }
    }

    public void onEntityHit(Entity entity) {
        if (this.causingEntity == entity) {
            return;
        }

        if (this.team == null) {
            this.explode();
            return;
        } else if (entity instanceof ProjectileEntity projectile) {
            entity = projectile.getOwner();
        }

        if (entity instanceof PlayerEntity player) {
            var game = DtmUtil.getGame(player);

            if (game != null) {
                var data = game.participants.get(PlayerRef.of(player));

                if (data != null && data.teamData.team != this.team) {
                    this.explode();
                }
            }

        }
    }

    protected void onBlockHit() {
        if (!this.isRemoved()) {
            this.explode();
        }
    }

    private void explode() {
        this.discard();
        this.world.createExplosion(this, DamageSource.explosion(this, this.causingEntity), new EntityExplosionBehavior(this), this.getX(), this.getBodyY(0.0625D), this.getZ(), 4.0F, false, World.ExplosionSourceType.TNT);
    }

    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.TNT;
    }
}
