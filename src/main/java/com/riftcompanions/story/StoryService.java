package com.riftcompanions.story;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.relationship.RelationService;
import com.riftcompanions.server.MilestoneTransactionService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.WorldAnnotationType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Optional story hooks, mystery board, and promises. No chapter blocks normal survival. */
public final class StoryService {
    private StoryService() {}

    public static void initialize(final ServerPlayer player) {
        TeamSavedData.get(player.server).blackboard(player.getUUID()).story();
    }

    public static void onFirstCompanionActive(final ServerPlayer player) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return;
        StoryState story = TeamSavedData.get(player.server).blackboard(player.getUUID()).story();
        if (story.status(StoryChapter.ARRIVAL) == StoryNodeStatus.AVAILABLE) {
            story.set(StoryChapter.ARRIVAL, StoryNodeStatus.ACTIVE);
            MilestoneTransactionService.recordUnlock(player, "guardian_companion");
        }
    }

    public static void onHomeEstablished(final ServerPlayer player) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return;
        TeamSavedData data=TeamSavedData.get(player.server);
        StoryState story=data.blackboard(player.getUUID()).story();
        if (story.complete(StoryChapter.ARRIVAL)) {
            story.set(StoryChapter.A_WAY_BACK, StoryNodeStatus.AVAILABLE);
            data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.MILESTONE, player.level().getGameTime()/24000L,
                    "Story: Arrival completed. A Way Back is available.", 90));
        }
        if (story.status(StoryChapter.A_WAY_BACK) == StoryNodeStatus.AVAILABLE) story.set(StoryChapter.A_WAY_BACK, StoryNodeStatus.ACTIVE);
        data.markChanged();
    }

    public static void onSafeRouteMarked(final ServerPlayer player) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return;
        TeamSavedData data=TeamSavedData.get(player.server);
        StoryState story=data.blackboard(player.getUUID()).story();
        if (story.complete(StoryChapter.A_WAY_BACK)) {
            story.set(StoryChapter.SIGNS_IN_THE_WORLD, StoryNodeStatus.AVAILABLE);
            data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.MILESTONE, player.level().getGameTime()/24000L,
                    "Story: A Way Back completed. Signs in the World is available.", 85));
            MilestoneTransactionService.recordUnlock(player, "seer_companion");
            MilestoneTransactionService.recordUnlock(player, "scout_companion");
        }
        data.markChanged();
    }

    public static void onHiveObserved(final ServerPlayer player) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return;
        TeamSavedData data=TeamSavedData.get(player.server);
        StoryState story=data.blackboard(player.getUUID()).story();
        addClue(player, "hive_echo_visible", "The team observed a visible Hive-linked signal. It remains evidence, not a map reveal.", MysteryClueConfidence.OBSERVED);
        if (story.isAtLeastAvailable(StoryChapter.SIGNS_IN_THE_WORLD)) {
            story.set(StoryChapter.THE_HIVE_ECHO, StoryNodeStatus.AVAILABLE);
            data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.ANOMALY_OBSERVATION, player.level().getGameTime()/24000L,
                    "Story: The Hive Echo is now available as an optional investigation.", 80));
            MilestoneTransactionService.recordUnlock(player, "gifted_companion");
            data.markChanged();
        }
    }

    /** An INVESTIGATE marker is player-authored evidence, never a hidden structure discovery. */
    public static void onInvestigationMarked(final ServerPlayer player, final BlockPos position) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH) || position == null) return;
        addClue(player, "investigate:" + player.level().dimension().location() + ":" + position.asLong(),
                "Player marked an investigation point at a visible location.", MysteryClueConfidence.OBSERVED);
        com.riftcompanions.behavior.RoleBehaviorService.observeWillEvidence(player, "investigate:" + position.asLong(), false);
    }

    private static void addClue(final ServerPlayer player, final String id, final String summary, final MysteryClueConfidence confidence) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final StoryState story = data.blackboard(player.getUUID()).story();
        if (story.mysteryBoard().add(new MysteryClue(id, summary, confidence, player.level().getDayTime() / 24000L, false))) {
            data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.ANOMALY_OBSERVATION,
                    player.level().getDayTime() / 24000L, summary, 76));
            data.markChanged();
        }
    }

    /** Offers only one optional promise at a time, and only from actual team/world facts. */
    public static void tickOptionalPromises(final ServerPlayer player) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH) || player == null || player.server == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        if (!board.isAtSafeBase(player)) return;
        final StoryPromiseState promises = board.story().promises();
        if (promises.active().isPresent()) return;
        final long day = player.level().getDayTime() / 24000L;
        StoryPromise offered = null;
        if (!promises.hasId("guardian_guard_post") && board.anchor(BaseAnchorType.GUARD_POST).isEmpty()) {
            offered = new StoryPromise("guardian_guard_post", CompanionRole.GUARDIAN,
                    "Guardian would like a Guard Post before the next risky trip.", "ANCHOR_GUARD_POST", day, PromiseStatus.OFFERED);
        } else if (!promises.hasId("seer_compare_clues") && board.story().mysteryBoard().observedOrBetterCount() >= 2) {
            offered = new StoryPromise("seer_compare_clues", CompanionRole.SEER,
                    "Seer would like to compare two observed clues before moving closer.", "ANNOTATION_INVESTIGATE", day, PromiseStatus.OFFERED);
        } else if (!promises.hasId("scout_safe_route") && board.annotations().stream().noneMatch(annotation -> annotation.type() == WorldAnnotationType.SAFE_ROUTE)) {
            offered = new StoryPromise("scout_safe_route", CompanionRole.SCOUT,
                    "Scout would like to mark one route the team can use to return.", "ANNOTATION_SAFE_ROUTE", day, PromiseStatus.OFFERED);
        } else if (!promises.hasId("gifted_protect_exit") && board.anchor(BaseAnchorType.ENTRY).isPresent()) {
            offered = new StoryPromise("gifted_protect_exit", CompanionRole.GIFTED,
                    "Gifted would like to test a protective exit plan at a player-chosen narrow point.", "ABILITY_GIFTED_SHIELD", day, PromiseStatus.OFFERED);
        }
        if (offered != null && promises.offer(offered)) {
            board.addMemory(new MemoryRecord(MemoryType.MILESTONE, day,
                    "Optional promise offered: " + offered.summary(), 72));
            data.markChanged();
        }
    }

    public static PromiseResult acceptPromise(final ServerPlayer player) {
        return changePromise(player, true);
    }

    public static PromiseResult deferPromise(final ServerPlayer player) {
        return changePromise(player, false);
    }

    private static PromiseResult changePromise(final ServerPlayer player, final boolean accept) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return PromiseResult.failure("STORY_GRAPH_DISABLED", "Optional story hooks are disabled by the world feature flag.");
        final TeamSavedData data = TeamSavedData.get(player.server);
        final StoryPromise promise = data.blackboard(player.getUUID()).story().promises().active().orElse(null);
        if (promise == null) return PromiseResult.failure("NO_OPEN_PROMISE", "No optional companion promise is available right now.");
        final boolean changed = accept ? promise.accept() : promise.defer();
        if (!changed) return PromiseResult.failure("PROMISE_STATE_REJECTED", "That optional promise cannot change state right now.");
        data.markChanged();
        return PromiseResult.success(accept ? "PROMISE_ACCEPTED" : "PROMISE_DEFERRED",
                accept ? "Optional promise accepted. It has no timer or punishment." : "Optional promise deferred. It remains available later.");
    }

    public static void completePromiseByKey(final ServerPlayer player, final String key) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH) || player == null || key == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final StoryPromiseState promises = data.blackboard(player.getUUID()).story().promises();
        final StoryPromise active = promises.active().orElse(null);
        if (active == null || !active.completionKey().equals(key) || !promises.completeByKey(key)) return;
        data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.MILESTONE,
                player.level().getDayTime() / 24000L, "Optional promise completed: " + active.summary(), 84));
        RelationService.adjust(player, active.role(), 1, true);
        if (data.blackboard(player.getUUID()).story().status(StoryChapter.WHAT_WE_PROTECT) == StoryNodeStatus.LOCKED) {
            data.blackboard(player.getUUID()).story().set(StoryChapter.WHAT_WE_PROTECT, StoryNodeStatus.AVAILABLE);
        }
        data.markChanged();
    }

    public static StoryResult deferMysteryClue(final ServerPlayer player, final String clueId) {
        if (player == null || clueId == null || clueId.isBlank()) return StoryResult.failure("MYSTERY_CLUE_INVALID", "Choose a valid optional clue id.");
        final boolean changed = TeamSavedData.get(player.server).blackboard(player.getUUID()).story().mysteryBoard().defer(clueId);
        if (!changed) return StoryResult.failure("MYSTERY_CLUE_NOT_FOUND", "That optional clue is not on the Mystery Board.");
        TeamSavedData.get(player.server).markChanged();
        return StoryResult.success("MYSTERY_CLUE_DEFERRED", "Optional clue deferred. It remains in the Journal without a penalty.");
    }

    public static String mysterySummary(final ServerPlayer player) {
        final MysteryClue clue = TeamSavedData.get(player.server).blackboard(player.getUUID()).story().mysteryBoard().latest().orElse(null);
        return clue == null ? "No optional mystery clue is recorded." : clue.confidence() + ": " + clue.summary();
    }

    public static String promiseSummary(final ServerPlayer player) {
        final StoryPromise promise = TeamSavedData.get(player.server).blackboard(player.getUUID()).story().promises().active().orElse(null);
        return promise == null ? "No optional promise is active." : promise.role().personalName() + " — " + promise.status() + ": " + promise.summary();
    }

    public static StoryResult start(final ServerPlayer player, final StoryChapter chapter) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return StoryResult.failure("STORY_GRAPH_DISABLED", "Optional story hooks are disabled by the world feature flag.");
        StoryState story=TeamSavedData.get(player.server).blackboard(player.getUUID()).story();
        if (!story.isAtLeastAvailable(chapter)) return StoryResult.failure("CHAPTER_LOCKED", "That optional chapter is not available yet.");
        if (story.status(chapter)==StoryNodeStatus.COMPLETED) return StoryResult.failure("CHAPTER_COMPLETED", "That chapter is already complete.");
        story.set(chapter, StoryNodeStatus.ACTIVE);
        TeamSavedData.get(player.server).markChanged();
        return StoryResult.success("CHAPTER_ACTIVE", "Optional chapter started: " + chapter);
    }

    public static StoryResult defer(final ServerPlayer player, final StoryChapter chapter) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return StoryResult.failure("STORY_GRAPH_DISABLED", "Optional story hooks are disabled by the world feature flag.");
        StoryState story=TeamSavedData.get(player.server).blackboard(player.getUUID()).story();
        if (!story.isAtLeastAvailable(chapter)) return StoryResult.failure("CHAPTER_LOCKED", "That optional chapter is not available yet.");
        story.set(chapter, StoryNodeStatus.DEFERRED);
        TeamSavedData.get(player.server).markChanged();
        return StoryResult.success("CHAPTER_DEFERRED", "Optional chapter deferred. You can return later.");
    }

    public static boolean isRoleUnlocked(final ServerPlayer player, final CompanionRole role) {
        if (!FeatureFlags.enabled(FeatureFlag.STORY_GRAPH)) return true;
        StoryState story=TeamSavedData.get(player.server).blackboard(player.getUUID()).story();
        return switch (role) {
            case GUARDIAN -> true;
            case SEER, SCOUT -> story.isAtLeastAvailable(StoryChapter.SIGNS_IN_THE_WORLD);
            case GIFTED -> story.isAtLeastAvailable(StoryChapter.THE_TEAM_LEARNS) || story.isAtLeastAvailable(StoryChapter.THE_HIVE_ECHO);
        };
    }

    public record StoryResult(boolean successful,String code,String detail) {
        public static StoryResult success(String c,String d){return new StoryResult(true,c,d);}
        public static StoryResult failure(String c,String d){return new StoryResult(false,c,d);}
    }
    public record PromiseResult(boolean successful,String code,String detail) {
        public static PromiseResult success(String c,String d){return new PromiseResult(true,c,d);}
        public static PromiseResult failure(String c,String d){return new PromiseResult(false,c,d);}
    }
}
