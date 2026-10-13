package net.pedroksl.advanced_ae.client;

import net.pedroksl.advanced_ae.common.helpers.AAEPersistentData;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.pedroksl.advanced_ae.events.AAEPlayerEvents;
import net.pedroksl.advanced_ae.AdvancedAE;
import net.pedroksl.advanced_ae.common.helpers.KeysPressed;
import net.pedroksl.advanced_ae.common.items.armors.*;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import net.pedroksl.advanced_ae.network.packet.KeysPressedPacket;

public class AAEClientPlayerEvents {
    public static void register() {
        AAEPlayerEvents.clientPreTick = AAEClientPlayerEvents::prePlayerTick;
        AAEPlayerEvents.clientPostTick = AAEClientPlayerEvents::playerTick;
    }

    public static final AttributeModifier flight =
            new AttributeModifier(AdvancedAE.makeId("flight"), 1.0, AttributeModifier.Operation.ADD_VALUE);

    public static void prePlayerTick(Player player) {
        if (player instanceof ServerPlayer) return;
        for (var slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof QuantumArmorBase item
                    && !item.getPassiveUpgrades(stack).isEmpty()) {
                item.tickUpgrades(player.level(), player, stack);
            }
        }
    }

    public static void playerTick(Player player) {
        // The pressed keys are only known for the local player
        if (player != Minecraft.getInstance().player) return;

        boolean shouldUpdateServer = false;
        var keys = new KeysPressed(AAEPersistentData.get(player).getByteOr(KeysPressed.KEYS_PRESSED, (byte) 0));
        ItemStack bootStack = player.getItemBySlot(EquipmentSlot.FEET);
        if (bootStack.getItem() instanceof QuantumBoots boots) {
            if (boots.isUpgradeEnabledAndPowered(bootStack, UpgradeType.FLIGHT_DRIFT)) {
                var options = Minecraft.getInstance().options;
                var noKey = !options.keyUp.isDown()
                        && !options.keyRight.isDown()
                        && !options.keyDown.isDown()
                        && !options.keyLeft.isDown();

                if (keys.noKey != noKey) {
                    // Send packet to server if data on player is different
                    keys.noKey = noKey;
                    shouldUpdateServer = true;
                    AAEPersistentData.get(player).putByte(KeysPressed.KEYS_PRESSED, keys.toByte());
                }
            }
        }

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() instanceof QuantumChestplate) {
            var options = Minecraft.getInstance().options;
            var downKey = options.keyShift.isDown();
            var upKey = options.keyJump.isDown();

            if (keys.downKey != downKey || keys.upKey != upKey) {
                // Send packet to server if data on player is different
                keys.downKey = downKey;
                keys.upKey = upKey;
                shouldUpdateServer = true;
                AAEPersistentData.get(player).putByte(KeysPressed.KEYS_PRESSED, keys.toByte());
            }
        }

        if (shouldUpdateServer) {
            ClientPlayNetworking.send(new KeysPressedPacket(keys));
        }
    }
}
