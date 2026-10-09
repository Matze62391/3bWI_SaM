package appeng.client.gui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

/**
 * A page (and optional anchor) in the AE2 guidebook. Replaces GuideME's {@code PageAnchor}, since GuideME is not
 * available on Fabric.
 */
public record HelpTopic(Identifier pageId, @Nullable String anchor) {
}
