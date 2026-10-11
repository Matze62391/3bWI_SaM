package net.pedroksl.advanced_ae.events;

import net.pedroksl.advanced_ae.common.helpers.AAEPersistentData;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.pedroksl.advanced_ae.AdvancedAE;
import net.pedroksl.advanced_ae.common.definitions.AAEComponents;
import net.pedroksl.advanced_ae.common.helpers.KeysPressed;
import net.pedroksl.advanced_ae.common.items.armors.*;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import net.pedroksl.advanced_ae.network.packet.ItemTrackingPacket;

public class AAEPlayerEvents {
    public static final AttributeModifier flight =
            new AttributeModifier(AdvancedAE.makeId("flight"), 1.0, AttributeModifier.Operation.ADD_VALUE);

    private static final String FLIGHT_GRANTED = "aae$flight_granted";

    /**
     * Client-side player tick handlers, set by the client (replaces NeoForge's client PlayerTickEvents).
     */
    public static Consumer<Player> clientPreTick = player -> {};

    public static Consumer<Player> clientPostTick = player -> {};

    public static void register() {
        EntityTrackingEvents.START_TRACKING.register(AAEPlayerEvents::onStartTracking);
    }

    @FunctionalInterface
    public interface ModifierSink {
        void addModifier(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot);
    }

