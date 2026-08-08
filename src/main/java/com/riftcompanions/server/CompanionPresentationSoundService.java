package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-triggered original presentation cues. It receives a confirmed visual
 * action only after gameplay validation; it never decides an effect, target,
 * plan, inventory change, or block action.
 */
public final class CompanionPresentationSoundService {
    private static final int HISTORY_LIMIT = 32;
    private static final Map<UUID, Long> LAST_CUE_AT = new LinkedHashMap<>();

    private CompanionPresentationSoundService() {}

    public static void playForAction(final CompanionEntity companion, final CompanionAction action) {
        if (companion == null || action == null || !CompanionConfig.COMPANION_AUDIO_ENABLED.get()
                || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        final Cue cue = cueFor(action);
        if (cue == null) return;
        final long now = level.getGameTime();
        final long previous = LAST_CUE_AT.getOrDefault(companion.getUUID(), Long.MIN_VALUE);
        if (previous != Long.MIN_VALUE && now - previous < cue.cooldownTicks()) return;
        if (!level.hasChunkAt(companion.blockPosition())) return;
        LAST_CUE_AT.put(companion.getUUID(), now);
        while (LAST_CUE_AT.size() > HISTORY_LIMIT) LAST_CUE_AT.remove(LAST_CUE_AT.keySet().iterator().next());
        level.playSound(null, companion.getX(), companion.getY() + companion.getBbHeight() * 0.5D, companion.getZ(),
                cue.event().get(), SoundSource.NEUTRAL, cue.volume(), cue.pitch());
    }

    public static void playPlanAccepted(final ServerPlayer player) {
        playUi(player, ModSounds.UI_PLAN_ACCEPT, 0.42F, 1.0F);
    }

    public static void playSafeMode(final ServerPlayer player) {
        playUi(player, ModSounds.UI_SAFE_MODE, 0.48F, 0.96F);
    }

    private static void playUi(final ServerPlayer player, final RegistryObject<SoundEvent> event,
                               final float volume, final float pitch) {
        if (player == null || !CompanionConfig.COMPANION_AUDIO_ENABLED.get()) return;
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                event.get(), SoundSource.PLAYERS, volume, pitch);
    }

    public static void clearEntity(final UUID entityId) {
        if (entityId != null) LAST_CUE_AT.remove(entityId);
    }

    private static Cue cueFor(final CompanionAction action) {
        return switch (action) {
            case GUARD -> cue(ModSounds.GUARDIAN_GUARD_SIGNAL, 0.90F, 0.95F, 20L);
            case GUARDIAN_BRACE -> cue(ModSounds.GUARDIAN_GUARD_SIGNAL, 0.95F, 1.00F, 24L);
            case MELEE_ATTACK -> cue(ModSounds.GUARDIAN_MELEE_SWING, 0.95F, 0.95F, 6L);
            case HIT_REACT -> cue(ModSounds.GUARDIAN_IMPACT, 0.85F, 0.90F, 6L);
            case RETREAT_SIGNAL -> cue(ModSounds.GUARDIAN_RETREAT, 0.85F, 1.00F, 16L);
            case SEER_NOTICE -> cue(ModSounds.SEER_NOTICE, 0.75F, 1.00F, 14L);
            case SEER_FOCUS -> cue(ModSounds.SEER_FOCUS, 0.80F, 0.95F, 24L);
            case SEER_RELEASE, SEER_RELEASE_REDIRECT -> cue(ModSounds.SEER_RELEASE, 0.95F, 0.92F, 10L);
            case SEER_RELEASE_SHATTER -> cue(ModSounds.SEER_SHATTER, 1.00F, 0.88F, 12L);
            case RECOVER -> cue(ModSounds.SEER_RECOVERY, 0.75F, 0.96F, 16L);
            case GIFTED_NOTICE -> cue(ModSounds.GIFTED_NOTICE, 0.75F, 1.00F, 14L);
            case GIFTED_FOCUS -> cue(ModSounds.GIFTED_FOCUS, 0.80F, 0.98F, 24L);
            case GIFTED_PUSH -> cue(ModSounds.GIFTED_PUSH, 0.95F, 1.05F, 10L);
            case GIFTED_SHIELD -> cue(ModSounds.GIFTED_SHIELD, 0.90F, 0.98F, 36L);
            case GIFTED_RESCUE -> cue(ModSounds.GIFTED_RESCUE, 0.95F, 1.00F, 14L);
            case GIFTED_EXHAUSTED -> cue(ModSounds.GIFTED_EXHAUSTED, 0.65F, 0.92F, 16L);
            case SCOUT_POINT, SCOUT_LOOKOUT -> cue(ModSounds.SCOUT_ROUTE, 0.80F, 1.02F, 14L);
            case SCOUT_ANCHOR -> cue(ModSounds.SCOUT_ANCHOR, 0.85F, 0.98F, 24L);
            case SCOUT_SIGNAL -> cue(ModSounds.SCOUT_ROUTE, 0.90F, 1.08F, 14L);
            // Social dialogue already has readable chat/HUD fallback. Suppress extra
            // cues here so a pair exchange never becomes an audio spam source.
            case NONE, TALK, DOWNED, SOCIAL_LISTEN, SOCIAL_POINT, SOCIAL_REASSURE, SOCIAL_GEAR_CHECK,
                    SOCIAL_OBSERVE, SOCIAL_CAMPFIRE, SOCIAL_WEATHER, SOCIAL_HORIZON, SOCIAL_BASE,
                    SOCIAL_WORK, SOCIAL_CAVE, SOCIAL_VILLAGE, SOCIAL_TRAVEL, SOCIAL_CALM,
                    CONTEXT_ANIMAL_GREET, CONTEXT_ANIMAL_OBSERVE, CONTEXT_FIELD_NOTE, CONTEXT_THREAT_BRIEF,
                    CONTEXT_LOOT_NOTE, CONTEXT_BIOME_BRIEF, CONTEXT_STRUCTURE_BRIEF, CONTEXT_REST_REQUEST,
                    CONTEXT_ROUTE_NOTE -> null;
        };
    }

    private static Cue cue(final RegistryObject<SoundEvent> event, final float volume, final float pitch, final long cooldownTicks) {
        return new Cue(event, volume, pitch, cooldownTicks);
    }

    private record Cue(RegistryObject<SoundEvent> event, float volume, float pitch, long cooldownTicks) {}
}
