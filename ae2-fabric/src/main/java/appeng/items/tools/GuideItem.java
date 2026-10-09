package appeng.items.tools;

import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;


import appeng.core.AppEng;
import appeng.items.AEBaseItem;

/**
 * Shows the guidebook when used.
 */
public class GuideItem extends AEBaseItem {
    public static final Identifier GUIDE_ID = AppEng.makeId("guide");

    public GuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // TODO: The guidebook is based on GuideME, which is not available for Fabric
        if (level.isClientSide()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component
                    .literal("The AE2 guidebook is not available in the Fabric port yet."));
        }

        return InteractionResult.FAIL;
    }
}
