package com.riftcompanions.server;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.resource.ItemCategory;
import com.riftcompanions.resource.ItemValueClassifier;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import com.riftcompanions.safety.SafeModeReason;
import com.riftcompanions.safety.SafeModeService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Explicit player-to-companion transfers only. No chest access or auto-loot is enabled here. */
public final class CompanionInventoryService {
    private CompanionInventoryService() {}

    public static InventoryResult giveOneHeldItem(final ServerPlayer player, final CompanionRole role) {
        final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            return InventoryResult.failure("NO_LOADED_COMPANION", "The companion is not currently loaded.");
        }
        final CompanionEntity target = companion.get();
        final ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return InventoryResult.failure("NO_HELD_ITEM", "Hold the item you want to give to the companion.");
        }
        // Manual transfer is always the player's deliberate choice; category is shown for transparency.
        final ItemCategory category = ItemValueClassifier.classify(held);
        final long now = player.level().getGameTime();
        final var data = TeamSavedData.get(player.server);
        final var transactionBegin = data.blackboard(player.getUUID()).actionLedger().begin(ActionType.INVENTORY_GIVE,
                "inventory-give:" + role.id() + ":" + now, held.getItem().toString(), now, 80L);
        if (transactionBegin.reused()) return InventoryResult.failure("INVENTORY_ACTION_ALREADY_PENDING", "That transfer is already pending confirmation.");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("HELD_ITEM_VALIDATED");
        final int beforeCount = held.getCount();
        if (!target.getPersonalInventory().insertOneFrom(held)) {
            transaction.rollback(now, "INVENTORY_FULL_OR_SOURCE_EMPTY");
            data.markChanged();
            DialogueService.get().speak(target, "resource_low", 2);
            return InventoryResult.failure("INVENTORY_FULL", "The companion inventory is full; no item was taken.");
        }
        if (held.getCount() != beforeCount - 1) {
            transaction.rollback(now, "SOURCE_POSTCONDITION_FAILED");
            data.markChanged();
            SafeModeService.enable(player, SafeModeReason.INVALID_WORLD_STATE,
                    "Manual inventory transfer postcondition did not match the expected one-item move.");
            return InventoryResult.failure("ITEM_TRANSFER_POSTCONDITION_FAILED", "The transfer needs review; Safe Mode was enabled to prevent further automated assumptions.");
        }
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("COMPANION_INVENTORY_UPDATED");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "ITEM_TRANSFER_CONFIRMED");
        data.markChanged();
        if (category == ItemCategory.RARE_OR_PROTECTED || category == ItemCategory.UNKNOWN) {
            DialogueService.get().speak(target, "resource_found", 2);
        }
        return InventoryResult.success("ITEM_TRANSFERRED", "Gave one item to " + role.personalName() + " (" + category + ").");
    }

    public static InventoryResult withdrawSlot(final ServerPlayer player, final CompanionRole role, final int slot) {
        final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            return InventoryResult.failure("NO_LOADED_COMPANION", "The companion is not currently loaded.");
        }
        final long now = player.level().getGameTime();
        final var data = TeamSavedData.get(player.server);
        final var transactionBegin = data.blackboard(player.getUUID()).actionLedger().begin(ActionType.INVENTORY_WITHDRAW,
                "inventory-withdraw:" + role.id() + ":" + slot + ":" + now, "slot:" + slot, now, 80L);
        if (transactionBegin.reused()) return InventoryResult.failure("INVENTORY_ACTION_ALREADY_PENDING", "That withdrawal is already pending confirmation.");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("INVENTORY_SLOT_VALIDATED");
        final ItemStack stack = companion.get().getPersonalInventory().withdraw(slot);
        if (stack.isEmpty()) {
            transaction.rollback(now, "EMPTY_OR_INVALID_SLOT");
            data.markChanged();
            return InventoryResult.failure("EMPTY_OR_INVALID_SLOT", "That slot is empty or invalid; no items changed.");
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("PLAYER_RECEIVED_WITHDRAWAL");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "ITEM_WITHDRAWAL_CONFIRMED");
        data.markChanged();
        return InventoryResult.success("ITEM_WITHDRAWN", "Returned the item from " + role.personalName() + " to the player.");
    }

    /** Dismissal anti-exploit rule: personal items return to the owner before entity removal. */
    public static void returnAllToPlayer(final ServerPlayer player, final CompanionEntity companion) {
        for (final ItemStack stack : companion.getPersonalInventory().withdrawAll()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    public static InventoryResult status(final ServerPlayer player, final CompanionRole role) {
        final Optional<CompanionEntity> companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            return InventoryResult.failure("NO_LOADED_COMPANION", "The companion is not currently loaded.");
        }
        return InventoryResult.success("INVENTORY_STATUS", role.personalName() + ": "
                + companion.get().getPersonalInventory().occupiedSlots() + "/" + companion.get().getPersonalInventory().size() + " slots used.");
    }

    public record InventoryResult(boolean successful, String code, String detail) {
        public static InventoryResult success(final String code, final String detail) { return new InventoryResult(true, code, detail); }
        public static InventoryResult failure(final String code, final String detail) { return new InventoryResult(false, code, detail); }
    }
}
