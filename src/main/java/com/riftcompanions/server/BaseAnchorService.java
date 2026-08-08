package com.riftcompanions.server;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.story.StoryService;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.TeamAnchor;
import com.riftcompanions.intention.IntentionService;
import com.riftcompanions.consequence.ConsequenceService;
import com.riftcompanions.consequence.ConsequenceType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Server-side player annotation service for Home/Rest/Guard/etc. anchors. */
public final class BaseAnchorService {
    private BaseAnchorService() {}

    public static AnchorResult setAnchor(final ServerPlayer player, final BaseAnchorType type) {
        if (!SafeTeleport.isSafeStanding(player.serverLevel(), player, player.blockPosition())) {
            return AnchorResult.failure("ANCHOR_POSITION_UNSAFE", "An anchor cannot be placed in an unsafe position.");
        }
        final long day = player.level().getGameTime() / 24000L;
        final TeamAnchor anchor = new TeamAnchor(player.level().dimension().location(), player.blockPosition().immutable(), day);
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        final TeamAnchor previous = board.anchor(type).orElse(null);
        board.setAnchor(type, anchor);
        if (type == BaseAnchorType.HOME) {
            board.registerHomeDay(day);
            if (previous == null) {
                board.addMemory(new MemoryRecord(MemoryType.MILESTONE, day, "First home established at " + anchor.position().getX() + ", " + anchor.position().getY() + ", " + anchor.position().getZ(), 100));
            } else if (!previous.dimension().equals(anchor.dimension()) || previous.position().distSqr(anchor.position()) > 16.0D) {
                board.addMemory(new MemoryRecord(MemoryType.MILESTONE, day, "Home moved; the previous home remains a memory, not the current anchor.", 90));
            }
        }
        data.markChanged();
        if (type == BaseAnchorType.HOME || type == BaseAnchorType.GUARD_POST || type == BaseAnchorType.REST) {
            ConsequenceService.record(player, ConsequenceType.WORLD, "anchor:" + type.name() + ":" + anchor.position().asLong(),
                    "Player established " + type + "; the team now has a visible shared reference point.", 84);
        }
        IntentionService.observeCompletion(player, "ANCHOR_" + type.name());
        if (type == BaseAnchorType.GUARD_POST) StoryService.completePromiseByKey(player, "ANCHOR_GUARD_POST");
        if (type == BaseAnchorType.HOME) {
            StoryService.onHomeEstablished(player);
        }
        return AnchorResult.success("ANCHOR_SET_" + type.name(), "Set " + type.name() + " at " + anchor.position().getX() + ", " + anchor.position().getY() + ", " + anchor.position().getZ());
    }

    public static AnchorResult clearAnchor(final ServerPlayer player, final BaseAnchorType type) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        if (data.blackboard(player.getUUID()).removeAnchor(type).isEmpty()) {
            return AnchorResult.failure("ANCHOR_NOT_SET", "That anchor is not set.");
        }
        data.markChanged();
        return AnchorResult.success("ANCHOR_CLEARED_" + type.name(), "Removed " + type.name() + ".");
    }

    public static AnchorResult returnHome(final ServerPlayer player) {
        final Optional<TeamAnchor> home = TeamSavedData.get(player.server).blackboard(player.getUUID()).anchor(BaseAnchorType.HOME);
        if (home.isEmpty()) {
            return AnchorResult.failure("HOME_ANCHOR_NOT_SET", "Set a HOME anchor from a safe location first.");
        }
        final TeamAnchor anchor = home.get();
        if (!player.level().dimension().location().equals(anchor.dimension())) {
            return AnchorResult.failure("HOME_IN_OTHER_DIMENSION", "Automatic dimension transfer is disabled. Return to the same dimension first.");
        }
        if (!player.serverLevel().hasChunkAt(anchor.position())) {
            return AnchorResult.failure("HOME_CHUNK_NOT_LOADED", "The home chunk is not loaded; companions will not force it to load.");
        }
        int assigned = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final var companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent() && companion.get().beginReturnHome(anchor.position())) {
                assigned++;
            }
        }
        return assigned > 0 ? AnchorResult.success("RETURN_HOME_ASSIGNED", "Assigned " + assigned + " companions to the HOME anchor.")
                : AnchorResult.failure("NO_COMPANION_FOR_RETURN_HOME", "No loaded companion can be directed home.");
    }

    public record AnchorResult(boolean successful, String code, String detail) {
        public static AnchorResult success(final String code, final String detail) { return new AnchorResult(true, code, detail); }
        public static AnchorResult failure(final String code, final String detail) { return new AnchorResult(false, code, detail); }
    }
}
