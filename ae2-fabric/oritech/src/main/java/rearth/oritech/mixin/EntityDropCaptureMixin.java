package rearth.oritech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import rearth.oritech.util.LaserMachinePlayer;

@Mixin(Entity.class)
public abstract class EntityDropCaptureMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void oritech$captureDrop(ServerLevel level, ItemStack stack, Vec3 offset,
            CallbackInfoReturnable<ItemEntity> cir) {
        var capture = LaserMachinePlayer.CAPTURED_DROPS.get();
        if (capture != null && capture.entity() == (Object) this) {
            if (!stack.isEmpty()) {
                capture.drops().add(stack.copy());
            }
            cir.setReturnValue(null);
        }
    }
}
