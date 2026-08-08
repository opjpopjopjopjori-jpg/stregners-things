package com.riftcompanions.client.gui;

import com.riftcompanions.client.ClientTeamState;
import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionLifecycle;
import com.riftcompanions.network.C2SCompanionCommandPacket;
import com.riftcompanions.network.C2SRequestTeamStatusPacket;
import com.riftcompanions.network.CompanionCommand;
import com.riftcompanions.network.ModNetwork;
import com.riftcompanions.network.C2SSelectDuoPacket;
import com.riftcompanions.network.C2SPowerPolicyPacket;
import com.riftcompanions.duo.TeamPair;
import com.riftcompanions.network.TeamStatusSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Read-only team journal plus bounded player commands. It displays server
 * reasons/state instead of pretending client UI can make authoritative choices.
 */
public final class TeamJournalScreen extends Screen {
    public enum Tab { SUMMARY, SQUAD, PLAN, POLICIES, JOURNAL, GUIDE, SOCIAL, DIAGNOSTICS }

    private Tab activeTab;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public TeamJournalScreen() {
        this(Tab.SUMMARY);
    }

    public TeamJournalScreen(final Tab initialTab) {
        super(Component.literal("Team Journal"));
        this.activeTab = initialTab;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(518, this.width - 16);
        this.panelHeight = Math.min(332, this.height - 16);
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;
        ModNetwork.sendToServer(new C2SRequestTeamStatusPacket());

        final int tabY = panelY + 28;
        int tabX = panelX + 12;
        final int tabWidth = Math.max(36, (panelWidth - 24 - (Tab.values().length - 1) * 2) / Tab.values().length);
        for (final Tab tab : Tab.values()) {
            final Tab target = tab;
            this.addRenderableWidget(Button.builder(Component.literal(tabLabel(tab)), button -> {
                        this.activeTab = target;
                        ModNetwork.sendToServer(new C2SRequestTeamStatusPacket());
                    })
                    .bounds(tabX, tabY, tabWidth, 20).build());
            tabX += tabWidth + 2;
        }

        // Bounded global orders.
        int x = panelX + 12;
        final int commandY = panelY + panelHeight - 52;
        addCommandButton("Follow Me", x, commandY, 78, CompanionCommand.FOLLOW_ALL, null, null); x += 82;
        addCommandButton("Retreat", x, commandY, 78, CompanionCommand.RETREAT, null, null); x += 82;
        addCommandButton("Recall", x, commandY, 78, CompanionCommand.RECALL_ALL, null, null); x += 82;
        final TeamStatusSnapshot initialSnapshot = ClientTeamState.snapshot();
        addCommandButton(initialSnapshot.safeModeActive() ? "Resume" : "Stop All", x, commandY, 70,
                initialSnapshot.safeModeActive() ? CompanionCommand.CLEAR_SAFE_MODE : CompanionCommand.STOP_ALL_ACTIONS, null, null); x += 74;
        addCommandButton("Clear Plan", x, commandY, 70, CompanionCommand.CANCEL_PLAN, null, null); x += 74;
        addCommandButton("Rescue", x, commandY, 70, CompanionCommand.REVIVE_NEAREST, null, null);

        // Plan controls are safe to show at all times: server rejects them unless a draft exists.
        final int planControlY = panelY + panelHeight - 78;
        addCommandButton("Disrupt", panelX + 12, planControlY, 58, CompanionCommand.USE_ABILITY, null, CompanionAbility.SEER_DISRUPT);
        addCommandButton("Rescue", panelX + 74, planControlY, 58, CompanionCommand.USE_ABILITY, null, CompanionAbility.GIFTED_RESCUE);
        addCommandButton("Ground", panelX + 136, planControlY, 58, CompanionCommand.USE_ABILITY, null, CompanionAbility.SCOUT_GROUNDING);
        addCommandButton("Brace", panelX + 198, planControlY, 58, CompanionCommand.USE_ABILITY, null, CompanionAbility.GUARDIAN_BRACE);
        addCommandButton("Check", panelX + panelWidth - 196, planControlY, 62, CompanionCommand.CHECK_STRUCTURE, null, null);
        addCommandButton("Accept", panelX + panelWidth - 130, planControlY, 58, CompanionCommand.ACCEPT_PLAN, null, null);
        addCommandButton("Decline", panelX + panelWidth - 68, planControlY, 56, CompanionCommand.DECLINE_PLAN, null, null);

        // Role calls and explicitly selected powers stay visible; unavailable
        // requests are rejected server-side with an explainable feedback code.
        x = panelX + 12;
        final int roleY = panelY + panelHeight - 26;
        for (final CompanionRole role : CompanionRole.values()) {
            addCommandButton(shortRole(role), x, roleY, 72, CompanionCommand.CALL_ROLE, role, null);
            x += 76;
        }
        addCommandButton("Sense", panelX + panelWidth - 192, roleY, 38, CompanionCommand.USE_ABILITY, null, CompanionAbility.SEER_SENSE);
        addCommandButton("Push", panelX + panelWidth - 150, roleY, 38, CompanionCommand.USE_ABILITY, null, CompanionAbility.GIFTED_PUSH);
        addCommandButton("Shield", panelX + panelWidth - 108, roleY, 48, CompanionCommand.USE_ABILITY, null, CompanionAbility.GIFTED_SHIELD);
        addCommandButton("Signal", panelX + panelWidth - 56, roleY, 50, CompanionCommand.USE_ABILITY, null, CompanionAbility.SCOUT_SIGNAL);
    }

