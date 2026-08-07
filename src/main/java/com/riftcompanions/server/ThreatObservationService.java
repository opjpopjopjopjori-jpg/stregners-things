package com.riftcompanions.server;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.memory.ThreatObservationKind;
import com.riftcompanions.registry.ModTags;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.events.TeamEventType;
import com.riftcompanions.story.StoryService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event-driven local observation, not a general AI model. It writes only a
 * bounded summary when the team actually sees a threat or receives its damage.
 */
public final class ThreatObservationService {
    private static final long VISUAL_OBSERVATION_COOLDOWN = 240L;
    private static final Map<UUID, Map<String, Long>> lastVisualObservation = new HashMap<>();

    private ThreatObservationService() {}

    public static void observeNearestVisibleThreat(final ServerPlayer player) {
        final Monster nearest = player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16.0D), player::hasLineOfSight)
                .stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (nearest == null) {
            return;
        }
        final ThreatObservationKind kind = nearest.getType().is(ModTags.HIVE_LINKED)
                ? ThreatObservationKind.HIVE_TAG_DETECTED : ThreatObservationKind.VISUAL_CONTACT;
        observe(player, nearest, kind, true);
    }

    public static void observeDirectDamage(final ServerPlayer player, final Monster attacker) {
        observe(player, attacker, ThreatObservationKind.DIRECT_DAMAGE, false);
    }

    private static void observe(final ServerPlayer player, final Monster threat, final ThreatObservationKind kind, final boolean visual) {
        if (player.level() != threat.level()) {
            return;
        }
        final ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(threat.getType());
        final String typeId = key.toString();
        final long now = player.level().getGameTime();
        if (visual) {
            final Map<String, Long> cooldowns = lastVisualObservation.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
            final Long lastObserved = cooldowns.get(typeId);
            if (lastObserved != null && now - lastObserved < VISUAL_OBSERVATION_COOLDOWN) {
                return;
            }
            cooldowns.put(typeId, now);
        }

        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final TeamBlackboard.ThreatObservationResult result = board.observeThreat(typeId, kind, now / 24000L);
        if (!result.firstEncounter() && !result.stageAdvanced()) {
            return;
        }
        final String summary = result.firstEncounter()
                ? "First observed hostile: " + typeId
                : "Threat knowledge updated: " + typeId + " → " + result.record().stage();
        board.addMemory(new MemoryRecord(MemoryType.THREAT_OBSERVATION, now / 24000L, summary,
                result.record().stage().ordinal() * 25 + 25));
        data.markChanged();
        TeamDirector.submit(player, TeamEventType.UNKNOWN_MOB_FIRST_SEEN, threat.blockPosition(),
                List.of("OBSERVED_" + typeId, "STAGE_" + result.record().stage(), kind.name()));
        if (kind == ThreatObservationKind.HIVE_TAG_DETECTED) {
            StoryService.onHiveObserved(player);
        }
    }

    public static void clearSession(final UUID player) {
        lastVisualObservation.remove(player);
    }
}
