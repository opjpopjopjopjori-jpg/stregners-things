package com.riftcompanions.identity;

import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned identity editor and safe address formatter. */
public final class PlayerIdentityService {
    private PlayerIdentityService() {}

    public static String address(final ServerPlayer player, final boolean allowCalmNickname) {
        if (player == null || player.server == null) return "Player";
        final PlayerIdentityState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).playerIdentity();
        if (allowCalmNickname && state.nicknameEnabled()) return state.approvedNickname();
        return player.getGameProfile().getName();
    }

    public static IdentityResult setNickname(final ServerPlayer player, final String nickname) {
        if (player == null || player.server == null) return IdentityResult.failure("IDENTITY_SERVER_UNAVAILABLE", "No logical server is available.");
        final PlayerIdentityState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).playerIdentity();
        if (!state.setNickname(nickname)) {
            return IdentityResult.failure("NICKNAME_REJECTED", "Use 2–16 English letters, digits, spaces, hyphens, or underscores.");
        }
        TeamSavedData.get(player.server).markChanged();
        return IdentityResult.success("NICKNAME_SET", "Optional calm-scene nickname set to " + state.approvedNickname() + ".");
    }

    public static IdentityResult clearNickname(final ServerPlayer player) {
        if (player == null || player.server == null) return IdentityResult.failure("IDENTITY_SERVER_UNAVAILABLE", "No logical server is available.");
        TeamSavedData.get(player.server).blackboard(player.getUUID()).playerIdentity().clearNickname();
        TeamSavedData.get(player.server).markChanged();
        return IdentityResult.success("NICKNAME_CLEARED", "Alerts and scenes will use the player name only.");
    }

    public static IdentityResult setPlayStyle(final ServerPlayer player, final PlayerPlayStyle style) {
        if (player == null || player.server == null || style == null) return IdentityResult.failure("PLAY_STYLE_INVALID", "Choose a valid play style.");
        TeamSavedData.get(player.server).blackboard(player.getUUID()).playerIdentity().setPlayStyle(style);
        TeamSavedData.get(player.server).markChanged();
        return IdentityResult.success("PLAY_STYLE_SET", "Play style set to " + style + ". This changes presentation only.");
    }

    public record IdentityResult(boolean successful, String code, String detail) {
        public static IdentityResult success(final String code, final String detail) { return new IdentityResult(true, code, detail); }
        public static IdentityResult failure(final String code, final String detail) { return new IdentityResult(false, code, detail); }
    }
}
