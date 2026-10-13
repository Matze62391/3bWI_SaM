package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.loot.LootTable;

import appeng.init.InitVillager;

/**
 * Lets the fluix researcher give its own gifts to the hero of the village (NeoForge's raid hero gifts data map).
 */
@Mixin(GiveGiftToHero.class)
public abstract class GiveGiftToHeroMixin {
    @Inject(method = "getLootTableToThrow", at = @At("HEAD"), cancellable = true)
    private static void ae2$fluixResearcherGifts(Villager villager,
            CallbackInfoReturnable<ResourceKey<LootTable>> cir) {
        if (!villager.isBaby() && villager.getVillagerData().profession().is(InitVillager.ID)) {
            cir.setReturnValue(InitVillager.LOOT_TABLE_KEY);
        }
    }
}
