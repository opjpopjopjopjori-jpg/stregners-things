package com.riftcompanions.mental;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.SafeTeleport;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamPlanService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Comparator;
import java.util.List;

/**
 * Max's grounded mental-support actions. They affect only explicit supported
 * effects, consume Focus, require safe local space, and never heal physical
 * damage, teleport, control monsters, or bypass boss mechanics.
 */
public final class MindAnchorService {
    private MindAnchorService() {}

    public static Result grounding(final ServerPlayer player, final CompanionEntity scout) {
        final MobEffect effect = highestSupportedEffect(player);
        if (effect == null) return Result.failure("NO_SUPPORTED_MENTAL_EFFECT", "No supported mental effect is active.");
        if (!scout.consumeFocus(25.0F)) return Result.failure("SCOUT_FOCUS_LOW", "Max does not have enough Focus for Grounding.");
        player.removeEffect(effect);
        scout.setAbilityCooldown(CompanionAbility.SCOUT_GROUNDING, CompanionConfig.MIND_ANCHOR_GROUNDING_COOLDOWN_TICKS.get());
        scout.beginVisualAction(CompanionAction.SCOUT_ANCHOR, 16L);
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(), effectCount(18), 0.45D, 0.75D, 0.45D, 0.03D);
        return Result.success("MIND_ANCHOR_GROUNDING", "Grounding cleared one supported mental effect.");
    }

    public static Result createAnchor(final ServerPlayer player, final CompanionEntity scout) {
        final long now = player.level().getGameTime();
        final MindAnchorState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).mindAnchor();
        if (state.cooldownRemaining(now) > 0L) return Result.failure("MIND_ANCHOR_COOLDOWN", "Mind Anchor is still recovering.");
        final BlockPos safe = SafeTeleport.findSafeSpot(player.serverLevel(), scout, player.blockPosition(), 3).orElse(null);
        if (safe == null) return Result.failure("MIND_ANCHOR_NO_SAFE_POSITION", "No safe local anchor position is available.");
        if (!scout.consumeFocus(35.0F)) return Result.failure("SCOUT_FOCUS_LOW", "Max does not have enough Focus for an Anchor Point.");
        state.activate(player.level().dimension().location(), safe, now, CompanionConfig.MIND_ANCHOR_DURATION_TICKS.get(),
                CompanionConfig.MIND_ANCHOR_COOLDOWN_TICKS.get());
        scout.setAbilityCooldown(CompanionAbility.SCOUT_ANCHOR_POINT, CompanionConfig.MIND_ANCHOR_COOLDOWN_TICKS.get());
        scout.beginVisualAction(CompanionAction.SCOUT_ANCHOR, 20L);
        player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                safe.getX() + 0.5D, safe.getY() + 0.7D, safe.getZ() + 0.5D, effectCount(24), 0.55D, 0.45D, 0.55D, 0.02D);
        TeamSavedData.get(player.server).markChanged();
        return Result.success("MIND_ANCHOR_POINT_ACTIVE", "Max established a temporary safe mental anchor. Stay within the local radius.");
    }

    public static Result breakFree(final ServerPlayer player, final CompanionEntity scout) {
        final long now = player.level().getGameTime();
        final MindAnchorState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).mindAnchor();
        if (!state.activeAt(player.level().dimension().location(), now)) return Result.failure("MIND_ANCHOR_NOT_ACTIVE", "Create a safe Anchor Point before trying Break Free.");
        final MobEffect effect = highestSupportedEffect(player);
        if (effect == null) return Result.failure("NO_SUPPORTED_MENTAL_EFFECT", "No supported mental effect is active.");
        if (!scout.consumeFocus(30.0F)) return Result.failure("SCOUT_FOCUS_LOW", "Max does not have enough Focus for Break Free.");
        player.removeEffect(effect);
        scout.setAbilityCooldown(CompanionAbility.SCOUT_BREAK_FREE, CompanionConfig.MIND_ANCHOR_BREAK_FREE_COOLDOWN_TICKS.get());
        scout.beginVisualAction(CompanionAction.SCOUT_ANCHOR, 18L);
        return Result.success("MIND_ANCHOR_BREAK_FREE", "Break Free cleared one supported mental effect near the anchor.");
    }

    public static Result escapeWindow(final ServerPlayer player, final CompanionEntity scout) {
        final long now = player.level().getGameTime();
        final MindAnchorState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).mindAnchor();
        final boolean needsAnchor = !state.activeAt(player.level().dimension().location(), now);
        final float totalFocus = needsAnchor ? 55.0F : 20.0F;
        if (scout.getFocus() < totalFocus) return Result.failure("SCOUT_FOCUS_LOW", "Max does not have enough Focus for an Escape Window.");
        if (needsAnchor) {
            final Result anchor = createAnchor(player, scout);
            if (!anchor.successful()) return anchor;
        }
        final MobEffect effect = highestSupportedEffect(player);
        if (effect != null) player.removeEffect(effect);
        if (!scout.consumeFocus(20.0F)) return Result.failure("SCOUT_FOCUS_LOW", "Max does not have enough Focus for an Escape Window.");
        scout.setAbilityCooldown(CompanionAbility.SCOUT_ESCAPE_WINDOW, CompanionConfig.MIND_ANCHOR_ESCAPE_COOLDOWN_TICKS.get());
        scout.beginVisualAction(CompanionAction.SCOUT_POINT, 16L);
        final boolean retreat = TeamPlanService.beginRetreat(player, "MIND_ANCHOR_ESCAPE_WINDOW");
        return Result.success(retreat ? "MIND_ANCHOR_ESCAPE_WINDOW" : "MIND_ANCHOR_ESCAPE_WINDOW_READY",
                "Max opened a short mental escape window; follow the known retreat route.");
    }

    /** Active anchor only shortens explicitly supported effects; it never grants damage immunity. */
    public static void tick(final ServerPlayer player) {
        if (player == null || player.server == null || !CompanionConfig.MIND_ANCHOR_ENABLED.get()) return;
        final MindAnchorState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).mindAnchor();
        final long now = player.level().getGameTime();
        if (!state.activeAt(player.level().dimension().location(), now)) return;
        final BlockPos position = state.position().orElse(null);
        if (position == null || player.blockPosition().distSqr(position) > CompanionConfig.MIND_ANCHOR_RADIUS.get() * CompanionConfig.MIND_ANCHOR_RADIUS.get()) return;
        if (now % 20L != 0L) return;
        for (final MobEffect effect : MentalEffectRegistry.supportedEffects()) {
            final MobEffectInstance active = player.getEffect(effect);
            if (active == null) continue;
            final int reduced = Math.max(1, active.getDuration() - CompanionConfig.MIND_ANCHOR_DURATION_REDUCTION_PER_SECOND.get());
            player.addEffect(new MobEffectInstance(effect, reduced, active.getAmplifier(), false, true, true));
        }
    }

    private static MobEffect highestSupportedEffect(final ServerPlayer player) {
        return MentalEffectRegistry.supportedEffects().stream()
                .filter(player::hasEffect)
                .max(Comparator.comparingInt(effect -> MentalEffectRegistry.profile(effect).map(MentalEffectProfile::priority).orElse(0)))
                .orElse(null);
    }

    private static int effectCount(final int normal) {
        return CompanionConfig.LOW_EFFECTS.get() ? Math.max(1, normal / 4) : normal;
    }

    public record Result(boolean successful, String code, String detail) {
        public static Result success(final String code, final String detail) { return new Result(true, code, detail); }
        public static Result failure(final String code, final String detail) { return new Result(false, code, detail); }
    }
}
