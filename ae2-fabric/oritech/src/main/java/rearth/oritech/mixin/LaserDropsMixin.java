package rearth.oritech.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import rearth.oritech.util.LaserMachinePlayer;

/**
 * Entities killed by a laser drop their loot into the laser's inventory and drop no experience.
 */
@Mixin(LivingEntity.class)
public abstract class LaserDropsMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void oritech$captureDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (source.getEntity() instanceof LaserMachinePlayer) {
            LaserMachinePlayer.CAPTURED_DROPS.set(new Capture((Entity) (Object) this, new ArrayList<>()));
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void oritech$collectDrops(ServerLevel level, DamageSource source, CallbackInfo ci) {
        var capture = LaserMachinePlayer.CAPTURED_DROPS.get();
        if (capture != null && capture.entity() == (Object) this) {
            LaserMachinePlayer.CAPTURED_DROPS.remove();
            if (source.getEntity() instanceof LaserMachinePlayer player) {
                player.collectDrops(capture.drops());
            }
        }
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void oritech$suppressExperience(ServerLevel level, Entity killer, CallbackInfo ci) {
        if (killer instanceof LaserMachinePlayer) {
            ci.cancel();
        }
    }

    public record Capture(Entity entity, List<ItemStack> drops) {
    }
}
