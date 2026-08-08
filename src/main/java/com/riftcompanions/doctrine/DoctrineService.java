package com.riftcompanions.doctrine;

import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/** Explicit accept/decline layer so habits never silently become rules. */
public final class DoctrineService {
    private DoctrineService() {}

    public static DoctrineResult accept(final ServerPlayer player, final TeamDoctrine doctrine) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        if (!board.acceptDoctrine(doctrine)) {
            return DoctrineResult.failure("DOCTRINE_NOT_READY", "This rule has not repeated enough yet; it needs three clear observations.");
        }
        board.addMemory(new MemoryRecord(MemoryType.MILESTONE, player.level().getGameTime() / 24000L,
                "Player accepted team doctrine: " + doctrine, 100));
        data.markChanged();
        return DoctrineResult.success("DOCTRINE_ACCEPTED", "Accepted " + doctrine + " as a team doctrine.");
    }

    public static DoctrineResult decline(final ServerPlayer player, final TeamDoctrine doctrine) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        data.blackboard(player.getUUID()).declineDoctrine(doctrine);
        data.markChanged();
        return DoctrineResult.success("DOCTRINE_DECLINED", "The team will not suggest " + doctrine + " again until new habits appear.");
    }

    public static DoctrineResult status(final ServerPlayer player, final TeamDoctrine doctrine) {
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        return DoctrineResult.success("DOCTRINE_STATUS", doctrine + ": approved=" + board.hasDoctrine(doctrine) + ", candidate=" + board.doctrineCandidateCount(doctrine) + "/3");
    }

    public record DoctrineResult(boolean successful, String code, String detail) {
        public static DoctrineResult success(final String code, final String detail) { return new DoctrineResult(true, code, detail); }
        public static DoctrineResult failure(final String code, final String detail) { return new DoctrineResult(false, code, detail); }
    }
}
