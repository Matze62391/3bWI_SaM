package appeng.client.api.model.parts;

import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Register a listener for {@link #EVENT} in your client initializer to register part model types with AE2.
 */
public class RegisterPartModelsEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class,
            listeners -> event -> {
                for (var listener : listeners) {
                    listener.registerPartModels(event);
                }
            });

    private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper;

    @ApiStatus.Internal
    public RegisterPartModelsEvent(
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper) {
        this.modelIdMapper = modelIdMapper;
    }

    public void registerModelType(Identifier type, MapCodec<? extends PartModel.Unbaked> codec) {
        this.modelIdMapper.put(type, codec);
    }

    @FunctionalInterface
    public interface Listener {
        void registerPartModels(RegisterPartModelsEvent event);
    }
}
