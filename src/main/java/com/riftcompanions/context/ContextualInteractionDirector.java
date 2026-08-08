package com.riftcompanions.context;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-stage authored scene director for local field-guide interactions. It
 * selects Discovery then Revisit scenes from visible/safe facts, delivers one
 * bounded lead and optional paired reply, and never changes AI authority,
 * animal ownership, inventory, loot, blocks, player input, or progression.
 */
public final class ContextualInteractionDirector {
    private static final long TICK_INTERVAL = 40L;
    private static final long GLOBAL_COOLDOWN = 300L;
    private static final long FOCUSED_MONSTER_LIFETIME = 100L;
    private static final long PLAYER_CUE_LIFETIME = 160L;
    private static final long REPLY_DELAY_TICKS = 26L;
    private static final int RECENT_LIMIT = 72;
    private static final int GROUP_LIMIT = 128;
    private static final double ENTITY_RADIUS = 12.0D;
    private static final Map<UUID, ContextState> STATES = new LinkedHashMap<>();

    private ContextualInteractionDirector() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null || !FeatureFlags.enabled(FeatureFlag.CONTEXTUAL_INTERACTIONS)) return;
        final ContextState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ContextState());
        state.purge(now);

        if (state.pendingReply != null) {
            if (!safeAmbientContext(player, board)) {
                state.pendingReply = null;
            } else if (now >= state.pendingReply.replyAt()) {
                deliverReply(player, state, now);
            }
            return;
        }
        if (now % TICK_INTERVAL != 0L || !safeAmbientContext(player, board) || now < state.nextAnyAt) return;

        if (processFatigue(player, board, state, now)) return;
        if (processPlayerBlock(player, board, state, now)) return;
        if (processStructure(player, board, state, now)) return;
        if (processWeatherAndTime(player, board, state, now)) return;
        if (processBiome(player, board, state, now)) return;
        if (processFocusedMonster(player, board, state, now)) return;
        processVisibleEntity(player, board, state, now);
    }

    /** Called only from a visible local threat perception event. */
    public static void observeVisibleMonster(final ServerPlayer player, final Monster monster) {
        if (player == null || monster == null || !FeatureFlags.enabled(FeatureFlag.CONTEXTUAL_INTERACTIONS)) return;
        final ContextState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ContextState());
        state.focusedMonster = monster.getUUID();
        state.focusedMonsterUntil = player.level().getGameTime() + FOCUSED_MONSTER_LIFETIME;
    }

    /** Called only after a player right-clicks a visible safe work/non-container block. */
    public static void observePlayerBlock(final ServerPlayer player, final BlockPos position) {
        if (player == null || position == null || !FeatureFlags.enabled(FeatureFlag.CONTEXTUAL_INTERACTIONS)
                || !player.serverLevel().hasChunkAt(position)) return;
        final BlockState block = player.serverLevel().getBlockState(position);
        final String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(block.getBlock()));
        if (ContextInteractionRegistry.find(ContextInteractionKind.PLAYER_BLOCK, id, ContextInteractionStage.DISCOVERY).isEmpty()) return;
        final ContextState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ContextState());
        state.pendingBlock = id;
        state.pendingBlockUntil = player.level().getGameTime() + PLAYER_CUE_LIFETIME;
    }

    /** A structure scene follows a player-selected, visible perimeter assessment only. */
    public static void observePlayerSelectedStructure(final ServerPlayer player, final String profileId) {
        if (player == null || profileId == null || profileId.isBlank() || !FeatureFlags.enabled(FeatureFlag.CONTEXTUAL_INTERACTIONS)) return;
        final ContextState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ContextState());
        state.pendingStructure = profileId;
        state.pendingStructureUntil = player.level().getGameTime() + PLAYER_CUE_LIFETIME;
    }

    public static void clearOwner(final UUID owner) {
        if (owner != null) STATES.remove(owner);
    }

    private static boolean processFatigue(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        final CompanionEntity tired = activeTeam(player).stream()
                .filter(companion -> companion.getEnergy() <= 24.0F)
                .min(Comparator.comparingDouble(CompanionEntity::getEnergy)).orElse(null);
        return tired != null && deliver(player, board, state, now, ContextInteractionKind.FATIGUE, "LOW_ENERGY", tired,
                "fatigue:low_energy");
    }

    private static boolean processPlayerBlock(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        if (state.pendingBlock == null || state.pendingBlockUntil < now) return false;
        final String match = state.pendingBlock;
        final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.PLAYER_BLOCK, null);
        final boolean delivered = deliver(player, board, state, now, ContextInteractionKind.PLAYER_BLOCK, match, speaker,
                "block:" + match);
        if (delivered || state.pendingBlockUntil < now) {
            state.pendingBlock = null;
            state.pendingBlockUntil = 0L;
        }
        return delivered;
    }

    private static boolean processStructure(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        String profile = state.pendingStructureUntil >= now ? state.pendingStructure : null;
        if (profile == null || profile.isBlank()) profile = board.encounterContext().profileId();
        if (profile == null || profile.isBlank() || "unknown".equals(profile)) return false;
        final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.STRUCTURE, null);
        final boolean delivered = deliver(player, board, state, now, ContextInteractionKind.STRUCTURE, profile, speaker,
                "structure:" + profile);
        if (delivered || state.pendingStructureUntil < now) {
            state.pendingStructure = null;
            state.pendingStructureUntil = 0L;
        }
        return delivered;
    }

    private static boolean processWeatherAndTime(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        final String weather = player.serverLevel().isThundering() ? "THUNDER" : player.serverLevel().isRaining() ? "RAIN" : "";
        if (!weather.isBlank()) {
            final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.WEATHER, null);
            if (deliver(player, board, state, now, ContextInteractionKind.WEATHER, weather, speaker, "weather:" + weather)) return true;
        }
        final long time = player.level().getDayTime() % 24000L;
        final String phase = (time >= 22500L || time <= 400L) ? "DAWN" : (time >= 11600L && time <= 12800L) ? "DUSK"
                : (time >= 13000L && time <= 22000L) ? "NIGHT" : "";
        if (phase.isBlank()) return false;
        final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.TIME, null);
        return deliver(player, board, state, now, ContextInteractionKind.TIME, phase, speaker, "time:" + phase);
    }

    private static boolean processBiome(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        final String biome = String.valueOf(player.serverLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getKey(player.serverLevel().getBiome(player.blockPosition()).value()));
        final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.BIOME, null);
        return deliver(player, board, state, now, ContextInteractionKind.BIOME, biome, speaker, "biome:" + biome);
    }

    private static boolean processFocusedMonster(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        if (state.focusedMonster == null || state.focusedMonsterUntil < now) return false;
        final Entity entity = player.serverLevel().getEntity(state.focusedMonster);
        state.focusedMonster = null;
        if (!(entity instanceof Monster monster) || !canBriefMonster(player, monster)) return false;
        final String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(monster.getType()));
        final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.FOCUSED_HOSTILE, monster);
        return deliver(player, board, state, now, ContextInteractionKind.FOCUSED_HOSTILE, id, speaker, "mob:" + id);
    }

    private static boolean processVisibleEntity(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now) {
        final List<Entity> candidates = player.serverLevel().getEntities(player,
                player.getBoundingBox().inflate(ENTITY_RADIUS), entity -> entity instanceof LivingEntity living
                        && living.isAlive() && !(living instanceof Monster) && player.hasLineOfSight(living));
        final Entity candidate = candidates.stream()
                .filter(entity -> ContextInteractionRegistry.find(ContextInteractionKind.ENTITY,
                        String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())), ContextInteractionStage.DISCOVERY).isPresent())
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (candidate == null) return false;
        final String id = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType()));
        final CompanionEntity speaker = chooseSpeaker(player, ContextInteractionKind.ENTITY, candidate);
        return deliver(player, board, state, now, ContextInteractionKind.ENTITY, id, speaker, "entity:" + id);
    }

    private static boolean deliver(final ServerPlayer player, final TeamBlackboard board, final ContextState state, final long now,
                                   final ContextInteractionKind kind, final String match, final CompanionEntity speaker,
                                   final String signature) {
        if (speaker == null) return false;
        final ContextInteractionDefinition discovery = ContextInteractionRegistry.find(kind, match, ContextInteractionStage.DISCOVERY).orElse(null);
        if (discovery == null) return false;
        if (state.groupUntil(discovery.group(), now) > now) return false;
        final ContextInteractionStage stage = state.stageFor(discovery.group());
        ContextInteractionDefinition definition = ContextInteractionRegistry.find(kind, match, stage).orElse(discovery);
        if (definition.requiresFocusedMonster() && kind != ContextInteractionKind.FOCUSED_HOSTILE) return false;
        if (state.recentUntil(definition.id(), now) > now) return false;
        final DialogueService.SpeakResult result = DialogueService.get().speak(speaker, definition.leadTrigger(), definition.priority());
        if (!result.sent()) return false;
        speaker.beginVisualAction(definition.action(), 34L);
        state.record(definition.id(), now + definition.cooldownTicks());
        state.recordGroup(definition.group(), now + definition.cooldownTicks());
        state.advanceStage(definition.group());
        state.nextAnyAt = now + GLOBAL_COOLDOWN;

        final CompanionEntity reply = chooseReply(player, speaker);
        if (reply != null && !definition.replyTrigger().isBlank()) {
            reply.beginVisualAction(CompanionAction.SOCIAL_LISTEN, REPLY_DELAY_TICKS + 16L);
            state.pendingReply = new PendingReply(reply.getUUID(), definition.replyTrigger(), definition.replyAction(),
                    definition.priority(), now + REPLY_DELAY_TICKS);
        }
        return true;
    }

    private static void deliverReply(final ServerPlayer player, final ContextState state, final long now) {
        final PendingReply pending = state.pendingReply;
        state.pendingReply = null;
        if (pending == null || !safeAmbientContext(player, com.riftcompanions.server.TeamSavedData.get(player.server).blackboard(player.getUUID()))) return;
        final CompanionEntity reply = CompanionLifecycleService.findLoaded(player.server, pending.replyUuid()).orElse(null);
        if (reply == null || reply.getCompanionState() == CompanionState.DOWNED || reply.getCompanionState() == CompanionState.FIGHTING
                || reply.distanceToSqr(player) > ENTITY_RADIUS * ENTITY_RADIUS) return;
        final DialogueService.SpeakResult result = DialogueService.get().speakPairedReply(reply, pending.replyTrigger(), pending.priority());
        if (result.sent()) reply.beginVisualAction(pending.replyAction(), 30L);
    }

    private static boolean safeAmbientContext(final ServerPlayer player, final TeamBlackboard board) {
        if (board.safeMode().enabled() || board.plan().isActive() || board.plan().awaitsApproval()) return false;
        if (player.getHealth() <= player.getMaxHealth() * 0.55F || player.isInLava() || player.isOnFire() || player.fallDistance > 3.0F) return false;
        if (board.dangerScore() >= 25) return false;
        return player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10.0D), Monster::isAlive).isEmpty();
    }

    private static boolean canBriefMonster(final ServerPlayer player, final Monster monster) {
        if (!monster.isAlive() || monster.level() != player.level() || !player.hasLineOfSight(monster)) return false;
        if (player.distanceToSqr(monster) < 10.0D * 10.0D) return false;
        final LivingEntity target = monster.getTarget();
        if (target == player || target instanceof CompanionEntity) return false;
        return true;
    }

    private static List<CompanionEntity> activeTeam(final ServerPlayer player) {
        final List<CompanionEntity> result = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role)
                    .filter(companion -> companion.distanceToSqr(player) <= ENTITY_RADIUS * ENTITY_RADIUS)
                    .filter(companion -> switch (companion.getCompanionState()) {
                        case DOWNED, FIGHTING, RETREATING, STUCK_RECOVERY, EXHAUSTED, RECOVERING, RESTING -> false;
                        default -> true;
                    }).ifPresent(result::add);
        }
        return result;
    }

    private static CompanionEntity chooseSpeaker(final ServerPlayer player, final ContextInteractionKind kind, final Entity subject) {
        final List<CompanionEntity> team = activeTeam(player);
        if (team.isEmpty()) return null;
        final List<CompanionRole> preference = switch (kind) {
            case ENTITY -> List.of(CompanionRole.GIFTED, CompanionRole.SCOUT, CompanionRole.SEER, CompanionRole.GUARDIAN);
            case FOCUSED_HOSTILE -> List.of(CompanionRole.GUARDIAN, CompanionRole.SEER, CompanionRole.SCOUT, CompanionRole.GIFTED);
            case BIOME, WEATHER, TIME -> List.of(CompanionRole.SCOUT, CompanionRole.SEER, CompanionRole.GUARDIAN, CompanionRole.GIFTED);
            case STRUCTURE -> List.of(CompanionRole.GUARDIAN, CompanionRole.SEER, CompanionRole.SCOUT, CompanionRole.GIFTED);
            case PLAYER_BLOCK -> List.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT, CompanionRole.GIFTED, CompanionRole.SEER);
            case FATIGUE -> team.stream().sorted(Comparator.comparingDouble(CompanionEntity::getEnergy)).map(CompanionEntity::getRole).toList();
        };
        for (final CompanionRole role : preference) {
            final CompanionEntity candidate = team.stream().filter(companion -> companion.getRole() == role).findFirst().orElse(null);
            if (candidate != null && (subject == null || candidate.hasLineOfSight(subject))) return candidate;
        }
        return team.get(0);
    }

    private static CompanionEntity chooseReply(final ServerPlayer player, final CompanionEntity lead) {
        final List<CompanionEntity> team = activeTeam(player);
        if (team.size() != 2 || lead == null) return null;
        return team.stream().filter(companion -> companion != lead).findFirst().orElse(null);
    }

    private static final class ContextState {
        private long nextAnyAt;
        private UUID focusedMonster;
        private long focusedMonsterUntil;
        private String pendingStructure;
        private long pendingStructureUntil;
        private String pendingBlock;
        private long pendingBlockUntil;
        private PendingReply pendingReply;
        private final LinkedHashMap<String, Long> recent = new LinkedHashMap<>();
        private final LinkedHashMap<String, Long> groupCooldowns = new LinkedHashMap<>();
        private final LinkedHashMap<String, ContextInteractionStage> stages = new LinkedHashMap<>();

        private ContextInteractionStage stageFor(final String group) {
            return stages.getOrDefault(group, ContextInteractionStage.DISCOVERY);
        }

        private void advanceStage(final String group) {
            stages.put(group, ContextInteractionStage.REVISIT);
            while (stages.size() > GROUP_LIMIT) stages.remove(stages.keySet().iterator().next());
        }

        private void record(final String signature, final long until) {
            recent.put(signature, until);
            while (recent.size() > RECENT_LIMIT) recent.remove(recent.keySet().iterator().next());
        }

        private void recordGroup(final String group, final long until) {
            groupCooldowns.put(group, until);
            while (groupCooldowns.size() > GROUP_LIMIT) groupCooldowns.remove(groupCooldowns.keySet().iterator().next());
        }

        private long groupUntil(final String group, final long now) {
            purge(now);
            return groupCooldowns.getOrDefault(group, 0L);
        }

        private long recentUntil(final String signature, final long now) {
            purge(now);
            return recent.getOrDefault(signature, 0L);
        }

        private void purge(final long now) {
            recent.entrySet().removeIf(entry -> entry.getValue() <= now);
            groupCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
            if (focusedMonsterUntil <= now) focusedMonster = null;
            if (pendingStructureUntil <= now) pendingStructure = null;
            if (pendingBlockUntil <= now) pendingBlock = null;
        }
    }

    private record PendingReply(UUID replyUuid, String replyTrigger, com.riftcompanions.entity.CompanionAction replyAction,
                                int priority, long replyAt) {}
}
