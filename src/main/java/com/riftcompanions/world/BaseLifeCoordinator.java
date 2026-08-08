package com.riftcompanions.world;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.SafeTeleport;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

/**
 * Safe Base Life v1: companions go to player-authored anchors and idle. It
 * deliberately does not open chests, sleep, harvest, use Redstone, or alter
 * any player build.
 */
public final class BaseLifeCoordinator {
    private BaseLifeCoordinator() {}

    public static void update(final ServerPlayer player, final TeamBlackboard board) {
        if (!(player.level() instanceof ServerLevel level) || board.plan().isActive() || board.plan().awaitsApproval()) {
            return;
        }
        final TeamAnchor home = board.anchor(BaseAnchorType.HOME).orElse(null);
        if (home == null || !home.dimension().equals(level.dimension().location()) || player.blockPosition().distSqr(home.position()) > 26.0D * 26.0D) {
            return;
        }
        if (!level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12.0D)).isEmpty()) {
            return;
        }
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion == null || !eligible(companion.getCompanionState())) {
                continue;
            }
            final TeamAnchor target = anchorFor(board, role, home);
            if (target == null || !target.dimension().equals(level.dimension().location()) || !level.hasChunkAt(target.position())) {
                continue;
            }
            if (SafeTeleport.isSafeStanding(level, companion, target.position())) {
                companion.beginBaseActivity(target.position());
            }
        }
    }

    private static boolean eligible(final CompanionState state) {
        return state == CompanionState.FOLLOWING || state == CompanionState.IDLE || state == CompanionState.RESTING || state == CompanionState.BASE_ACTIVITY;
    }

    private static TeamAnchor anchorFor(final TeamBlackboard board, final CompanionRole role, final TeamAnchor home) {
        return switch (role) {
            case GUARDIAN -> board.anchor(BaseAnchorType.GUARD_POST).or(() -> board.anchor(BaseAnchorType.ENTRY)).orElse(home);
            case SEER -> board.anchor(BaseAnchorType.JOURNAL).orElse(home);
            case GIFTED -> board.anchor(BaseAnchorType.QUIET).or(() -> board.anchor(BaseAnchorType.REST)).orElse(home);
            case SCOUT -> board.anchor(BaseAnchorType.LOOKOUT).or(() -> board.anchor(BaseAnchorType.ENTRY)).orElse(home);
        };
    }
}
