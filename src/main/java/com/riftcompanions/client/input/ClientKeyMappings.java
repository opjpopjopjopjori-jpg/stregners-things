package com.riftcompanions.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** All keys are rebindable through Minecraft's standard Controls menu. */
public final class ClientKeyMappings {
    public static final String CATEGORY = "key.categories.riftcompanions";
    public static final KeyMapping OPEN_COMMAND_WHEEL = new KeyMapping(
            "key.riftcompanions.command_wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping OPEN_TEAM_JOURNAL = new KeyMapping(
            "key.riftcompanions.team_journal", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping QUICK_RECALL = new KeyMapping(
            "key.riftcompanions.quick_recall", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping QUICK_STATUS = new KeyMapping(
            "key.riftcompanions.quick_status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping TOGGLE_DEVELOPER_OVERLAY = new KeyMapping(
            "key.riftcompanions.toggle_developer_overlay", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY);

    private ClientKeyMappings() {}

    public static void register(final RegisterKeyMappingsEvent event) {
        event.register(OPEN_COMMAND_WHEEL);
        event.register(OPEN_TEAM_JOURNAL);
        event.register(QUICK_RECALL);
        event.register(QUICK_STATUS);
        event.register(TOGGLE_DEVELOPER_OVERLAY);
    }
}
