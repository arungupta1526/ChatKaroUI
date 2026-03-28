package com.prem.chatkaroui;

/**
 * Data model representing a single chat item (message, date header, system
 * message, or typing indicator).
 *
 * v3.1 additions:
 * - isStarred : star/bookmark a message
 * - isEdited : marks a message as edited (shows label near timestamp)
 * - replyToId : ID of the message being replied to (0 = no reply)
 * - replyToText : snapshot of the replied-to message text
 * - replyToSender : snapshot of the replied-to sender name
 *
 * v3.2 additions:
 * - previewTitle / previewDescription / previewImageUrl / previewSiteName
 * for link-preview cards (populated asynchronously by ChatKaroUI)
 * - rawMarkdown : original markdown/HTML source before rendering
 */
public class MessageModel {

    // ── View types ───────────────────────────────────────────────────────────
    public static final int TYPE_SENT_SIMPLE = 0;
    public static final int TYPE_RECEIVED_SIMPLE = 1;
    public static final int TYPE_SENT_AVATAR = 2;
    public static final int TYPE_RECEIVED_AVATAR = 3;
    public static final int TYPE_SENT_TEXT_IMAGE = 4;
    public static final int TYPE_RECEIVED_TEXT_IMAGE = 5;
    public static final int TYPE_SYSTEM = 6;
    public static final int TYPE_DATE_HEADER = 7;
    public static final int TYPE_TYPING_INDICATOR = 8;

    // ── Core (immutable) fields ──────────────────────────────────────────────
    public final int messageId;
    public final int viewType;
    public final String imageUrl;
    public final String avatarUrl;
    public final String senderName;
    public final String timestamp;
    public final boolean messageOnTop; // text-above-image ordering

    // ── Mutable state ────────────────────────────────────────────────────────
    public String message; // mutable for UpdateMessageById
    public boolean isSelected; // selection highlight
    public boolean isStarred; // ★ star/bookmark
    public boolean isEdited; // "edited" label near metadata

    // ── Reply fields (set when this message is a reply to another) ───────────
    public int replyToId; // 0 = not a reply
    public String replyToText; // preview text of the quoted message
    public String replyToSender; // sender name of the quoted message
    public boolean replyToIsSent; // was the quoted message sent by us?

    // ── Link-preview fields (populated asynchronously) ───────────────────────
    public String previewTitle;
    public String previewDescription;
    public String previewImageUrl;
    public String previewSiteName;
    public boolean previewLoading; // true while fetch is in flight

    // ── Rendering hint ───────────────────────────────────────────────────────
    /**
     * Raw source text (Markdown or HTML) before rendering — stored so that
     * export/import can round-trip correctly without losing markup.
     */
    public String rawSource;

    // ────────────────────────────────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────────────────────────────────

    /** Simple / avatar messages (no image). */
    public MessageModel(int messageId, int viewType, String message,
            String avatarUrl, String senderName, String timestamp) {
        this.messageId = messageId;
        this.viewType = viewType;
        this.message = message;
        this.imageUrl = "";
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
        this.senderName = senderName != null ? senderName : "";
        this.timestamp = timestamp != null ? timestamp : "";
        this.messageOnTop = true;
        resetMutableState();
    }

    /** Text + image messages. */
    public MessageModel(int messageId, int viewType, String message,
            String imageUrl, String avatarUrl, String senderName,
            String timestamp, boolean messageOnTop) {
        this.messageId = messageId;
        this.viewType = viewType;
        this.message = message;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
        this.senderName = senderName != null ? senderName : "";
        this.timestamp = timestamp != null ? timestamp : "";
        this.messageOnTop = messageOnTop;
        resetMutableState();
    }

    /**
     * Non-message items: date headers, system messages, typing indicator.
     * messageId is set to -1 for these.
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
        resetMutableState();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void resetMutableState() {
        isSelected = false;
        isStarred = false;
        isEdited = false;
        replyToId = 0;
        replyToText = "";
        replyToSender = "";
        replyToIsSent = false;
        previewTitle = "";
        previewDescription = "";
        previewImageUrl = "";
        previewSiteName = "";
        previewLoading = false;
        rawSource = "";
    }

    /** Returns true if this model carries a valid reply reference. */
    public boolean hasReply() {
        return replyToId > 0 && replyToText != null && !replyToText.isEmpty();
    }

    /** Returns true if this model has a fetched link-preview. */
    public boolean hasLinkPreview() {
        return previewTitle != null && !previewTitle.isEmpty();
    }

    /** True for sent message view types. */
    public boolean isSentType() {
        return viewType == TYPE_SENT_SIMPLE
                || viewType == TYPE_SENT_AVATAR
                || viewType == TYPE_SENT_TEXT_IMAGE;
    }
}