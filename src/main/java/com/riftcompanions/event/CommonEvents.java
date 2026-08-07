package com.riftcompanions.event;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.debug.DecisionTraceService;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.AbilityService;
import com.riftcompanions.server.BaseAnchorService;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.server.TeamStatusService;
import com.riftcompanions.server.ThreatObservationService;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.TeamPlanService;
import com.riftcompanions.team.events.TeamEventType;
import com.riftcompanions.scene.SetPieceService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Event-driven safety reactions; no expensive world scan runs every tick. */
@Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void onEntityJoin(final EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof CompanionEntity companion) {
            CompanionLifecycleService.acceptEntityLoad(companion);
        }
    }

    @SubscribeEvent
    public static void onCompanionDeath(final LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof CompanionEntity companion
                && CompanionConfig.STORY_DOWNED.get()) {
            event.setCanceled(true);
            companion.enterDownedState();
            companion.getOwnerPlayer().ifPresent(owner -> {
                final TeamSavedData data = TeamSavedData.get(owner.server);
                final var board = data.blackboard(owner.getUUID());
                board.addMemory(new MemoryRecord(MemoryType.TEAM_RESCUE,
                        owner.level().getGameTime() / 24000L, companion.getRole().personalName() + " is down and needs a safe rescue.", 100));
                board.beginQuietWindow(owner.level().getGameTime(), 300L);
                SetPieceService.beginSilentWalk(owner, 300L);
                data.markChanged();
                TeamDirector.submit(owner, TeamEventType.COMPANION_DOWNED, companion.blockPosition(), List.of("COMPANION_DOWNED_" + companion.getRole().id()));
                com.riftcompanions.server.RescueChoreographyService.onCompanionDowned(owner, companion);
                CompanionLifecycleService.findForOwner(owner, CompanionRole.GUARDIAN)
                        .filter(guardian -> guardian != companion)
                        .ifPresent(guardian -> DialogueService.get().speak(guardian, "team_rescue", 0));
                TeamStatusService.sync(owner);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(final LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TeamPlanService.cancel(player);
        final var returnResult = BaseAnchorService.returnHome(player);
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion -> {
                companion.setTarget(null);
                companion.setCombatEnabled(false);
                if (!returnResult.successful()) {
                    companion.setCompanionState(com.riftcompanions.entity.CompanionState.HOLDING, "PLAYER_DEATH_HOLD_SAFE_POSITION");
                }
            });
        }
        final TeamSavedData data = TeamSavedData.get(player.server);
        data.blackboard(player.getUUID()).addMemory(new MemoryRecord(MemoryType.MILESTONE,
                player.level().getGameTime() / 24000L, "Player death suspended team plans; companions moved to a safe fallback.", 90));
        data.blackboard(player.getUUID()).beginQuietWindow(player.level().getGameTime(), 300L);
        data.markChanged();
    }

    @SubscribeEvent
    public static void onLivingTick(final LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) {
            com.riftcompanions.hive.control.HiveControlManager.tick(event.getEntity());
        }
    }

    /** Downed companions must not become invulnerable bait for hostile farms. */
    @SubscribeEvent
    public static void onLivingChangeTarget(final LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getNewTarget() instanceof CompanionEntity companion)
                || companion.getCompanionState() != com.riftcompanions.entity.CompanionState.DOWNED) {
            return;
        }
        final ServerPlayer owner = companion.getOwnerPlayer().orElse(null);
        event.setNewTarget(owner != null && owner.isAlive() && owner.level() == companion.level() ? owner : null);
    }

    @SubscribeEvent
    public static void onLivingHurt(final LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        final var attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof ServerPlayer player) {
            if (attacker instanceof Monster monster) {
                ThreatObservationService.observeDirectDamage(player, monster);
            }
            float protectionMultiplier = 1.0F;
            if (com.riftcompanions.server.GuardianBraceService.protects(player)) {
                // Guardian Brace is intentionally weaker than Gifted Shield and
                // can stack only as bounded mitigation, never invulnerability.
                protectionMultiplier *= 0.80F;
            }
            if (AbilityService.hasShieldFor(player)) {
                // Protection, not invulnerability. The player still needs to make
                // choices and cannot AFK through a hostile encounter.
                protectionMultiplier *= 0.58F;
            }
            if (protectionMultiplier < 1.0F) event.setAmount(event.getAmount() * protectionMultiplier);
            return;
        }
        if (event.getEntity() instanceof CompanionEntity companion) {
            com.riftcompanions.hive.control.HiveChannelManager.interrupt(companion, event.getAmount());
            if (companion.getCompanionState() != com.riftcompanions.entity.CompanionState.DOWNED) {
                companion.beginVisualAction(com.riftcompanions.entity.CompanionAction.HIT_REACT, 8L);
            }
            if (attacker instanceof Monster monster) {
                companion.getOwnerPlayer().ifPresent(owner -> ThreatObservationService.observeDirectDamage(owner, monster));
            }
        }
    }

    /** Records only the player-selected interaction block; companions never inspect its contents. */
    @SubscribeEvent
    public static void onPlayerRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            com.riftcompanions.spatial.SpatialEtiquetteService.observeInteraction(player, event.getPos());
            com.riftcompanions.social.CompanionSocialDirector.observePlayerInteraction(player, event.getPos());
            com.riftcompanions.context.ContextualInteractionDirector.observePlayerBlock(player, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!player.server.isSingleplayer()) {
            return;
        }
        com.riftcompanions.hive.control.HiveChannelManager.tick(player);
        com.riftcompanions.server.GuardianBraceService.tick(player);
        com.riftcompanions.resource.WorldFreePickupService.tick(player);
        com.riftcompanions.mental.MindAnchorService.tick(player);
        if (player.tickCount % 40 == 0 && com.riftcompanions.mode.GameModePolicyService.apply(player).permitsAutomaticSurvivalPlans()) {
            final boolean criticalHealth = player.getHealth() <= player.getMaxHealth() * 0.25F;
            final boolean hostilesClose = !player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(10.0D)).isEmpty();
            if (CompanionConfig.AUTO_CRITICAL_RETREAT.get() && criticalHealth && hostilesClose) {
                TeamDirector.submit(player, TeamEventType.PLAYER_HEALTH_CRITICAL, List.of("PLAYER_HEALTH_CRITICAL", "HOSTILES_CLOSE"));
            }
            if (player.fallDistance > 3.0F || player.isInLava()) {
                TeamDirector.submit(player, TeamEventType.PLAYER_FALL_RISK, List.of(player.isInLava() ? "LAVA_RISK" : "FALL_RISK"));
            }
            ThreatObservationService.observeNearestVisibleThreat(player);
        }
        if (player.tickCount % 10 == 0) {
            TeamDirector.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DialogueService.get().clearSession(player.getUUID());
            ThreatObservationService.clearSession(player.getUUID());
            com.riftcompanions.conversation.ConversationService.clearSession(player.getUUID());
            com.riftcompanions.social.CompanionSocialDirector.clearSession(player.getUUID());
            com.riftcompanions.resource.ResourceRequestService.clear(player.getUUID());
            com.riftcompanions.server.GuardianBraceService.cancelForOwner(player, "PLAYER_LOGOUT");
            com.riftcompanions.server.SafeTeleport.clearOwnerReservations(player.getUUID());
            com.riftcompanions.presentation.CompanionGaitPresentationService.clearOwner(player.getUUID());
            TeamDirector.clearSession(player.getUUID());
            DecisionTraceService.clear(player.getUUID());
            com.riftcompanions.performance.CompanionPerformanceMonitor.clear(player.getUUID());
        }
    }
}
