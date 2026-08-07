package com.riftcompanions.team;

import com.riftcompanions.doctrine.TeamDoctrine;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.relationship.CompanionRelation;
import com.riftcompanions.story.StoryState;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.ThreatKnowledgeRecord;
import com.riftcompanions.memory.ThreatObservationKind;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.TeamAnchor;
import com.riftcompanions.world.WorldAnnotation;
import com.riftcompanions.world.WorldAnnotationType;
import com.riftcompanions.transaction.ActionLedger;
import com.riftcompanions.transaction.ReservationBook;
import com.riftcompanions.transaction.ReservationType;
import com.riftcompanions.safety.SafeModeState;
import com.riftcompanions.safety.CompanionFaultRecord;
import com.riftcompanions.arc.ArcState;
import com.riftcompanions.duo.TeamPair;
import com.riftcompanions.intention.CompanionIntention;
import com.riftcompanions.persistence.SaveVersions;
import com.riftcompanions.policy.PlayerPolicyState;
import com.riftcompanions.identity.PlayerIdentityState;
import com.riftcompanions.consequence.ConsequenceRecord;
import com.riftcompanions.perception.PerceptionSignal;
import com.riftcompanions.scene.SceneState;
import com.riftcompanions.playtest.PlaytestTelemetryState;
import com.riftcompanions.resource.TeamSupplyState;
import com.riftcompanions.mental.MindAnchorState;
import com.riftcompanions.encounter.EncounterContextState;
import com.riftcompanions.behavior.WillAwarenessState;
import com.riftcompanions.behavior.GiftedBehaviorState;
import com.riftcompanions.behavior.GuardianReviewState;
import com.riftcompanions.onboarding.OnboardingState;
import com.riftcompanions.decision.TeamDecisionState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Shared team facts with a strict memory budget. It does not grant omniscience. */
public final class TeamBlackboard {
    private static final int MEMORY_LIMIT = 128;
    private static final int THREAT_KNOWLEDGE_LIMIT = 64;
    private static final int ANNOTATION_LIMIT = 64;
    private static final int FAULT_RECORD_LIMIT = 8;

    private TeamPlan plan = TeamPlan.none();
    private FormationType formation = FormationType.FOLLOW;
    private java.util.UUID focusTargetUuid;
    private long focusTargetExpiresAt;
    private java.util.UUID rescueTargetUuid;
    private long rescueReservationExpiresAt;
    private long firstHomeDay = -1L;
    private long lastTemporalDay = -1L;
    private long quietUntilGameTime;
    private BlockPos lastSafeWaypoint;
    private int dangerScore;
    private List<String> lastReasonCodes = new ArrayList<>();
    private final List<MemoryRecord> memories = new ArrayList<>();
    private final EnumMap<BaseAnchorType, TeamAnchor> anchors = new EnumMap<>(BaseAnchorType.class);
    private final LinkedHashMap<String, ThreatKnowledgeRecord> threatKnowledge = new LinkedHashMap<>();
    private final List<WorldAnnotation> annotations = new ArrayList<>();
    private final EnumSet<TeamDoctrine> doctrines = EnumSet.noneOf(TeamDoctrine.class);
    private final EnumMap<TeamDoctrine, Integer> doctrineCandidates = new EnumMap<>(TeamDoctrine.class);
    private final EnumMap<CompanionRole, CompanionRelation> relations = new EnumMap<>(CompanionRole.class);
    private StoryState story = new StoryState();
    private TeamPlanType lastPlanType = TeamPlanType.NONE;
    private long lastPlanSuccessAt;

    // Reliability state is owned by the team board, not by a rendering client
    // or a particular entity. It survives an integrated-server restart.
    private ActionLedger actionLedger = new ActionLedger();
    private ReservationBook reservations = new ReservationBook();
    private SafeModeState safeMode = new SafeModeState();
    private ArcState arcs = new ArcState();
    private TeamPair requestedPair;
    private final LinkedHashMap<String, Long> recentSynergies = new LinkedHashMap<>();
    private final List<CompanionIntention> intentions = new ArrayList<>();
    private long lastIntentionOfferDay = -1L;
    private CompanionRole lastDailyAmbientRole;
    private boolean firstSafeReturnRecorded;
    private boolean firstPlanFromHomeRecorded;
    private final LinkedHashMap<String, CompanionFaultRecord> faultRecords = new LinkedHashMap<>();
    private PlayerPolicyState playerPolicy = new PlayerPolicyState();
    private PlayerIdentityState playerIdentity = new PlayerIdentityState();
    private final List<ConsequenceRecord> consequences = new ArrayList<>();
    private final List<PerceptionSignal> perceptions = new ArrayList<>();
    private SceneState sceneState = new SceneState();
    private PlaytestTelemetryState playtestTelemetry = new PlaytestTelemetryState();
    private TeamSupplyState teamSupply = new TeamSupplyState();
    private MindAnchorState mindAnchor = new MindAnchorState();
    private EncounterContextState encounterContext = new EncounterContextState();
    private WillAwarenessState willAwareness = new WillAwarenessState();
    private GiftedBehaviorState giftedBehavior = new GiftedBehaviorState();
    private GuardianReviewState guardianReview = new GuardianReviewState();
    private OnboardingState onboarding = new OnboardingState();
    private TeamDecisionState decisionState = new TeamDecisionState();

