package com.riftcompanions.scene;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.world.BaseAnchorType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/**
 * Executes one short, skippable-in-practice in-world line at a safe moment.
 * It never locks input, moves the camera, freezes time, or starts during combat.
 */
public final class SetPieceService {
    private SetPieceService() {}

    public static void request(final ServerPlayer player, final SetPieceType type, final long lifetime) {
        if (player == null || player.server == null || type == null) return;
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (board.sceneState().request(type, player.level().getGameTime(), lifetime)) {
            TeamSavedData.get(player.server).markChanged();
        }
    }

    public static void beginSilentWalk(final ServerPlayer player, final long duration) {
        if (player == null || player.server == null) return;
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        final long now = player.level().getGameTime();
        board.sceneState().beginSilentWalk(now, duration);
        board.beginQuietWindow(now, duration);
        TeamSavedData.get(player.server).markChanged();
    }

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null || !com.riftcompanions.mode.PerformancePolicy.allowsAmbientScenes()) return;
        board.sceneState().expire(now);
        final SetPieceType type = board.sceneState().pending();
        if (type == SetPieceType.NONE || type == SetPieceType.SILENT_WALK) return;
        if (!board.isAtSafeBase(player) && type != SetPieceType.THRESHOLD_MOMENT && type != SetPieceType.LANDMARK_MEMORY) return;
        if (!player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, player.getBoundingBox().inflate(12.0D)).isEmpty()) return;
        final CompanionRole speakerRole = speakerFor(type);
        final CompanionEntity speaker = CompanionLifecycleService.findForOwner(player, speakerRole).orElse(null);
        if (speaker == null || speaker.getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED) return;
        if (type == SetPieceType.CAMPFIRE_CHECK && !hasNearbyCampfire(player.serverLevel(), player.blockPosition())) return;
        final String trigger = switch (type) {
            case CAMPFIRE_CHECK -> "setpiece_campfire";
            case THRESHOLD_MOMENT -> "setpiece_threshold";
            case RETURN_HOME -> "setpiece_return";
            case LANDMARK_MEMORY -> "setpiece_landmark";
            default -> "";
        };
        if (trigger.isBlank()) return;
        final DialogueService.SpeakResult result = DialogueService.get().speak(speaker, trigger, 3);
        if (result.sent()) {
            final com.riftcompanions.entity.CompanionAction action = switch (type) {
                case CAMPFIRE_CHECK -> com.riftcompanions.entity.CompanionAction.SOCIAL_CAMPFIRE;
                case THRESHOLD_MOMENT -> com.riftcompanions.entity.CompanionAction.SOCIAL_HORIZON;
                case RETURN_HOME -> com.riftcompanions.entity.CompanionAction.SOCIAL_BASE;
                case LANDMARK_MEMORY -> com.riftcompanions.entity.CompanionAction.SOCIAL_OBSERVE;
                default -> com.riftcompanions.entity.CompanionAction.NONE;
            };
            speaker.beginVisualAction(action, 28L);
        }
        board.sceneState().consume(now);
        TeamSavedData.get(player.server).markChanged();
    }

    private static CompanionRole speakerFor(final SetPieceType type) {
        return switch (type) {
            case CAMPFIRE_CHECK, THRESHOLD_MOMENT, RETURN_HOME -> CompanionRole.GUARDIAN;
            case LANDMARK_MEMORY -> CompanionRole.SEER;
            default -> CompanionRole.GUARDIAN;
        };
    }

    private static boolean hasNearbyCampfire(final ServerLevel level, final BlockPos origin) {
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, 1, 4))) {
            if (level.hasChunkAt(pos) && (level.getBlockState(pos).is(Blocks.CAMPFIRE) || level.getBlockState(pos).is(Blocks.SOUL_CAMPFIRE))) {
                return true;
            }
        }
        return false;
    }
}
