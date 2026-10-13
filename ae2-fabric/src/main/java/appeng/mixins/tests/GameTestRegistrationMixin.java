package appeng.mixins.tests;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;

import appeng.server.testworld.GameTestPlotAdapter;

/**
 * Registers AE2's test plots as game test instances when the dynamic registries are loaded. NeoForge offers an event
 * for this; Fabric's game test API registers its own tests at the same point.
 */
@Mixin(RegistryDataLoader.class)
public class GameTestRegistrationMixin {
    @Unique
    private static final AtomicBoolean ae2$loadingTestInstances = new AtomicBoolean(false);

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Ljava/util/List;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
    private static void ae2$checkForTestInstances(ResourceManager resourceManager,
            List<HolderLookup.RegistryLookup<?>> registries, List<RegistryDataLoader.RegistryData<?>> entries,
            Executor executor, CallbackInfoReturnable<RegistryAccess.Frozen> cir) {
        ae2$loadingTestInstances.set(entries.stream().anyMatch(entry -> entry.key() == Registries.TEST_INSTANCE));
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "lambda$load$2(Ljava/util/List;Ljava/util/Map;Ljava/lang/Void;)Lnet/minecraft/core/RegistryAccess$Frozen;", at = @At("HEAD"))
    private static void ae2$registerTestPlots(List<RegistryLoadTask<?>> loadTasks,
            Map<ResourceKey<?>, Exception> loadingErrors, Void ignored,
            CallbackInfoReturnable<RegistryAccess.Frozen> cir) {
        // Only when loading from data packs, not when the client receives the synchronized registry
        if (!ae2$loadingTestInstances.getAndSet(false) || !Boolean.getBoolean("appeng.tests")) {
            return;
        }
        for (var task : loadTasks) {
            if (task.registry.key() == Registries.TEST_INSTANCE) {
                var testInstances = (Registry<GameTestInstance>) task.registry;
                GameTestPlotAdapter.registerAll((id, instance) -> Registry.register(testInstances, id, instance));
            }
        }
    }
}