    public TeamPlan plan() { return plan; }
    public FormationType formation() { return formation; }
    public int dangerScore() { return dangerScore; }
    public TeamCondition condition() { return TeamCondition.fromDangerScore(dangerScore); }
    public BlockPos lastSafeWaypoint() { return lastSafeWaypoint; }
    public List<String> lastReasonCodes() { return List.copyOf(lastReasonCodes); }
    public List<MemoryRecord> memories() { return List.copyOf(memories); }
    public Map<BaseAnchorType, TeamAnchor> anchors() { return Map.copyOf(anchors); }
    public ActionLedger actionLedger() { return actionLedger; }
    public ReservationBook reservations() { return reservations; }
    public SafeModeState safeMode() { return safeMode; }
    public ArcState arcs() { return arcs; }
    public Optional<TeamPair> requestedPair() { return Optional.ofNullable(requestedPair); }
    public List<CompanionIntention> intentions() { return List.copyOf(intentions); }
    public long lastIntentionOfferDay() { return lastIntentionOfferDay; }
    public Optional<CompanionRole> lastDailyAmbientRole() { return Optional.ofNullable(lastDailyAmbientRole); }
    public Map<String, CompanionFaultRecord> faultRecords() { return Map.copyOf(faultRecords); }
    public boolean firstSafeReturnRecorded() { return firstSafeReturnRecorded; }
    public boolean firstPlanFromHomeRecorded() { return firstPlanFromHomeRecorded; }
    public PlayerPolicyState playerPolicy() { return playerPolicy; }
    public PlayerIdentityState playerIdentity() { return playerIdentity; }
    public List<ConsequenceRecord> consequences() { return List.copyOf(consequences); }
    public List<PerceptionSignal> perceptions() { return List.copyOf(perceptions); }
    public SceneState sceneState() { return sceneState; }
    public PlaytestTelemetryState playtestTelemetry() { return playtestTelemetry; }
    public TeamSupplyState teamSupply() { return teamSupply; }
    public MindAnchorState mindAnchor() { return mindAnchor; }
    public EncounterContextState encounterContext() { return encounterContext; }
    public WillAwarenessState willAwareness() { return willAwareness; }
    public GiftedBehaviorState giftedBehavior() { return giftedBehavior; }
    public GuardianReviewState guardianReview() { return guardianReview; }
    public OnboardingState onboarding() { return onboarding; }
    public TeamDecisionState decisionState() { return decisionState; }

    public void setRequestedPair(final TeamPair pair) { this.requestedPair = pair; }
    public void setLastIntentionOfferDay(final long day) { this.lastIntentionOfferDay = Math.max(-1L, day); }
    public void setLastDailyAmbientRole(final CompanionRole role) { this.lastDailyAmbientRole = role; }

    /** Replaces same-key local evidence and returns true only when it is a new perception. */
    public boolean publishPerception(final PerceptionSignal signal, final long now) {
        if (signal == null || signal.key().isBlank()) return false;
        expirePerceptions(now);
        for (int i = 0; i < perceptions.size(); i++) {
            if (perceptions.get(i).key().equals(signal.key())) {
                perceptions.set(i, signal);
                return false;
            }
        }
        if (perceptions.size() >= 24) perceptions.remove(0);
        perceptions.add(signal);
        return true;
    }

    public void expirePerceptions(final long now) {
        perceptions.removeIf(signal -> !signal.activeAt(now));
    }

    /** Keeps one consequence per stable key and never treats a missing choice as a penalty. */
    public boolean recordConsequence(final ConsequenceRecord record) {
        if (record == null || record.key().isBlank()) return false;
        if (consequences.stream().anyMatch(existing -> existing.key().equals(record.key()))) return false;
        if (consequences.size() >= 32) consequences.remove(0);
        consequences.add(record);
        return true;
    }

    public boolean recordFirstSafeReturn() {
        if (firstSafeReturnRecorded) return false;
        firstSafeReturnRecorded = true;
        return true;
    }

