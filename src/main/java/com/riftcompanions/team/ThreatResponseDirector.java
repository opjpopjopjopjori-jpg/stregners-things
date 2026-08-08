package com.riftcompanions.team;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bounded local response to visible or directly attacking vanilla/mod hostile
 * mobs. It assigns existing combat-capable companions a confirmed nearby threat
 * so Follow/formation movement cannot overwrite melee response. It never
 * creates monsters, scans unloaded terrain, auto-casts powers, or forces a
 * player plan choice.
 */
public final class ThreatResponseDirector {
    private static final double RESPONSE_RADIUS = 14.0D;
    private static final long PLAN_PROPOSAL_COOLDOWN = 240L;
    private static final Map<UUID, ContactState> STATES = new HashMap<>();

    private ThreatResponseDirector() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null || player.level().isClientSide) return;
        final List<CompanionEntity> team = activeTeam(player);
        if (team.isEmpty()) return;
        final List<Monster> threats = player.level().getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(RESPONSE_RADIUS), Monster::isAlive).stream()
                .filter(monster -> relevant(player, team, monster))
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .toList();
        final ContactState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ContactState());
        if (threats.isEmpty()) {
            state.active = false;
            state.band = 0;
            return;
        }

        final int band = threats.size() >= 3 ? 2 : 1;
        final boolean newContact = !state.active || state.band != band;
        state.active = true;
        state.band = band;

        assignCombatTargets(player, team, threats);
        signalSeer(player, threats, newContact);

        if (newContact) {
            final CompanionEntity speaker = preferredSpeaker(team);
            if (speaker != null) {
                final String trigger = band >= 2 ? "hostile_crowd" : "hostile_contact";
                final DialogueService.SpeakResult lead = DialogueService.get().speak(speaker, trigger, band >= 2 ? 0 : 1);
                if (lead.sent()) {
                    pairedReply(team, speaker, trigger, band >= 2 ? 0 : 1);
                }
            }
        }

        if (shouldRetreat(player, team, threats) && !board.plan().isActive()) {
            TeamPlanService.beginRetreat(player, "VISIBLE_HOSTILE_PRESSURE");
            return;
        }
        if (band >= 2 && !board.plan().isOpen() && now >= state.nextPlanProposalAt) {
            // Proposal preserves player approval. Companions still defend
            // themselves locally rather than freezing while it is pending.
            TeamPlanService.proposeDefend(player,
                    List.of("VISIBLE_HOSTILE_CROWD_" + threats.size(), "PLAYER_VISIBLE_HOSTILE_CONTACT"));
            state.nextPlanProposalAt = now + PLAN_PROPOSAL_COOLDOWN;
        }
    }

    public static void clearOwner(final UUID owner) {
        if (owner != null) STATES.remove(owner);
    }

    private static List<CompanionEntity> activeTeam(final ServerPlayer player) {
        final List<CompanionEntity> result = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role)
                    .filter(companion -> companion.getCompanionState() != CompanionState.DOWNED
                            && companion.getCompanionState() != CompanionState.RESTING)
                    .ifPresent(result::add);
        }
        return result;
    }

    private static boolean relevant(final ServerPlayer player, final List<CompanionEntity> team, final Monster monster) {
        final LivingEntity target = monster.getTarget();
        if (target == player || team.contains(target)) return true;
        // A distant visible monster can produce a calm field-guide observation.
        // Automatic defensive targeting begins only once it is close enough to
        // create immediate local pressure.
        return player.hasLineOfSight(monster) && player.distanceToSqr(monster) <= 8.0D * 8.0D;
    }

    private static void assignCombatTargets(final ServerPlayer player, final List<CompanionEntity> team, final List<Monster> threats) {
        final Set<UUID> claimed = new HashSet<>();
        for (final CompanionEntity companion : team) {
            if (!companion.allowsCombatAction()) continue;
            final Monster selected = threats.stream()
                    .filter(monster -> !claimed.contains(monster.getUUID()))
                    .min(Comparator.comparingDouble(companion::distanceToSqr))
                    .orElse(threats.get(0));
            claimed.add(selected.getUUID());
            companion.setTarget(selected);
            companion.clearFormationSlot("VISIBLE_HOSTILE_RESPONSE");
            if (companion.getCompanionState() != CompanionState.FIGHTING) {
                companion.setCompanionState(CompanionState.FIGHTING, "VISIBLE_HOSTILE_RESPONSE");
            }
        }
    }

    private static void signalSeer(final ServerPlayer player, final List<Monster> threats, final boolean newContact) {
        final CompanionEntity seer = CompanionLifecycleService.findForOwner(player, CompanionRole.SEER).orElse(null);
        if (seer == null || !newContact) return;
        // The Seer acknowledges an immediate visible threat visually; the team
        // dialogue lead/reply below owns the bounded chat budget.
        seer.beginVisualAction(CompanionAction.SEER_NOTICE, 18L);
    }

    private static void pairedReply(final List<CompanionEntity> team, final CompanionEntity lead,
                                    final String trigger, final int priority) {
        if (team.size() != 2 || lead == null) return;
        final CompanionEntity reply = team.stream().filter(companion -> companion != lead).findFirst().orElse(null);
        if (reply == null || reply.getCompanionState() == CompanionState.DOWNED
                || reply.getCompanionState() == CompanionState.RECOVERING || reply.getCompanionState() == CompanionState.EXHAUSTED) {
            return;
        }
        DialogueService.get().speakPairedReply(reply, trigger, priority);
    }

    private static boolean shouldRetreat(final ServerPlayer player, final List<CompanionEntity> team, final List<Monster> threats) {
        if (player.getHealth() <= player.getMaxHealth() * 0.40F || threats.size() >= 5) return true;
        return team.stream().anyMatch(companion -> companion.getHealth() <= companion.getMaxHealth() * 0.25F);
    }

    private static CompanionEntity preferredSpeaker(final List<CompanionEntity> team) {
        final CompanionEntity guardian = byRole(team, CompanionRole.GUARDIAN);
        if (guardian != null) return guardian;
        final CompanionEntity gifted = byRole(team, CompanionRole.GIFTED);
        if (gifted != null) return gifted;
        final CompanionEntity scout = byRole(team, CompanionRole.SCOUT);
        if (scout != null) return scout;
        return byRole(team, CompanionRole.SEER);
    }

    private static CompanionEntity byRole(final List<CompanionEntity> team, final CompanionRole role) {
        return team.stream().filter(companion -> companion.getRole() == role).findFirst().orElse(null);
    }

    private static final class ContactState {
        private boolean active;
        private int band;
        private long nextPlanProposalAt;
    }
}
