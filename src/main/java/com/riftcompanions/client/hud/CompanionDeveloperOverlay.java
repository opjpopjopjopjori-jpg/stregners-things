package com.riftcompanions.client.hud;

import com.riftcompanions.client.ClientTeamState;
import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.network.TeamStatusSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Opt-in developer-only display. It renders an owner-synchronized diagnostic
 * projection and never changes server state, input, camera, or AI behavior.
 */
public final class CompanionDeveloperOverlay {
    public static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private static boolean initialized;
    private static boolean visible;

    private CompanionDeveloperOverlay() {}

    public static void toggle() {
        ensureInitialized();
        visible = !visible;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            visible = CompanionConfig.DEVELOPER_OVERLAY_DEFAULT.get();
            initialized = true;
        }
    }

    private static void render(final GuiGraphics graphics, final int width, final int height) {
        ensureInitialized();
        final Minecraft minecraft = Minecraft.getInstance();
        if (!visible || minecraft.player == null || minecraft.options.hideGui) return;

        final TeamStatusSnapshot snapshot = ClientTeamState.snapshot();
        final TeamStatusSnapshot.DeveloperSummary developer = snapshot.developerSummary();
        final int panelWidth = Math.min(360, Math.max(220, width / 2));
        final int x = Math.max(4, width - panelWidth - 6);
        int y = 8;
        final int lineHeight = 11;
        final int lineCount = 5 + snapshot.developerViews().size() * 2;
        final int panelHeight = Math.min(height - 12, 10 + lineCount * lineHeight);
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xD908111C);
        graphics.fill(x, y, x + panelWidth, y + 14, 0xFF26364D);
        graphics.drawString(minecraft.font, "Rift Developer Overlay  [F8]", x + 5, y + 3, 0xFFF1F5FF, false);
        y += 17;

        final String timing = developer.enabled()
                ? "Work: " + developer.sampledWorkMicros() + "us latest / " + developer.averageWorkMicros() + "us avg"
                : "Work timing: disabled in common config";
        graphics.drawString(minecraft.font, trim(timing, 54), x + 5, y, developer.enabled() ? 0xFF9FE5B3 : 0xFFFFD17A, false); y += lineHeight;
        final String costs = "AI " + developer.companionWorkMicros() + "/" + developer.companionAverageMicros()
                + "  Nav " + developer.navigationWorkMicros() + "/" + developer.navigationAverageMicros()
                + "  Director " + developer.directorWorkMicros() + "/" + developer.directorAverageMicros();
        graphics.drawString(minecraft.font, trim(costs, 54), x + 5, y, 0xFFB9D8FF, false); y += lineHeight;
        graphics.drawString(minecraft.font, "Events " + developer.pendingEvents() + " | Memory " + developer.memoryUsed() + "/" + developer.memoryLimit(), x + 5, y, 0xFF9FAEC5, false); y += lineHeight;
        final String chat = "Chat " + emptyAs(developer.chatTrigger(), "none") + " -> " + emptyAs(developer.chatOutcome(), "idle")
                + " | normal cooldown " + developer.chatBudgetRemainingTicks() + "t";
        graphics.drawString(minecraft.font, trim(chat, 54), x + 5, y, 0xFFD0C0FF, false); y += lineHeight + 2;

        for (final TeamStatusSnapshot.DeveloperView view : snapshot.developerViews()) {
            final String state = "[" + view.role().id() + "] " + view.task() + " | " + view.navigationIntent() + " / " + view.navigationOutcome();
            graphics.drawString(minecraft.font, trim(state, 54), x + 5, y, 0xFFE4EBF6, false); y += lineHeight;
            final String path = "target " + emptyAs(view.selectedTarget(), "none") + " | retries " + view.recoveryAttempts()
                    + " | " + emptyAs(view.navigationReason(), "NO_PATH_REASON");
            graphics.drawString(minecraft.font, trim(path, 54), x + 11, y, pathColor(view.navigationOutcome()), false); y += lineHeight;
        }
    }

    private static String emptyAs(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String trim(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, Math.max(1, maximum - 1)) + "…";
    }

    private static int pathColor(final String outcome) {
        return switch (outcome == null ? "" : outcome) {
            case "STUCK", "TARGET_REJECTED" -> 0xFFFF8E8E;
            case "RETRYING_ALTERNATE" -> 0xFFFFD17A;
            case "MOVING", "ARRIVED" -> 0xFF9FE5B3;
            default -> 0xFF9FAEC5;
        };
    }
}
