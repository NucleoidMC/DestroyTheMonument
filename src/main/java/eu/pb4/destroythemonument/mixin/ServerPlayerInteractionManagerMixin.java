package eu.pb4.destroythemonument.mixin;

import eu.pb4.destroythemonument.other.DtmResetable;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin implements DtmResetable {

    @Shadow private boolean mining;

    @Override
    public void dtm$reset() {
        this.mining = false;
    }
}
