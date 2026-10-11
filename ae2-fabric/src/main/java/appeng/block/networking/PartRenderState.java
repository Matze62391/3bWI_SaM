package appeng.block.networking;

import appeng.api.model.ModelData;

import appeng.api.parts.IPartItem;

public record PartRenderState(IPartItem<?> partItem, ModelData modelData) {
}
