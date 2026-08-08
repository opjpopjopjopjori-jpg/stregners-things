package com.riftcompanions.client.hud;

import com.riftcompanions.client.ClientTeamState;
import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.dialogue.ChatProfile;
import com.riftcompanions.network.TeamStatusSnapshot;
import com.riftcompanions.entity.CompanionLifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Compact owner HUD rendered from a synchronized display snapshot. */
public final class CompanionHudOverlay {
    public static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private CompanionHudOverlay() {}

    private static void render(final GuiGraphics graphics, final int width, final int height) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!CompanionConfig.HUD_ENABLED.get() || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        final TeamStatusSnapshot snapshot = ClientTeamState.snapshot();
        final boolean compact = CompanionConfig.COMPACT_HUD.get();
        final int x = CompanionConfig.HUD_X_OFFSET.get();
        int y = CompanionConfig.HUD_Y_OFFSET.get();
        final int cardWidth = compact ? 150 : 220;
        final int cardHeight = compact ? 19 : 31;

        graphics.fill(x, y, x + cardWidth, y + 18, snapshot.safeModeActive() ? 0xD94A2A18 : 0xD9192639);
        final boolean criticalOnly = CompanionConfig.CHAT_PROFILE.get() == ChatProfile.CRITICAL_ONLY;
        final String teamLine = snapshot.safeModeActive() ? "SAFE MODE  " + snapshot.safeModeReason()
                : criticalOnly ? "TEAM  ALERTS ONLY  Danger " + snapshot.dangerScore()
                : "TEAM  " + snapshot.planType() + "  Danger " + snapshot.dangerScore();
        graphics.drawString(minecraft.font, teamLine, x + 6, y + 5,
                snapshot.safeModeActive() ? 0xFFFFD17A : dangerColor(snapshot.dangerScore()), false);
        y += 22;

        for (final TeamStatusSnapshot.CompanionView view : snapshot.companions()) {
            if (view.lifecycle() != CompanionLifecycle.ACTIVE && view.lifecycle() != CompanionLifecycle.DOWNED) {
                continue;
            }
            final int stateColor = stateColor(view.state().name());
            graphics.fill(x, y, x + cardWidth, y + cardHeight, 0xC9111826);
            graphics.fill(x, y, x + 4, y + cardHeight, stateColor);
            graphics.drawString(minecraft.font, roleCode(view) + " " + shortName(view), x + 8, y + 4, 0xFFF2F6FF, false);
            if (compact) {
                final String compactState = view.state().name().equals("DOWNED") ? "R " + view.rescueProgress() + "/60" : view.state().name();
                graphics.drawString(minecraft.font, compactState, x + 66, y + 4, stateColor, false);
                graphics.drawString(minecraft.font, Math.round(view.energy()) + "%", x + cardWidth - 31, y + 4, energyColor(view.energy()), false);
            } else {
                graphics.drawString(minecraft.font, view.lifecycle() + " / " + view.state(), x + 66, y + 4, stateColor, false);
                final int energyX = x + 8;
                final int energyY = y + 19;
                final int energyWidth = cardWidth - 16;
                graphics.fill(energyX, energyY, energyX + energyWidth, energyY + 5, 0xFF2B3444);
                graphics.fill(energyX, energyY, energyX + Math.round(energyWidth * Math.max(0.0F, Math.min(100.0F, view.energy())) / 100.0F), energyY + 5, energyColor(view.energy()));
            }
            y += cardHeight + 3;
        }

