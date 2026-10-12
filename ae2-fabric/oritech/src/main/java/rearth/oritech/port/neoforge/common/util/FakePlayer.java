package rearth.oritech.port.neoforge.common.util;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.level.ServerLevel;

public class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer {
    public FakePlayer(ServerLevel level, GameProfile profile) {
        super(level, profile);
    }
}
