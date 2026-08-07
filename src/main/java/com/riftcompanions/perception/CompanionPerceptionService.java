package com.riftcompanions.perception;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.registry.ModTags;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.events.TeamEventType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.Comparator;
import java.util.List;

/**
 * Bounded local sight sensor. It does not scan unloaded chunks, see through
 * walls, decode every sound, or turn a team report into direct vision.
 */
public final class CompanionPerceptionService {
    private CompanionPerceptionService() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null || now % com.riftcompanions.mode.PerformancePolicy.scaledInterval(CompanionConfig.PERCEPTION_INTERVAL_TICKS.get()) != 0L) return;
        board.expirePerceptions(now);
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion == null || companion.level() != player.level() || companion.getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED) continue;
            observeVisibleThreat(player, board, companion, now);
            observeSeparation(player, board, companion, now);
        }
    }

    private static void observeVisibleThreat(final ServerPlayer player, final TeamBlackboard board,
                                             final CompanionEntity companion, final long now) {
        final double range = switch (companion.getRole()) {
            case GUARDIAN -> 14.0D;
            case SEER -> 16.0D;
            case GIFTED -> 10.0D;
            case SCOUT -> 18.0D;
        };
        final List<Monster> visible = player.level().getEntitiesOfClass(Monster.class, companion.getBoundingBox().inflate(range),
                target -> target.isAlive() && companion.hasLineOfSight(target));
        final Monster nearest = visible.stream().min(Comparator.comparingDouble(companion::distanceToSqr)).orElse(null);
        if (nearest == null) return;
        final String entityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(nearest.getType()));
        final var advisory = com.riftcompanions.encounter.ThreatProfileRegistry.forEntity(nearest).orElse(null);
        final boolean hiveTagged = nearest.getType().is(ModTags.HIVE_LINKED) && companion.getRole() == CompanionRole.SEER;
        final double distance = Math.sqrt(companion.distanceToSqr(nearest));
        final PerceptionActionability action = hiveTagged || distance <= 6.0D ? PerceptionActionability.ALERT : PerceptionActionability.CAUTION;
        final PerceptionSignal signal = new PerceptionSignal("sight:" + companion.getRole().id() + ":" + nearest.getUUID(), companion.getRole(),
                hiveTagged ? PerceptionSource.TAG : PerceptionSource.VISION,
                PerceptionConfidence.HIGH, action, now, now + (action == PerceptionActionability.ALERT ? 80L : 140L),
                companion.getRole().personalName() + " has visible evidence of " + entityId + " nearby."
                        + (advisory == null || advisory.observableTell().isBlank() ? "" : " " + advisory.observableTell()));
        final boolean newSignal = board.publishPerception(signal, now);
        if (companion.getRole() == CompanionRole.SEER && hiveTagged) {
            com.riftcompanions.behavior.RoleBehaviorService.observeWillEvidence(player,
                    "hive:" + nearest.getUUID(), true);
        }
        if (newSignal && hiveTagged) {
            TeamDirector.submit(player, TeamEventType.HIVE_LINKED_TARGET_DETECTED, nearest.blockPosition(), List.of("HIVE_TAG_VISIBLE_TO_SEER"));
        }
        if (newSignal && distance >= 8.0D) {
            // A field-guide cue is only queued from an already visible local
            // monster. The contextual director later rejects it if combat risk
            // becomes active, so danger dialogue always wins over trivia.
            com.riftcompanions.context.ContextualInteractionDirector.observeVisibleMonster(player, nearest);
        }
    }

    private static void observeSeparation(final ServerPlayer player, final TeamBlackboard board,
                                          final CompanionEntity companion, final long now) {
        if (companion.distanceToSqr(player) <= 18.0D * 18.0D) return;
        board.publishPerception(new PerceptionSignal("separation:" + companion.getRole().id(), companion.getRole(),
                PerceptionSource.TEAM_REPORT, PerceptionConfidence.MEDIUM, PerceptionActionability.CAUTION,
                now, now + 80L, companion.getRole().personalName() + " is separated from the player formation."), now);
    }
}
