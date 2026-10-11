package net.pedroksl.advanced_ae.events;

import java.util.Random;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.pedroksl.advanced_ae.common.definitions.AAEConfig;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;

import appeng.api.config.Actionable;

/**
 * Replaces NeoForge's living entity events. The methods are called by {@code MixinPlayer} and
 * {@code MixinLivingEntity}.
 */
public class AAELivingEntityEvents {

    /**
     * @return true if the player should be invulnerable to the damage source.
     */
    public static boolean invulnerability(Player player, DamageSource source) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof QuantumArmorBase item
                && item.isUpgradeEnabledAndPowered(chestStack, UpgradeType.LAVA_IMMUNITY)) {
            if (source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)) {
                player.setRemainingFireTicks(0);
                item.consumeEnergy(player, chestStack, UpgradeType.LAVA_IMMUNITY);
                return true;
            }
        }
        ItemStack bootStack = player.getItemBySlot(EquipmentSlot.FEET);
        if (bootStack.getItem() instanceof QuantumArmorBase item
                && item.isUpgradeEnabledAndPowered(bootStack, UpgradeType.EVASION)) {
            Random randomGenerator = new Random();
            var chance = randomGenerator.nextDouble(100);
            if (chance < AAEConfig.instance().getEvasionChance()) {
                item.consumeEnergy(player, bootStack, UpgradeType.EVASION);
                return true;
            }
        }
        return false;
    }

    /**
     * @return the damage that remains after the armor absorbed some of it. Damage below 1 is cancelled.
     */
    public static float incomingDamage(Player player, float amount) {
        if (player.isAlive() && amount > 0) {
            var maxAbsorption = amount * AAEConfig.instance().getPercentageDamageAbsorption() / 100f;
            var amountPerPiece = maxAbsorption / 4f;
            float absorbed = 0;
            for (var slot : EquipmentSlot.values()) {
                var stack = player.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof QuantumArmorBase item) {
                    var extracted = item.extractAEPower(stack, amountPerPiece * 1000f, Actionable.MODULATE);
                    absorbed += (float) extracted / 1000f;
                }
            }
            if (absorbed > 0) {
                amount = Math.max(0, amount - absorbed);
            }
        }
        return amount;
    }

    /**
     * @return true if the player can breathe under water thanks to the helmet.
     */
    public static boolean breath(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (stack.getItem() instanceof QuantumArmorBase item
                && item.isUpgradeEnabledAndPowered(stack, UpgradeType.WATER_BREATHING)) {
            item.consumeEnergy(player, stack, UpgradeType.WATER_BREATHING);
            return true;
        }
        return false;
    }

    public static void jumpEvent(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.FEET);
        if (stack.getItem() instanceof QuantumArmorBase item
                && item.isUpgradeEnabledAndPowered(stack, UpgradeType.JUMP_HEIGHT)) {
            UpgradeType.JUMP_HEIGHT.ability.execute(player.level(), player, stack);
            item.consumeEnergy(player, stack, UpgradeType.JUMP_HEIGHT);
        }
    }

    /**
     * @return true if the fall damage should be cancelled.
     */
    public static boolean cancelFallDamage(Player player) {
        if (player instanceof ServerPlayer) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.FEET);
            if (stack.getItem() instanceof QuantumArmorBase item) {
                return item.extractAEPower(stack, 10, Actionable.SIMULATE) > 0;
            }
        }
        return false;
    }
}