    public boolean recordFirstPlanFromHome() {
        if (firstPlanFromHomeRecorded) return false;
        firstPlanFromHomeRecorded = true;
        return true;
    }

    /** Records only bounded circuit-breaker evidence, never a full throwable or player data. */
    public int recordFault(final CompanionRole role, final String identity, final long now, final String code, final long windowTicks) {
        if (role == null) return 0;
        final String key = role.id() + "|" + (identity == null ? "unknown" : identity);
        final CompanionFaultRecord existing = faultRecords.get(key);
        final CompanionFaultRecord next = existing == null ? CompanionFaultRecord.first(now, code) : existing.observe(now, code, windowTicks);
        if (existing == null && faultRecords.size() >= FAULT_RECORD_LIMIT) {
            faultRecords.remove(faultRecords.keySet().iterator().next());
        }
        faultRecords.put(key, next);
        return next.count();
    }

    public boolean tryRecordSynergy(final String key, final long now, final long cooldown) {
        if (key == null || key.isBlank()) return false;
        final long previous = recentSynergies.getOrDefault(key, Long.MIN_VALUE);
        if (previous != Long.MIN_VALUE && now - previous < Math.max(20L, cooldown)) return false;
        recentSynergies.put(key, now);
        while (recentSynergies.size() > 16) recentSynergies.remove(recentSynergies.keySet().iterator().next());
        return true;
    }

    public Optional<CompanionIntention> openIntention() {
        return intentions.stream().filter(value -> value.status().open()).findFirst();
    }

    public void offerIntention(final CompanionIntention intention) {
        if (intention == null || openIntention().isPresent()) return;
        if (intentions.size() >= 8) {
            final int terminal = java.util.stream.IntStream.range(0, intentions.size())
                    .filter(index -> !intentions.get(index).status().open()).findFirst().orElse(0);
            intentions.remove(terminal);
        }
        intentions.add(intention);
    }

    public boolean hasCompletedIntention(final String definitionId) {
        return intentions.stream().anyMatch(value -> value.definitionId().equals(definitionId)
                && value.status() == com.riftcompanions.intention.IntentionStatus.COMPLETED);
    }

