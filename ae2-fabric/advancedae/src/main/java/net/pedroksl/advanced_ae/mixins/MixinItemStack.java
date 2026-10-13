package net.pedroksl.advanced_ae.mixins;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.pedroksl.advanced_ae.events.AAEPlayerEvents;

/**
 * Adds the attribute modifiers of active quantum armor upgrades (replaces NeoForge's ItemAttributeModifierEvent).
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {
    @Inject(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
            at = @At("TAIL"))
    private void aae$forEachModifierInGroup(
            EquipmentSlotGroup slot,
            TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> consumer,
            CallbackInfo ci) {
        AAEPlayerEvents.itemAttributes((ItemStack) (Object) this, (attribute, modifier, group) -> {
            if (group == slot) {
                consumer.accept(attribute, modifier, ItemAttributeModifiers.Display.attributeModifiers());
            }
        });
    }

    @Inject(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @At("TAIL"))
    private void aae$forEachModifierInSlot(
            EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
        AAEPlayerEvents.itemAttributes((ItemStack) (Object) this, (attribute, modifier, group) -> {
            if (group.test(slot)) {
                consumer.accept(attribute, modifier);
            }
        });
    }
}
