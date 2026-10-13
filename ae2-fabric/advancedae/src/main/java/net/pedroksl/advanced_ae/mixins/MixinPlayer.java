package net.pedroksl.advanced_ae.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.events.AAELivingEntityEvents;
import net.pedroksl.advanced_ae.events.AAEPlayerEvents;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity {

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    public void hurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> ci) {
        if (this.lastHurt < 10f) {
            for (var slot : EquipmentSlot.values()) {
                if (getItemBySlot(slot).getItem() instanceof QuantumArmorBase) {
                    ci.setReturnValue(null);
                    ci.cancel();
                    return;
                }
            }
        }
    }

    /**
     * Replaces NeoForge's PlayerTickEvent.Pre.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void aae$tick(CallbackInfo ci) {
        AAEPlayerEvents.playerTick((Player) (Object) this);
    }

    /**
     * Replaces NeoForge's PlayerTickEvent.Post on the client.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void aae$tickPost(CallbackInfo ci) {
        if (level().isClientSide()) {
            AAEPlayerEvents.clientPostTick.accept((Player) (Object) this);
        }
    }

    /**
     * Replaces NeoForge's PlayerEvent.BreakSpeed.
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void aae$breakSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(AAEPlayerEvents.breakSpeed((Player) (Object) this, cir.getReturnValueF()));
    }

    /**
     * Replaces NeoForge's EntityInvulnerabilityCheckEvent.
     */
    @Inject(method = "isInvulnerableTo", at = @At("RETURN"), cancellable = true)
    private void aae$invulnerability(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && AAELivingEntityEvents.invulnerability((Player) (Object) this, source)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Replaces NeoForge's LivingFallEvent.
     */
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void aae$fallDamage(
            double fallDistance, float damageModifier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (AAELivingEntityEvents.cancelFallDamage((Player) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    protected MixinPlayer(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }
}
