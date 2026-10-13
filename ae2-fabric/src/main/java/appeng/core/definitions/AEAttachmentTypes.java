package appeng.core.definitions;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import appeng.core.AppEng;

public final class AEAttachmentTypes {
    /**
     * Whether the player is currently holding the CTRL key (synced from the client, not persisted).
     */
    public static final AttachmentType<Boolean> HOLDING_CTRL = AttachmentRegistry.createDefaulted(
            AppEng.makeId("ctrl"), () -> false);

    private AEAttachmentTypes() {
    }

    public static void register() {
        // Attachment types are registered when this class is loaded
    }
}
