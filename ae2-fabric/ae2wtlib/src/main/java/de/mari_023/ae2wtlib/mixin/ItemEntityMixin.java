package de.mari_023.ae2wtlib.mixin;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Inserts picked up items into the ME system (NeoForge: ItemEntityPickupEvent.Pre).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    private int pickupDelay;
    @Shadow
    private @Nullable UUID target;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void ae2wtlib$insertInME(Player player, CallbackInfo ci) {
        var self = (ItemEntity) (Object) this;
        if (self.level().isClientSide() || this.pickupDelay != 0) {
            return;
        }
        if (this.target != null && !this.target.equals(player.getUUID())) {
            return;
        }
        AE2wtlibEvents.insertStackInME(self, player);
    }
}
