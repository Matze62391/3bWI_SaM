package appeng.client.api.renderer.parts;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import appeng.api.parts.IPart;

/**
 * Register a listener for {@link #EVENT} in your client initializer to register renderers for your parts. The event
 * is fired on every resource reload.
 */
public class RegisterPartRendererEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class,
            listeners -> event -> {
                for (var listener : listeners) {
                    listener.registerPartRenderers(event);
                }
            });

    private final PartRegistrationSink delegate;

    @ApiStatus.Internal
    public RegisterPartRendererEvent(PartRegistrationSink delegate) {
        this.delegate = delegate;
    }

    public <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> renderer) {
        delegate.register(partClass, renderer);
    }

    @FunctionalInterface
    @ApiStatus.Internal
    public interface PartRegistrationSink {
        <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> factory);
    }

    @FunctionalInterface
    public interface Listener {
        void registerPartRenderers(RegisterPartRendererEvent event);
    }
}
