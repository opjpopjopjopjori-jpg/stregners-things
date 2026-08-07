package com.riftcompanions.policy;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.power.PowerPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Per-owner power-policy overrides. Missing values deliberately inherit world config. */
public final class PlayerPolicyState {
    private final EnumMap<CompanionRole, PowerPolicy> powerOverrides = new EnumMap<>(CompanionRole.class);

    public Optional<PowerPolicy> override(final CompanionRole role) {
        return Optional.ofNullable(powerOverrides.get(role));
    }

    public void setOverride(final CompanionRole role, final PowerPolicy policy) {
        if (role == null) return;
        if (policy == null) powerOverrides.remove(role);
        else powerOverrides.put(role, policy);
    }

    public Map<CompanionRole, PowerPolicy> overrides() { return Map.copyOf(powerOverrides); }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final ListTag entries = new ListTag();
        powerOverrides.forEach((role, policy) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putString("Role", role.name());
            entry.putString("Policy", policy.name());
            entries.add(entry);
        });
        tag.put("PowerOverrides", entries);
        return tag;
    }

    public static PlayerPolicyState load(final CompoundTag tag) {
        final PlayerPolicyState state = new PlayerPolicyState();
        if (tag == null) return state;
        final ListTag entries = tag.getList("PowerOverrides", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            final CompoundTag entry = entries.getCompound(i);
            try {
                final CompanionRole role = CompanionRole.valueOf(entry.getString("Role"));
                final PowerPolicy policy = PowerPolicy.valueOf(entry.getString("Policy"));
                if (PlayerPolicyService.supportsPowerPolicy(role, policy)) state.powerOverrides.put(role, policy);
            } catch (final IllegalArgumentException ignored) { }
        }
        return state;
    }
}
