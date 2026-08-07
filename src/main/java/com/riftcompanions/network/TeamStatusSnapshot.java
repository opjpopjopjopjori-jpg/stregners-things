package com.riftcompanions.network;

import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.DownedStatus;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionLifecycle;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.TeamPlan;
import com.riftcompanions.team.TeamPlanStatus;
import com.riftcompanions.team.TeamPlanType;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.TeamAnchor;
import com.riftcompanions.duo.DuoDynamicsService;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.arc.ArcService;
import com.riftcompanions.story.StoryChapter;
import com.riftcompanions.story.StoryNodeStatus;
import com.riftcompanions.story.MysteryClue;
import com.riftcompanions.story.StoryPromise;
import com.riftcompanions.story.PromiseStatus;
import com.riftcompanions.resource.CompanionResourceProfile;
import com.riftcompanions.resource.TeamSupplyState;
import com.riftcompanions.policy.PlayerPolicyService;
import com.riftcompanions.power.PowerPolicy;
import com.riftcompanions.navigation.CompanionNavigationService;
import com.riftcompanions.navigation.NavigationDebugSnapshot;
import com.riftcompanions.performance.CompanionPerformanceMonitor;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.onboarding.OnboardingHint;
import com.riftcompanions.mode.VanillaRulePolicyService;
import com.riftcompanions.mode.VanillaRuleSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Small owner-only projection of server data for the HUD/Journal. It contains
 * no world scanning data, inventory, or hidden chest data. Opt-in developer
 * fields may show only the owner's current companion path targets.
 */
