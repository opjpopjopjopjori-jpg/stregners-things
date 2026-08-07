package com.riftcompanions.client.gui;

import com.riftcompanions.story.StregnerChapterStoryFramework;
import com.riftcompanions.story.StregnerTaskSelectionInterface;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class StregnerStoryScreen extends net.minecraft.client.gui.screens.Screen {
    private final StregnerTaskSelectionInterface taskInterface;

    public StregnerStoryScreen(final StregnerTaskSelectionInterface interfaceRef) {
        super(Component.literal("Stregner Five Chapter Story"));
        this.taskInterface = interfaceRef;
    }

    @Override
    protected void init() {
        int y = 40;
        for (StregnerChapterStoryFramework.Chapter ch : StregnerChapterStoryFramework.Chapter.values()) {
            final StregnerChapterStoryFramework.Chapter chapter = ch;
            this.addRenderableWidget(Button.builder(Component.literal(chapter.name() + " (Seq: " + chapter.sequence + ")"),
                    btn -> this.showChapterDetails(chapter)).bounds(this.width / 2 - 120, y, 240, 20).build());
            y += 24;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Selected Tasks: " + taskInterface.getSelectionSummary()), btn -> {})
                .bounds(this.width / 2 - 120, y + 10, 240, 20).build());
    }

    private void showChapterDetails(StregnerChapterStoryFramework.Chapter ch) {
        // In a real implementation, open sub-screen or send packet to server
        System.out.println("Showing details for chapter: " + ch.name());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, Component.literal("Stregner 5-Chapter Quest Story"), this.width / 2, 16, 0xFFF1F5FF);
        graphics.drawCenteredString(this.font, Component.literal("Select exactly 5 tasks (5-10) from interface. Only selected tasks appear here."),
                this.width / 2, 34, 0xFFB9D8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
