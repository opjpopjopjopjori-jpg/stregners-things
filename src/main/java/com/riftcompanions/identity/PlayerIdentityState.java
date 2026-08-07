package com.riftcompanions.identity;

import net.minecraft.nbt.CompoundTag;

/** Bounded, opt-in player identity data; no hidden labels or generated insults. */
public final class PlayerIdentityState {
    private boolean nicknameEnabled;
    private String approvedNickname = "";
    private PlayerPlayStyle playStyle = PlayerPlayStyle.UNSET;

    public boolean nicknameEnabled() { return nicknameEnabled; }
    public String approvedNickname() { return approvedNickname; }
    public PlayerPlayStyle playStyle() { return playStyle; }

    public boolean setNickname(final String value) {
        final String normalized = value == null ? "" : value.trim();
        if (!normalized.matches("[A-Za-z0-9 _-]{2,16}")) return false;
        approvedNickname = normalized;
        nicknameEnabled = true;
        return true;
    }

    public void clearNickname() {
        approvedNickname = "";
        nicknameEnabled = false;
    }

    public void setPlayStyle(final PlayerPlayStyle value) {
        playStyle = value == null ? PlayerPlayStyle.UNSET : value;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("NicknameEnabled", nicknameEnabled);
        tag.putString("Nickname", approvedNickname);
        tag.putString("PlayStyle", playStyle.name());
        return tag;
    }

    public static PlayerIdentityState load(final CompoundTag tag) {
        final PlayerIdentityState state = new PlayerIdentityState();
        if (tag == null) return state;
        if (tag.getBoolean("NicknameEnabled")) state.setNickname(tag.getString("Nickname"));
        try { state.playStyle = PlayerPlayStyle.valueOf(tag.getString("PlayStyle")); }
        catch (final IllegalArgumentException ignored) { state.playStyle = PlayerPlayStyle.UNSET; }
        return state;
    }
}
