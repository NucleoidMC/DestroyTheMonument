package eu.pb4.destroythemonument.mixin;

import eu.pb4.destroythemonument.other.DtmResetable;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin implements DtmResetable {

    @Shadow private boolean isDestroyingBlock;

    @Override
    public void dtm$reset() {
        this.isDestroyingBlock = false;
    }
}
