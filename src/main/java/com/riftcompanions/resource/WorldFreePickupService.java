package com.riftcompanions.resource;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * Optional P3/P5 ground-item assistance. It never opens containers, mines a
 * block, walks toward distant loot, or consumes unknown/rare items. Default
 * policy and feature flag keep it disabled until explicit testing enables it.
 */
public final class WorldFreePickupService {
    private WorldFreePickupService() {}

    public static void tick(final ServerPlayer player) {
        if (player == null || player.server == null || !FeatureFlags.enabled(FeatureFlag.INVENTORY_ASSIST) || !com.riftcompanions.mode.PerformancePolicy.allowsInventoryAssist()) return;
        final AutoPickupPolicy policy = CompanionConfig.INVENTORY_PICKUP_POLICY.get();
        if (policy == AutoPickupPolicy.OFF || policy == AutoPickupPolicy.TEAM_CHEST_ONLY) return;
        if (player.level().getGameTime() % CompanionConfig.RESOURCE_PICKUP_SCAN_INTERVAL_TICKS.get() != 0L) return;
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion == null || companion.level() != player.level() || companion.getCompanionState() == com.riftcompanions.entity.CompanionState.DOWNED) continue;
            attemptOne(player, companion, board, policy);
        }
    }

    private static void attemptOne(final ServerPlayer player, final CompanionEntity companion,
                                   final com.riftcompanions.team.TeamBlackboard board, final AutoPickupPolicy policy) {
        if (!player.level().getEntitiesOfClass(Monster.class, companion.getBoundingBox().inflate(5.0D)).isEmpty()) return;
        final double radius = CompanionConfig.RESOURCE_PICKUP_RADIUS.get();
        final List<ItemEntity> candidates = player.level().getEntitiesOfClass(ItemEntity.class, companion.getBoundingBox().inflate(radius),
                item -> item.isAlive() && ResourceOwnershipResolver.worldItemOwnership(item) == ResourceOwnership.WORLD_FREE)
                .stream().sorted(Comparator.comparingDouble(companion::distanceToSqr))
                .limit(CompanionConfig.RESOURCE_PICKUP_MAX_PER_SCAN.get()).toList();
        for (final ItemEntity item : candidates) {
            if (companion.distanceToSqr(item) > radius * radius) continue;
            if (board.hasSensitiveAnnotation(player.level().dimension().location(), item.blockPosition(), 6.0D)) continue;
            final ItemStack stack = item.getItem();
            final ItemCategory category = ItemValueClassifier.classify(stack);
            if (CompanionResourceProfile.neverAutoTake(category)) {
                if (policy == AutoPickupPolicy.ASK_BEFORE_TAKING || category == ItemCategory.UNKNOWN) {
                    ResourceRequestService.request(player, companion, category);
                }
                continue;
            }
            final boolean emergencyOnly = policy == AutoPickupPolicy.EMERGENCY_ONLY;
            if (policy == AutoPickupPolicy.ASK_BEFORE_TAKING) {
                ResourceRequestService.request(player, companion, category);
                continue;
            }
            final int utility = ItemUtilityScorer.score(player, companion, category, emergencyOnly);
            if (utility < CompanionConfig.RESOURCE_PICKUP_MIN_UTILITY.get()) continue;
            if (tryTransferOne(player, companion, item, category, board)) return;
        }
    }

    private static boolean tryTransferOne(final ServerPlayer player, final CompanionEntity companion, final ItemEntity item,
                                          final ItemCategory category, final com.riftcompanions.team.TeamBlackboard board) {
        final long now = player.level().getGameTime();
        final var begun = board.actionLedger().begin(ActionType.WORLD_ITEM_PICKUP,
                "world-item:" + item.getUUID(), String.valueOf(item.getItem().getItem()), now, 80L);
        if (begun.reused()) return false;
        final ActionTransaction transaction = begun.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("WORLD_FREE_ITEM_VALIDATED");
        if (!board.reservations().claim(com.riftcompanions.transaction.ReservationType.ITEM_ENTITY, item.getUUID().toString(),
                transaction.actionId(), now, 60L)) {
            transaction.rollback(now, "ITEM_ALREADY_RESERVED");
            return false;
        }
        final ItemStack source = item.getItem();
        final int before = source.getCount();
        if (!companion.getPersonalInventory().insertOneFrom(source) || source.getCount() != before - 1) {
            board.reservations().release(com.riftcompanions.transaction.ReservationType.ITEM_ENTITY, item.getUUID().toString());
            transaction.rollback(now, "WORLD_ITEM_TRANSFER_REJECTED");
            return false;
        }
        if (source.isEmpty()) item.discard();
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("WORLD_ITEM_INVENTORY_CONFIRMED");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "WORLD_ITEM_PICKUP_COMMITTED");
        board.reservations().release(com.riftcompanions.transaction.ReservationType.ITEM_ENTITY, item.getUUID().toString());
        if (category == ItemCategory.HEALING || category == ItemCategory.PERSONAL_AMMO || category == ItemCategory.QUEST_OR_MEMORY_ITEM) {
            board.addMemory(new MemoryRecord(MemoryType.MILESTONE, now / 24000L,
                    companion.getRole().personalName() + " collected one approved " + category + " item from the ground.", 72));
        }
        TeamSavedData.get(player.server).markChanged();
        return true;
    }
}
