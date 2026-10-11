package guideme.internal;

import guideme.internal.command.GuideCommand;
import guideme.internal.command.GuideIdArgument;
import guideme.internal.command.PageAnchorArgument;
import guideme.internal.item.GuideItem;
import guideme.internal.network.OpenGuideRequest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

public class GuideME implements ModInitializer {

    static GuideMEProxy PROXY = new GuideMEServerProxy();

    public static final String MOD_ID = "guideme";

    public static final Supplier<GuideItem> GUIDE_ITEM;

    /**
     * Attaches the guide ID to a generic guide item.
     */
    public static final DataComponentType<Identifier> GUIDE_ID_COMPONENT = DataComponentType
            .<Identifier>builder()
            .networkSynchronized(Identifier.STREAM_CODEC)
            .persistent(Identifier.CODEC)
            .build();

    static {
        // Registered statically, since GUIDE_ITEM is final and referenced by other classes
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, makeId("guide_id"), GUIDE_ID_COMPONENT);
        var itemKey = ResourceKey.create(Registries.ITEM, makeId("guide"));
        var guideItem = Registry.register(BuiltInRegistries.ITEM, itemKey,
                new GuideItem(new Item.Properties().setId(itemKey)));
        GUIDE_ITEM = () -> guideItem;
    }

    @Override
    public void onInitialize() {
        ArgumentTypeRegistry.registerArgumentType(makeId("guide_id"), GuideIdArgument.class,
                SingletonArgumentInfo.contextFree(GuideIdArgument::argument));
        ArgumentTypeRegistry.registerArgumentType(makeId("page_anchor"), PageAnchorArgument.class,
                SingletonArgumentInfo.contextFree(PageAnchorArgument::argument));

        PayloadTypeRegistry.clientboundPlay().register(OpenGuideRequest.TYPE, OpenGuideRequest.STREAM_CODEC);

        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> GuideCommand.register(dispatcher));

        // Vanilla no longer sends recipes to the client. We need the ones for which we have default handlers.
        for (var serializer : BuiltInRegistries.RECIPE_SERIALIZER) {
            var id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
            if (id != null && id.getNamespace().equals("minecraft")) {
                RecipeSynchronization.synchronizeRecipeSerializer(serializer);
            }
        }
    }

    public static Identifier makeId(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
