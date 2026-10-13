package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Lets survival players fly. On NeoForge this is a modifier on the {@code neoforge:creative_flight} attribute.
 */
public class FlightAugment extends Augment {

    public static final Identifier ATTRIBUTE_ID = Identifier.fromNamespaceAndPath("oritech", "creative_flight");

    public FlightAugment(Identifier id, boolean toggleable) {
        super(id, toggleable);
    }

    @Override
    public void activate(Player player) {
        setFlight(player, true);
    }

    @Override
    public void deactivate(Player player) {
        setFlight(player, false);
    }

    @Override
    public void refreshServer(Player player) {
        setFlight(player, true);
    }

    @Override
    public int refreshInterval() {
        return 20;
    }

    private static void setFlight(Player player, boolean enabled) {
        if (player.isCreative() || player.isSpectator()) return;
        var abilities = player.getAbilities();
        if (abilities.mayfly == enabled && (enabled || !abilities.flying)) return;
        abilities.mayfly = enabled;
        if (!enabled) abilities.flying = false;
        player.onUpdateAbilities();
    }
}
