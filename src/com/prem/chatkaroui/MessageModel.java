package com.prem.chatkaroui;

/**
 * Data model representing a single chat item (message or date header or system
 * message).
 * The RecyclerView Adapter holds a List<MessageModel> — NO View references.
 */
public class MessageModel {

    // View types for RecyclerView
    public static final int TYPE_SENT_SIMPLE = 0;
    public static final int TYPE_RECEIVED_SIMPLE = 1;
    public static final int TYPE_SENT_AVATAR = 2;
    public static final int TYPE_RECEIVED_AVATAR = 3;
    public static final int TYPE_SENT_TEXT_IMAGE = 4;
    public static final int TYPE_RECEIVED_TEXT_IMAGE = 5;
    public static final int TYPE_SYSTEM = 6;
    public static final int TYPE_DATE_HEADER = 7;
    public static final int TYPE_TYPING_INDICATOR = 8;

    // Core fields
    public final int messageId;
    public final int viewType;
    public String message; // mutable for UpdateMessageById
    public final String imageUrl;
    public final String avatarUrl;
    public final String senderName;
    public final String timestamp;
    public final boolean messageOnTop; // for text+image messages
    public boolean isSelected; // mutable for selection state

    /**
     * Constructor for messages (sent/received simple or avatar)
     */
    public MessageModel(int messageId, int viewType, String message,
            String avatarUrl, String senderName, String timestamp) {
        this.messageId = messageId;
        this.viewType = viewType;
        this.message = message;
        this.imageUrl = "";
        this.avatarUrl = avatarUrl;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.messageOnTop = true;
        this.isSelected = false;
    }

    /**
     * Constructor for text+image messages
     */
    public MessageModel(int messageId, int viewType, String message,
            String imageUrl, String avatarUrl, String senderName,
            String timestamp, boolean messageOnTop) {
        this.messageId = messageId;
        this.viewType = viewType;
        this.message = message;
        this.imageUrl = imageUrl;
        this.avatarUrl = avatarUrl;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.messageOnTop = messageOnTop;
        this.isSelected = false;
    }

    /**
     * Constructor for system messages and date headers
     * messageId = -1 for non-message items (date headers, typing indicator)
     */
    public MessageModel(int viewType, String text) {
        this.messageId = -1;
        this.viewType = viewType;
        this.message = text;
        this.imageUrl = "";
        this.avatarUrl = "";
        this.senderName = "";
        this.timestamp = "";
        this.messageOnTop = true;
        this.isSelected = false;
    }
}