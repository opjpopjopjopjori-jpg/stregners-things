package com.riftcompanions.team;

import com.riftcompanions.debug.DecisionTraceService;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.formation.FormationCoordinator;
import com.riftcompanions.server.TemporalMemoryService;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.BaseLifeCoordinator;
import com.riftcompanions.world.TeamAnchor;
import com.riftcompanions.world.assessment.EnvironmentRiskAssessment;
import com.riftcompanions.world.assessment.EnvironmentRiskService;
import com.riftcompanions.world.assessment.StructurePerimeterAssessment;
import com.riftcompanions.world.assessment.StructurePerimeterService;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.scene.SetPieceService;
import com.riftcompanions.scene.SetPieceType;
import com.riftcompanions.interrupt.ActionInterruptService;
import com.riftcompanions.mode.GameModePolicyService;
import com.riftcompanions.mode.PerformancePolicy;
import com.riftcompanions.performance.CompanionPerformanceMonitor;
import com.riftcompanions.performance.PerformanceWorkType;
import com.riftcompanions.team.events.TeamEvent;
import com.riftcompanions.team.events.TeamEventPriority;
import com.riftcompanions.team.events.TeamEventQueue;
import com.riftcompanions.team.events.TeamEventType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event-driven group coordinator. It consumes at most one new event per tick
 * pass, lets P0 interrupt low-value work, and delegates authoritative plans to
 * TeamPlanService. It never executes block actions or overrides player agency
 * for non-emergency plans.
 */
public final class TeamDirector {
    private static final Map<UUID, TeamEventQueue> QUEUES = new HashMap<>();
    private static final Map<UUID, Long> LAST_FORMATION_UPDATE = new HashMap<>();
    private static final Map<UUID, Long> LAST_NIGHT_EVENT_DAY = new HashMap<>();
    private static final Map<UUID, Long> LAST_ENVIRONMENT_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_BASE_LIFE_TICK = new HashMap<>();
    private static final Map<UUID, Long> LAST_SOCIAL_TICK = new HashMap<>();
    private static final Map<UUID, EnvironmentRiskAssessment.Band> LAST_ENVIRONMENT_BAND = new HashMap<>();
    private static final Map<UUID, StructurePerimeterAssessment> PENDING_STRUCTURE = new HashMap<>();

    private TeamDirector() {}

    public static void submit(final ServerPlayer player, final TeamEventType type, final List<String> reasons) {
        submit(player, type, null, reasons);
    }

    public static void submit(final ServerPlayer player, final TeamEventType type, final net.minecraft.core.BlockPos location, final List<String> reasons) {
        if (player == null || type == null) {
            return;
        }
        queue(player.getUUID()).submit(TeamEvent.create(type, player.level().getGameTime(), location, reasons));
    }

    public static TeamPlanService.PlanResult requestStructureEntry(final ServerPlayer player) {
        final var assessment = StructurePerimeterService.inspectLookTarget(player);
        if (assessment.isEmpty()) {
            return TeamPlanService.PlanResult.failure("NO_VALID_STRUCTURE_LOOK_TARGET", "Look at a nearby entrance or block within 12 blocks to inspect the perimeter.");
        }
        final StructurePerimeterAssessment value = assessment.get();
        PENDING_STRUCTURE.put(player.getUUID(), value);
        com.riftcompanions.context.ContextualInteractionDirector.observePlayerSelectedStructure(player,
                com.riftcompanions.encounter.EncounterProfileRegistry.structureProfile(value).id());
        SetPieceService.request(player, SetPieceType.THRESHOLD_MOMENT, 240L);
        submit(player, TeamEventType.STRUCTURE_REQUESTED, value.focus(), value.reasonCodes());
        // Process immediately so the Journal can display the approval card this tick.
        processOne(player);
        final TeamPlan plan = com.riftcompanions.server.TeamSavedData.get(player.server).blackboard(player.getUUID()).plan();
        return plan.awaitsApproval()
                ? TeamPlanService.PlanResult.success("STRUCTURE_PLAN_AWAITING_APPROVAL", "The perimeter was checked and an entry plan was drafted. Review the reasons before accepting.")
                : TeamPlanService.PlanResult.failure("STRUCTURE_PLAN_NOT_CREATED", "An entry plan could not be created in the current situation.");
    }