    private void addCommandButton(final String label, final int x, final int y, final int width,
                                  final CompanionCommand command, final CompanionRole role, final CompanionAbility ability) {
        this.addRenderableWidget(Button.builder(Component.literal(label), button -> {
                    ModNetwork.sendToServer(C2SCompanionCommandPacket.of(command, role, ability));
                })
                .bounds(x, y, width, 20).build());
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        this.renderBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE6111824);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + 24, 0xFF26364D);
        graphics.drawCenteredString(this.font, Component.literal("Team Journal — Rift Companions"), panelX + panelWidth / 2, panelY + 9, 0xFFF1F5FF);
        graphics.fill(panelX + 10, panelY + 54, panelX + panelWidth - 10, panelY + panelHeight - 60, 0xA9121A27);

        final TeamStatusSnapshot snapshot = ClientTeamState.snapshot();
        switch (activeTab) {
            case SUMMARY -> renderSummary(graphics, snapshot);
            case SQUAD -> renderSquad(graphics, snapshot);
            case PLAN -> renderPlan(graphics, snapshot);
            case POLICIES -> renderPolicies(graphics, snapshot);
            case JOURNAL -> renderJournal(graphics, snapshot);
            case GUIDE -> renderGuide(graphics, snapshot);
            case SOCIAL -> renderSocial(graphics, snapshot);
            case DIAGNOSTICS -> renderDiagnostics(graphics, snapshot);
        }
        renderFeedback(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSummary(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        int y = panelY + 65;
        graphics.drawString(font, "Team Overview", panelX + 18, y, 0xFFB9D8FF, false); y += 16;
        graphics.drawString(font, "Plan: " + snapshot.planType() + " / " + snapshot.planStatus(), panelX + 18, y, planColor(snapshot.dangerScore()), false); y += 14;
        graphics.drawString(font, "Story: " + snapshot.storySummary(), panelX + 18, y, 0xFFB9D8FF, false); y += 14;
        graphics.drawString(font, "Duo: " + snapshot.duoSummary(), panelX + 18, y, 0xFF9FE5B3, false); y += 14;
        graphics.drawString(font, "Intention: " + snapshot.intentionSummary(), panelX + 18, y, 0xFFD0C0FF, false); y += 14;
        graphics.drawString(font, "Gifted readiness: " + shorten(snapshot.giftedReadinessSummary(), 52), panelX + 18, y, 0xFFD0C0FF, false); y += 14;
        graphics.drawString(font, "Guardian review: " + shorten(snapshot.guardianReviewSummary(), 48), panelX + 18, y, 0xFFB9D8FF, false); y += 14;
        graphics.drawString(font, snapshot.encounterSummary(), panelX + 18, y, 0xFF9CCBFF, false); y += 14;
        graphics.drawString(font, snapshot.playerIdentitySummary(), panelX + 18, y, 0xFF9FAEC5, false); y += 14;
        if (snapshot.safeModeActive()) {
            graphics.drawString(font, "SAFE MODE: " + snapshot.safeModeReason() + " — " + snapshot.safeModeDetail(), panelX + 18, y, 0xFFFFC26A, false); y += 14;
        }
        graphics.drawString(font, "Danger Level: " + snapshot.dangerScore() + "/100", panelX + 18, y, dangerColor(snapshot.dangerScore()), false); y += 16;
        if (snapshot.hasHomeAnchor()) {
            final BlockPos home = BlockPos.of(snapshot.homeAnchor());
            graphics.drawString(font, "HOME: " + home.getX() + ", " + home.getY() + ", " + home.getZ(), panelX + 18, y, 0xFFB0E8C0, false);
        } else {
            graphics.drawString(font, "HOME: not set — open the Command Wheel and select Set Home.", panelX + 18, y, 0xFF9FAEC5, false);
        }
        y += 18;
        for (final TeamStatusSnapshot.CompanionView view : snapshot.companions()) {
            if (view.lifecycle() != CompanionLifecycle.ACTIVE && view.lifecycle() != CompanionLifecycle.DOWNED) continue;
            final int color = stateColor(view.state().name());
            graphics.fill(panelX + 18, y + 2, panelX + 22, y + 12, color);
            graphics.drawString(font, view.role().personalName() + " — " + view.lifecycle() + " / " + view.state(), panelX + 28, y, 0xFFE4EBF6, false);
            graphics.drawString(font, "Energy " + Math.round(view.energy()) + "%", panelX + 320, y, energyColor(view.energy()), false);
            y += 18;
        }
        y += 6;
        graphics.drawString(font, "G: Command Wheel   J: Team Journal   R: Safe Recall   F8: Developer Overlay", panelX + 18, y, 0xFF91A4BF, false);
    }

    private void renderSquad(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        int y = panelY + 66;
        graphics.drawString(font, "Choose an Active Duo only at a quiet HOME or REST anchor", panelX + 18, y, 0xFFB9D8FF, false); y += 14;
        graphics.drawString(font, snapshot.duoSummary(), panelX + 18, y, 0xFF9FE5B3, false); y += 18;
        graphics.drawString(font, "Click a pair to switch safely. The logical server validates it; client UI never changes a plan directly.", panelX + 18, y, 0xFF9FAEC5, false); y += 18;
        for (int i = 0; i < TeamPair.values().length; i++) {
            final TeamPair pair = TeamPair.values()[i];
            final int column = i % 2;
            final int row = i / 2;
            final int x = panelX + 18 + column * 238;
            final int cardY = y + row * 46;
            graphics.fill(x, cardY, x + 224, cardY + 38, 0xAA1A2638);
            graphics.drawString(font, pair.first().personalName() + " + " + pair.second().personalName(), x + 8, cardY + 6, 0xFFF2F6FF, false);
            graphics.drawString(font, pair.identity(), x + 8, cardY + 19, 0xFF9FE5B3, false);
            graphics.drawString(font, "Best for: " + shorten(pair.recommendedContexts(), 28), x + 8, cardY + 30, 0xFF9FAEC5, false);
        }
        // The server returns a readable feedback reason when the requested pair is blocked by danger, a plan, or DOWNED state.
    }

    private void renderPlan(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        int y = panelY + 66;
        graphics.drawString(font, "Current Plan: " + snapshot.planType(), panelX + 18, y, 0xFFDFECFF, false); y += 16;
        graphics.drawString(font, "Status: " + snapshot.planStatus() + " | Formation: " + snapshot.formation(), panelX + 18, y, planColor(snapshot.dangerScore()), false); y += 16;
        graphics.drawString(font, "Danger: " + snapshot.dangerScore() + "/100", panelX + 18, y, dangerColor(snapshot.dangerScore()), false); y += 18;
        if (!snapshot.planObjective().isBlank()) {
            graphics.drawString(font, "Objective: " + snapshot.planObjective(), panelX + 18, y, 0xFFE4EBF6, false); y += 15;
        }
        if (snapshot.planAwaitingApproval()) {
            graphics.drawString(font, "This plan needs your decision: Accept or Decline.", panelX + 18, y, 0xFFFFD17A, false); y += 15;
            graphics.drawString(font, "A: " + snapshot.planA(), panelX + 26, y, 0xFFB9D8FF, false); y += 14;
            graphics.drawString(font, "B: " + snapshot.planB(), panelX + 26, y, 0xFF9FE5B3, false); y += 14;
            graphics.drawString(font, "Cancel when: " + snapshot.abortCondition(), panelX + 26, y, 0xFFFF9C9C, false); y += 16;
        }
        graphics.drawString(font, "Decision reasons:", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        if (snapshot.reasonCodes().isEmpty()) {
            graphics.drawString(font, "No active plan or warning.", panelX + 28, y, 0xFF8D9AAF, false);
        } else {
            for (final String reason : snapshot.reasonCodes()) {
                if (y >= panelY + panelHeight - 122) {
                    break;
                }
                graphics.drawString(font, "• " + reason, panelX + 28, y, 0xFFE4EBF6, false);
                y += 14;
            }
        }
        graphics.drawString(font, "Building automation: disabled by design. No companion places or breaks blocks.", panelX + 18, panelY + panelHeight - 114, 0xFFFFD17A, false);
    }

    private void renderPolicies(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        int y = panelY + 66;
        graphics.drawString(font, "Personal Power Policies", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        graphics.drawString(font, "Left-click cycles a safe policy. Right-click resets to the world config.", panelX + 18, y, 0xFF9FAEC5, false); y += 18;
        for (int i = 0; i < snapshot.powerPolicies().size(); i++) {
            final TeamStatusSnapshot.PolicyView policy = snapshot.powerPolicies().get(i);
            final int cardY = y + i * 46;
            graphics.fill(panelX + 18, cardY, panelX + panelWidth - 18, cardY + 38, 0xAA1A2638);
            graphics.drawString(font, policy.role().personalName() + " — " + policy.policy(), panelX + 28, cardY + 7, 0xFFF2F6FF, false);
            graphics.drawString(font, policy.overridden() ? "Personal override active" : "Inheriting world config", panelX + 28, cardY + 21,
                    policy.overridden() ? 0xFFD0C0FF : 0xFF9FE5B3, false);
        }
        graphics.drawString(font, "Policies never bypass Safe Mode, target validation, cooldowns, energy, strain, or Safety Gates.", panelX + 18, y + 146, 0xFFFFD17A, false);
    }

    private void renderJournal(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        final TeamStatusSnapshot.StoryJournalView story = snapshot.storyJournal();
        int y = panelY + 66;
        graphics.drawString(font, "Story Board — optional evidence, never a progression gate", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        int visibleChapters = 0;
        for (final TeamStatusSnapshot.StoryChapterView chapter : story.chapters()) {
            if (chapter.status() == com.riftcompanions.story.StoryNodeStatus.LOCKED || visibleChapters >= 2) continue;
            graphics.drawString(font, chapter.title() + " — " + chapter.status(), panelX + 22, y, storyStatusColor(chapter.status()), false); y += 13;
            visibleChapters++;
        }
        if (visibleChapters == 0) {
            graphics.drawString(font, "No optional chapter is active or available right now.", panelX + 22, y, 0xFF8D9AAF, false); y += 13;
        }
        final TeamStatusSnapshot.StoryPromiseView promise = story.activePromise();
        graphics.drawString(font, promise.present() ? "Promise: " + promise.role().personalName() + " — " + promise.status()
                : "Promise: no optional promise is open.", panelX + 18, y, 0xFFD0C0FF, false); y += 13;
        if (promise.present()) {
            graphics.drawString(font, shorten(promise.summary(), 72), panelX + 28, y, 0xFFE4EBF6, false); y += 15;
        }
        graphics.drawString(font, "Mystery Board: " + story.totalClueCount() + " evidence record(s)", panelX + 18, y, 0xFF9CCBFF, false); y += 13;
        if (story.clues().isEmpty()) {
            graphics.drawString(font, "No visible-evidence clue has been recorded.", panelX + 28, y, 0xFF8D9AAF, false); y += 13;
        } else {
            for (final TeamStatusSnapshot.StoryClueView clue : story.clues()) {
                if (y >= panelY + panelHeight - 126) break;
                final String prefix = "[Day " + clue.day() + "] " + clue.confidence() + (clue.deferred() ? " — deferred" : "");
                graphics.drawString(font, prefix, panelX + 28, y, clue.deferred() ? 0xFF9FAEC5 : 0xFF9CCBFF, false); y += 12;
                graphics.drawString(font, shorten(clue.summary(), 70), panelX + 36, y, 0xFFE4EBF6, false); y += 14;
            }
        }
        graphics.drawString(font, "Recent memory: " + (snapshot.recentMemories().isEmpty() ? "none" : shorten(snapshot.recentMemories().get(snapshot.recentMemories().size() - 1).summary(), 58)),
                panelX + 18, panelY + panelHeight - 121, 0xFF9FE5B3, false);
        graphics.drawString(font, "Commands: /companions mystery status | promise status | story status", panelX + 18, panelY + panelHeight - 107, 0xFF91A4BF, false);
    }

    private void renderGuide(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        final TeamStatusSnapshot.GuidanceView guidance = snapshot.guidance();
        int y = panelY + 66;
        graphics.drawString(font, "Getting Started", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        graphics.drawString(font, "1. Call a role.  2. Use Follow, Hold, Recall, or the Team Journal.  3. You keep the final decision.", panelX + 18, y, 0xFFE4EBF6, false); y += 18;
        graphics.drawString(font, "Optional guidance: " + guidance.completedHints() + "/" + guidance.totalHints()
                + (guidance.tutorialDismissed() ? " — dismissed" : guidance.tutorialConfigEnabled() ? " — active" : " — disabled in config"), panelX + 18, y, 0xFFD0C0FF, false); y += 15;
        graphics.drawString(font, "Next: " + guidance.nextHintTitle(), panelX + 18, y, 0xFF9FE5B3, false); y += 14;
        graphics.drawString(font, shorten(guidance.nextHintDetail(), 76), panelX + 28, y, 0xFF9FAEC5, false); y += 20;
        graphics.drawString(font, "Vanilla Rule Policy", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        graphics.drawString(font, "Mode: " + guidance.mode() + " | Difficulty: " + guidance.difficulty()
                + " | Hardcore: " + guidance.hardcore(), panelX + 18, y, 0xFFE4EBF6, false); y += 14;
        graphics.drawString(font, "Mob Griefing: " + guidance.mobGriefing() + " | Keep Inventory: " + guidance.keepInventory()
                + " | Daylight Cycle: " + guidance.daylightCycle(), panelX + 18, y, 0xFF9FAEC5, false); y += 14;
        graphics.drawString(font, "Companion world edits: " + (guidance.companionWorldEditsAllowed() ? "allowed" : "disabled by design"), panelX + 18, y, 0xFFFFD17A, false); y += 14;
        graphics.drawString(font, shorten(guidance.operationalSummary(), 76), panelX + 28, y, 0xFF9FAEC5, false); y += 14;
        graphics.drawString(font, "Team Supply: " + shorten(snapshot.teamSupply().availability(), 65), panelX + 18, y,
                snapshot.teamSupply().enabled() && snapshot.teamSupply().bound() ? 0xFF9FE5B3 : 0xFF9FAEC5, false); y += 20;
        graphics.drawString(font, "Safety: Stop All pauses optional action. Clear Plan returns Follow. Reset Task cancels one local route.", panelX + 18, y, 0xFFFFD17A, false); y += 14;
        graphics.drawString(font, "Accessibility: ChatProfile CRITICAL_ONLY keeps P0 alerts only; HUD and Journal remain readable.", panelX + 18, y, 0xFFB9D8FF, false); y += 18;
        graphics.drawString(font, "Commands: /companions tutorial status | dismiss | resume", panelX + 18, y, 0xFF9FE5B3, false);
    }

    private void renderSocial(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        final TeamStatusSnapshot.SocialView social = snapshot.social();
        int y = panelY + 66;
        graphics.drawString(font, "Social Director", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        graphics.drawString(font, social.enabled() ? "Status: enabled" : "Status: disabled by the world feature flag", panelX + 18, y,
                social.enabled() ? 0xFF9FE5B3 : 0xFFFFC26A, false); y += 16;
        graphics.drawString(font, "Recent cue: " + social.cue() + " / " + social.phase(), panelX + 18, y, 0xFFE4EBF6, false); y += 15;
        final String speakers = social.lead() == null ? "No active pair exchange." : "Lead: " + social.lead().personalName()
                + (social.reply() == null ? "" : " | Reply: " + social.reply().personalName());
        graphics.drawString(font, speakers, panelX + 18, y, 0xFFD0C0FF, false); y += 15;
        if (social.pendingReplyTicks() > 0L) {
            graphics.drawString(font, "Reply window: " + social.pendingReplyTicks() + " ticks", panelX + 18, y, 0xFF9CCBFF, false); y += 15;
        }
        graphics.drawString(font, shorten(social.summary(), 74), panelX + 18, y, 0xFF9FAEC5, false); y += 22;
        graphics.drawString(font, "Cues: campfire, weather, horizon, safe base, player-selected work block, or calm local context.", panelX + 18, y, 0xFFB9D8FF, false); y += 15;
        graphics.drawString(font, "Safety gate: no danger, no active plan, no Safe Mode, two nearby active companions, and normal chat budget.", panelX + 18, y, 0xFFFFD17A, false); y += 15;
        graphics.drawString(font, "No container reading, hidden-world scan, camera lock, player-input change, power cast, or gameplay authority.", panelX + 18, y, 0xFF9FAEC5, false); y += 20;
        graphics.drawString(font, "Player conversations: Read the world, Team check, Last encounter, and Check in are available when the team is safe.", panelX + 18, y, 0xFF9FE5B3, false);
    }

    private void renderDiagnostics(final GuiGraphics graphics, final TeamStatusSnapshot snapshot) {
        int y = panelY + 66;
        graphics.drawString(font, "Diagnostics — readable information, no stack traces", panelX + 18, y, 0xFFB9D8FF, false); y += 14;
        graphics.drawString(font, snapshot.safeModeActive() ? "Safe Mode: " + snapshot.safeModeReason() + " — " + snapshot.safeModeDetail() : "Safe Mode: inactive", panelX + 18, y, snapshot.safeModeActive() ? 0xFFFFC26A : 0xFF9FE5B3, false); y += 14;
        graphics.drawString(font, "Gifted: " + snapshot.giftedReadinessSummary(), panelX + 18, y, 0xFFD0C0FF, false); y += 14;
        graphics.drawString(font, "Guardian review: " + shorten(snapshot.guardianReviewSummary(), 52), panelX + 18, y, 0xFFB9D8FF, false); y += 18;
        for (final TeamStatusSnapshot.CompanionView view : snapshot.companions()) {
            graphics.drawString(font, view.role().id() + " | " + view.lifecycle() + " | " + view.state() + " | energy=" + Math.round(view.energy()), panelX + 18, y, 0xFFE4EBF6, false); y += 13;
            graphics.drawString(font, "strain=" + Math.round(view.hiveStrain()) + " | focus=" + Math.round(view.focus()) + " | rescue=" + view.rescueProgress() + "/60", panelX + 28, y, 0xFF9FAEC5, false); y += 13;
            graphics.drawString(font, "reason=" + view.reason() + " | combat=" + view.combatEnabled() + " | trust=" + view.trust() + " | entity=" + view.entityId(), panelX + 28, y, 0xFF9FAEC5, false); y += 17;
        }
        if (snapshot.hasSafeWaypoint()) {
            final BlockPos pos = BlockPos.of(snapshot.safeWaypoint());
            graphics.drawString(font, "Last Safe Waypoint: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), panelX + 18, y, 0xFFB0E8C0, false);
        } else {
            graphics.drawString(font, "Last Safe Waypoint: unavailable", panelX + 18, y, 0xFFE6B1B1, false);
        }
        y += 15;
        if (snapshot.hasHomeAnchor()) {
            final BlockPos home = BlockPos.of(snapshot.homeAnchor());
            graphics.drawString(font, "HOME: " + snapshot.homeAnchorDimension() + " @ " + home.getX() + ", " + home.getY() + ", " + home.getZ(), panelX + 18, y, 0xFFB0E8C0, false);
        }
    }

    private void renderFeedback(final GuiGraphics graphics) {
        final ClientTeamState.Feedback feedback = ClientTeamState.feedback();
        if (!feedback.active()) {
            return;
        }
        final int color = feedback.success() ? 0xFF92E5B1 : 0xFFFF9C9C;
        graphics.fill(panelX + 12, panelY + panelHeight - 104, panelX + panelWidth - 12, panelY + panelHeight - 84, 0xD91B2637);
        graphics.drawString(font, feedback.detail(), panelX + 18, panelY + panelHeight - 98, color, false);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if ((button == 0 || button == 1) && activeTab == Tab.POLICIES) {
            final int startY = panelY + 99;
            final TeamStatusSnapshot snapshot = ClientTeamState.snapshot();
            for (int i = 0; i < snapshot.powerPolicies().size(); i++) {
                final int y = startY + i * 46;
                if (mouseX >= panelX + 18 && mouseX <= panelX + panelWidth - 18 && mouseY >= y && mouseY <= y + 38) {
                    final CompanionRole role = snapshot.powerPolicies().get(i).role();
                    ModNetwork.sendToServer(button == 1 ? C2SPowerPolicyPacket.reset(role) : C2SPowerPolicyPacket.cycle(role));
                    return true;
                }
            }
        }
        if (button == 0 && activeTab == Tab.SQUAD) {
            final int startY = panelY + 116;
            for (int i = 0; i < TeamPair.values().length; i++) {
                final int column = i % 2;
                final int row = i / 2;
                final int x = panelX + 18 + column * 238;
                final int y = startY + row * 46;
                if (mouseX >= x && mouseX <= x + 224 && mouseY >= y && mouseY <= y + 38) {
                    ModNetwork.sendToServer(C2SSelectDuoPacket.of(TeamPair.values()[i]));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String shorten(final String value, final int limit) {
        return value.length() <= limit ? value : value.substring(0, Math.max(1, limit - 1)) + "…";
    }

    private static String tabLabel(final Tab tab) {
        return switch (tab) {
            case SUMMARY -> "Team";
            case SQUAD -> "Squad";
            case PLAN -> "Plan";
            case POLICIES -> "Policy";
            case JOURNAL -> "Journal";
            case GUIDE -> "Guide";
            case SOCIAL -> "Social";
            case DIAGNOSTICS -> "Status";
        };
    }

    private static String shortRole(final CompanionRole role) {
        return switch (role) {
            case SEER -> "Will";
            case GUARDIAN -> "Hopper";
            case GIFTED -> "Eleven";
            case SCOUT -> "Max";
        };
    }

    private static int dangerColor(final int danger) {
        return danger >= 70 ? 0xFFFF7070 : danger >= 40 ? 0xFFFFC26A : 0xFF8FE2B0;
    }

    private static int planColor(final int danger) {
        return danger >= 60 ? 0xFFFFC26A : 0xFFB9D8FF;
    }

    private static int energyColor(final float energy) {
        return energy <= 25.0F ? 0xFFFF8E8E : energy <= 55.0F ? 0xFFFFD17A : 0xFF91DBFF;
    }

    private static int storyStatusColor(final com.riftcompanions.story.StoryNodeStatus status) {
        return switch (status) {
            case ACTIVE -> 0xFF9FE5B3;
            case AVAILABLE -> 0xFFB9D8FF;
            case DEFERRED -> 0xFF9FAEC5;
            case COMPLETED -> 0xFFD0C0FF;
            case LOCKED -> 0xFF6F7C90;
        };
    }

    private static int stateColor(final String state) {
        return switch (state) {
            case "DOWNED" -> 0xFFFF6F6F;
            case "RETREATING", "STUCK_RECOVERY" -> 0xFFFFC26A;
            case "FIGHTING", "GUARDING" -> 0xFF9CCBFF;
            case "EXHAUSTED", "RECOVERING" -> 0xFFD0A3FF;
            default -> 0xFF9FE5B3;
        };
    }
}
