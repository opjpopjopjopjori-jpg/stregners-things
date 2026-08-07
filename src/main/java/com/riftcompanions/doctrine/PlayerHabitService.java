package com.riftcompanions.doctrine;

import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Counts repeated, observable player actions. A candidate is not a doctrine
 * until the player explicitly accepts it via command/UI.
 */
public final class PlayerHabitService {
    private PlayerHabitService() {}

    public static void observe(final ServerPlayer player, final TeamDoctrine doctrine) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        if (board.hasDoctrine(doctrine)) {
            return;
        }
        final int count = board.incrementDoctrineCandidate(doctrine);
        if (count == 3) {
            board.addMemory(new MemoryRecord(MemoryType.MILESTONE, player.level().getGameTime() / 24000L,
                    "Doctrine candidate available: " + doctrine + ". Confirm or decline it in the team commands.", 75));
        }
        data.markChanged();
    }
}