        final ClientTeamState.HiveLinkIndicator hive = ClientTeamState.hiveLink();
        if (hive.active()) {
            final int linkY = Math.min(height - 60, y + 2);
            final int linkWidth = Math.max(cardWidth, 370);
            final boolean dangerMode = hive.message() != null && hive.message().contains("DANGER MODE");
            final int bgColor = dangerMode ? 0xD9401020 : 0xD9192A40;
            final int titleColor = dangerMode ? 0xFFFF5555 : 0xFFB9E8FF;
            final int detailColor = dangerMode ? 0xFFFFAA88 : 0xFFDAE8FF;
            graphics.fill(x, linkY, x + linkWidth, linkY + 31, bgColor);
            if (dangerMode) {
                // Pulsing danger border
                final long pulse = System.currentTimeMillis() % 800L;
                final int borderAlpha = pulse < 400 ? 0xFF : (int) (0xFF * (1.0D - (pulse - 400) / 400.0D));
                final int borderColor = (borderAlpha << 24) | 0x00FF3333;
                graphics.fill(x, linkY, x + 3, linkY + 31, borderColor);
                graphics.fill(x + linkWidth - 3, linkY, x + linkWidth, linkY + 31, borderColor);
                graphics.drawString(minecraft.font, "\u26A1 WILL DANGER MODE \u26A1", x + 6, linkY + 4, titleColor, true);
            } else {
                graphics.drawString(minecraft.font, "WILL LINK: " + hive.message() + " (" + hive.remainingTicksNow() + "t)",
                        x + 6, linkY + 4, titleColor, false);
            }
            if (hive.controlCapacityHearts() > 0) {
                graphics.drawString(minecraft.font, "Targets " + hive.targetCount() + "/" + hive.targetLimit()
                                + " | Load " + hive.controlLoadHearts() + "/" + hive.controlCapacityHearts()
                                + " hearts (" + hive.controlLoadPercent() + "%)"
                                + (dangerMode ? " | 2x DURATION" : ""),
                        x + 6, linkY + 17, detailColor, false);
            }
        }

        final ClientTeamState.Feedback feedback = ClientTeamState.feedback();
        if (feedback.active()) {
            final int feedbackY = Math.min(height - 26, y + 2);
            graphics.fill(x, feedbackY, x + Math.max(cardWidth, 300), feedbackY + 19, 0xD9152030);
            graphics.drawString(minecraft.font, feedback.detail(), x + 6, feedbackY + 5, feedback.success() ? 0xFF98E9B7 : 0xFFFF9C9C, false);
        }
    }

    private static String roleCode(final TeamStatusSnapshot.CompanionView view) {
        return switch (view.role()) {
            case GUARDIAN -> "[G]";
            case SEER -> "[S]";
            case GIFTED -> "[P]";
            case SCOUT -> "[R]";
        };
    }

    private static String shortName(final TeamStatusSnapshot.CompanionView view) {
        return switch (view.role()) {
            case SEER -> "Will";
            case GUARDIAN -> "Hopper";
            case GIFTED -> "Eleven";
            case SCOUT -> "Max";
        };
    }

    private static int dangerColor(final int danger) {
        return danger >= 70 ? 0xFFFF8A8A : danger >= 40 ? 0xFFFFD17A : 0xFFB7D7FF;
    }

    private static int energyColor(final float energy) {
        return energy <= 25.0F ? 0xFFFF8585 : energy <= 55.0F ? 0xFFFFD17A : 0xFF78D9FF;
    }

    private static int stateColor(final String state) {
        if (CompanionConfig.HIGH_CONTRAST_MARKERS.get()) {
            return switch (state) {
                case "DOWNED" -> 0xFFFF2020;
                case "RETREATING", "STUCK_RECOVERY" -> 0xFFFFB000;
                case "FIGHTING", "GUARDING" -> 0xFF21A8FF;
                default -> 0xFF2EFF7B;
            };
        }
        return switch (state) {
            case "DOWNED" -> 0xFFFF6F6F;
            case "RETREATING", "STUCK_RECOVERY" -> 0xFFFFC26A;
            case "FIGHTING", "GUARDING" -> 0xFF9CCBFF;
            case "EXHAUSTED", "RECOVERING" -> 0xFFD0A3FF;
            default -> 0xFF9FE5B3;
        };
    }
}