    public static void tick(final ServerPlayer player) {
        if (player == null) return;
        final long performanceStartedAt = CompanionPerformanceMonitor.start();
        try {
            final TeamBlackboard board = com.riftcompanions.server.TeamSavedData.get(player.server).blackboard(player.getUUID());
            if (SafeModeService.enabled(player)) return;
            final GameModePolicyService.ModeDisposition mode = GameModePolicyService.apply(player);
            if (mode == GameModePolicyService.ModeDisposition.OBSERVER) return;
            final long now = player.level().getGameTime();
            if (mode.permitsThreatScan()) {
                observeWorld(player, now);
                ThreatResponseDirector.tick(player, board, now);
                com.riftcompanions.context.ContextualInteractionDirector.tick(player, board, now);
                observeEnvironment(player, now);
                com.riftcompanions.perception.CompanionPerceptionService.tick(player, board, now);
                com.riftcompanions.encounter.EncounterContextService.tick(player, board, now);
                com.riftcompanions.behavior.GiftedBehaviorService.tick(player, board, now);
                processOne(player);
            }
            TeamPlanService.tick(player);
            final Long socialTick = LAST_SOCIAL_TICK.get(player.getUUID());
            if (socialTick == null || now - socialTick >= PerformancePolicy.scaledInterval(20L)) {
                com.riftcompanions.social.CompanionSocialDirector.tick(player, board, now);
                LAST_SOCIAL_TICK.put(player.getUUID(), now);
            }
            final long last = LAST_FORMATION_UPDATE.getOrDefault(player.getUUID(), Long.MIN_VALUE);
            if (board.focusTargetUuid() != null && !board.hasActiveFocusTarget(now)) {
                board.clearFocusTarget();
            }
            if (last == Long.MIN_VALUE || now - last >= PerformancePolicy.scaledInterval(10L)) {
                FormationCoordinator.update(player, board, now);
                LAST_FORMATION_UPDATE.put(player.getUUID(), now);
            }
            final Long baseTick = LAST_BASE_LIFE_TICK.get(player.getUUID());
            if (baseTick == null || now - baseTick >= PerformancePolicy.scaledInterval(100L)) {
                BaseLifeCoordinator.update(player, board);
                TemporalMemoryService.tick(player, board, now);
                IntentionService.tick(player, board, now);
                com.riftcompanions.story.StoryService.tickOptionalPromises(player);
                SetPieceService.tick(player, board, now);
                LAST_BASE_LIFE_TICK.put(player.getUUID(), now);
            }
        } finally {
            CompanionPerformanceMonitor.record(player.getUUID(), PerformanceWorkType.TEAM_DIRECTOR, performanceStartedAt);
        }
    }

