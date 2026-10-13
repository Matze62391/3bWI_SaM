package net.pedroksl.advanced_ae.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.pedroksl.advanced_ae.events.AAELivingEntityEvents;

/**
 * Hooks that replace NeoForge's living entity events for players.
 */
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    /**
     * Replaces LivingIncomingDamageEvent, which is fired after the invulnerability checks.
     */
    @Inject(
            method = "hurtServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"),
            cancellable = true)
    private void aae$incomingDamage(
            ServerLevel level,
            DamageSource source,
            float originalDamage,
            CallbackInfoReturnable<Boolean> cir,
            @Local(argsOnly = true) LocalFloatRef damage) {
        if ((Object) this instanceof Player player && damage.get() > 0) {
            var remaining = AAELivingEntityEvents.incomingDamage(player, damage.get());
            if (remaining < 1) {
                cir.setReturnValue(false);
                return;
            }
            damage.set(remaining);
        }
    }

    /**
     * Replaces LivingBreatheEvent.
     */
    @ModifyExpressionValue(
            method = "baseTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;canBreatheUnderwater()Z"))
    private boolean aae$canBreatheUnderwater(boolean canBreathe) {
        if (!canBreathe && (Object) this instanceof Player player) {
            return AAELivingEntityEvents.breath(player);
        }
        return canBreathe;
    }

    /**
     * Replaces LivingJumpEvent.
     */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void aae$jump(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            AAELivingEntityEvents.jumpEvent(player);
        }
    }
}
