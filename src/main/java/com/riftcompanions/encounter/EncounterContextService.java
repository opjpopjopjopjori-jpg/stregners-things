package com.riftcompanions.encounter;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.Comparator;
import java.util.List;

/**
 * Converts actual biome/visible threat/active structure-plan evidence into a
 * bounded encounter context. It does not create content, scan hidden chunks,
 * force a plan, or make every biome produce dialogue.
 */
public final class EncounterContextService {
    private EncounterContextService() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null || now % com.riftcompanions.mode.PerformancePolicy.scaledInterval(CompanionConfig.ENCOUNTER_CONTEXT_INTERVAL_TICKS.get()) != 0L) return;
        final WorldEncounterProfile profile = selectProfile(player, board);
        final int risk = Math.max(board.dangerScore(), localRisk(player));
        final FormationType formation = recommendedFormation(profile);
        final boolean changed = board.encounterContext().update(profile, player.level().dimension().location(), player.blockPosition(), risk, formation, now);
        if (changed && risk >= CompanionConfig.ENCOUNTER_MEMORY_RISK_THRESHOLD.get()) {
            board.addMemory(new MemoryRecord(MemoryType.THREAT_OBSERVATION, now / 24000L,
                    "Encounter context: " + profile.id() + " — " + profile.primaryRisk() + ".", 70));
            TeamSavedData.get(player.server).markChanged();
        }
        if (changed && board.encounterContext().canAnnounce(now, CompanionConfig.ENCOUNTER_DIALOGUE_COOLDOWN_TICKS.get())) {
            announce(player, board, profile, risk, now);
        }
    }

    private static WorldEncounterProfile selectProfile(final ServerPlayer player, final TeamBlackboard board) {
        final Monster threat = player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(12.0D))
                .stream().filter(monster -> monster.isAlive() && player.hasLineOfSight(monster))
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (threat != null) {
            final ThreatArchetype archetype = EncounterProfileRegistry.threatArchetype(threat);
            return EncounterProfileRegistry.threatProfile(archetype);
        }
        if (board.plan().type() == com.riftcompanions.team.TeamPlanType.STRUCTURE_ENTRY) {
            final String id = board.plan().reasonCodes().stream().filter(reason -> reason.startsWith("ENCOUNTER_"))
                    .map(reason -> reason.substring("ENCOUNTER_".length()).toLowerCase(java.util.Locale.ROOT)).findFirst().orElse("unknown_structure");
            return EncounterProfileRegistry.profileById(id);
        }
        return EncounterProfileRegistry.biomeProfile(player.serverLevel(), player.blockPosition());
    }

    private static int localRisk(final ServerPlayer player) {
        int risk = 0;
        if (player.isInLava() || player.isOnFire() || player.fallDistance > 3.0F) risk += 50;
        if (player.getHealth() <= player.getMaxHealth() * 0.35F) risk += 30;
        risk += Math.min(30, player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10.0D)).size() * 6);
        return Math.min(100, risk);
    }

    private static FormationType recommendedFormation(final WorldEncounterProfile profile) {
        return switch (profile.id()) {
            case "mountain_cliff", "dungeon", "mineshaft", "stronghold" -> FormationType.CAVE;
            case "nether", "end" -> FormationType.RETREAT;
            default -> FormationType.FOLLOW;
        };
    }

    private static void announce(final ServerPlayer player, final TeamBlackboard board, final WorldEncounterProfile profile,
                                 final int risk, final long now) {
        CompanionRole chosen = profile.suitableRoles().stream()
                .filter(role -> CompanionLifecycleService.findForOwner(player, role).isPresent()).findFirst()
                .orElse(CompanionRole.GUARDIAN);
        final EncounterDialogueDefinition definition = EncounterDialogueRegistry.find(profile.id(), chosen).orElse(null);
        if (definition == null || risk < definition.minRisk()) return;
        final CompanionEntity speaker = CompanionLifecycleService.findForOwner(player, chosen).orElse(null);
        if (speaker == null) return;
        DialogueService.get().speak(speaker, definition.trigger(), risk >= 70 ? 2 : 3);
        board.encounterContext().markAnnounced(now);
        TeamSavedData.get(player.server).markChanged();
    }
}