    /**
     * Adds the attribute modifiers of active upgrades. Called by a mixin into {@link ItemStack#forEachModifier}
     * (replaces NeoForge's ItemAttributeModifierEvent).
     */
    public static void itemAttributes(ItemStack itemStack, ModifierSink event) {
        if (itemStack.getItem() instanceof QuantumArmorBase armor) {
            if (armor.isUpgradeEnabledAndPowered(itemStack, UpgradeType.STEP_ASSIST)) {
                int value = itemStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.STEP_ASSIST), 0);
                event.addModifier(Attributes.STEP_HEIGHT, getStepAssist(value), EquipmentSlotGroup.FEET);
            }
            // Flight has no attribute on Fabric, see updateFlight
            if (armor.isUpgradeEnabledAndPowered(itemStack, UpgradeType.HP_BUFFER)) {
                int value = itemStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.HP_BUFFER), 0);
                event.addModifier(Attributes.MAX_HEALTH, getHpBuffer(value), EquipmentSlotGroup.CHEST);
            }
            if (armor.isUpgradeEnabledAndPowered(itemStack, UpgradeType.STRENGTH)) {
                int value = itemStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.STRENGTH), 0);
                event.addModifier(Attributes.ATTACK_DAMAGE, getStrengthBoost(value), EquipmentSlotGroup.CHEST);
            }
            if (armor.isUpgradeEnabledAndPowered(itemStack, UpgradeType.ATTACK_SPEED)) {
                int value = itemStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.ATTACK_SPEED), 0);
                event.addModifier(Attributes.ATTACK_SPEED, getAttackSpeedBoost(value), EquipmentSlotGroup.CHEST);
            }
            if (armor.isUpgradeEnabledAndPowered(itemStack, UpgradeType.LUCK)) {
                int value = itemStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.LUCK), 0);
                event.addModifier(Attributes.LUCK, getLuckBoost(value), EquipmentSlotGroup.HEAD);
            }
            if (armor.isUpgradeEnabledAndPowered(itemStack, UpgradeType.REACH)) {
                int value = itemStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.REACH), 0);
                var attValue = getReachBoost(value);
                event.addModifier(Attributes.BLOCK_INTERACTION_RANGE, attValue, EquipmentSlotGroup.LEGS);
                event.addModifier(Attributes.ENTITY_INTERACTION_RANGE, attValue, EquipmentSlotGroup.LEGS);
            }
        }
    }

    /**
     * Called by a mixin at the end of {@link Player#getDestroySpeed} (replaces NeoForge's BreakSpeed event).
     */
    public static float breakSpeed(Player player, float originalSpeed) {
        if (!player.onGround()) {
            ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
            if (armor.getItem() instanceof QuantumChestplate) {
                return Math.min(Float.MAX_VALUE, originalSpeed * 5);
            }
        } else if (player.isEyeInFluid(FluidTags.WATER)) {
            ItemStack armor = player.getItemBySlot(EquipmentSlot.CHEST);
            if (armor.getItem() instanceof QuantumChestplate) {
                var att = player.getAttribute(Attributes.SUBMERGED_MINING_SPEED);
                if (att != null) {
                    var value = (float) att.getValue();
                    if (value < 1) {
                        return originalSpeed / value;
                    }
                }
            }
        }
        return originalSpeed;
    }

    /**
     * Called by a mixin at the start of {@link Player#tick} (replaces NeoForge's PlayerTickEvent.Pre).
     */
    public static void playerTick(Player player) {
        if (player.level().isClientSide()) {
            clientPreTick.accept(player);
        }
        updateFlight(player);

        var nv = player.getEffect(MobEffects.NIGHT_VISION);
        var wb = player.getEffect(MobEffects.WATER_BREATHING);

        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!stack.isEmpty()) {
            if (stack.getItem() instanceof QuantumHelmet helmet) {
                addEffect(
                        player,
                        helmet,
                        stack,
                        MobEffects.NIGHT_VISION,
                        nv,
                        UpgradeType.NIGHT_VISION,
                        AAEComponents.NIGHT_VISION_ACTIVATED);
                addEffect(
                        player,
                        helmet,
                        stack,
                        MobEffects.WATER_BREATHING,
                        wb,
                        UpgradeType.WATER_BREATHING,
                        AAEComponents.WATER_BREATHING_ACTIVATED);
            }
        }

        ItemStack bootStack = player.getItemBySlot(EquipmentSlot.FEET);
        var keys = new KeysPressed(AAEPersistentData.get(player).getByteOr(KeysPressed.KEYS_PRESSED, (byte) 0));
        if (bootStack.getItem() instanceof QuantumBoots boots) {
            if (player.getAbilities().flying && boots.isUpgradeEnabledAndPowered(bootStack, UpgradeType.FLIGHT_DRIFT)) {
                if (keys.noKey) {
                    var motion = player.getDeltaMovement();
                    if (motion.x != 0 || motion.z != 0) {
                        var value =
                                bootStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.FLIGHT_DRIFT), 100)
                                        / 100f;
                        player.setDeltaMovement(motion.x * value, motion.y, motion.z * value);
                        boots.consumeEnergy(player, bootStack, UpgradeType.FLIGHT_DRIFT);
                    }
                }
            }
        }
        if (keys.upKey != keys.downKey) {
            ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chestStack.getItem() instanceof QuantumChestplate chest) {
                var upgrade = UpgradeType.FLIGHT;
                if (player.getAbilities().flying && chest.isUpgradeEnabledAndPowered(chestStack, upgrade)) {
                    var value = upgrade.getSettings().multiplier
                            * chestStack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(upgrade), 0)
                            / 25f;
                    var direction = keys.upKey ? 1 : -1;
                    player.moveRelative(value, new Vec3(0, direction, 0));
                }
            }
        }
    }

    private static void addEffect(
            Player player,
            QuantumArmorBase item,
            ItemStack stack,
            Holder<@NotNull MobEffect> effect,
            MobEffectInstance effectInstance,
            UpgradeType upgrade,
            DataComponentType<@NotNull Boolean> tag) {
        if (item.isUpgradeEnabledAndPowered(stack, upgrade)) {
            if (effectInstance == null || effectInstance.getDuration() < 210) {
                stack.set(tag, true);
                player.addEffect(new MobEffectInstance(effect, 210, 0, false, false, false));
                item.consumeEnergy(player, stack, UpgradeType.NIGHT_VISION);
            }
        }

        if (stack.getOrDefault(tag, false)) {
            if (effectInstance != null && effectInstance.getDuration() < 210) {
                player.removeEffect(effect);
                stack.remove(tag);
            }
        }
    }

    /**
     * NeoForge grants flight through its creative flight attribute. On Fabric, the abilities are toggled directly while
     * the flight upgrade of the chestplate is active.
     */
    private static void updateFlight(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        var data = AAEPersistentData.get(player);
        var chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        var canFly = chestStack.getItem() instanceof QuantumChestplate chest
                && chest.isUpgradeEnabledAndPowered(chestStack, UpgradeType.FLIGHT);
        var abilities = player.getAbilities();
        if (canFly) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                data.putBoolean(FLIGHT_GRANTED, true);
                player.onUpdateAbilities();
            }
        } else if (data.getBooleanOr(FLIGHT_GRANTED, false)) {
            data.remove(FLIGHT_GRANTED);
            if (!player.isCreative() && !player.isSpectator()) {
                abilities.mayfly = false;
                abilities.flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    private static void onStartTracking(Entity target, ServerPlayer serverPlayer) {
        if (target instanceof ItemEntity item && item.thrower != null) {
            ServerPlayNetworking.send(
                    serverPlayer, new ItemTrackingPacket(item.thrower.getUUID(), item.getId(), item.pickupDelay));
        }
    }

    private static AttributeModifier getStepAssist(int value) {
        return new AttributeModifier(AdvancedAE.makeId("step_assist"), value, AttributeModifier.Operation.ADD_VALUE);
    }

    private static AttributeModifier getHpBuffer(int value) {
        return new AttributeModifier(AdvancedAE.makeId("hp_buffer"), value, AttributeModifier.Operation.ADD_VALUE);
    }

    private static AttributeModifier getStrengthBoost(int value) {
        return new AttributeModifier(AdvancedAE.makeId("strength_boost"), value, AttributeModifier.Operation.ADD_VALUE);
    }

    private static AttributeModifier getAttackSpeedBoost(int value) {
        return new AttributeModifier(AdvancedAE.makeId("attack_speed"), value, AttributeModifier.Operation.ADD_VALUE);
    }

    private static AttributeModifier getLuckBoost(int value) {
        return new AttributeModifier(AdvancedAE.makeId("luck"), value, AttributeModifier.Operation.ADD_VALUE);
    }

    private static AttributeModifier getReachBoost(int value) {
        return new AttributeModifier(AdvancedAE.makeId("reach_boost"), value, AttributeModifier.Operation.ADD_VALUE);
    }
}
