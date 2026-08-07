package com.riftcompanions.network;

import net.minecraft.network.chat.Component;

public final class StregnerStoryCommand {
    private StregnerStoryCommand() {}

    public static String execute() {
        return "STORY_SCREEN_ACTIVATED: Use /stregners story activate to open the 5-chapter interface.";
    }
}
