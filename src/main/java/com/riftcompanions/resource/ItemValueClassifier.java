package com.riftcompanions.resource;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Vanilla-only conservative classifier. Modded items remain UNKNOWN until a safe support pack classifies them. */
public final class ItemValueClassifier {
    private ItemValueClassifier() {}

    public static ItemCategory classify(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemCategory.JUNK_OR_LOW_PRIORITY;
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return ItemCategory.UNKNOWN;
        final ItemCategory dataOverride = ItemClassificationRegistry.override(id).orElse(null);
        if (dataOverride != null) return dataOverride;
        if (!"minecraft".equals(id.getNamespace())) return ItemCategory.UNKNOWN;

        if (stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.NETHER_STAR) || stack.is(Items.DRAGON_EGG)
                || stack.is(Items.DIAMOND) || stack.is(Items.NETHERITE_INGOT)
                || stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_PICKAXE)
                || stack.is(Items.NETHERITE_AXE) || stack.is(Items.NETHERITE_SHOVEL)
                || stack.is(Items.LAVA_BUCKET) || stack.is(Items.TNT) || stack.is(Items.REDSTONE)) {
            return ItemCategory.RARE_OR_PROTECTED;
        }
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.SUSPICIOUS_STEW)) return ItemCategory.HEALING;
        if (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)) return ItemCategory.PERSONAL_AMMO;
        if (stack.is(Items.MAP) || stack.is(Items.FILLED_MAP) || stack.is(Items.COMPASS) || stack.is(Items.RECOVERY_COMPASS)
                || stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK)) return ItemCategory.QUEST_OR_MEMORY_ITEM;
        if (stack.is(Items.IRON_SWORD) || stack.is(Items.IRON_AXE) || stack.is(Items.BOW) || stack.is(Items.CROSSBOW)
                || stack.is(Items.SHIELD)) return ItemCategory.PERSONAL_WEAPON;
        if (stack.is(Items.COBBLESTONE) || stack.is(Items.DIRT) || stack.is(Items.OAK_PLANKS)) return ItemCategory.BUILDING_MATERIAL_SAFE;
        if (stack.isEdible()) return ItemCategory.BASIC_FOOD;
        return ItemCategory.JUNK_OR_LOW_PRIORITY;
    }
}
