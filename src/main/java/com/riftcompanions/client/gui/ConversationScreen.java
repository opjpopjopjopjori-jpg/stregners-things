package com.riftcompanions.client.gui;

import com.riftcompanions.conversation.ConversationTopic;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.network.C2SConversationTopicPacket;
import com.riftcompanions.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Small topic menu; responses are generated on the server from visible team state. */
public final class ConversationScreen extends Screen {
    private final CompanionRole role;
    private int conversationTop;
    private int conversationHeight;

    public ConversationScreen(final CompanionRole role) {
        super(Component.literal("Talk to " + role.personalName()));
        this.role = role;
    }

    @Override
    protected void init() {
        final int x = this.width / 2 - 100;
        final List<ConversationTopic> available = availableTopics();
        this.conversationHeight = Math.min(342, Math.max(146, available.size() * 24 + 70));
        this.conversationTop = this.height / 2 - this.conversationHeight / 2;
        int y = this.conversationTop + 46;
        for (final ConversationTopic topic : available) {
            final ConversationTopic selected = topic;
            this.addRenderableWidget(Button.builder(Component.literal(label(topic)), button ->
                            ModNetwork.sendToServer(new C2SConversationTopicPacket(role.ordinal(), selected.ordinal())))
                    .bounds(x, y, 200, 20).build());
            y += 24;
        }
        this.addRenderableWidget(Button.builder(Component.literal("End Conversation"), button -> this.onClose())
                .bounds(x, y + 4, 200, 20).build());
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        this.renderBackground(graphics);
        final int left = this.width / 2 - 112;
        final int top = this.conversationTop;
        graphics.fill(left, top, left + 224, top + this.conversationHeight, 0xE6111824);
        graphics.drawCenteredString(this.font, Component.literal("Talk to " + role.personalName()), this.width / 2, top + 12, 0xFFF1F5FF);
        graphics.drawCenteredString(this.font, Component.literal("Replies use visible world, team, memory, and current state."), this.width / 2, top + 28, 0xFFB9D8FF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private List<ConversationTopic> availableTopics() {
        final var snapshot = com.riftcompanions.client.ClientTeamState.snapshot();
        final var view = snapshot.companions().stream().filter(entry -> entry.role() == role).findFirst().orElse(null);
        if (view != null && view.state() == com.riftcompanions.entity.CompanionState.DOWNED) {
            return List.of(ConversationTopic.ARE_YOU_OKAY, ConversationTopic.PLAN);
        }
        if (snapshot.planAwaitingApproval()) {
            return List.of(ConversationTopic.PLAN, ConversationTopic.ACCEPT_PLAN, ConversationTopic.DELAY_PLAN, ConversationTopic.ARE_YOU_OKAY);
        }
        if (snapshot.planStatus() == com.riftcompanions.team.TeamPlanStatus.ACTIVE) {
            return List.of(ConversationTopic.PLAN, ConversationTopic.ARE_YOU_OKAY, ConversationTopic.OFFER_REST);
        }
        return List.of(ConversationTopic.PLAN, ConversationTopic.ARE_YOU_OKAY, ConversationTopic.CHECK_IN,
                ConversationTopic.WORLD, ConversationTopic.TEAM, ConversationTopic.LAST_ENCOUNTER,
                ConversationTopic.MEMORY, ConversationTopic.PLACE, ConversationTopic.ROLE_POLICY, ConversationTopic.OFFER_REST);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String label(final ConversationTopic topic) {
        return switch (topic) {
            case PLAN -> "What is the plan?";
            case ARE_YOU_OKAY -> "Are you okay?";
            case MEMORY -> "What do you remember?";
            case PLACE -> "What do you think of this place?";
            case ROLE_POLICY -> "Your role and limits";
            case ACCEPT_PLAN -> "Accept the plan";
            case DELAY_PLAN -> "Delay the plan";
            case OFFER_REST -> "Take a rest";
            case WORLD -> "Read the world";
            case TEAM -> "Team check";
            case LAST_ENCOUNTER -> "Last encounter";
            case CHECK_IN -> "Check in";
        };
    }
}