    /** Safe local base test used only for optional content offers, never pathfinding. */
    public boolean isAtSafeBase(final ServerPlayer player) {
        if (player == null) return false;
        final TeamAnchor home = anchor(BaseAnchorType.HOME).orElse(null);
        final TeamAnchor rest = anchor(BaseAnchorType.REST).orElse(null);
        final boolean nearHome = home != null && home.dimension().equals(player.level().dimension().location())
                && player.blockPosition().distSqr(home.position()) <= 26.0D * 26.0D;
        final boolean nearRest = rest != null && rest.dimension().equals(player.level().dimension().location())
                && player.blockPosition().distSqr(rest.position()) <= 12.0D * 12.0D;
        return (nearHome || nearRest) && player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12.0D)).isEmpty();
    }

    public Optional<TeamAnchor> anchor(final BaseAnchorType type) {
        return Optional.ofNullable(anchors.get(type));
    }

    public void setAnchor(final BaseAnchorType type, final TeamAnchor anchor) {
        if (type != null && anchor != null) {
            anchors.put(type, anchor);
        }
    }

    public Optional<TeamAnchor> removeAnchor(final BaseAnchorType type) {
        return Optional.ofNullable(anchors.remove(type));
    }

    public Map<String, ThreatKnowledgeRecord> threatKnowledge() {
        return Map.copyOf(threatKnowledge);
    }

    public List<WorldAnnotation> annotations() {
        return List.copyOf(annotations);
    }

    public void addAnnotation(final WorldAnnotation annotation) {
        annotations.removeIf(existing -> existing.type() == annotation.type() && existing.dimension().equals(annotation.dimension())
                && existing.position().distSqr(annotation.position()) <= 1.0D);
        if (annotations.size() >= ANNOTATION_LIMIT) {
            annotations.remove(0);
        }
        annotations.add(annotation);
    }

    public WorldAnnotation removeNearestAnnotation(final net.minecraft.resources.ResourceLocation dimension, final BlockPos around, final double radius) {
        WorldAnnotation nearest = null;
        double best = radius * radius;
        for (final WorldAnnotation annotation : annotations) {
            if (!annotation.dimension().equals(dimension)) {
                continue;
            }
            final double distance = annotation.position().distSqr(around);
            if (distance <= best) {
                nearest = annotation;
                best = distance;
            }
        }
        if (nearest != null) {
            annotations.remove(nearest);
        }
        return nearest;
    }

    public boolean hasAvoidanceAnnotation(final net.minecraft.resources.ResourceLocation dimension, final BlockPos position, final double radius) {
        final double range = radius * radius;
        return annotations.stream().anyMatch(annotation -> annotation.dimension().equals(dimension)
                && (annotation.type() == WorldAnnotationType.MACHINE_NO_GO || annotation.type() == WorldAnnotationType.PROTECTED || annotation.type() == WorldAnnotationType.DANGER)
                && annotation.position().distSqr(position) <= range);
    }

    public boolean hasSensitiveAnnotation(final net.minecraft.resources.ResourceLocation dimension, final BlockPos position, final double radius) {
        final double range = radius * radius;
        return annotations.stream().anyMatch(annotation -> annotation.dimension().equals(dimension)
                && (annotation.type() == WorldAnnotationType.MACHINE_NO_GO || annotation.type() == WorldAnnotationType.PROTECTED)
                && annotation.position().distSqr(position) <= range);
    }

    public java.util.Set<TeamDoctrine> doctrines() {
        return java.util.Set.copyOf(doctrines);
    }

    public boolean hasDoctrine(final TeamDoctrine doctrine) {
        return doctrines.contains(doctrine);
    }

    public int doctrineCandidateCount(final TeamDoctrine doctrine) {
        return doctrineCandidates.getOrDefault(doctrine, 0);
    }

    public int incrementDoctrineCandidate(final TeamDoctrine doctrine) {
        final int next = Math.min(10, doctrineCandidates.getOrDefault(doctrine, 0) + 1);
        doctrineCandidates.put(doctrine, next);
        return next;
    }

    public boolean acceptDoctrine(final TeamDoctrine doctrine) {
        if (doctrineCandidateCount(doctrine) < 3) {
            return false;
        }
        doctrines.add(doctrine);
        doctrineCandidates.remove(doctrine);
        return true;
    }

    public void declineDoctrine(final TeamDoctrine doctrine) {
        doctrineCandidates.remove(doctrine);
    }

    public StoryState story() {
        return story;
    }

    public boolean recentlySucceededPlan(final TeamPlanType type, final long now, final long cooldown) {
        return lastPlanType == type && now - lastPlanSuccessAt < cooldown;
    }

    public void recordPlanSuccess(final TeamPlanType type, final long now) {
        lastPlanType = type;
        lastPlanSuccessAt = now;
    }

    public CompanionRelation relation(final CompanionRole role) {
        return relations.computeIfAbsent(role, ignored -> CompanionRelation.initial());
    }

    public void adjustRelation(final CompanionRole role, final int delta, final boolean milestone) {
        relations.put(role, relation(role).adjust(delta, milestone));
    }

    /** Adds a fact only from a real, local observation; never from hidden mob internals. */
    public ThreatObservationResult observeThreat(final String entityTypeId, final ThreatObservationKind kind, final long day) {
        final ThreatKnowledgeRecord previous = threatKnowledge.get(entityTypeId);
        if (previous == null && threatKnowledge.size() >= THREAT_KNOWLEDGE_LIMIT) {
            final String eldest = threatKnowledge.keySet().iterator().next();
            threatKnowledge.remove(eldest);
        }
        final ThreatKnowledgeRecord baseline = previous == null ? ThreatKnowledgeRecord.first(entityTypeId, day) : previous;
        final ThreatKnowledgeRecord updated = baseline.observe(kind, day);
        threatKnowledge.put(entityTypeId, updated);
        final boolean stageAdvanced = previous != null && previous.stage() != updated.stage();
        return new ThreatObservationResult(updated, previous == null, stageAdvanced);
    }

    public void setFormation(final FormationType formation) {
        this.formation = formation == null ? FormationType.FOLLOW : formation;
    }

    public java.util.UUID focusTargetUuid() {
        return focusTargetUuid;
    }

    public boolean hasActiveFocusTarget(final long now) {
        return focusTargetUuid != null && now < focusTargetExpiresAt;
    }

    public void setFocusTarget(final java.util.UUID target, final long expiresAt) {
        this.focusTargetUuid = target;
        this.focusTargetExpiresAt = expiresAt;
    }

    public void clearFocusTarget() {
        this.focusTargetUuid = null;
        this.focusTargetExpiresAt = 0L;
    }

    public boolean claimRescue(final UUID target, final long now, final long duration) {
        return claimRescue(target, UUID.randomUUID(), now, duration);
    }

    public boolean claimRescue(final UUID target, final UUID actionId, final long now, final long duration) {
        if (target == null || actionId == null) return false;
        if (rescueTargetUuid != null && now < rescueReservationExpiresAt && !rescueTargetUuid.equals(target)) return false;
        if (!reservations.claim(ReservationType.RESCUE_TARGET, target.toString(), actionId, now, duration)) return false;
        rescueTargetUuid = target;
        rescueReservationExpiresAt = now + Math.max(20L, duration);
        return true;
    }

    public void releaseRescue(final UUID target) {
        if (target == null || target.equals(rescueTargetUuid)) {
            if (rescueTargetUuid != null) reservations.release(ReservationType.RESCUE_TARGET, rescueTargetUuid.toString());
            rescueTargetUuid = null;
            rescueReservationExpiresAt = 0L;
        }
    }

    /** Safe Mode/recovery can clear all reservations; no reservation is a persistent entitlement. */
    public void clearReservations() {
        reservations.clear();
        rescueTargetUuid = null;
        rescueReservationExpiresAt = 0L;
    }

    public long firstHomeDay() {
        return firstHomeDay;
    }

    public void registerHomeDay(final long day) {
        if (firstHomeDay < 0L) {
            firstHomeDay = day;
        }
    }

    public long lastTemporalDay() {
        return lastTemporalDay;
    }

    public void setLastTemporalDay(final long day) {
        lastTemporalDay = day;
    }

    public void beginQuietWindow(final long now, final long duration) {
        quietUntilGameTime = Math.max(quietUntilGameTime, now + Math.max(0L, duration));
    }

    public boolean isQuiet(final long now) {
        return now < quietUntilGameTime;
    }

    public boolean startPlan(final TeamPlan newPlan) {
        if (newPlan == null) return false;
        if (this.plan.actionId().equals(newPlan.actionId())) return false;
        this.plan = newPlan;
        this.lastReasonCodes = new ArrayList<>(newPlan.reasonCodes());
        return true;
    }

    public boolean completePlan(final TeamPlanStatus status) {
        return this.plan.finish(status);
    }

    public void setLastSafeWaypoint(final BlockPos waypoint) {
        this.lastSafeWaypoint = waypoint == null ? null : waypoint.immutable();
    }

    public void updateThreat(final int dangerScore, final List<String> reasons) {
        this.dangerScore = Math.max(0, Math.min(100, dangerScore));
        this.lastReasonCodes = new ArrayList<>(reasons.stream().limit(8).toList());
    }

    public void addMemory(final MemoryRecord memory) {
        if (memory.summary().isBlank()) {
            return;
        }
        if (memories.size() >= MEMORY_LIMIT) {
            memories.remove(0);
        }
        memories.add(memory);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("BlackboardDataVersion", SaveVersions.BLACKBOARD_DATA);
        tag.putInt("MemoryDataVersion", SaveVersions.MEMORY_DATA);
        tag.putInt("JournalDataVersion", SaveVersions.JOURNAL_DATA);
        tag.putInt("DoctrineDataVersion", SaveVersions.DOCTRINE_DATA);
        tag.put("Plan", plan.save());
        tag.putString("Formation", formation.name());
        if (focusTargetUuid != null) {
            tag.putUUID("FocusTarget", focusTargetUuid);
            tag.putLong("FocusTargetExpires", focusTargetExpiresAt);
        }
        if (rescueTargetUuid != null) {
            tag.putUUID("RescueTarget", rescueTargetUuid);
            tag.putLong("RescueReservationExpires", rescueReservationExpiresAt);
        }
        tag.putLong("FirstHomeDay", firstHomeDay);
        tag.putLong("LastTemporalDay", lastTemporalDay);
        tag.putLong("QuietUntil", quietUntilGameTime);
        if (lastSafeWaypoint != null) {
            tag.putLong("LastSafeWaypoint", lastSafeWaypoint.asLong());
        }
        tag.putInt("DangerScore", dangerScore);
        final ListTag reasons = new ListTag();
        lastReasonCodes.stream().limit(8).forEach(reason -> reasons.add(net.minecraft.nbt.StringTag.valueOf(reason)));
        tag.put("Reasons", reasons);
        final ListTag memoryTags = new ListTag();
        memories.forEach(memory -> memoryTags.add(memory.save()));
        tag.put("Memories", memoryTags);
        final ListTag anchorTags = new ListTag();
        anchors.forEach((type, anchor) -> {
            final CompoundTag anchorTag = anchor.save();
            anchorTag.putString("Type", type.name());
            anchorTags.add(anchorTag);
        });
        tag.put("Anchors", anchorTags);
        final ListTag threatTags = new ListTag();
        threatKnowledge.values().forEach(record -> threatTags.add(record.save()));
        tag.put("ThreatKnowledge", threatTags);
        final ListTag annotationTags = new ListTag();
        annotations.forEach(annotation -> annotationTags.add(annotation.save()));
        tag.put("Annotations", annotationTags);
        final ListTag doctrineTags = new ListTag();
        doctrines.forEach(doctrine -> doctrineTags.add(net.minecraft.nbt.StringTag.valueOf(doctrine.name())));
        tag.put("Doctrines", doctrineTags);
        final ListTag candidateTags = new ListTag();
        doctrineCandidates.forEach((doctrine, count) -> {
            final CompoundTag candidate = new CompoundTag();
            candidate.putString("Doctrine", doctrine.name());
            candidate.putInt("Count", count);
            candidateTags.add(candidate);
        });
        tag.put("DoctrineCandidates", candidateTags);
        final ListTag relationTags = new ListTag();
        relations.forEach((role, relation) -> {
            final CompoundTag relationTag = relation.save();
            relationTag.putString("Role", role.name());
            relationTags.add(relationTag);
        });
        tag.put("Relations", relationTags);
        tag.put("Story", story.save());
        tag.putString("LastPlanType", lastPlanType.name());
        tag.putLong("LastPlanSuccessAt", lastPlanSuccessAt);
        tag.put("ActionLedger", actionLedger.save());
        tag.put("Reservations", reservations.save());
        tag.put("SafeMode", safeMode.save());
        tag.put("Arcs", arcs.save());
        if (requestedPair != null) tag.putString("RequestedPair", requestedPair.name());
        final ListTag synergyTags = new ListTag();
        recentSynergies.forEach((key, time) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putString("Key", key);
            entry.putLong("Time", time);
            synergyTags.add(entry);
        });
        tag.put("RecentSynergies", synergyTags);
        final ListTag intentionTags = new ListTag();
        intentions.forEach(value -> intentionTags.add(value.save()));
        tag.put("Intentions", intentionTags);
        tag.putLong("LastIntentionOfferDay", lastIntentionOfferDay);
        if (lastDailyAmbientRole != null) tag.putString("LastDailyAmbientRole", lastDailyAmbientRole.name());
        tag.putBoolean("FirstSafeReturnRecorded", firstSafeReturnRecorded);
        tag.putBoolean("FirstPlanFromHomeRecorded", firstPlanFromHomeRecorded);
        final ListTag faultTags = new ListTag();
        faultRecords.forEach((key, record) -> {
            final CompoundTag entry = record.save();
            entry.putString("Key", key);
            faultTags.add(entry);
        });
        tag.put("FaultRecords", faultTags);
        tag.put("PlayerPolicy", playerPolicy.save());
        tag.put("PlayerIdentity", playerIdentity.save());
        final ListTag consequenceTags = new ListTag();
        consequences.forEach(record -> consequenceTags.add(record.save()));
        tag.put("Consequences", consequenceTags);
        final ListTag perceptionTags = new ListTag();
        perceptions.forEach(signal -> perceptionTags.add(signal.save()));
        tag.put("Perceptions", perceptionTags);
        tag.put("SceneState", sceneState.save());
        tag.put("PlaytestTelemetry", playtestTelemetry.save());
        tag.put("TeamSupply", teamSupply.save());
        tag.put("MindAnchor", mindAnchor.save());
        tag.put("EncounterContext", encounterContext.save());
        tag.put("WillAwareness", willAwareness.save());
        tag.put("GiftedBehavior", giftedBehavior.save());
        tag.put("GuardianReview", guardianReview.save());
        tag.put("Onboarding", onboarding.save());
        tag.put("DecisionState", decisionState.save());
        return tag;
    }

    public static TeamBlackboard load(final CompoundTag tag) {
        final TeamBlackboard board = new TeamBlackboard();
        if (tag.contains("Plan", Tag.TAG_COMPOUND)) {
            board.plan = TeamPlan.load(tag.getCompound("Plan"));
        }
        try {
            board.formation = tag.contains("Formation") ? FormationType.valueOf(tag.getString("Formation")) : FormationType.FOLLOW;
        } catch (final IllegalArgumentException ignored) {
            board.formation = FormationType.FOLLOW;
        }
        if (tag.hasUUID("FocusTarget")) {
            board.focusTargetUuid = tag.getUUID("FocusTarget");
            board.focusTargetExpiresAt = tag.getLong("FocusTargetExpires");
        }
        if (tag.hasUUID("RescueTarget")) {
            board.rescueTargetUuid = tag.getUUID("RescueTarget");
            board.rescueReservationExpiresAt = tag.getLong("RescueReservationExpires");
        }
        board.firstHomeDay = tag.contains("FirstHomeDay") ? tag.getLong("FirstHomeDay") : -1L;
        board.lastTemporalDay = tag.contains("LastTemporalDay") ? tag.getLong("LastTemporalDay") : -1L;
        board.quietUntilGameTime = tag.getLong("QuietUntil");
        if (tag.contains("LastSafeWaypoint")) {
            board.lastSafeWaypoint = BlockPos.of(tag.getLong("LastSafeWaypoint"));
        }
        board.dangerScore = Math.max(0, Math.min(100, tag.getInt("DangerScore")));
        final ListTag reasons = tag.getList("Reasons", Tag.TAG_STRING);
        for (int i = 0; i < reasons.size() && i < 8; i++) {
            board.lastReasonCodes.add(reasons.getString(i));
        }
        final ListTag memoryTags = tag.getList("Memories", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, memoryTags.size() - MEMORY_LIMIT); i < memoryTags.size(); i++) {
            board.memories.add(MemoryRecord.load(memoryTags.getCompound(i)));
        }
        final ListTag anchorTags = tag.getList("Anchors", Tag.TAG_COMPOUND);
        for (int i = 0; i < anchorTags.size(); i++) {
            final CompoundTag anchorTag = anchorTags.getCompound(i);
            try {
                final BaseAnchorType type = BaseAnchorType.valueOf(anchorTag.getString("Type"));
                final TeamAnchor anchor = TeamAnchor.load(anchorTag);
                if (anchor != null) {
                    board.anchors.put(type, anchor);
                }
            } catch (final IllegalArgumentException ignored) {
                // Invalid/old anchor data is discarded instead of blocking world load.
            }
        }
        final ListTag threatTags = tag.getList("ThreatKnowledge", Tag.TAG_COMPOUND);
        final int firstThreat = Math.max(0, threatTags.size() - THREAT_KNOWLEDGE_LIMIT);
        for (int i = firstThreat; i < threatTags.size(); i++) {
            final ThreatKnowledgeRecord record = ThreatKnowledgeRecord.load(threatTags.getCompound(i));
            if (!"invalid".equals(record.entityTypeId())) {
                board.threatKnowledge.put(record.entityTypeId(), record);
            }
        }
        final ListTag annotationTags = tag.getList("Annotations", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, annotationTags.size() - ANNOTATION_LIMIT); i < annotationTags.size(); i++) {
            final WorldAnnotation annotation = WorldAnnotation.load(annotationTags.getCompound(i));
            if (annotation != null) {
                board.annotations.add(annotation);
            }
        }
        final ListTag doctrineTags = tag.getList("Doctrines", Tag.TAG_STRING);
        for (int i = 0; i < doctrineTags.size(); i++) {
            try {
                board.doctrines.add(TeamDoctrine.valueOf(doctrineTags.getString(i)));
            } catch (final IllegalArgumentException ignored) { }
        }
        final ListTag candidateTags = tag.getList("DoctrineCandidates", Tag.TAG_COMPOUND);
        for (int i = 0; i < candidateTags.size(); i++) {
            final CompoundTag candidate = candidateTags.getCompound(i);
            try {
                board.doctrineCandidates.put(TeamDoctrine.valueOf(candidate.getString("Doctrine")), Math.max(0, Math.min(10, candidate.getInt("Count"))));
            } catch (final IllegalArgumentException ignored) { }
        }
        final ListTag relationTags = tag.getList("Relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < relationTags.size(); i++) {
            final CompoundTag relation = relationTags.getCompound(i);
            try {
                board.relations.put(CompanionRole.valueOf(relation.getString("Role")), CompanionRelation.load(relation));
            } catch (final IllegalArgumentException ignored) { }
        }
        if (tag.contains("Story", Tag.TAG_COMPOUND)) {
            board.story = StoryState.load(tag.getCompound("Story"));
        }
        try { board.lastPlanType = tag.contains("LastPlanType") ? TeamPlanType.valueOf(tag.getString("LastPlanType")) : TeamPlanType.NONE; }
        catch (IllegalArgumentException ignored) { board.lastPlanType = TeamPlanType.NONE; }
        board.lastPlanSuccessAt = tag.getLong("LastPlanSuccessAt");
        if (tag.contains("ActionLedger", Tag.TAG_COMPOUND)) board.actionLedger = ActionLedger.load(tag.getCompound("ActionLedger"));
        if (tag.contains("Reservations", Tag.TAG_COMPOUND)) board.reservations = ReservationBook.load(tag.getCompound("Reservations"));
        if (tag.contains("SafeMode", Tag.TAG_COMPOUND)) board.safeMode = SafeModeState.load(tag.getCompound("SafeMode"));
        if (tag.contains("Arcs", Tag.TAG_COMPOUND)) board.arcs = ArcState.load(tag.getCompound("Arcs"));
        try { board.requestedPair = tag.contains("RequestedPair") ? TeamPair.valueOf(tag.getString("RequestedPair")) : null; }
        catch (final IllegalArgumentException ignored) { board.requestedPair = null; }
        final ListTag synergyTags = tag.getList("RecentSynergies", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, synergyTags.size() - 16); i < synergyTags.size(); i++) {
            final CompoundTag entry = synergyTags.getCompound(i);
            final String key = entry.getString("Key");
            if (!key.isBlank()) board.recentSynergies.put(key, entry.getLong("Time"));
        }
        final ListTag intentionTags = tag.getList("Intentions", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, intentionTags.size() - 8); i < intentionTags.size(); i++) {
            CompanionIntention.load(intentionTags.getCompound(i)).ifPresent(board.intentions::add);
        }
        board.lastIntentionOfferDay = tag.contains("LastIntentionOfferDay") ? tag.getLong("LastIntentionOfferDay") : -1L;
        try { board.lastDailyAmbientRole = tag.contains("LastDailyAmbientRole") ? CompanionRole.valueOf(tag.getString("LastDailyAmbientRole")) : null; }
        catch (final IllegalArgumentException ignored) { board.lastDailyAmbientRole = null; }
        board.firstSafeReturnRecorded = tag.getBoolean("FirstSafeReturnRecorded");
        board.firstPlanFromHomeRecorded = tag.getBoolean("FirstPlanFromHomeRecorded");
        final ListTag faultTags = tag.getList("FaultRecords", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, faultTags.size() - FAULT_RECORD_LIMIT); i < faultTags.size(); i++) {
            final CompoundTag entry = faultTags.getCompound(i);
            final String key = entry.getString("Key");
            if (!key.isBlank()) board.faultRecords.put(key, CompanionFaultRecord.load(entry));
        }
        if (tag.contains("PlayerPolicy", Tag.TAG_COMPOUND)) board.playerPolicy = PlayerPolicyState.load(tag.getCompound("PlayerPolicy"));
        if (tag.contains("PlayerIdentity", Tag.TAG_COMPOUND)) board.playerIdentity = PlayerIdentityState.load(tag.getCompound("PlayerIdentity"));
        final ListTag consequenceTags = tag.getList("Consequences", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, consequenceTags.size() - 32); i < consequenceTags.size(); i++) {
            final ConsequenceRecord record = ConsequenceRecord.load(consequenceTags.getCompound(i));
            if (!record.key().isBlank() && !"invalid".equals(record.key())) board.consequences.add(record);
        }
        final ListTag perceptionTags = tag.getList("Perceptions", Tag.TAG_COMPOUND);
        for (int i = Math.max(0, perceptionTags.size() - 24); i < perceptionTags.size(); i++) {
            final PerceptionSignal signal = PerceptionSignal.load(perceptionTags.getCompound(i));
            if (!signal.key().isBlank() && !"invalid".equals(signal.key())) board.perceptions.add(signal);
        }
        if (tag.contains("SceneState", Tag.TAG_COMPOUND)) board.sceneState = SceneState.load(tag.getCompound("SceneState"));
        if (tag.contains("PlaytestTelemetry", Tag.TAG_COMPOUND)) board.playtestTelemetry = PlaytestTelemetryState.load(tag.getCompound("PlaytestTelemetry"));
        if (tag.contains("TeamSupply", Tag.TAG_COMPOUND)) board.teamSupply = TeamSupplyState.load(tag.getCompound("TeamSupply"));
        if (tag.contains("MindAnchor", Tag.TAG_COMPOUND)) board.mindAnchor = MindAnchorState.load(tag.getCompound("MindAnchor"));
        if (tag.contains("EncounterContext", Tag.TAG_COMPOUND)) board.encounterContext = EncounterContextState.load(tag.getCompound("EncounterContext"));
        if (tag.contains("WillAwareness", Tag.TAG_COMPOUND)) board.willAwareness = WillAwarenessState.load(tag.getCompound("WillAwareness"));
        if (tag.contains("GiftedBehavior", Tag.TAG_COMPOUND)) board.giftedBehavior = GiftedBehaviorState.load(tag.getCompound("GiftedBehavior"));
        if (tag.contains("GuardianReview", Tag.TAG_COMPOUND)) board.guardianReview = GuardianReviewState.load(tag.getCompound("GuardianReview"));
        if (tag.contains("Onboarding", Tag.TAG_COMPOUND)) board.onboarding = OnboardingState.load(tag.getCompound("Onboarding"));
        if (tag.contains("DecisionState", Tag.TAG_COMPOUND)) board.decisionState = TeamDecisionState.load(tag.getCompound("DecisionState"));
        return board;
    }

    public record ThreatObservationResult(ThreatKnowledgeRecord record, boolean firstEncounter, boolean stageAdvanced) {}
}