    private static void observeWorld(final ServerPlayer player, final long now) {
        final long timeOfDay = player.level().getDayTime() % 24000L;
        final long day = player.level().getDayTime() / 24000L;
        if (timeOfDay >= 12000L && timeOfDay <= 12200L && LAST_NIGHT_EVENT_DAY.getOrDefault(player.getUUID(), -1L) != day) {
            submit(player, TeamEventType.NIGHT_APPROACHING, List.of("NIGHT_APPROACHING"));
            LAST_NIGHT_EVENT_DAY.put(player.getUUID(), day);
        }
        final List<Monster> hostile = player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12.0D));
        if (hostile.size() >= 4) {
            submit(player, TeamEventType.HOSTILE_CROWD, List.of("HOSTILE_CROWD_" + hostile.size()));
        }
        final TeamAnchor home = com.riftcompanions.server.TeamSavedData.get(player.server).blackboard(player.getUUID())
                .anchor(BaseAnchorType.HOME).orElse(null);
        if (home != null && home.dimension().equals(player.level().dimension().location())
                && player.blockPosition().distSqr(home.position()) <= 24.0D * 24.0D
                && hostile.stream().anyMatch(Creeper.class::isInstance)) {
            submit(player, TeamEventType.EXPLOSIVE_NEAR_PROTECTED_AREA, List.of("CREEPER_NEAR_HOME_ANCHOR"));
        }
    }

    private static void observeEnvironment(final ServerPlayer player, final long now) {
        final Long last = LAST_ENVIRONMENT_TICK.get(player.getUUID());
        if (last != null && now - last < 100L) {
            return;
        }
        LAST_ENVIRONMENT_TICK.put(player.getUUID(), now);
        final EnvironmentRiskAssessment assessment = EnvironmentRiskService.assess(player);
        final var profile = com.riftcompanions.encounter.EncounterProfileRegistry.biomeProfile(player.serverLevel(), player.blockPosition());
        final EnvironmentRiskAssessment.Band previous = LAST_ENVIRONMENT_BAND.put(player.getUUID(), assessment.band());
        if (previous == assessment.band()) {
            return;
        }
        if (assessment.band().ordinal() >= EnvironmentRiskAssessment.Band.DANGEROUS.ordinal() || previous == null) {
            final List<String> reasons = new ArrayList<>(assessment.reasonCodes());
            reasons.add("BIOME_" + assessment.biomeId());
            reasons.add("ENCOUNTER_" + profile.id().toUpperCase(java.util.Locale.ROOT));
            reasons.add("RISK_" + assessment.band());
            submit(player, TeamEventType.BIOME_RISK_CHANGED, reasons);
        }
    }

    private static void processOne(final ServerPlayer player) {
        final TeamEvent event = queue(player.getUUID()).takeHighest(player.level().getGameTime()).orElse(null);
        if (event == null) {
            return;
        }
        DecisionTraceService.log(player, "EVENT", event.type() + " " + event.reasonCodes());
        com.riftcompanions.decision.TeamDecisionService.observeEvent(player, event.type());
        ActionInterruptService.applyForEvent(player, event.type());
        switch (event.type()) {
            case PLAYER_HEALTH_CRITICAL -> {
                if (TeamPlanService.beginRetreat(player, "EVENT_PLAYER_HEALTH_CRITICAL")) {
                    CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                            .ifPresent(companion -> DialogueService.get().speak(companion, "player_health_critical", 0));
                }
            }
            case PLAYER_FALL_RISK -> CompanionLifecycleService.findForOwner(player, CompanionRole.GIFTED)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "rescue_start", 0));
            case COMPANION_DOWNED -> {
                CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                        .ifPresent(companion -> {
                            companion.setCompanionState(CompanionState.GUARDING, "EVENT_COMPANION_DOWNED_GUARD");
                            DialogueService.get().speak(companion, "team_rescue", 0);
                        });
            }
            case EXPLOSIVE_NEAR_PROTECTED_AREA -> CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "creeper_near", 0));
            case HOSTILE_CROWD -> {
                if (com.riftcompanions.config.CompanionConfig.AUTONOMY_PROFILE.get() == com.riftcompanions.policy.AutonomyProfile.CAREFUL) {
                    CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                            .ifPresent(companion -> DialogueService.get().speak(companion, "guard_order", 1));
                } else {
                    TeamPlanService.proposeDefend(player, event.reasonCodes());
                }
            }
            case ROUTE_BLOCKED -> CompanionLifecycleService.findForOwner(player, CompanionRole.SCOUT)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "route_blocked", 1));
            case PLAN_CANCELLED -> CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "plan_cancelled", 2));
            case POWER_UNAVAILABLE -> CompanionLifecycleService.findForOwner(player, CompanionRole.GIFTED)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "power_low", 2));
            case UNKNOWN_MOB_FIRST_SEEN -> CompanionLifecycleService.findForOwner(player, CompanionRole.SEER)
                    .ifPresent(companion -> DialogueService.get().speak(companion,
                            event.reasonCodes().contains("HIVE_TAG_DETECTED") ? "anomaly_found" : "unknown_threat", 2));
            case HIVE_LINKED_TARGET_DETECTED -> CompanionLifecycleService.findForOwner(player, CompanionRole.SEER)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "anomaly_found", 2));
            case STRUCTURE_REQUESTED -> {
                final StructurePerimeterAssessment assessment = PENDING_STRUCTURE.remove(player.getUUID());
                if (assessment != null) {
                    TeamPlanService.proposeStructureEntry(player, assessment);
                }
            }
            case NIGHT_APPROACHING -> CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "night_risk", 2));
            case BIOME_RISK_CHANGED -> CompanionLifecycleService.findForOwner(player, CompanionRole.SEER)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "anomaly_found", 3));
            case BASE_RETURN_AFTER_CRISIS -> CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "base_return", 3));
            case MEMORY_LANDMARK_REVISITED -> CompanionLifecycleService.findForOwner(player, CompanionRole.SEER)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "memory_revisit", 3));
        }
    }

    public static void clearSession(final UUID owner) {
        QUEUES.remove(owner);
        LAST_FORMATION_UPDATE.remove(owner);
        LAST_NIGHT_EVENT_DAY.remove(owner);
        LAST_ENVIRONMENT_TICK.remove(owner);
        LAST_BASE_LIFE_TICK.remove(owner);
        LAST_SOCIAL_TICK.remove(owner);
        LAST_ENVIRONMENT_BAND.remove(owner);
        PENDING_STRUCTURE.remove(owner);
        ThreatResponseDirector.clearOwner(owner);
        com.riftcompanions.context.ContextualInteractionDirector.clearOwner(owner);
        com.riftcompanions.navigation.CompanionNavigationService.clearOwner(owner);
        com.riftcompanions.spatial.SpatialEtiquetteService.clear(owner);
    }

    public static int pendingEvents(final UUID owner) {
        final TeamEventQueue queue = owner == null ? null : QUEUES.get(owner);
        return queue == null ? 0 : queue.size();
    }

    private static TeamEventQueue queue(final UUID owner) {
        return QUEUES.computeIfAbsent(owner, ignored -> new TeamEventQueue());
    }
}
