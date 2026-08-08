package com.riftcompanions.server;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** In-game day rhythm only; no real-world calendar, forced drama, or repeating announcements. */
public final class TemporalMemoryService {
    private TemporalMemoryService() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (!FeatureFlags.enabled(FeatureFlag.MEMORY_LITE)) return;
        final long day = player.level().getDayTime() / 24000L;
        if (board.lastTemporalDay() == day) return;
        board.setLastTemporalDay(day);
        final long firstHome = board.firstHomeDay();
        if (firstHome >= 0L && (day - firstHome == 10L || day - firstHome == 30L)) {
            board.addMemory(new MemoryRecord(MemoryType.MILESTONE, day,
                    "The team has had a home for " + (day - firstHome) + " in-game days.", 85));
            CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "base_return", 4));
        }
        emitBoundedDawnStatus(player, board, day);
        TeamSavedData.get(player.server).markChanged();
    }

    /** One rotating, context-safe dawn line at most; the normal chat budget remains authoritative. */
    private static void emitBoundedDawnStatus(final ServerPlayer player, final TeamBlackboard board, final long day) {
        if (!FeatureFlags.enabled(FeatureFlag.DAILY_AMBIENT) || board.safeMode().enabled() || !board.isAtSafeBase(player)) return;
        final List<CompanionRole> active = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            if (CompanionLifecycleService.findForOwner(player, role).isPresent()) active.add(role);
        }
        if (active.isEmpty()) return;
        int index = (int) (day % active.size());
        if (active.size() > 1 && board.lastDailyAmbientRole().filter(active.get(index)::equals).isPresent()) {
            index = (index + 1) % active.size();
        }
        final CompanionRole chosen = active.get(index);
        board.setLastDailyAmbientRole(chosen);
        CompanionLifecycleService.findForOwner(player, chosen)
                .ifPresent(companion -> DialogueService.get().speak(companion, "dawn_status", 3));
    }
}
