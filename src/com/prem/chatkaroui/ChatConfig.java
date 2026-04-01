package com.prem.chatkaroui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/**
 * Configuration bag passed from ChatKaroUI → ChatAdapter.
 *
 * v3.1 additions:
 * - starredIndicatorColor, starredIndicatorText
 * - editedLabelText, editedLabelColor, showEditedLabel
 * - htmlEnabledInChat, markdownEnabledInChat
 * - replyBubbleBgColor, replyAccentColor
 * - linkPreviewEnabled, linkPreviewBgColor, linkPreviewAccentColor
 * - showDefaultMenuItems
 */
public class ChatConfig {

    // ── Message colors ───────────────────────────────────────────────────────
    public int sentMessageBackgroundColor;
    public int receivedMessageBackgroundColor;
    public int sentMessageTextColor;
    public int receivedMessageTextColor;
    public int sentNameTextColor;
    public int receivedNameTextColor;
    public int selectedMessageBgColor;
    public int timestampTextColor;
    public int sentStatusTextColor;
    public int receivedStatusTextColor;
    public int systemMessageTextColor;
    public int typingIndicatorTextColor;
    public int avatarBackgroundColor;

    // ── Star / bookmark ──────────────────────────────────────────────────────
    public int starredIndicatorColor = 0xFFFFD700; // gold
    public String starredIndicatorText = "★";

    // ── Edited label ─────────────────────────────────────────────────────────
    public boolean showEditedLabel = true;
    public String editedLabelText = "edited";
    public int editedLabelColor = 0xFF888888;

    // ── Reply bubble ─────────────────────────────────────────────────────────
    public int replyBubbleBgColor = 0x220084FF; // semi-transparent sent color
    public int replyAccentColor = 0xFF0084FF; // left-border accent
    public int replyPreviewTextColor = 0xFF444444; // message preview text color

    // ── Link preview ─────────────────────────────────────────────────────────
    public boolean linkPreviewEnabled = true;
    public int linkPreviewBgColor = 0xFFF5F5F5;
    public int linkPreviewAccentColor = 0xFF0084FF;

    // ── Rendering modes ──────────────────────────────────────────────────────
    public boolean htmlEnabledInChat = false;
    public boolean markdownEnabledInChat = false;

    // ── Menu ─────────────────────────────────────────────────────────────────
    public boolean showDefaultMenuItems = true;

    // ── Sizes ────────────────────────────────────────────────────────────────
    public int messageFontSize;
    public int systemMessageFontSize;
    public int nameFontSize;
    public int timestampFontSize;
    public int avatarSize;
    public int textMessageMaxWidth;
    public int imageMessageMaxWidth;
    public int messageHorizontalPadding;
    public int messageVerticalPadding;
    public int arrangementWidthPx;

    // ── Floats ───────────────────────────────────────────────────────────────
    public float messageCornerRadius;
    public float squareEdgeCornerRadius;

    // ── Booleans ─────────────────────────────────────────────────────────────
    public boolean showTimestamp;
    public boolean showReadStatus;
    public boolean showMetadataInsideBubble;
    public boolean showMetadataOutBubble;
    public boolean showDateHeaders = true;
    public boolean squareBubbleEdge;
    public boolean autoLinkEnabledInChat;
    public boolean useResponsiveWidth;
    public boolean imageFunctionWidthFix;

    // ── Status text ──────────────────────────────────────────────────────────
    public String sentStatusText;
    public String receivedStatusText;

    // ── Typeface ─────────────────────────────────────────────────────────────
    public Typeface typeface;

    // ────────────────────────────────────────────────────────────────────────
    // Factory
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Creates a bubble GradientDrawable for regular message backgrounds.
     */
    public GradientDrawable createBubbleDrawable(boolean isSent) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(isSent ? sentMessageBackgroundColor : receivedMessageBackgroundColor);
        applyCorners(shape, isSent);
        return shape;
    }

    /**
     * Creates a subtle reply-quote bubble drawable.
     */
    public GradientDrawable createReplyDrawable(boolean isSent) {
        GradientDrawable shape = new GradientDrawable();
        // Sent messages (blue) look better with a semi-transparent white quote box.
        // Received messages (light gray) look better with the existing replyBubbleBgColor.
        shape.setColor(isSent ? 0x44FFFFFF : replyBubbleBgColor);
        shape.setCornerRadius(messageCornerRadius * 0.5f);
        return shape;
    }

    /**
     * Creates a link-preview card drawable.
     */
    public GradientDrawable createLinkPreviewDrawable() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(linkPreviewBgColor);
        shape.setCornerRadius(messageCornerRadius * 0.5f);
        return shape;
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private void applyCorners(GradientDrawable shape, boolean isSent) {
        if (squareBubbleEdge) {
            float[] radii = new float[8];
            if (isSent) {
                // top-left, top-right, bottom-right (square), bottom-left
                radii[0] = messageCornerRadius;
                radii[1] = messageCornerRadius;
                radii[2] = squareEdgeCornerRadius;
                radii[3] = squareEdgeCornerRadius;
                radii[4] = messageCornerRadius;
                radii[5] = messageCornerRadius;
                radii[6] = messageCornerRadius;
                radii[7] = messageCornerRadius;
            } else {
                radii[0] = squareEdgeCornerRadius;
                radii[1] = squareEdgeCornerRadius;
                radii[2] = messageCornerRadius;
                radii[3] = messageCornerRadius;
                radii[4] = messageCornerRadius;
                radii[5] = messageCornerRadius;
                radii[6] = messageCornerRadius;
                radii[7] = messageCornerRadius;
            }
            shape.setCornerRadii(radii);
        } else {
            shape.setCornerRadius(messageCornerRadius);
        }
    }
}