public record TeamStatusSnapshot(
        List<CompanionView> companions,
        TeamPlanType planType,
        TeamPlanStatus planStatus,
        FormationType formation,
        String storySummary,
        String duoSummary,
        String intentionSummary,
        String playerIdentitySummary,
        String consequenceSummary,
        String perceptionSummary,
        String encounterSummary,
        boolean safeModeActive,
        String safeModeReason,
        String safeModeDetail,
        List<PolicyView> powerPolicies,
        String planObjective,
        String planA,
        String planB,
        String abortCondition,
        boolean planAwaitingApproval,
        int dangerScore,
        List<String> reasonCodes,
        boolean hasSafeWaypoint,
        long safeWaypoint,
        boolean hasHomeAnchor,
        long homeAnchor,
        String homeAnchorDimension,
        List<MemoryView> recentMemories,
        String giftedReadinessSummary,
        String guardianReviewSummary,
        DeveloperSummary developerSummary,
        List<DeveloperView> developerViews,
        GuidanceView guidance,
        StoryJournalView storyJournal,
        TeamSupplyView teamSupply,
        SocialView social
) {
    private static final int MAX_REASONS = 8;
    private static final int MAX_MEMORIES = 8;
    private static final int MAX_DEBUG_VIEWS = 4;
    private static final int MAX_STORY_CLUES = 4;

    public static TeamStatusSnapshot fromServer(final ServerPlayer player) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final List<CompanionView> companions = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            final var live = CompanionLifecycleService.findForOwner(player, role);
            if (live.isPresent()) {
                final CompanionEntity companion = live.get();
                final CompanionLifecycle lifecycle = companion.getCompanionState() == CompanionState.DOWNED
                        ? CompanionLifecycle.DOWNED : CompanionLifecycle.ACTIVE;
                companions.add(new CompanionView(role, lifecycle, companion.getCompanionState(), companion.getVisualAction(),
                        companion.getId(), companion.getEnergy(), companion.getHiveStrain(), companion.getFocus(), companion.getDownedStatus(), companion.getRescueProgress(), companion.getPersonalInventory().occupiedSlots(), board.relation(role).trust(), companion.isCombatEnabled(), truncate(companion.getLastReasonCode(), 96)));
            } else {
                final TeamSavedData.RosterEntry rosterEntry = data.entry(player.getUUID(), role).orElse(null);
                final CompanionLifecycle lifecycle = rosterEntry == null ? CompanionLifecycle.UNAVAILABLE : rosterEntry.lifecycle();
                final String reason = lifecycle == CompanionLifecycle.UNAVAILABLE ? "NOT_UNLOCKED"
                        : lifecycle == CompanionLifecycle.RESTING && rosterEntry != null && rosterEntry.hasRestingSnapshot()
                        ? "RESTING_PERSONAL_STATE_PRESERVED" : "NOT_LOADED";
                companions.add(new CompanionView(role, lifecycle, CompanionState.IDLE, CompanionAction.NONE,
                        -1, 0.0F, 0.0F, 0.0F, DownedStatus.STABLE, 0, 0, board.relation(role).trust(), false, reason));
            }
        }
        final TeamPlan plan = board.plan();
        final List<String> reasons = board.lastReasonCodes().stream().limit(MAX_REASONS).map(reason -> truncate(reason, 96)).toList();
        final BlockPos waypoint = board.lastSafeWaypoint();
        final TeamAnchor homeAnchor = board.anchor(BaseAnchorType.HOME).orElse(null);
        final List<MemoryRecord> memories = board.memories();
        final List<MemoryView> view = new ArrayList<>();
        for (int i = Math.max(0, memories.size() - MAX_MEMORIES); i < memories.size(); i++) {
            final MemoryRecord memory = memories.get(i);
            view.add(new MemoryView(memory.type().name(), memory.day(), truncate(memory.summary(), 160), memory.confidence()));
        }
        final List<PolicyView> policies = new ArrayList<>();
        for (final CompanionRole role : List.of(CompanionRole.SEER, CompanionRole.GIFTED, CompanionRole.SCOUT)) {
            final boolean overridden = board.playerPolicy().override(role).isPresent();
            policies.add(new PolicyView(role, PlayerPolicyService.effectivePowerPolicy(player, role), overridden));
        }
        final DeveloperSummary developer = developerSummary(player, board);
        final List<DeveloperView> developerViews = developer.enabled() ? developerViews(player) : List.of();
        final GuidanceView guidance = guidance(player, board);
        final StoryJournalView storyJournal = storyJournal(board);
        final TeamSupplyView teamSupply = teamSupply(player, board);
        final SocialView social = socialView(player);
        return new TeamStatusSnapshot(List.copyOf(companions), plan.type(), plan.status(), board.formation(),
                compactStory(board), truncate(DuoDynamicsService.statusSummary(player), 220), truncate(IntentionService.summary(board), 220),
                truncate(identitySummary(player, board), 120), truncate(consequenceSummary(board), 160), truncate(perceptionSummary(board, player.level().getGameTime()), 160), truncate(encounterSummary(board), 160), board.safeMode().enabled(), board.safeMode().reason().name(), truncate(board.safeMode().detail(), 160), List.copyOf(policies),
                truncate(plan.objective(), 180), truncate(plan.planA(), 160), truncate(plan.planB(), 160),
                truncate(plan.abortCondition(), 160), plan.awaitsApproval(), board.dangerScore(), reasons,
                waypoint != null, waypoint == null ? 0L : waypoint.asLong(), homeAnchor != null,
                homeAnchor == null ? 0L : homeAnchor.position().asLong(), homeAnchor == null ? "" : homeAnchor.dimension().toString(), List.copyOf(view),
                truncate(board.giftedBehavior().summary(), 120), truncate(board.guardianReview().summary(), 180), developer, List.copyOf(developerViews), guidance, storyJournal, teamSupply, social);
    }

    public static TeamStatusSnapshot empty() {
        final List<CompanionView> rows = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            rows.add(new CompanionView(role, CompanionLifecycle.UNAVAILABLE, CompanionState.IDLE, CompanionAction.NONE, -1, 0.0F, 0.0F, 0.0F, DownedStatus.STABLE, 0, 0, 20, false, "WAITING_FOR_SERVER"));
        }
        return new TeamStatusSnapshot(List.copyOf(rows), TeamPlanType.NONE, TeamPlanStatus.SUCCEEDED, FormationType.FOLLOW,
                "", "", "", "", "", "", "", false, "", "", List.of(), "", "", "", "", false, 0, List.of(), false, 0L, false, 0L, "", List.of(),
                "WAITING_FOR_SERVER", "No after-action review has been recorded.", DeveloperSummary.disabled(), List.of(), GuidanceView.waiting(), StoryJournalView.waiting(), TeamSupplyView.waiting(), SocialView.waiting());
    }

    public void write(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(companions.size());
        for (final CompanionView view : companions) {
            view.write(buffer);
        }
        buffer.writeEnum(planType);
        buffer.writeEnum(planStatus);
        buffer.writeEnum(formation);
        buffer.writeUtf(storySummary, 240);
        buffer.writeUtf(duoSummary, 220);
        buffer.writeUtf(intentionSummary, 220);
        buffer.writeUtf(playerIdentitySummary, 120);
        buffer.writeUtf(consequenceSummary, 160);
        buffer.writeUtf(perceptionSummary, 160);
        buffer.writeUtf(encounterSummary, 160);
        buffer.writeBoolean(safeModeActive);
        buffer.writeUtf(safeModeReason, 64);
        buffer.writeUtf(safeModeDetail, 160);
        buffer.writeVarInt(powerPolicies.size());
        for (final PolicyView policy : powerPolicies) policy.write(buffer);
        buffer.writeUtf(planObjective, 180);
        buffer.writeUtf(planA, 160);
        buffer.writeUtf(planB, 160);
        buffer.writeUtf(abortCondition, 160);
        buffer.writeBoolean(planAwaitingApproval);
        buffer.writeVarInt(dangerScore);
        buffer.writeVarInt(reasonCodes.size());
        for (final String reason : reasonCodes) {
            buffer.writeUtf(reason, 96);
        }
        buffer.writeBoolean(hasSafeWaypoint);
        if (hasSafeWaypoint) {
            buffer.writeLong(safeWaypoint);
        }
        buffer.writeBoolean(hasHomeAnchor);
        if (hasHomeAnchor) {
            buffer.writeLong(homeAnchor);
            buffer.writeUtf(homeAnchorDimension, 96);
        }
        buffer.writeVarInt(recentMemories.size());
        for (final MemoryView memory : recentMemories) {
            memory.write(buffer);
        }
        buffer.writeUtf(giftedReadinessSummary, 120);
        buffer.writeUtf(guardianReviewSummary, 180);
        developerSummary.write(buffer);
        buffer.writeVarInt(Math.min(MAX_DEBUG_VIEWS, developerViews.size()));
        for (int i = 0; i < developerViews.size() && i < MAX_DEBUG_VIEWS; i++) {
            developerViews.get(i).write(buffer);
        }
        guidance.write(buffer);
        storyJournal.write(buffer);
        teamSupply.write(buffer);
        social.write(buffer);
    }

    public static TeamStatusSnapshot read(final FriendlyByteBuf buffer) {
        final int companionCount = Math.min(4, Math.max(0, buffer.readVarInt()));
        final List<CompanionView> companions = new ArrayList<>();
        for (int i = 0; i < companionCount; i++) {
            companions.add(CompanionView.read(buffer));
        }
        final TeamPlanType planType = buffer.readEnum(TeamPlanType.class);
        final TeamPlanStatus planStatus = buffer.readEnum(TeamPlanStatus.class);
        final FormationType formation = buffer.readEnum(FormationType.class);
        final String storySummary = buffer.readUtf(240);
        final String duoSummary = buffer.readUtf(220);
        final String intentionSummary = buffer.readUtf(220);
        final String playerIdentitySummary = buffer.readUtf(120);
        final String consequenceSummary = buffer.readUtf(160);
        final String perceptionSummary = buffer.readUtf(160);
        final String encounterSummary = buffer.readUtf(160);
        final boolean safeModeActive = buffer.readBoolean();
        final String safeModeReason = buffer.readUtf(64);
        final String safeModeDetail = buffer.readUtf(160);
        final int policyCount = Math.min(3, Math.max(0, buffer.readVarInt()));
        final List<PolicyView> powerPolicies = new ArrayList<>();
        for (int i = 0; i < policyCount; i++) powerPolicies.add(PolicyView.read(buffer));
        final String planObjective = buffer.readUtf(180);
        final String planA = buffer.readUtf(160);
        final String planB = buffer.readUtf(160);
        final String abortCondition = buffer.readUtf(160);
        final boolean awaitingApproval = buffer.readBoolean();
        final int danger = Math.min(100, Math.max(0, buffer.readVarInt()));
        final int reasonCount = Math.min(MAX_REASONS, Math.max(0, buffer.readVarInt()));
        final List<String> reasons = new ArrayList<>();
        for (int i = 0; i < reasonCount; i++) {
            reasons.add(buffer.readUtf(96));
        }
        final boolean hasWaypoint = buffer.readBoolean();
        final long waypoint = hasWaypoint ? buffer.readLong() : 0L;
        final boolean hasHomeAnchor = buffer.readBoolean();
        final long homeAnchor = hasHomeAnchor ? buffer.readLong() : 0L;
        final String homeDimension = hasHomeAnchor ? buffer.readUtf(96) : "";
        final int memoryCount = Math.min(MAX_MEMORIES, Math.max(0, buffer.readVarInt()));
        final List<MemoryView> memories = new ArrayList<>();
        for (int i = 0; i < memoryCount; i++) {
            memories.add(MemoryView.read(buffer));
        }
        final String giftedReadiness = buffer.readUtf(120);
        final String guardianReview = buffer.readUtf(180);
        final DeveloperSummary developer = DeveloperSummary.read(buffer);
        final int debugViewCount = Math.min(MAX_DEBUG_VIEWS, Math.max(0, buffer.readVarInt()));
        final List<DeveloperView> developerViews = new ArrayList<>();
        for (int i = 0; i < debugViewCount; i++) developerViews.add(DeveloperView.read(buffer));
        final GuidanceView guidance = GuidanceView.read(buffer);
        final StoryJournalView storyJournal = StoryJournalView.read(buffer);
        final TeamSupplyView teamSupply = TeamSupplyView.read(buffer);
        final SocialView social = SocialView.read(buffer);
        return new TeamStatusSnapshot(List.copyOf(companions), planType, planStatus, formation,
                storySummary, duoSummary, intentionSummary, playerIdentitySummary, consequenceSummary, perceptionSummary, encounterSummary, safeModeActive, safeModeReason, safeModeDetail, List.copyOf(powerPolicies),
                planObjective, planA, planB, abortCondition, awaitingApproval, danger, List.copyOf(reasons), hasWaypoint, waypoint,
                hasHomeAnchor, homeAnchor, homeDimension, List.copyOf(memories), giftedReadiness, guardianReview, developer, List.copyOf(developerViews), guidance, storyJournal, teamSupply, social);
    }

    /** Owner-only social trace; it excludes dialogue body, block position, inventory, and hidden-world details. */
    private static SocialView socialView(final ServerPlayer player) {
        final var status = com.riftcompanions.social.CompanionSocialDirector.status(player.getUUID(), player.level().getGameTime());
        return new SocialView(status.enabled(), truncate(status.cue(), 32), truncate(status.phase(), 32),
                status.lead(), status.reply(), Math.max(0L, status.pendingReplyTicks()), truncate(status.summary(), 160));
    }

    private static String consequenceSummary(final TeamBlackboard board) {
        final var consequences = board.consequences();
        return consequences.isEmpty() ? "No major player consequence recorded." : consequences.get(consequences.size() - 1).summary();
    }

    private static String perceptionSummary(final TeamBlackboard board, final long now) {
        return board.perceptions().stream().filter(signal -> signal.activeAt(now)).reduce((first, second) -> second)
                .map(com.riftcompanions.perception.PerceptionSignal::summary).orElse("No active local perception signal.");
    }

    private static String encounterSummary(final TeamBlackboard board) {
        final var context = board.encounterContext();
        return "Encounter: " + context.profileId() + " | Risk " + context.riskScore() + " | Formation " + context.recommendedFormation();
    }

    private static GuidanceView guidance(final ServerPlayer player, final TeamBlackboard board) {
        final VanillaRuleSnapshot rules = VanillaRulePolicyService.snapshot(player);
        final OnboardingHint next = board.onboarding().recommendedHint().orElse(null);
        final String mode = rules.playerSpectator() ? "SPECTATOR"
                : rules.playerCreative() ? "CREATIVE"
                : rules.peaceful() ? "PEACEFUL" : "SURVIVAL";
        return new GuidanceView(mode, rules.difficulty().name(), rules.mobGriefing(), rules.keepInventory(),
                rules.daylightCycle(), rules.hardcore(), rules.companionWorldEditsAllowed(),
                com.riftcompanions.config.CompanionConfig.TUTORIAL_ENABLED.get(), board.onboarding().dismissed(),
                board.onboarding().deliveredCount(), board.onboarding().totalHints(),
                next == null ? "complete" : next.id(), next == null ? "Guidance Complete" : next.title(),
                truncate(next == null ? "All optional guidance moments have been delivered. Use the Guide tab whenever you need a reminder." : next.detail(), 180),
                truncate(rules.operationalSummary(), 180));
    }

    private static DeveloperSummary developerSummary(final ServerPlayer player, final TeamBlackboard board) {
        final CompanionPerformanceMonitor.Snapshot performance = CompanionPerformanceMonitor.snapshot(player.getUUID());
        final DialogueService.DialogueDebugSnapshot chat = DialogueService.get().debugSnapshot(player.getUUID(), player.level().getGameTime());
        return new DeveloperSummary(performance.enabled(), TeamDirector.pendingEvents(player.getUUID()), board.memories().size(), 128,
                truncate(chat.trigger(), 64), truncate(chat.outcome(), 96), chat.normalBudgetRemainingTicks(),
                performance.sampledTotalMicros(), performance.averageTotalMicros(),
                performance.latestCompanionMicros(), performance.averageCompanionMicros(),
                performance.latestNavigationMicros(), performance.averageNavigationMicros(),
                performance.latestDirectorMicros(), performance.averageDirectorMicros(),
                performance.latestDialogueMicros(), performance.averageDialogueMicros());
    }

    private static List<DeveloperView> developerViews(final ServerPlayer player) {
        final List<DeveloperView> views = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion == null) continue;
            final NavigationDebugSnapshot navigation = CompanionNavigationService.debugSnapshot(companion.getUUID());
            views.add(new DeveloperView(role, companion.getCompanionState().name(),
                    targetText(navigation.requestedTarget()), targetText(navigation.selectedTarget()),
                    navigation.intent(), navigation.outcome(), truncate(navigation.reasonCode(), 96), navigation.recoveryAttempts()));
        }
        return views;
    }

    private static String targetText(final BlockPos position) {
        if (position == null) return "none";
        return position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private static String identitySummary(final ServerPlayer player, final TeamBlackboard board) {
        final var identity = board.playerIdentity();
        final String label = identity.nicknameEnabled() ? identity.approvedNickname() : player.getGameProfile().getName();
        return "Player: " + label + " | Style: " + identity.playStyle();
    }

    /**
     * Safe Team Supply summary. It intentionally excludes container type, slots,
     * item stacks, bound coordinates, and any discovery behavior.
     */
    private static TeamSupplyView teamSupply(final ServerPlayer player, final TeamBlackboard board) {
        final TeamSupplyState supply = board.teamSupply();
        final long day = player.level().getDayTime() / 24000L;
        final boolean enabled = com.riftcompanions.config.CompanionConfig.TEAM_SUPPLY_ENABLED.get();
        final boolean atSafeBase = board.isAtSafeBase(player);
        final String availability = !enabled ? "Team Supply is disabled in world settings."
                : !supply.isBound() ? "No Team Supply container is bound."
                : !atSafeBase ? "Team Supply is bound but requires a safe HOME or REST anchor."
                : "Team Supply is bound; withdrawals remain role-whitelisted and capped.";
        final List<TeamSupplyRoleView> roles = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            final String categories = CompanionResourceProfile.allowedAutoCategories(role).stream()
                    .map(category -> category.name()).sorted().reduce((first, second) -> first + ", " + second).orElse("NONE");
            roles.add(new TeamSupplyRoleView(role, supply.withdrawnTodayReadOnly(role, day),
                    CompanionResourceProfile.dailyTeamSupplyCap(role), categories));
        }
        return new TeamSupplyView(enabled, supply.isBound(), atSafeBase, availability, List.copyOf(roles));
    }

    /** Bounded owner-only story projection; no coordinates, raw clue IDs, or hidden-world data cross this boundary. */
    private static StoryJournalView storyJournal(final TeamBlackboard board) {
        final List<StoryChapterView> chapters = new ArrayList<>();
        for (final StoryChapter chapter : StoryChapter.values()) {
            final StoryNodeStatus status = board.story().status(chapter);
            chapters.add(new StoryChapterView(status == StoryNodeStatus.LOCKED ? "Undiscovered Chapter" : title(chapter), status));
        }
        final List<MysteryClue> clues = board.story().mysteryBoard().clues();
        final List<StoryClueView> visibleClues = new ArrayList<>();
        for (int index = Math.max(0, clues.size() - MAX_STORY_CLUES); index < clues.size(); index++) {
            final MysteryClue clue = clues.get(index);
            visibleClues.add(new StoryClueView(truncate(clue.summary(), 160), clue.confidence().name(), clue.day(), clue.deferred()));
        }
        final StoryPromise promise = board.story().promises().active().orElse(null);
        final StoryPromiseView promiseView = promise == null ? StoryPromiseView.none()
                : new StoryPromiseView(true, promise.role(), promise.status(), truncate(promise.summary(), 160), promise.createdDay());
        return new StoryJournalView(List.copyOf(chapters), List.copyOf(visibleClues), promiseView,
                clues.size(), board.story().promises().promises().size());
    }

    private static String compactStory(final TeamBlackboard board) {
        for (final StoryChapter chapter : StoryChapter.values()) {
            if (board.story().status(chapter) == StoryNodeStatus.ACTIVE) return "Optional story: " + title(chapter) + " is active.";
        }
        for (final StoryChapter chapter : StoryChapter.values()) {
            if (board.story().status(chapter) == StoryNodeStatus.AVAILABLE) return "Optional story available: " + title(chapter) + ".";
        }
        final var promise = board.story().promises().active().orElse(null);
        if (promise != null) return "Optional promise: " + promise.role().personalName() + " — " + promise.summary();
        final var clue = board.story().mysteryBoard().latest().orElse(null);
        if (clue != null) return "Mystery clue: " + clue.confidence() + " — " + clue.summary();
        return ArcService.summary(board);
    }

    private static String title(final StoryChapter chapter) {
        return chapter.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private static String truncate(final String value, final int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record CompanionView(
            CompanionRole role,
            CompanionLifecycle lifecycle,
            CompanionState state,
            CompanionAction action,
            int entityId,
            float energy,
            float hiveStrain,
            float focus,
            DownedStatus downedStatus,
            int rescueProgress,
            int inventoryUsed,
            int trust,
            boolean combatEnabled,
            String reason
    ) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeEnum(role);
            buffer.writeEnum(lifecycle);
            buffer.writeEnum(state);
            buffer.writeEnum(action);
            buffer.writeVarInt(entityId + 1); // -1 is encoded as zero
            buffer.writeFloat(energy);
            buffer.writeFloat(hiveStrain);
            buffer.writeFloat(focus);
            buffer.writeEnum(downedStatus);
            buffer.writeVarInt(rescueProgress);
            buffer.writeVarInt(inventoryUsed);
            buffer.writeVarInt(trust);
            buffer.writeBoolean(combatEnabled);
            buffer.writeUtf(reason, 96);
        }

        private static CompanionView read(final FriendlyByteBuf buffer) {
            return new CompanionView(buffer.readEnum(CompanionRole.class), buffer.readEnum(CompanionLifecycle.class),
                    buffer.readEnum(CompanionState.class), buffer.readEnum(CompanionAction.class),
                    buffer.readVarInt() - 1, buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readEnum(DownedStatus.class),
                    Math.min(60, Math.max(0, buffer.readVarInt())), Math.min(6, Math.max(0, buffer.readVarInt())), Math.min(100, Math.max(0, buffer.readVarInt())), buffer.readBoolean(), buffer.readUtf(96));
        }
    }

    public record PolicyView(CompanionRole role, PowerPolicy policy, boolean overridden) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeEnum(role);
            buffer.writeEnum(policy);
            buffer.writeBoolean(overridden);
        }

        private static PolicyView read(final FriendlyByteBuf buffer) {
            return new PolicyView(buffer.readEnum(CompanionRole.class), buffer.readEnum(PowerPolicy.class), buffer.readBoolean());
        }
    }

    public record MemoryView(String type, long day, String summary, int confidence) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeUtf(type, 48);
            buffer.writeVarLong(day);
            buffer.writeUtf(summary, 160);
            buffer.writeVarInt(confidence);
        }

        private static MemoryView read(final FriendlyByteBuf buffer) {
            return new MemoryView(buffer.readUtf(48), buffer.readVarLong(), buffer.readUtf(160), Math.min(100, Math.max(0, buffer.readVarInt())));
        }
    }

    /** One role cap projection; category names do not expose container contents. */
    public record TeamSupplyRoleView(CompanionRole role, int usedToday, int dailyCap, String allowedCategories) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeEnum(role == null ? CompanionRole.GUARDIAN : role);
            buffer.writeVarInt(Math.max(0, usedToday));
            buffer.writeVarInt(Math.max(0, dailyCap));
            buffer.writeUtf(allowedCategories == null ? "" : allowedCategories, 128);
        }

        private static TeamSupplyRoleView read(final FriendlyByteBuf buffer) {
            return new TeamSupplyRoleView(buffer.readEnum(CompanionRole.class), Math.max(0, buffer.readVarInt()),
                    Math.max(0, buffer.readVarInt()), buffer.readUtf(128));
        }
    }

    /** No container position, slot, stack, or raw inventory data is sent to the client. */
    public record TeamSupplyView(boolean enabled, boolean bound, boolean atSafeBase, String availability,
                                 List<TeamSupplyRoleView> roles) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeBoolean(enabled);
            buffer.writeBoolean(bound);
            buffer.writeBoolean(atSafeBase);
            buffer.writeUtf(availability == null ? "" : availability, 160);
            buffer.writeVarInt(Math.min(CompanionRole.values().length, roles.size()));
            for (int index = 0; index < roles.size() && index < CompanionRole.values().length; index++) roles.get(index).write(buffer);
        }

        private static TeamSupplyView read(final FriendlyByteBuf buffer) {
            final boolean enabled = buffer.readBoolean();
            final boolean bound = buffer.readBoolean();
            final boolean atSafeBase = buffer.readBoolean();
            final String availability = buffer.readUtf(160);
            final int count = Math.min(CompanionRole.values().length, Math.max(0, buffer.readVarInt()));
            final List<TeamSupplyRoleView> roles = new ArrayList<>();
            for (int index = 0; index < count; index++) roles.add(TeamSupplyRoleView.read(buffer));
            return new TeamSupplyView(enabled, bound, atSafeBase, availability, List.copyOf(roles));
        }

        public static TeamSupplyView waiting() {
            return new TeamSupplyView(false, false, false, "Waiting for the logical server.", List.of());
        }
    }

    /** Bounded social director projection; never carries dialogue text or world coordinates. */
    public record SocialView(boolean enabled, String cue, String phase, CompanionRole lead, CompanionRole reply,
                             long pendingReplyTicks, String summary) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeBoolean(enabled);
            buffer.writeUtf(cue == null ? "" : cue, 32);
            buffer.writeUtf(phase == null ? "" : phase, 32);
            buffer.writeBoolean(lead != null);
            if (lead != null) buffer.writeEnum(lead);
            buffer.writeBoolean(reply != null);
            if (reply != null) buffer.writeEnum(reply);
            buffer.writeVarLong(Math.max(0L, pendingReplyTicks));
            buffer.writeUtf(summary == null ? "" : summary, 160);
        }

        private static SocialView read(final FriendlyByteBuf buffer) {
            final boolean enabled = buffer.readBoolean();
            final String cue = buffer.readUtf(32);
            final String phase = buffer.readUtf(32);
            final CompanionRole lead = buffer.readBoolean() ? buffer.readEnum(CompanionRole.class) : null;
            final CompanionRole reply = buffer.readBoolean() ? buffer.readEnum(CompanionRole.class) : null;
            return new SocialView(enabled, cue, phase, lead, reply, Math.max(0L, buffer.readVarLong()), buffer.readUtf(160));
        }

        public static SocialView waiting() {
            return new SocialView(false, "NONE", "WAITING", null, null, 0L, "Waiting for social director status.");
        }
    }

    /** Compact player-facing chapter progress; chapter titles never reveal undiscovered world locations. */
    public record StoryChapterView(String title, StoryNodeStatus status) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeUtf(title == null ? "" : title, 64);
            buffer.writeEnum(status == null ? StoryNodeStatus.LOCKED : status);
        }

        private static StoryChapterView read(final FriendlyByteBuf buffer) {
            return new StoryChapterView(buffer.readUtf(64), buffer.readEnum(StoryNodeStatus.class));
        }
    }

    /** Visible clue projection excludes internal IDs and any coordinates. */
    public record StoryClueView(String summary, String confidence, long day, boolean deferred) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeUtf(summary == null ? "" : summary, 160);
            buffer.writeUtf(confidence == null ? "" : confidence, 32);
            buffer.writeVarLong(Math.max(0L, day));
            buffer.writeBoolean(deferred);
        }

        private static StoryClueView read(final FriendlyByteBuf buffer) {
            return new StoryClueView(buffer.readUtf(160), buffer.readUtf(32), Math.max(0L, buffer.readVarLong()), buffer.readBoolean());
        }
    }

    /** One active optional promise only; terminal history stays in bounded memory. */
    public record StoryPromiseView(boolean present, CompanionRole role, PromiseStatus status, String summary, long createdDay) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeBoolean(present);
            if (!present) return;
            buffer.writeEnum(role == null ? CompanionRole.GUARDIAN : role);
            buffer.writeEnum(status == null ? PromiseStatus.OFFERED : status);
            buffer.writeUtf(summary == null ? "" : summary, 160);
            buffer.writeVarLong(Math.max(0L, createdDay));
        }

        private static StoryPromiseView read(final FriendlyByteBuf buffer) {
            if (!buffer.readBoolean()) return none();
            return new StoryPromiseView(true, buffer.readEnum(CompanionRole.class), buffer.readEnum(PromiseStatus.class),
                    buffer.readUtf(160), Math.max(0L, buffer.readVarLong()));
        }

        public static StoryPromiseView none() {
            return new StoryPromiseView(false, CompanionRole.GUARDIAN, PromiseStatus.OFFERED, "", 0L);
        }
    }

    /** Bounded Journal payload; it cannot reveal map coordinates, hidden structures, or future events. */
    public record StoryJournalView(
            List<StoryChapterView> chapters,
            List<StoryClueView> clues,
            StoryPromiseView activePromise,
            int totalClueCount,
            int totalPromiseCount
    ) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeVarInt(Math.min(StoryChapter.values().length, chapters.size()));
            for (int index = 0; index < chapters.size() && index < StoryChapter.values().length; index++) chapters.get(index).write(buffer);
            buffer.writeVarInt(Math.min(MAX_STORY_CLUES, clues.size()));
            for (int index = 0; index < clues.size() && index < MAX_STORY_CLUES; index++) clues.get(index).write(buffer);
            (activePromise == null ? StoryPromiseView.none() : activePromise).write(buffer);
            buffer.writeVarInt(Math.max(0, totalClueCount));
            buffer.writeVarInt(Math.max(0, totalPromiseCount));
        }

        private static StoryJournalView read(final FriendlyByteBuf buffer) {
            final int chapterCount = Math.min(StoryChapter.values().length, Math.max(0, buffer.readVarInt()));
            final List<StoryChapterView> chapters = new ArrayList<>();
            for (int index = 0; index < chapterCount; index++) chapters.add(StoryChapterView.read(buffer));
            final int clueCount = Math.min(MAX_STORY_CLUES, Math.max(0, buffer.readVarInt()));
            final List<StoryClueView> clues = new ArrayList<>();
            for (int index = 0; index < clueCount; index++) clues.add(StoryClueView.read(buffer));
            return new StoryJournalView(List.copyOf(chapters), List.copyOf(clues), StoryPromiseView.read(buffer),
                    Math.max(0, buffer.readVarInt()), Math.max(0, buffer.readVarInt()));
        }

        public static StoryJournalView waiting() {
            return new StoryJournalView(List.of(), List.of(), StoryPromiseView.none(), 0, 0);
        }
    }

    /** Owner-only world-rule and optional tutorial projection for the Guide tab. */
    public record GuidanceView(
            String mode,
            String difficulty,
            boolean mobGriefing,
            boolean keepInventory,
            boolean daylightCycle,
            boolean hardcore,
            boolean companionWorldEditsAllowed,
            boolean tutorialConfigEnabled,
            boolean tutorialDismissed,
            int completedHints,
            int totalHints,
            String nextHintId,
            String nextHintTitle,
            String nextHintDetail,
            String operationalSummary
    ) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeUtf(mode == null ? "" : mode, 24);
            buffer.writeUtf(difficulty == null ? "" : difficulty, 24);
            buffer.writeBoolean(mobGriefing);
            buffer.writeBoolean(keepInventory);
            buffer.writeBoolean(daylightCycle);
            buffer.writeBoolean(hardcore);
            buffer.writeBoolean(companionWorldEditsAllowed);
            buffer.writeBoolean(tutorialConfigEnabled);
            buffer.writeBoolean(tutorialDismissed);
            buffer.writeVarInt(Math.max(0, completedHints));
            buffer.writeVarInt(Math.max(1, totalHints));
            buffer.writeUtf(nextHintId == null ? "" : nextHintId, 64);
            buffer.writeUtf(nextHintTitle == null ? "" : nextHintTitle, 64);
            buffer.writeUtf(nextHintDetail == null ? "" : nextHintDetail, 180);
            buffer.writeUtf(operationalSummary == null ? "" : operationalSummary, 180);
        }

        private static GuidanceView read(final FriendlyByteBuf buffer) {
            return new GuidanceView(buffer.readUtf(24), buffer.readUtf(24), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    Math.max(0, buffer.readVarInt()), Math.max(1, buffer.readVarInt()), buffer.readUtf(64), buffer.readUtf(64),
                    buffer.readUtf(180), buffer.readUtf(180));
        }

        public static GuidanceView waiting() {
            return new GuidanceView("WAITING", "", false, false, true, false, false, false, false,
                    0, OnboardingHint.values().length, "waiting", "Guidance", "Waiting for the logical server.", "Waiting for world-rule data.");
        }
    }

    /** Owner-only developer summary; all values are bounded and display-only. */
    public record DeveloperSummary(
            boolean enabled,
            int pendingEvents,
            int memoryUsed,
            int memoryLimit,
            String chatTrigger,
            String chatOutcome,
            long chatBudgetRemainingTicks,
            long sampledWorkMicros,
            long averageWorkMicros,
            long companionWorkMicros,
            long companionAverageMicros,
            long navigationWorkMicros,
            long navigationAverageMicros,
            long directorWorkMicros,
            long directorAverageMicros,
            long dialogueWorkMicros,
            long dialogueAverageMicros
    ) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeBoolean(enabled);
            if (!enabled) return;
            buffer.writeVarInt(Math.max(0, pendingEvents));
            buffer.writeVarInt(Math.max(0, memoryUsed));
            buffer.writeVarInt(Math.max(1, memoryLimit));
            buffer.writeUtf(chatTrigger == null ? "" : chatTrigger, 64);
            buffer.writeUtf(chatOutcome == null ? "" : chatOutcome, 96);
            buffer.writeVarLong(Math.max(0L, chatBudgetRemainingTicks));
            buffer.writeVarLong(Math.max(0L, sampledWorkMicros));
            buffer.writeVarLong(Math.max(0L, averageWorkMicros));
            buffer.writeVarLong(Math.max(0L, companionWorkMicros));
            buffer.writeVarLong(Math.max(0L, companionAverageMicros));
            buffer.writeVarLong(Math.max(0L, navigationWorkMicros));
            buffer.writeVarLong(Math.max(0L, navigationAverageMicros));
            buffer.writeVarLong(Math.max(0L, directorWorkMicros));
            buffer.writeVarLong(Math.max(0L, directorAverageMicros));
            buffer.writeVarLong(Math.max(0L, dialogueWorkMicros));
            buffer.writeVarLong(Math.max(0L, dialogueAverageMicros));
        }

        private static DeveloperSummary read(final FriendlyByteBuf buffer) {
            final boolean enabled = buffer.readBoolean();
            if (!enabled) return disabled();
            return new DeveloperSummary(true, Math.max(0, buffer.readVarInt()), Math.max(0, buffer.readVarInt()),
                    Math.max(1, buffer.readVarInt()), buffer.readUtf(64), buffer.readUtf(96),
                    Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()),
                    Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()),
                    Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()),
                    Math.max(0L, buffer.readVarLong()), Math.max(0L, buffer.readVarLong()));
        }

        public static DeveloperSummary disabled() {
            return new DeveloperSummary(false, 0, 0, 128, "", "NO_DEVELOPER_DATA", 0L,
                    0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }

    /** Current bounded path/task projection for one live companion. */
    public record DeveloperView(
            CompanionRole role,
            String task,
            String requestedTarget,
            String selectedTarget,
            String navigationIntent,
            String navigationOutcome,
            String navigationReason,
            int recoveryAttempts
    ) {
        private void write(final FriendlyByteBuf buffer) {
            buffer.writeEnum(role);
            buffer.writeUtf(task == null ? "" : task, 48);
            buffer.writeUtf(requestedTarget == null ? "" : requestedTarget, 48);
            buffer.writeUtf(selectedTarget == null ? "" : selectedTarget, 48);
            buffer.writeUtf(navigationIntent == null ? "" : navigationIntent, 48);
            buffer.writeUtf(navigationOutcome == null ? "" : navigationOutcome, 48);
            buffer.writeUtf(navigationReason == null ? "" : navigationReason, 96);
            buffer.writeVarInt(Math.max(0, recoveryAttempts));
        }

        private static DeveloperView read(final FriendlyByteBuf buffer) {
            return new DeveloperView(buffer.readEnum(CompanionRole.class), buffer.readUtf(48), buffer.readUtf(48),
                    buffer.readUtf(48), buffer.readUtf(48), buffer.readUtf(48), buffer.readUtf(96),
                    Math.max(0, buffer.readVarInt()));
        }
    }
}
