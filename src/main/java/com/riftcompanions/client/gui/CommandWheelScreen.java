package com.riftcompanions.client.gui;

import com.riftcompanions.network.C2SCompanionCommandPacket;
import com.riftcompanions.network.CompanionCommand;
import com.riftcompanions.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Lightweight radial command surface. It exposes only safe, bounded team
 * commands; role-specific/power decisions remain available in Team Journal.
 */
public final class CommandWheelScreen extends Screen {
    private static final int WHEEL_RADIUS = 64;
    private int centerX;
    private int centerY;

    public CommandWheelScreen() {
        super(Component.literal("Team Commands"));
    }

    @Override
    protected void init() {
        this.centerX = this.width / 2;
        this.centerY = this.height / 2;
        final WheelEntry[] entries = {
                new WheelEntry("Follow Me", CompanionCommand.FOLLOW_ALL),
                new WheelEntry("Regroup", CompanionCommand.REGROUP),
                new WheelEntry("Focus Target", CompanionCommand.FOCUS_TARGET),
                new WheelEntry("Retreat", CompanionCommand.RETREAT),
                new WheelEntry("Safe Recall", CompanionCommand.RECALL_ALL),
                new WheelEntry("Combat: Off", CompanionCommand.COMBAT_OFF),
                new WheelEntry("Stop All Actions", CompanionCommand.STOP_ALL_ACTIONS),
                new WheelEntry("Set Home", CompanionCommand.SET_HOME_ANCHOR),
                new WheelEntry("Call Team Home", CompanionCommand.RETURN_HOME),
                new WheelEntry("Check Structure", CompanionCommand.CHECK_STRUCTURE),
                new WheelEntry("Rescue Ally", CompanionCommand.REVIVE_NEAREST),
                new WheelEntry("Cancel Plan", CompanionCommand.CANCEL_PLAN),
                new WheelEntry("Team Journal", null)
        };
        final int orbitX = Math.max(110, Math.min(190, (this.width - 110) / 2));
        final int orbitY = Math.max(78, Math.min(122, (this.height - 70) / 2));
        for (int i = 0; i < entries.length; i++) {
            final double angle = -Math.PI / 2.0D + (Math.PI * 2.0D * i / entries.length);
            final int x = centerX + (int) (Math.cos(angle) * orbitX) - 45;
            final int y = centerY + (int) (Math.sin(angle) * orbitY) - 10;
            final WheelEntry entry = entries[i];
            this.addRenderableWidget(Button.builder(Component.literal(entry.label()), button -> {
                        if (entry.command() == null) {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new TeamJournalScreen());
                            }
                        } else {
                            ModNetwork.sendToServer(C2SCompanionCommandPacket.of(entry.command(), null, null));
                            this.onClose();
                        }
                    })
                    .bounds(x, y, 90, 20).build());
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x88060A11);
        drawDisk(graphics, centerX, centerY, WHEEL_RADIUS, 0xE61A2637);
        drawDisk(graphics, centerX, centerY, 26, 0xFF314762);
        graphics.drawCenteredString(this.font, Component.literal("Team"), centerX, centerY - 4, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal("Press Esc to close"), centerX, centerY + WHEEL_RADIUS + 18, 0xFFB5C4DB);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawDisk(final GuiGraphics graphics, final int cx, final int cy, final int radius, final int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            final int halfWidth = (int) Math.sqrt(radius * radius - dy * dy);
            graphics.fill(cx - halfWidth, cy + dy, cx + halfWidth + 1, cy + dy + 1, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record WheelEntry(String label, CompanionCommand command) {}
}
