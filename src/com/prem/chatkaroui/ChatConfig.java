package com.prem.chatkaroui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

/**
 * Immutable-ish config bag passed to ChatAdapter.
 * Mirrors all the styling fields from ChatKaroUI.
 * When a property setter changes a value, ChatKaroUI updates this object
 * and calls adapter.notifyDataSetChanged() if needed.
 */
public class ChatConfig {

    // Colors
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

    // Sizes
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

    // Floats
    public float messageCornerRadius;
    public float squareEdgeCornerRadius;

    // Booleans
    public boolean showTimestamp;
    public boolean showReadStatus;
    public boolean showMetadataInsideBubble;
    public boolean showMetadataOutBubble;
    public boolean squareBubbleEdge;
    public boolean autoLinkEnabledInChat;
    public boolean useResponsiveWidth;
    public boolean imageFunctionWidthFix;

    // Strings
    public String sentStatusText;
    public String receivedStatusText;

    // Typeface
    public Typeface typeface;

    /**
     * Creates a bubble GradientDrawable based on current config.
     * Called by ChatAdapter during onBindViewHolder.
     */
    public GradientDrawable createBubbleDrawable(boolean isSent) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(isSent ? sentMessageBackgroundColor : receivedMessageBackgroundColor);

        if (squareBubbleEdge) {
            float[] radii = new float[8];
            if (isSent) {
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
        return shape;
    }
}