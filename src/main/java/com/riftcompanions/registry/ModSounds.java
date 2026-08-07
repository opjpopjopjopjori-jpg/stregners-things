package com.riftcompanions.registry;

import com.riftcompanions.RiftCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Original synthesized companion, Hive, UI, and ambience cue registrations. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RiftCompanions.MOD_ID);

    public static final RegistryObject<SoundEvent> HIVE_NOTICE = register("hive_notice");
    public static final RegistryObject<SoundEvent> HIVE_FOCUS = register("hive_focus");
    public static final RegistryObject<SoundEvent> HIVE_RELEASE = register("hive_release");
    public static final RegistryObject<SoundEvent> HIVE_RESIST = register("hive_resist");
    public static final RegistryObject<SoundEvent> HIVE_RECOVERY = register("hive_recovery");

    public static final RegistryObject<SoundEvent> GUARDIAN_GUARD_SIGNAL = register("guardian_guard_signal");
    public static final RegistryObject<SoundEvent> GUARDIAN_MELEE_SWING = register("guardian_melee_swing");
    public static final RegistryObject<SoundEvent> GUARDIAN_IMPACT = register("guardian_impact");
    public static final RegistryObject<SoundEvent> GUARDIAN_RETREAT = register("guardian_retreat");

    public static final RegistryObject<SoundEvent> SEER_NOTICE = register("seer_notice");
    public static final RegistryObject<SoundEvent> SEER_FOCUS = register("seer_focus");
    public static final RegistryObject<SoundEvent> SEER_RELEASE = register("seer_release");
    public static final RegistryObject<SoundEvent> SEER_SHATTER = register("seer_shatter");
    public static final RegistryObject<SoundEvent> SEER_RECOVERY = register("seer_recovery");

    public static final RegistryObject<SoundEvent> GIFTED_NOTICE = register("gifted_notice");
    public static final RegistryObject<SoundEvent> GIFTED_FOCUS = register("gifted_focus");
    public static final RegistryObject<SoundEvent> GIFTED_PUSH = register("gifted_push");
    public static final RegistryObject<SoundEvent> GIFTED_SHIELD = register("gifted_shield");
    public static final RegistryObject<SoundEvent> GIFTED_RESCUE = register("gifted_rescue");
    public static final RegistryObject<SoundEvent> GIFTED_EXHAUSTED = register("gifted_exhausted");

    public static final RegistryObject<SoundEvent> SCOUT_ROUTE = register("scout_route");
    public static final RegistryObject<SoundEvent> SCOUT_ANCHOR = register("scout_anchor");
    public static final RegistryObject<SoundEvent> SCOUT_DODGE = register("scout_dodge");

    public static final RegistryObject<SoundEvent> UI_PLAN_ACCEPT = register("ui_plan_accept");
    public static final RegistryObject<SoundEvent> UI_SAFE_MODE = register("ui_safe_mode");

    public static final RegistryObject<SoundEvent> AMBIENCE_GUARDIAN_BASE = register("ambience_guardian_base");
    public static final RegistryObject<SoundEvent> AMBIENCE_SEER_SIGNAL = register("ambience_seer_signal");
    public static final RegistryObject<SoundEvent> AMBIENCE_GIFTED_CALM = register("ambience_gifted_calm");
    public static final RegistryObject<SoundEvent> AMBIENCE_SCOUT_LOOKOUT = register("ambience_scout_lookout");

    private ModSounds() {}

    private static RegistryObject<SoundEvent> register(final String id) {
        return REGISTER.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RiftCompanions.MOD_ID, id)));
    }
}
