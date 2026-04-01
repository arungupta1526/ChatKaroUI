package com.prem.chatkaroui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.YailList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.graphics.Color.parseColor;

@DesignerComponent(version = 3, versionName = "3.3", description = "ChatKaroUI — customizable chat component with text, images, reply, star, "
        +
        "HTML/Markdown, export/import and more. " +
        "Made by: Arun Gupta <br>" +
        "<span><a href=\"https://community.appinventor.mit.edu/t/154865\" target=\"_blank\">" +
        "<small><mark>Mit AI2 Community</mark></small></a></span> | " +
        "<span><a href=\"https://community.kodular.io/t/301309\" target=\"_blank\">" +
        "<small><mark>Kodular Community</mark></small></a></span>", nonVisible = true, iconName = "icon.png", helpUrl = "https://www.telegram.me/Arungupta1526")
public class ChatKaroUI extends AndroidNonvisibleComponent
        implements Component, ImageLoader, OnDestroyListener {

    // ── Date/time formatters ─────────────────────────────────────────────────
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_KEY = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_DISPLAY = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat SDF_TIME = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_SHORT = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    // ── Core UI ──────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ChatAdapter chatAdapter;
    private ChatConfig chatConfig;
    private ItemTouchHelper swipeHelper;
    private final List<MessageModel> messageList = new ArrayList<>();
    private int typingIndicatorPosition = -1;
    private AndroidViewComponent verticalArrangement;

    // ── Message tracking ─────────────────────────────────────────────────────
    private int nextMessageId = 1;
    private final SparseArray<String> messageTextsById = new SparseArray<>();
    private final SparseArray<Boolean> messageSentFlagsById = new SparseArray<>();
    private final SparseArray<Integer> messagePositionById = new SparseArray<>();
    private final List<Integer> selectedMessageIds = new ArrayList<>();
    private final Set<Integer> starredMessageIds = new HashSet<>();
    private String lastMessageDate = "";

    // ── Threading ────────────────────────────────────────────────────────────
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final Context context;
    private Runnable typingRunnable;

    // ── Image cache ──────────────────────────────────────────────────────────
    private final LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    // ── Style / config properties ────────────────────────────────────────────
    private String sentStatusText = "✓✓";
    private String receivedStatusText = "🚀";
    private String customFontFamily = "";
    private String typingIndicatorText = "Typing";
    private String editedLabelText = "edited";
    private String starredIndicatorText = "★";

    private int sentMessageBackgroundColor = parseColor("#0084ff");
    private int receivedMessageBackgroundColor = parseColor("#f0f0f0");
    private int sentMessageTextColor = Color.WHITE;
    private int receivedMessageTextColor = Color.BLACK;
    private int sentNameTextColor = Color.BLACK;
    private int receivedNameTextColor = Color.BLACK;
    private int selectedMessageBgColor = 0x8CF9D3FF;
    private int timestampTextColor = Color.GRAY;
    private int sentStatusTextColor = Color.BLUE;
    private int receivedStatusTextColor = Color.MAGENTA;
    private int systemMessageTextColor = Color.GRAY;
    private int typingIndicatorTextColor = Color.GRAY;
    private int fullscreenImageBGColor = parseColor("#0c0c0c");
    private int avatarBackgroundColor = Color.LTGRAY;
    private int starredIndicatorColor = 0xFFFFD700;
    private int editedLabelColor = Color.GRAY;
    private int sentReplyBubbleBgColor = 0x44FFFFFF;
    private int replyBubbleBgColor = 0x220084FF;
    private int replyAccentColor = parseColor("#0084ff");
    private int replyPreviewTextColor = 0xFF444444;
    private int linkPreviewBgColor = 0xFFF5F5F5;
    private int linkPreviewAccentColor = parseColor("#0084ff");

    private int timestampFontSize = 12;
    private int avatarSize = 40;
    private int textMessageMaxWidth = 0;
    private int imageMessageMaxWidth = 300;
    private int messageHorizontalPadding = 8;
    private int messageVerticalPadding = 4;
    private int messageFontSize = 16;
    private int systemMessageFontSize = 14;
    private int nameFontSize = 12;

    private float messageCornerRadius = 20f;
    private float squareEdgeCornerRadius = 8f;

    private boolean showTimestamp = true;
    private boolean showReadStatus = true;
    private boolean showTypingIndicator = false;
    private boolean showMetadataInsideBubble = false;
    private boolean showDateHeaders = true;
    private boolean useResponsiveWidth = true;
    private boolean autoLinkEnabledInChat = true;
    private boolean squareBubbleEdge = false;
    private boolean imageFunctionWidthFix = true;
    private boolean htmlEnabledInChat = false;
    private boolean markdownEnabledInChat = false;
    private boolean linkPreviewEnabled = true;
    private boolean showDefaultMenuItems = true;
    private boolean showEditedLabel = true;
    private boolean swipeToReplyEnabled = true;

    private Typeface typeface;

    private final List<String> customTextMenuItems = new ArrayList<>();
    private final List<String> customImageMenuItems = new ArrayList<>();

    // ────────────────────────────────────────────────────────────────────────
    // Constructor
    // ────────────────────────────────────────────────────────────────────────

    public ChatKaroUI(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
        form.registerForOnDestroy(this);
    }

    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onDestroy() {
        imageExecutor.shutdownNow();
        if (typingHandler != null && typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        imageCache.evictAll();
    }
    // Initialize
    // ────────────────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Initialize the chat UI in a VerticalArrangement. " +
            "Must be called before adding messages.")
    public void Initialize(VerticalArrangement arrangement) {
        try {
            verticalArrangement = arrangement;
            chatConfig = buildConfig();

            recyclerView = new RecyclerView(context);
            recyclerView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            layoutManager = new LinearLayoutManager(context);
            layoutManager.setStackFromEnd(true);
            recyclerView.setLayoutManager(layoutManager);

            chatAdapter = new ChatAdapter(context, messageList, chatConfig, this);
            chatAdapter.setEventCallback(new ChatAdapter.EventCallback() {
                @Override
                public void onMessageSelected(String msg, int id) {
                    handleMessageSelectionToggle(msg, id);
                }

                @Override
                public void onTextMenuItemClicked(String item, String msg, int id) {
                    TextMenuItemClicked(item, msg, id);
                }

                @Override
                public void onImageMenuItemClicked(String item, String url, int id) {
                    ImageMenuItemClicked(item, url, id);
                }

                @Override
                public void onProfilePictureClicked(String name, String url) {
                    ProfilePictureClicked(name, url);
                }

                @Override
                public void showTextOptionsMenu(View anchor, String msg, int id,
                        boolean isSent, boolean isStarred, String senderName, String avatarUrl, String imageUrl) {
                    ChatKaroUI.this.showTextOptionsMenu(anchor, msg, id, isSent, isStarred, senderName, avatarUrl,
                            imageUrl);
                }

                @Override
                public void showImageOptionsMenu(View anchor, String url,
                        ImageView iv, int id) {
                    ChatKaroUI.this.showImageOptionsMenu(anchor, url, iv, id);
                }

                @Override
                public void showFullscreenImage(Drawable d) {
                    ChatKaroUI.this.showFullscreenImage(d);
                }

                @Override
                public boolean isMultiSelectionActive() {
                    return IsMultiSelectionActive();
                }

                @Override
                public void onReplyQuoteTapped(int replyToId) {
                    GotoMessageById(replyToId);
                    ReplyQuoteTapped(replyToId);
                }
            });
            recyclerView.setAdapter(chatAdapter);

            // ── Swipe-to-reply ────────────────────────────────────────────────
            attachSwipeHelper();

            FrameLayout frame = (FrameLayout) arrangement.getView();
            FrameLayout root = new FrameLayout(context);
            root.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            root.addView(recyclerView);
            frame.addView(root);

        } catch (Exception e) {
            Log.e("ChatUI", "Initialization error: " + e.getMessage());
        }
    }

    private void attachSwipeHelper() {
        if (swipeHelper != null)
            swipeHelper.attachToRecyclerView(null);
        if (!swipeToReplyEnabled) {
            swipeHelper = null;
            return;
        }

        SwipeReplyCallback callback = new SwipeReplyCallback(messageList, chatAdapter, adapterPos -> {
            if (adapterPos < 0 || adapterPos >= messageList.size())
                return;
            MessageModel m = messageList.get(adapterPos);
            if (m.messageId > 0) {
                String preview = m.message != null ? m.message : "";
                String replySender = (m.senderName != null && !m.senderName.isEmpty()) ? m.senderName
                        : (m.isSentType() ? "You" : "");
                // v3.3: Passing imageUrl to ReplyTriggered
                ReplyTriggered(m.messageId, preview, replySender, m.avatarUrl, m.imageUrl, m.isSentType());
            }
        });
        swipeHelper = new ItemTouchHelper(callback);
        swipeHelper.attachToRecyclerView(recyclerView);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Config builders
    // ────────────────────────────────────────────────────────────────────────

    private ChatConfig buildConfig() {
        ChatConfig c = new ChatConfig();
        c.sentMessageBackgroundColor = sentMessageBackgroundColor;
        c.receivedMessageBackgroundColor = receivedMessageBackgroundColor;
        c.sentMessageTextColor = sentMessageTextColor;
        c.receivedMessageTextColor = receivedMessageTextColor;
        c.sentNameTextColor = sentNameTextColor;
        c.receivedNameTextColor = receivedNameTextColor;
        c.selectedMessageBgColor = selectedMessageBgColor;
        c.timestampTextColor = timestampTextColor;
        c.sentStatusTextColor = sentStatusTextColor;
        c.receivedStatusTextColor = receivedStatusTextColor;
        c.systemMessageTextColor = systemMessageTextColor;
        c.typingIndicatorTextColor = typingIndicatorTextColor;
        c.avatarBackgroundColor = avatarBackgroundColor;
        c.starredIndicatorColor = starredIndicatorColor;
        c.starredIndicatorText = starredIndicatorText;
        c.showEditedLabel = showEditedLabel;
        c.editedLabelText = editedLabelText;
        c.editedLabelColor = editedLabelColor;
        c.sentReplyBubbleBgColor = sentReplyBubbleBgColor;
        c.replyBubbleBgColor = replyBubbleBgColor;
        c.replyAccentColor = replyAccentColor;
        c.replyPreviewTextColor = replyPreviewTextColor;
        c.linkPreviewEnabled = linkPreviewEnabled;
        c.linkPreviewBgColor = linkPreviewBgColor;
        c.linkPreviewAccentColor = linkPreviewAccentColor;
        c.htmlEnabledInChat = htmlEnabledInChat;
        c.markdownEnabledInChat = markdownEnabledInChat;
        c.showDefaultMenuItems = showDefaultMenuItems;
        c.messageFontSize = messageFontSize;
        c.systemMessageFontSize = systemMessageFontSize;
        c.nameFontSize = nameFontSize;
        c.timestampFontSize = timestampFontSize;
        c.avatarSize = avatarSize;
        c.textMessageMaxWidth = textMessageMaxWidth;
        c.imageMessageMaxWidth = imageMessageMaxWidth;
        c.messageHorizontalPadding = messageHorizontalPadding;
        c.messageVerticalPadding = messageVerticalPadding;
        c.messageCornerRadius = messageCornerRadius;
        c.squareEdgeCornerRadius = squareEdgeCornerRadius;
        c.showTimestamp = showTimestamp;
        c.showReadStatus = showReadStatus;
        c.showMetadataInsideBubble = showMetadataInsideBubble;
        c.showMetadataOutBubble = !showMetadataInsideBubble;
        c.showDateHeaders = showDateHeaders;
        c.squareBubbleEdge = squareBubbleEdge;
        c.autoLinkEnabledInChat = autoLinkEnabledInChat;
        c.useResponsiveWidth = useResponsiveWidth;
        c.imageFunctionWidthFix = imageFunctionWidthFix;
        c.sentStatusText = sentStatusText;
        c.receivedStatusText = receivedStatusText;
        c.typeface = typeface;
        c.arrangementWidthPx = ArrangementWidthPx();
        return c;
    }

    private void refreshChatConfig() {
        if (chatConfig == null)
            return;
        ChatConfig u = buildConfig();
        // Copy all fields from u → chatConfig
        chatConfig.sentMessageBackgroundColor = u.sentMessageBackgroundColor;
        chatConfig.receivedMessageBackgroundColor = u.receivedMessageBackgroundColor;
        chatConfig.sentMessageTextColor = u.sentMessageTextColor;
        chatConfig.receivedMessageTextColor = u.receivedMessageTextColor;
        chatConfig.sentNameTextColor = u.sentNameTextColor;
        chatConfig.receivedNameTextColor = u.receivedNameTextColor;
        chatConfig.selectedMessageBgColor = u.selectedMessageBgColor;
        chatConfig.timestampTextColor = u.timestampTextColor;
        chatConfig.sentStatusTextColor = u.sentStatusTextColor;
        chatConfig.receivedStatusTextColor = u.receivedStatusTextColor;
        chatConfig.systemMessageTextColor = u.systemMessageTextColor;
        chatConfig.typingIndicatorTextColor = u.typingIndicatorTextColor;
        chatConfig.avatarBackgroundColor = u.avatarBackgroundColor;
        chatConfig.starredIndicatorColor = u.starredIndicatorColor;
        chatConfig.starredIndicatorText = u.starredIndicatorText;
        chatConfig.showEditedLabel = u.showEditedLabel;
        chatConfig.editedLabelText = u.editedLabelText;
        chatConfig.editedLabelColor = u.editedLabelColor;
        chatConfig.sentReplyBubbleBgColor = u.sentReplyBubbleBgColor;
        chatConfig.replyBubbleBgColor = u.replyBubbleBgColor;
        chatConfig.replyAccentColor = u.replyAccentColor;
        chatConfig.replyPreviewTextColor = u.replyPreviewTextColor;
        chatConfig.linkPreviewEnabled = u.linkPreviewEnabled;
        chatConfig.linkPreviewBgColor = u.linkPreviewBgColor;
        chatConfig.linkPreviewAccentColor = u.linkPreviewAccentColor;
        chatConfig.htmlEnabledInChat = u.htmlEnabledInChat;
        chatConfig.markdownEnabledInChat = u.markdownEnabledInChat;
        chatConfig.showDefaultMenuItems = u.showDefaultMenuItems;
        chatConfig.messageFontSize = u.messageFontSize;
        chatConfig.systemMessageFontSize = u.systemMessageFontSize;
        chatConfig.nameFontSize = u.nameFontSize;
        chatConfig.timestampFontSize = u.timestampFontSize;
        chatConfig.avatarSize = u.avatarSize;
        chatConfig.textMessageMaxWidth = u.textMessageMaxWidth;
        chatConfig.imageMessageMaxWidth = u.imageMessageMaxWidth;
        chatConfig.messageHorizontalPadding = u.messageHorizontalPadding;
        chatConfig.messageVerticalPadding = u.messageVerticalPadding;
        chatConfig.messageCornerRadius = u.messageCornerRadius;
        chatConfig.squareEdgeCornerRadius = u.squareEdgeCornerRadius;
        chatConfig.showTimestamp = u.showTimestamp;
        chatConfig.showReadStatus = u.showReadStatus;
        chatConfig.showMetadataInsideBubble = u.showMetadataInsideBubble;
        chatConfig.showMetadataOutBubble = u.showMetadataOutBubble;
        chatConfig.squareBubbleEdge = u.squareBubbleEdge;
        chatConfig.autoLinkEnabledInChat = u.autoLinkEnabledInChat;
        chatConfig.useResponsiveWidth = u.useResponsiveWidth;
        chatConfig.imageFunctionWidthFix = u.imageFunctionWidthFix;
        chatConfig.sentStatusText = u.sentStatusText;
        chatConfig.receivedStatusText = u.receivedStatusText;
        chatConfig.typeface = u.typeface;
        chatConfig.arrangementWidthPx = u.arrangementWidthPx;
        if (chatAdapter != null)
            chatAdapter.notifyDataSetChanged();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Orientation change — refresh max-width at bind-time
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Call this from Screen.ScreenOrientationChanged event in App Inventor
     * so bubble widths update after rotation.
     */
    @SimpleFunction(description = "Call when screen orientation changes to reflow message bubbles.")
    public void OnOrientationChanged() {
        uiHandler.postDelayed(() -> {
            if (chatConfig != null) {
                chatConfig.arrangementWidthPx = ArrangementWidthPx();
            }
            if (chatAdapter != null)
                chatAdapter.notifyDataSetChanged();
        }, 150);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Send / Receive methods (unchanged API from v3.0)
    // ────────────────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Send a simple message without avatar or name.")
    public void SendSimple(String message, String timestamp) {
        int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_SENT_SIMPLE,
                message, "", "", timestamp));
    }

    @SimpleFunction(description = "Receive a simple message without avatar or name.")
    public void ReceiveSimple(String message, String timestamp) {
        int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_RECEIVED_SIMPLE,
                message, "", "", timestamp));
    }

    @SimpleFunction(description = "Send a message with avatar and sender name.")
    public void SendWithAvatar(String message, String avatarUrl,
            String senderName, String timestamp) {
        int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_SENT_AVATAR,
                message, avatarUrl, senderName, timestamp));
    }

    @SimpleFunction(description = "Receive a message with avatar and sender name.")
    public void ReceiveWithAvatar(String message, String avatarUrl,
            String receiverName, String timestamp) {
        int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_RECEIVED_AVATAR,
                message, avatarUrl, receiverName, timestamp));
    }

    @SimpleFunction(description = "Send a message with both text and image.")
    public void SendTextImage(String message, String imageUrl, String avatarUrl,
            String senderName, String timestamp, boolean messageOnTop) {
        int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_SENT_TEXT_IMAGE,
                message, imageUrl, avatarUrl, senderName, timestamp, messageOnTop));
    }

    @SimpleFunction(description = "Receive a message with both text and image.")
    public void ReceiveTextImage(String message, String imageUrl, String avatarUrl,
            String receiverName, String timestamp, boolean messageOnTop) {
        int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_RECEIVED_TEXT_IMAGE,
                message, imageUrl, avatarUrl, receiverName, timestamp, messageOnTop));
    }

    @SimpleFunction(description = "Send a simple message as a reply WITHOUT avatar or name.")
    public void SendReplySimple(String message, String timestamp,
            int replyToId, String replyToText, String replyToSender, boolean replyToIsSent) {
        int id = nextMessageId++;
        MessageModel m = new MessageModel(id, MessageModel.TYPE_SENT_SIMPLE,
                message, "", "", timestamp);
        m.replyToId = replyToId;
        m.replyToText = replyToText;
        m.replyToSender = replyToSender;
        m.replyToIsSent = replyToIsSent;
        addMessageInternal(m);
    }

    @SimpleFunction(description = "Send a message as a reply WITH avatar and sender name (and optional image).")
    public void SendReplyAdvance(String message, String avatarUrl, String imageUrl,
            String senderName, String timestamp, int replyToId, String replyToText,
            String replyToSender, boolean replyToIsSent) {
        int id = nextMessageId++;
        int type = (imageUrl != null && !imageUrl.isEmpty()) ? MessageModel.TYPE_SENT_TEXT_IMAGE
                : MessageModel.TYPE_SENT_AVATAR;

        MessageModel m;
        if (type == MessageModel.TYPE_SENT_TEXT_IMAGE) {
            m = new MessageModel(id, type, message, imageUrl, avatarUrl, senderName, timestamp, true);
        } else {
            m = new MessageModel(id, type, message, avatarUrl, senderName, timestamp);
        }

        m.replyToId = replyToId;
        m.replyToText = replyToText;
        m.replyToSender = replyToSender;
        m.replyToIsSent = replyToIsSent;
        addMessageInternal(m);
    }

    @SimpleFunction(description = "Receive a simple message as a reply WITHOUT avatar or name.")
    public void ReceiveReplySimple(String message, String timestamp,
            int replyToId, String replyToText, String replyToSender, boolean replyToIsSent) {
        int id = nextMessageId++;
        MessageModel m = new MessageModel(id, MessageModel.TYPE_RECEIVED_SIMPLE,
                message, "", "", timestamp);
        m.replyToId = replyToId;
        m.replyToText = replyToText;
        m.replyToSender = replyToSender;
        m.replyToIsSent = replyToIsSent;
        addMessageInternal(m);
    }

    @SimpleFunction(description = "Receive a message as a reply WITH avatar and sender name (and optional image).")
    public void ReceiveReplyAdvance(String message, String avatarUrl, String imageUrl,
            String senderName, String timestamp, int replyToId, String replyToText,
            String replyToSender, boolean replyToIsSent) {
        int id = nextMessageId++;
        int type = (imageUrl != null && !imageUrl.isEmpty()) ? MessageModel.TYPE_RECEIVED_TEXT_IMAGE
                : MessageModel.TYPE_RECEIVED_AVATAR;

        MessageModel m;
        if (type == MessageModel.TYPE_RECEIVED_TEXT_IMAGE) {
            m = new MessageModel(id, type, message, imageUrl, avatarUrl, senderName, timestamp, true);
        } else {
            m = new MessageModel(id, type, message, avatarUrl, senderName, timestamp);
        }

        m.replyToId = replyToId;
        m.replyToText = replyToText;
        m.replyToSender = replyToSender;
        m.replyToIsSent = replyToIsSent;
        addMessageInternal(m);
    }

    // ── System message ───────────────────────────────────────────────────────

    @SimpleFunction(description = "Add a system message (e.g., 'User joined').")
    public void AddSystemMessage(String message) {
        uiHandler.post(() -> {
            messageList.add(new MessageModel(MessageModel.TYPE_SYSTEM, message));
            if (chatAdapter != null)
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();
        });
    }

    // ── Internal add ─────────────────────────────────────────────────────────

    // private void addMessageInternal(MessageModel model) {
    // uiHandler.post(() -> {
    // // Date header
    // if (model.messageId > 0) {
    // String date = extractDateFrom(model.timestamp);
    // if (!date.equals(lastMessageDate)) {
    // messageList.add(new MessageModel(MessageModel.TYPE_DATE_HEADER,
    // formatDateReadable(date)));
    // if (chatAdapter != null)
    // chatAdapter.notifyItemInserted(messageList.size() - 1);
    // lastMessageDate = date;
    // }
    // }

    // // Track
    // if (model.messageId > 0) {
    // messageTextsById.put(model.messageId,
    // model.message != null ? model.message : "");
    // messageSentFlagsById.put(model.messageId, model.isSentType());
    // }

    // messageList.add(model);
    // int pos = messageList.size() - 1;
    // if (model.messageId > 0)
    // messagePositionById.put(model.messageId, pos);
    // if (chatAdapter != null)
    // chatAdapter.notifyItemInserted(pos);
    // scrollToBottom();

    // // Async link preview
    // if (linkPreviewEnabled && model.messageId > 0
    // && model.message != null && !model.message.isEmpty()) {
    // String firstUrl = extractFirstUrl(model.message);
    // if (firstUrl != null)
    // fetchLinkPreview(firstUrl, model.messageId);
    // }
    // });
    // }

    private void addMessageInternal(MessageModel model) {
        uiHandler.post(() -> {
            // ── Date header ──────────────────────────────────────────────────────
            if (model.messageId > 0 && model.timestamp != null && !model.timestamp.isEmpty()) {
                String date = extractDateFrom(model.timestamp);
                if (!date.equals(lastMessageDate)) {
                    if (showDateHeaders) {
                        MessageModel header = new MessageModel(
                                MessageModel.TYPE_DATE_HEADER, formatDateReadable(date));
                        messageList.add(header);
                        if (chatAdapter != null)
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                    }
                    lastMessageDate = date;
                }
            }

            // ── Register in lookup maps ──────────────────────────────────────────
            if (model.messageId > 0) {
                messageTextsById.put(model.messageId,
                        model.message != null ? model.message : "");
                messageSentFlagsById.put(model.messageId, model.isSentType());
            }

            // ── Insert ───────────────────────────────────────────────────────────
            // Always insert BEFORE the typing indicator so the indicator stays last
            int insertPos;
            if (typingIndicatorPosition >= 0
                    && typingIndicatorPosition < messageList.size()) {
                insertPos = typingIndicatorPosition;
                // Shift the typing indicator index forward
                typingIndicatorPosition++;
            } else {
                insertPos = messageList.size();
            }

            messageList.add(insertPos, model);

            if (model.messageId > 0)
                messagePositionById.put(model.messageId, insertPos);

            if (chatAdapter != null)
                chatAdapter.notifyItemInserted(insertPos);

            // Invalidate cached positions for everything after insertPos
            // (only positions > insertPos are stale; rebuild is cheap for typical chat
            // sizes)
            for (int i = insertPos + 1; i < messageList.size(); i++) {
                MessageModel m = messageList.get(i);
                if (m.messageId > 0)
                    messagePositionById.put(m.messageId, i);
            }

            scrollToBottom();

            // ── Async link preview ────────────────────────────────────────────────
            if (linkPreviewEnabled
                    && model.messageId > 0
                    && model.message != null
                    && !model.message.isEmpty()) {
                String firstUrl = extractFirstUrl(model.message);
                if (firstUrl != null)
                    fetchLinkPreview(firstUrl, model.messageId);
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Star / bookmark
    // ────────────────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Star (bookmark) a message by its ID.")
    public void StarMessageById(int messageId) {
        setStarred(messageId, true);
    }

    @SimpleFunction(description = "Unstar a previously starred message by its ID.")
    public void UnstarMessageById(int messageId) {
        setStarred(messageId, false);
    }

    @SimpleFunction(description = "Toggle the starred state of a message. Returns the new state.")
    public boolean ToggleStarById(int messageId) {
        boolean current = IsMessageStarred(messageId);
        setStarred(messageId, !current);
        return !current;
    }

    @SimpleFunction(description = "Returns true if the message with the given ID is starred.")
    public boolean IsMessageStarred(int messageId) {
        return starredMessageIds.contains(messageId);
    }

    @SimpleFunction(description = "Returns a list of all currently starred message IDs.")
    public YailList GetStarredMessageIds() {
        return YailList.makeList(new ArrayList<>(starredMessageIds));
    }

    private void setStarred(int messageId, boolean starred) {
        uiHandler.post(() -> {
            int pos = findMessagePositionById(messageId);
            if (pos < 0)
                return;
            MessageModel m = messageList.get(pos);
            m.isStarred = starred;
            if (starred)
                starredMessageIds.add(messageId);
            else
                starredMessageIds.remove(messageId);
            if (chatAdapter != null)
                chatAdapter.notifyItemChanged(pos);
            MessageStarred(messageId, starred);
        });
    }

    @SimpleEvent(description = "Fired when a message is starred or unstarred.")
    public void MessageStarred(int messageId, boolean isStarred) {
        EventDispatcher.dispatchEvent(this, "MessageStarred", messageId, isStarred);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Swipe-to-reply events
    // ────────────────────────────────────────────────────────────────────────

    @SimpleEvent(description = "Fired when a message is swiped or reply menu item is clicked.")
    public void ReplyTriggered(int messageId, String messageText, String replyToSender,
            String avatarUrl, String imageUrl, boolean isSent) {
        EventDispatcher.dispatchEvent(this, "ReplyTriggered", messageId, messageText,
                replyToSender, avatarUrl, imageUrl, isSent);
    }

    @SimpleEvent(description = "Fired when the user taps the reply-quote strip inside a message " +
            "to scroll to the original.")
    public void ReplyQuoteTapped(int originalMessageId) {
        EventDispatcher.dispatchEvent(this, "ReplyQuoteTapped", originalMessageId);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Link Preview (async OG-tag fetch)
    // ────────────────────────────────────────────────────────────────────────

    /** Extracts the first http/https URL from text, or null if none. */
    private String extractFirstUrl(String text) {
        Pattern p = Pattern.compile("https?://[^\\s]+");
        Matcher m = p.matcher(text);
        return m.find() ? m.group() : null;
    }

    private void fetchLinkPreview(String urlString, int messageId) {
        imageExecutor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (compatible; ChatKaroUI/4.0)");
                conn.connect();

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    int chars = 0;
                    while ((line = br.readLine()) != null && chars < 32000) {
                        sb.append(line).append('\n');
                        chars += line.length();
                        // Stop early once we've passed <head>
                        if (line.contains("</head>"))
                            break;
                    }
                }
                conn.disconnect();

                String html = sb.toString();
                String title = extractOgTag(html, "og:title", "title");
                String desc = extractOgTag(html, "og:description", "description");
                String imgUrl = extractOgTag(html, "og:image", null);
                String siteName = extractOgTag(html, "og:site_name", null);

                if (title == null || title.isEmpty())
                    return; // nothing useful

                uiHandler.post(() -> {
                    int pos = findMessagePositionById(messageId);
                    if (pos < 0)
                        return;
                    MessageModel m = messageList.get(pos);
                    m.previewTitle = title;
                    m.previewDescription = desc != null ? desc : "";
                    m.previewImageUrl = imgUrl != null ? imgUrl : "";
                    m.previewSiteName = siteName != null ? siteName : "";
                    m.previewLoading = false;
                    if (chatAdapter != null)
                        chatAdapter.notifyItemChanged(pos);
                    LinkPreviewLoaded(messageId, title, urlString);
                });
            } catch (Exception e) {
                // Silently ignore — link preview is best-effort
            }
        });
    }

    /** Extracts Open Graph or fallback meta tag value from raw HTML. */
    private String extractOgTag(String html, String ogProperty, String fallbackName) {
        // Try og: property first
        Pattern ogp = Pattern.compile(
                "<meta[^>]+property=[\"']" + Pattern.quote(ogProperty) +
                        "[\"'][^>]+content=[\"']([^\"']*)[\"']",
                Pattern.CASE_INSENSITIVE);
        Matcher m = ogp.matcher(html);
        if (m.find())
            return m.group(1);

        // Also try reversed attribute order
        Pattern ogp2 = Pattern.compile(
                "<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+property=[\"']" +
                        Pattern.quote(ogProperty) + "[\"']",
                Pattern.CASE_INSENSITIVE);
        m = ogp2.matcher(html);
        if (m.find())
            return m.group(1);

        // Fallback to <title> etc.
        if (fallbackName != null && fallbackName.equals("title")) {
            Pattern tp = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
            m = tp.matcher(html);
            if (m.find())
                return m.group(1).trim();
        }
        return null;
    }

    @SimpleEvent(description = "Fired when a link preview has been successfully loaded for a message.")
    public void LinkPreviewLoaded(int messageId, String title, String url) {
        EventDispatcher.dispatchEvent(this, "LinkPreviewLoaded", messageId, title, url);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Export / Import
    // ────────────────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Export all chat messages as a JSON string. " +
            "Store this string in a database or file to restore the chat later.")
    public String ExportChatAsJson() {
        try {
            JSONArray arr = new JSONArray();
            for (MessageModel m : messageList) {
                if (m.messageId <= 0)
                    continue; // skip headers / system / typing
                JSONObject obj = new JSONObject();
                obj.put("id", m.messageId);
                obj.put("type", m.viewType);
                obj.put("message", m.message != null ? m.message : "");
                obj.put("imageUrl", m.imageUrl != null ? m.imageUrl : "");
                obj.put("avatarUrl", m.avatarUrl != null ? m.avatarUrl : "");
                obj.put("senderName", m.senderName != null ? m.senderName : "");
                obj.put("timestamp", m.timestamp != null ? m.timestamp : "");
                obj.put("messageOnTop", m.messageOnTop);
                obj.put("isStarred", m.isStarred);
                obj.put("isEdited", m.isEdited);
                obj.put("replyToId", m.replyToId);
                obj.put("replyToText", m.replyToText != null ? m.replyToText : "");
                obj.put("replyToSender", m.replyToSender != null ? m.replyToSender : "");
                obj.put("replyToIsSent", m.replyToIsSent);
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    @SimpleFunction(description = "Export the chat as a plain-text transcript (human-readable).")
    public String ExportChatAsText() {
        StringBuilder sb = new StringBuilder();
        for (MessageModel m : messageList) {
            if (m.messageId <= 0)
                continue;
            String who = m.isSentType()
                    ? "You"
                    : (m.senderName != null && !m.senderName.isEmpty() ? m.senderName : "Other");
            sb.append('[').append(m.timestamp).append("] ")
                    .append(who).append(": ")
                    .append(m.message != null ? m.message : "")
                    .append(m.isStarred ? " ★" : "")
                    .append(m.isEdited ? " (edited)" : "")
                    .append('\n');
        }
        return sb.toString();
    }

    @SimpleFunction(description = "Save exported JSON to a file in the chat-exports folder under ASD. " +
            "Fires ExportSaved or ExportFailed.")
    public void SaveExportToFile(String json, String fileName) {
        imageExecutor.execute(() -> {
            try {
                File dir = new File(context.getExternalFilesDir(null), "chat-exports");
                if (!dir.exists())
                    dir.mkdirs();
                if (!fileName.endsWith(".json") && !fileName.endsWith(".txt")) {
                    throw new Exception("fileName must end with .json or .txt");
                }
                File file = new File(dir, fileName);
                try (FileWriter fw = new FileWriter(file)) {
                    fw.write(json);
                }
                String path = file.getAbsolutePath();
                uiHandler.post(() -> ExportSaved(path));
            } catch (Exception e) {
                uiHandler.post(() -> ExportFailed(e.getMessage()));
            }
        });
    }

    // @SimpleFunction(description = "Load a previously exported JSON string and
    // restore all messages.")
    // public void ImportChatFromJson(String json) {
    // try {
    // ClearAllMessages();
    // JSONArray arr = new JSONArray(json);
    // // Post after clear settles
    // uiHandler.postDelayed(() -> {
    // try {
    // for (int i = 0; i < arr.length(); i++) {
    // JSONObject obj = arr.getJSONObject(i);
    // int type = obj.getInt("type");
    // String msg = obj.optString("message", "");
    // String imgUrl = obj.optString("imageUrl", "");
    // String avUrl = obj.optString("avatarUrl", "");
    // String sender = obj.optString("senderName", "");
    // String ts = obj.optString("timestamp", "");
    // boolean onTop = obj.optBoolean("messageOnTop", true);

    // MessageModel m;
    // if (type == MessageModel.TYPE_SENT_TEXT_IMAGE
    // || type == MessageModel.TYPE_RECEIVED_TEXT_IMAGE) {
    // m = new MessageModel(nextMessageId++, type,
    // msg, imgUrl, avUrl, sender, ts, onTop);
    // } else {
    // m = new MessageModel(nextMessageId++, type,
    // msg, avUrl, sender, ts);
    // }
    // m.isStarred = obj.optBoolean("isStarred", false);
    // m.isEdited = obj.optBoolean("isEdited", false);
    // m.replyToId = obj.optInt("replyToId", 0);
    // m.replyToText = obj.optString("replyToText", "");
    // m.replyToSender = obj.optString("replyToSender", "");
    // m.replyToIsSent = obj.optBoolean("replyToIsSent", false);
    // if (m.isStarred)
    // starredMessageIds.add(m.messageId);

    // addMessageInternal(m);
    // }
    // ImportCompleted(arr.length());
    // } catch (Exception e) {
    // ImportFailed(e.getMessage());
    // }
    // }, 300);
    // } catch (Exception e) {
    // ImportFailed(e.getMessage());
    // }
    // }

    @SimpleFunction(description = "Load a previously exported JSON string and restore all messages.")
    public void ImportChatFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            ImportFailed("JSON string is empty.");
            return;
        }

        // Parse eagerly on the calling thread so we can surface syntax errors fast
        final JSONArray arr;
        try {
            arr = new JSONArray(json);
        } catch (Exception e) {
            ImportFailed("Invalid JSON: " + e.getMessage());
            return;
        }

        // Clear synchronously, then restore after the clear settles on the UI thread
        ClearAllMessages();

        uiHandler.postDelayed(() -> {
            try {
                // ── Pre-scan: find the max original ID so nextMessageId never collides ──
                int maxOriginalId = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        int oid = obj.optInt("id", 0);
                        if (oid > maxOriginalId)
                            maxOriginalId = oid;
                    }
                }
                // Reserve space above all original IDs
                nextMessageId = Math.max(nextMessageId, maxOriginalId + 1);

                int restoredCount = 0;

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj == null)
                        continue;

                    int originalId = obj.optInt("id", 0);
                    int type = obj.optInt("type", MessageModel.TYPE_SENT_SIMPLE);
                    String msg = obj.optString("message", "");
                    String imgUrl = obj.optString("imageUrl", "");
                    String avUrl = obj.optString("avatarUrl", "");
                    String sender = obj.optString("senderName", "");
                    String ts = obj.optString("timestamp", "");
                    boolean onTop = obj.optBoolean("messageOnTop", true);

                    // ── Skip non-message rows that may have slipped into the export ──
                    if (type == MessageModel.TYPE_DATE_HEADER
                            || type == MessageModel.TYPE_SYSTEM
                            || type == MessageModel.TYPE_TYPING_INDICATOR) {
                        continue;
                    }

                    // ── Honour the original ID so reply references survive ────────
                    // If originalId is already taken (shouldn't happen in clean exports)
                    // fall back to the auto-increment counter.
                    int assignedId;
                    if (originalId > 0 && !MessageExists(originalId)) {
                        assignedId = originalId;
                    } else {
                        assignedId = nextMessageId++;
                    }

                    // ── Rebuild the model ────────────────────────────────────────
                    final MessageModel m;
                    if (type == MessageModel.TYPE_SENT_TEXT_IMAGE
                            || type == MessageModel.TYPE_RECEIVED_TEXT_IMAGE) {
                        m = new MessageModel(assignedId, type,
                                msg, imgUrl, avUrl, sender, ts, onTop);
                    } else {
                        m = new MessageModel(assignedId, type,
                                msg, avUrl, sender, ts);
                    }

                    m.isStarred = obj.optBoolean("isStarred", false);
                    m.isEdited = obj.optBoolean("isEdited", false);
                    m.replyToId = obj.optInt("replyToId", 0);
                    m.replyToText = obj.optString("replyToText", "");
                    m.replyToSender = obj.optString("replyToSender", "");
                    m.replyToIsSent = obj.optBoolean("replyToIsSent", false);
                    m.rawSource = obj.optString("rawSource", "");

                    if (m.isStarred)
                        starredMessageIds.add(assignedId);

                    addMessageInternal(m);
                    restoredCount++;
                }

                // Rebuild position cache once everything is inserted
                // uiHandler.post(() -> {
                // rebuildMessagePositions();
                // ImportCompleted(restoredCount);
                // });

                final int finalRestoredCount = restoredCount;
                uiHandler.post(() -> {
                    rebuildMessagePositions();
                    ImportCompleted(finalRestoredCount);
                });

            } catch (Exception e) {
                ImportFailed("Restore error: " + e.getMessage());
            }
        }, 300);
    }

    @SimpleEvent(description = "Fired after SaveExportToFile succeeds.")
    public void ExportSaved(String filePath) {
        EventDispatcher.dispatchEvent(this, "ExportSaved", filePath);
    }

    @SimpleEvent(description = "Fired if SaveExportToFile fails.")
    public void ExportFailed(String error) {
        EventDispatcher.dispatchEvent(this, "ExportFailed", error);
    }

    @SimpleEvent(description = "Fired when ImportChatFromJson finishes successfully.")
    public void ImportCompleted(int messageCount) {
        EventDispatcher.dispatchEvent(this, "ImportCompleted", messageCount);
    }

    @SimpleEvent(description = "Fired if ImportChatFromJson fails.")
    public void ImportFailed(String error) {
        EventDispatcher.dispatchEvent(this, "ImportFailed", error);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Message management (same API as v3.0)
    // ────────────────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Delete a message by its ID.")
    public void DeleteMessageById(int messageId) {
        uiHandler.post(() -> {
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if (messageList.get(i).messageId == messageId) {
                    messageList.remove(i);
                    if (chatAdapter != null)
                        chatAdapter.notifyItemRemoved(i);
                    selectedMessageIds.remove((Integer) messageId);
                    starredMessageIds.remove(messageId);
                    messagePositionById.remove(messageId);
                    rebuildMessagePositions();
                    removeOrphanDateHeaderIfNeeded(i);
                    break;
                }
            }
            messageTextsById.remove(messageId);
            messageSentFlagsById.remove(messageId);
        });
    }

    @SimpleFunction(description = "Update an existing message's text by ID. Marks it as edited.")
    public void UpdateMessageById(int messageId, String newMessage) {
        uiHandler.post(() -> {
            int pos = findMessagePositionById(messageId);
            if (pos < 0)
                return;
            MessageModel m = messageList.get(pos);
            m.message = newMessage;
            m.isEdited = true;
            messageTextsById.put(messageId, newMessage);
            if (chatAdapter != null)
                chatAdapter.notifyItemChanged(pos);
            MessageUpdated(messageId, newMessage);
        });
    }

    @SimpleFunction(description = "Get the text of a message by its ID.")
    public String GetMessageTextById(int messageId) {
        String t = messageTextsById.get(messageId);
        return t != null ? t : "";
    }

    @SimpleFunction(description = "Smooth-scroll to a message by its ID.")
    public void GotoMessageById(int messageId) {
        uiHandler.post(() -> {
            int pos = findMessagePositionById(messageId);
            if (pos >= 0 && recyclerView != null) {
                recyclerView.smoothScrollToPosition(pos);
                MessageScrolledTo(messageId);
            }
        });
    }

    @SimpleFunction(description = "Returns true if a message with the given ID exists.")
    public boolean MessageExists(int messageId) {
        for (MessageModel m : messageList)
            if (m.messageId == messageId)
                return true;
        return false;
    }

    @SimpleFunction(description = "Get all active message IDs (excludes date headers and system messages).")
    public YailList GetAllMessageIds() {
        List<Integer> ids = new ArrayList<>();
        for (MessageModel m : messageList)
            if (m.messageId > 0)
                ids.add(m.messageId);
        return YailList.makeList(ids);
    }

    @SimpleFunction(description = "Returns true if the message was sent (false = received).")
    public boolean IsMessageSent(int messageId) {
        Boolean b = messageSentFlagsById.get(messageId);
        return b != null && b;
    }

    @SimpleFunction(description = "Returns true if a message has been edited.")
    public boolean IsMessageEdited(int messageId) {
        int pos = findMessagePositionById(messageId);
        if (pos < 0)
            return false;
        return messageList.get(pos).isEdited;
    }

    @SimpleFunction(description = "Manually mark a message as edited without changing its content.")
    public void MarkAsEdited(int messageId) {
        uiHandler.post(() -> {
            int pos = findMessagePositionById(messageId);
            if (pos < 0)
                return;
            messageList.get(pos).isEdited = true;
            if (chatAdapter != null)
                chatAdapter.notifyItemChanged(pos);
        });
    }

    @SimpleFunction(description = "Clean up internal ID tracking. Call after ClearAllMessages.")
    public void CleanupIdTracking() {
        messageTextsById.clear();
        messageSentFlagsById.clear();
        messagePositionById.clear();
        selectedMessageIds.clear();
        starredMessageIds.clear();
        nextMessageId = 1;
    }

    @SimpleFunction(description = "Clear all chat messages.")
    public void ClearAllMessages() {
        uiHandler.post(() -> {
            int sz = messageList.size();
            messageList.clear();
            if (chatAdapter != null && sz > 0)
                chatAdapter.notifyItemRangeRemoved(0, sz);
            selectedMessageIds.clear();
            starredMessageIds.clear();
            messageTextsById.clear();
            messageSentFlagsById.clear();
            messagePositionById.clear();
            nextMessageId = 1;
            lastMessageDate = "";
            typingIndicatorPosition = -1;
            if (typingRunnable != null)
                typingHandler.removeCallbacks(typingRunnable);
        });
    }

    @SimpleFunction(description = "Get the total count of chat messages (excluding system/date rows).")
    public int GetMessageCount() {
        int count = 0;
        for (MessageModel m : messageList) {
            if (m.messageId > 0
                    && m.viewType != MessageModel.TYPE_DATE_HEADER
                    && m.viewType != MessageModel.TYPE_SYSTEM
                    && m.viewType != MessageModel.TYPE_TYPING_INDICATOR)
                count++;
        }
        return count;
    }

    @SimpleFunction(description = "Get the first (oldest) active message ID. Returns 0 if none.")
    public int GetFirstMessageId() {
        for (MessageModel m : messageList)
            if (m.messageId > 0)
                return m.messageId;
        return 0;
    }

    @SimpleFunction(description = "Get the last (newest) active message ID. Returns 0 if none.")
    public int GetLastMessageId() {
        for (int i = messageList.size() - 1; i >= 0; i--)
            if (messageList.get(i).messageId > 0)
                return messageList.get(i).messageId;
        return 0;
    }

    // ── Selection ────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Clear all message selections.")
    public void ClearSelection() {
        uiHandler.post(() -> {
            List<Integer> copy = new ArrayList<>(selectedMessageIds);
            selectedMessageIds.clear();
            for (int id : copy) {
                int pos = findMessagePositionById(id);
                if (pos >= 0) {
                    messageList.get(pos).isSelected = false;
                    if (chatAdapter != null)
                        chatAdapter.notifyItemChanged(pos);
                }
            }
            SelectionCleared();
        });
    }

    @SimpleFunction(description = "Get a list of all currently selected message IDs.")
    public com.google.appinventor.components.runtime.util.YailList GetSelectedMessageIds() {
        return com.google.appinventor.components.runtime.util.YailList.makeList(selectedMessageIds);
    }

    @SimpleFunction(description = "Star/Unstar all currently selected messages.")
    public void StarSelectedMessages(boolean isStarred) {
        uiHandler.post(() -> {
            for (int id : selectedMessageIds) {
                int pos = findMessagePositionById(id);
                if (pos >= 0) {
                    messageList.get(pos).isStarred = isStarred;
                    if (chatAdapter != null)
                        chatAdapter.notifyItemChanged(pos);
                }
            }
            ClearSelection();
        });
    }

    @SimpleFunction(description = "Get the number of selected messages.")
    public int GetSelectedCount() {
        return selectedMessageIds.size();
    }

    @SimpleFunction(description = "Delete all currently selected messages.")
    public void DeleteSelectedMessages() {
        uiHandler.post(() -> {
            List<Integer> copy = new ArrayList<>(selectedMessageIds);
            selectedMessageIds.clear();
            for (int id : copy)
                DeleteMessageById(id);
        });
    }

    @SimpleFunction(description = "Returns true if multi-selection mode is active.")
    public boolean IsMultiSelectionActive() {
        return !selectedMessageIds.isEmpty();
    }

    // ── Typing indicator ─────────────────────────────────────────────────────

    @SimpleFunction(description = "Show typing indicator.")
    public void ShowTypingIndicator() {
        showTypingIndicator = true;
        uiHandler.post(() -> {
            if (typingIndicatorPosition >= 0)
                return;
            MessageModel tm = new MessageModel(MessageModel.TYPE_TYPING_INDICATOR,
                    typingIndicatorText.isEmpty() ? "Typing" : typingIndicatorText);
            messageList.add(tm);
            typingIndicatorPosition = messageList.size() - 1;
            if (chatAdapter != null)
                chatAdapter.notifyItemInserted(typingIndicatorPosition);
            scrollToBottom();
            startTypingAnimation();
        });
    }

    @SimpleFunction(description = "Hide typing indicator.")
    public void HideTypingIndicator() {
        showTypingIndicator = false;
        if (typingRunnable != null)
            typingHandler.removeCallbacks(typingRunnable);
        uiHandler.post(() -> {
            if (typingIndicatorPosition < 0)
                return;
            messageList.remove(typingIndicatorPosition);
            if (chatAdapter != null)
                chatAdapter.notifyItemRemoved(typingIndicatorPosition);
            typingIndicatorPosition = -1;
            rebuildMessagePositions();
        });
    }

    private void startTypingAnimation() {
        if (typingRunnable != null)
            typingHandler.removeCallbacks(typingRunnable);
        typingRunnable = new Runnable() {
            private int dots = 0;

            @Override
            public void run() {
                if (!showTypingIndicator || typingIndicatorPosition < 0)
                    return;
                StringBuilder s = new StringBuilder(
                        typingIndicatorText.isEmpty() ? "Typing" : typingIndicatorText);
                for (int i = 0; i < dots; i++)
                    s.append('.');
                if (typingIndicatorPosition < messageList.size()) {
                    messageList.get(typingIndicatorPosition).message = s.toString();
                    if (chatAdapter != null)
                        chatAdapter.notifyItemChanged(typingIndicatorPosition);
                }
                dots = (dots + 1) % 4;
                typingHandler.postDelayed(this, 500);
            }
        };
        typingHandler.post(typingRunnable);
    }

    // ── Image download ───────────────────────────────────────────────────────

    @SimpleFunction(description = "Download an image from URL and save to chat-images folder. " +
            "Fires ImageSaved, ImageAlreadyExists, or ImageSaveFailed.")
    public void DownloadImage(String imageUrl, String format) {
        imageExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(imageUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.connect();
                Bitmap bitmap;
                try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                    bitmap = BitmapFactory.decodeStream(in);
                }
                if (bitmap == null) {
                    uiHandler.post(() -> ImageSaveFailed("Decode failed."));
                    return;
                }

                File dir = new File(context.getExternalFilesDir(null), "chat-images");
                if (!dir.exists())
                    dir.mkdirs();
                String ext = "jpg".equalsIgnoreCase(format) ? "jpg" : "png";
                Bitmap.CompressFormat fmt = "jpg".equals(ext)
                        ? Bitmap.CompressFormat.JPEG
                        : Bitmap.CompressFormat.PNG;
                String base = new File(url.getPath()).getName();
                if (base.isEmpty() || base.matches("^\\d+$"))
                    base = "url_" + Math.abs(imageUrl.hashCode());
                base = base.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
                String fn = "img_" + base + "." + ext;
                File file = new File(dir, fn);
                if (file.exists()) {
                    uiHandler.post(() -> ImageAlreadyExists(file.getAbsolutePath(), fn));
                    return;
                }
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    bitmap.compress(fmt, 100, fos);
                }
                String path = file.getAbsolutePath();
                uiHandler.post(() -> ImageSaved(path, fn));
            } catch (Exception e) {
                uiHandler.post(() -> ImageSaveFailed("Error: " + e.getMessage()));
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        });
    }

    // ── Context / menu ───────────────────────────────────────────────────────
    public void showTextOptionsMenu(View anchor, String message, int messageId,
            boolean isSent, boolean isStarred, String senderName, String avatarUrl, String imageUrl) {
        PopupMenu menu = new PopupMenu(context, anchor);

        if (showDefaultMenuItems) {
            menu.getMenu().add("Reply");
            if (isSent)
                menu.getMenu().add("Edit");
            menu.getMenu().add(isStarred ? "Unstar ★" : "Star ☆");
            menu.getMenu().add("Copy");
            menu.getMenu().add("Forward");
            menu.getMenu().add("Delete");
        }
        for (String item : customTextMenuItems)
            menu.getMenu().add(item);

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            switch (title) {
                case "Copy":
                    CopyToClipboard(message);
                    break;
                case "Delete":
                    DeleteMessageById(messageId);
                    break;
                case "Reply":
                    String replySender = (senderName != null && !senderName.isEmpty()) ? senderName
                            : (isSent ? "You" : "");
                    ReplyTriggered(messageId, message, replySender, avatarUrl, imageUrl, isSent);
                    break;
                case "Forward":
                    ForwardTriggered(messageId, message);
                    break;
                case "Edit":
                    EditRequested(messageId, message);
                    break;
                case "Star ☆":
                    StarMessageById(messageId);
                    break;
                case "Unstar ★":
                    UnstarMessageById(messageId);
                    break;
                default:
                    TextMenuItemClicked(title, message, messageId);
                    break;
            }
            ClearSelection();
            return true;
        });
        menu.show();
    }

    private void showImageOptionsMenu(View anchor, String imageUrl,
            ImageView imageView, int messageId) {
        PopupMenu menu = new PopupMenu(context, anchor);
        if (showDefaultMenuItems)
            menu.getMenu().add("Reload Image");
        for (String item : customImageMenuItems)
            menu.getMenu().add(item);

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Reload Image".equals(title)) {
                loadImageWithPlaceholder(imageView, imageUrl, imageMessageMaxWidth);
            } else {
                ImageMenuItemClicked(title, imageUrl, messageId);
            }
            ClearSelection();
            return true;
        });
        menu.show();
    }

    // ── Fullscreen image ─────────────────────────────────────────────────────

    private void showFullscreenImage(Drawable drawable) {
        if (drawable == null)
            return;
        Dialog dlg = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView iv = new ImageView(context);
        iv.setImageDrawable(drawable);
        iv.setBackgroundColor(fullscreenImageBGColor);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeDown() {
                dlg.dismiss();
            }
        });
        dlg.setContentView(iv);
        dlg.show();
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private void handleMessageSelectionToggle(String message, int messageId) {
        int pos = findMessagePositionById(messageId);
        if (pos < 0)
            return;
        MessageModel m = messageList.get(pos);
        m.isSelected = !m.isSelected;
        if (m.isSelected) {
            if (!selectedMessageIds.contains(messageId))
                selectedMessageIds.add(messageId);
            MessageSelected(message, messageId);
        } else {
            selectedMessageIds.remove((Integer) messageId);
            if (selectedMessageIds.isEmpty())
                SelectionCleared();
        }
        if (chatAdapter != null)
            chatAdapter.notifyItemChanged(pos);
    }

    private int findMessagePositionById(int messageId) {
        Integer cached = messagePositionById.get(messageId);
        if (cached != null && cached >= 0 && cached < messageList.size()
                && messageList.get(cached).messageId == messageId)
            return cached;
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).messageId == messageId) {
                messagePositionById.put(messageId, i);
                return i;
            }
        }
        return -1;
    }

    private void rebuildMessagePositions() {
        messagePositionById.clear();
        String latestDate = "";
        for (int i = 0; i < messageList.size(); i++) {
            MessageModel m = messageList.get(i);
            if (m.messageId > 0) {
                messagePositionById.put(m.messageId, i);
                // latestDate = extractDateFrom(m.timestamp);
                if (m.timestamp != null && !m.timestamp.isEmpty())
                    latestDate = extractDateFrom(m.timestamp);
            }
        }
        lastMessageDate = latestDate;
        if (typingIndicatorPosition >= messageList.size())
            typingIndicatorPosition = -1;
    }

    private void removeOrphanDateHeaderIfNeeded(int removed) {
        int hdr = removed - 1;
        if (hdr < 0 || hdr >= messageList.size())
            return;
        MessageModel cand = messageList.get(hdr);
        if (cand.viewType != MessageModel.TYPE_DATE_HEADER)
            return;
        boolean nextReal = hdr + 1 < messageList.size()
                && messageList.get(hdr + 1).messageId > 0;
        if (!nextReal) {
            messageList.remove(hdr);
            if (chatAdapter != null)
                chatAdapter.notifyItemRemoved(hdr);
            rebuildMessagePositions();
        }
        if (messageList.isEmpty())
            lastMessageDate = "";
    }

    private void scrollToBottom() {
        if (recyclerView == null || layoutManager == null || messageList.isEmpty())
            return;
        recyclerView.post(() -> layoutManager.scrollToPositionWithOffset(messageList.size() - 1, 0));
    }

    // ── Date helpers ─────────────────────────────────────────────────────────

    private String extractDateFrom(String timestamp) {
        if (timestamp == null || timestamp.isEmpty())
            return formatDate(new Date());
        if (timestamp.contains(" "))
            return timestamp.split(" ")[0];
        if (timestamp.matches("\\d{4}-\\d{2}-\\d{2}"))
            return timestamp;
        return formatDate(new Date());
    }

    private String formatDateReadable(String date) {
        try {
            Date d = parseStorageDate(date);
            Date today = new Date();
            if (isSameDay(d, today))
                return "Today";
            Date yesterday = new Date(today.getTime() - 86400000L);
            if (isSameDay(d, yesterday))
                return "Yesterday";
            return formatDisplayDate(d);
        } catch (Exception e) {
            return date;
        }
    }

    private boolean isSameDay(Date a, Date b) {
        return formatDateKey(a).equals(formatDateKey(b));
    }

    private String formatDate(Date d) {
        synchronized (SDF_DATE) {
            return SDF_DATE.format(d);
        }
    }

    private String formatDisplayDate(Date d) {
        synchronized (SDF_DATE_DISPLAY) {
            return SDF_DATE_DISPLAY.format(d);
        }
    }

    private String formatDateKey(Date d) {
        synchronized (SDF_DATE_KEY) {
            return SDF_DATE_KEY.format(d);
        }
    }

    private String formatCurrentTime() {
        synchronized (SDF_TIME) {
            return SDF_TIME.format(new Date());
        }
    }

    private String formatShortDate(Date d) {
        synchronized (SDF_DATE_SHORT) {
            return SDF_DATE_SHORT.format(d);
        }
    }

    private Date parseStorageDate(String s) throws java.text.ParseException {
        synchronized (SDF_DATE) {
            return SDF_DATE.parse(s);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Image loading (ImageLoader interface)
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void loadCircular(ImageView iv, String url, int sizeDp) {
        loadImage(iv, url, sizeDp);
    }

    @Override
    public void loadWithPlaceholder(ImageView iv, String url, int maxDp) {
        loadImageWithPlaceholder(iv, url, maxDp);
    }

    private void loadImage(ImageView imageView, String imageUrl, int avatarSize) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }
        Bitmap cached = imageCache.get(imageUrl + "_" + avatarSize);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        imageExecutor.execute(() -> {
            try {
                int px = dpToPx(avatarSize);
                File tmp = downloadToTempFile(imageUrl);
                Bitmap bm = null;
                if (tmp != null) {
                    try {
                        BitmapFactory.Options bo = new BitmapFactory.Options();
                        bo.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(tmp.getAbsolutePath(), bo);
                        BitmapFactory.Options do2 = new BitmapFactory.Options();
                        do2.inSampleSize = calculateSampleSize(Math.max(bo.outWidth, bo.outHeight), px);
                        bm = BitmapFactory.decodeFile(tmp.getAbsolutePath(), do2);
                    } finally {
                        if (tmp.exists())
                            tmp.delete();
                    }
                }

                final Bitmap final1 = bm != null ? getCircularBitmap(bm, px) : null;
                uiHandler.post(() -> {
                    if (final1 != null) {
                        imageCache.put(imageUrl + "_" + avatarSize, final1);
                        imageView.setImageBitmap(final1);
                        ViewGroup.LayoutParams p = imageView.getLayoutParams();
                        p.width = px;
                        p.height = px;
                        imageView.setLayoutParams(p);
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> imageView.setImageResource(android.R.drawable.ic_menu_report_image));
            }
        });
    }

    private void loadImageWithPlaceholder(ImageView imageView, String url, int maxDpWidth) {
        if (url == null || url.isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        imageView.setAlpha(0.3f);

        Bitmap cached = imageCache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            imageView.setAlpha(1f);
            return;
        }

        imageExecutor.execute(() -> {
            try {
                File tmp = downloadToTempFile(url);
                if (tmp == null)
                    throw new Exception("Failed to download or find image");

                int tw = dpToPx(maxDpWidth);
                Bitmap fb = null;
                int th = tw;

                try {
                    BitmapFactory.Options bo = new BitmapFactory.Options();
                    bo.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(tmp.getAbsolutePath(), bo);
                    th = bo.outWidth > 0 ? Math.max(1, (int) (tw * (float) bo.outHeight / bo.outWidth)) : tw;
                    BitmapFactory.Options do2 = new BitmapFactory.Options();
                    do2.inSampleSize = calculateSampleSize(bo.outWidth > 0 ? bo.outWidth : tw, tw);
                    fb = BitmapFactory.decodeFile(tmp.getAbsolutePath(), do2);
                } finally {
                    if (tmp.exists())
                        tmp.delete();
                }

                final Bitmap finalFb = fb;
                final int finalTh = th;
                uiHandler.post(() -> {
                    if (finalFb != null) {
                        ViewGroup.LayoutParams p = imageView.getLayoutParams();
                        p.width = tw;
                        p.height = finalTh;
                        imageView.setLayoutParams(p);
                        imageCache.put(url, finalFb);
                        imageView.setImageBitmap(finalFb);
                        imageView.animate().alpha(1f).setDuration(300).start();
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                    imageView.animate().alpha(1f).setDuration(300).start();
                });
            }
        });
    }

    private File downloadToTempFile(String urlStr) throws Exception {
        if (!urlStr.startsWith("http")) {
            File f = new File(urlStr);
            return f.exists() ? f : null;
        }
        File tmp = new File(context.getCacheDir(), "img_tmp_" + System.nanoTime());
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setDoInput(true);
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.connect();
        try (InputStream in = c.getInputStream();
                FileOutputStream fos = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                fos.write(buf, 0, n);
            }
            return tmp;
        } finally {
            c.disconnect();
        }
    }

    private Bitmap getCircularBitmap(Bitmap src, int sizePx) {
        int s = Math.min(src.getWidth(), src.getHeight());
        Bitmap cr = Bitmap.createBitmap(src, (src.getWidth() - s) / 2, (src.getHeight() - s) / 2, s, s);
        Bitmap sc = Bitmap.createScaledBitmap(cr, sizePx, sizePx, true);
        if (sc != cr)
            cr.recycle();
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sc, 0, 0, p);
        return out;
    }

    private int calculateSampleSize(int orig, int target) {
        int ss = 1;
        if (orig > target) {
            int half = orig / 2;
            while ((half / ss) >= target)
                ss *= 2;
        }
        return ss;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Clipboard / Utility
    // ────────────────────────────────────────────────────────────────────────

    @SimpleFunction(description = "Copy text to clipboard.")
    public void CopyToClipboard(String text) {
        try {
            android.content.ClipboardManager cb = (android.content.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(android.content.ClipData.newPlainText("ChatMessage", text));
            ClipboardCopySuccess(text);
        } catch (Exception e) {
            ClipboardCopyFailed(e.getMessage());
        }
    }

    @SimpleFunction(description = "Get current time formatted as hh:mm a.")
    public String GetCurrentTime() {
        return formatCurrentTime();
    }

    @SimpleFunction(description = "Get current date formatted as dd-MM-yyyy.")
    public String GetCurrentDate() {
        return formatShortDate(new Date());
    }

    @SimpleFunction(description = "Get YouTube thumbnail URL from a YouTube URL or video ID.")
    public String GetYouTubeThumbnail(String youTubeURL) {
        String id = extractVideoId(youTubeURL);
        return id.isEmpty() ? "Invalid YouTube URL or ID." : "https://img.youtube.com/vi/" + id + "/mqdefault.jpg";
    }

    private String extractVideoId(String input) {
        if (input == null || input.trim().isEmpty())
            return "";
        if (!input.contains("http") && input.length() == 11)
            return input;
        try {
            Uri uri = Uri.parse(input);
            String v = uri.getQueryParameter("v");
            if (v != null)
                return v;
            List<String> segs = uri.getPathSegments();
            if (!segs.isEmpty())
                return segs.get(segs.size() - 1);
        } catch (Exception ignored) {
        }
        return "";
    }

    @SimpleFunction(description = "Add a reaction emoji to a message.")
    public void AddReaction(int messageId, String emoji) {
        uiHandler.post(() -> {
            int pos = findMessagePositionById(messageId);
            if (pos < 0)
                return;
            MessageModel m = messageList.get(pos);
            if (m.reactions == null)
                m.reactions = new LinkedHashMap<>();
            m.reactions.merge(emoji, 1, Integer::sum);
            if (chatAdapter != null)
                chatAdapter.notifyItemChanged(pos);
        });
    }

    @SimpleFunction(description = "Insert an 'Unread messages' separator at current position.")
    public void InsertUnreadSeparator() {
        uiHandler.post(() -> {
            messageList.add(new MessageModel(MessageModel.TYPE_UNREAD_SEPARATOR, "Unread Messages"));
            if (chatAdapter != null)
                chatAdapter.notifyItemInserted(messageList.size() - 1);
        });
    }

    @SimpleFunction(description = "Search messages containing query text. Returns list of matching IDs.")
    public YailList SearchMessages(String query) {
        List<Integer> results = new ArrayList<>();
        if (query == null || query.isEmpty())
            return YailList.makeList(results);
        String lower = query.toLowerCase(Locale.getDefault());
        for (MessageModel m : messageList) {
            if (m.messageId > 0 && m.message != null && m.message.toLowerCase(Locale.getDefault()).contains(lower)) {
                results.add(m.messageId);
            }
        }
        return YailList.makeList(results);
    }

    @SimpleFunction(description = "Returns the width in pixels of the VerticalArrangement (or screen width).")
    public int ArrangementWidthPx() {
        if (verticalArrangement != null && verticalArrangement.getView().getWidth() > 0)
            return verticalArrangement.getView().getWidth();
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    @SimpleFunction(description = "Returns the screen width in pixels.")
    public int ScreenWidthPx() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    @SimpleFunction(description = "Reset max width to 80% of screen width.")
    public void ResetTextMessageMaxWidth() {
        useResponsiveWidth = true;
        textMessageMaxWidth = 0;
        refreshChatConfig();
    }

    @SimpleFunction(description = "Reset date-header tracking (useful when starting a fresh conversation).")
    public void ResetDateTracking() {
        lastMessageDate = "";
    }

    // ── Menu item helpers ────────────────────────────────────────────────────

    @SimpleFunction(description = "Add custom text menu items (replaces existing custom items).")
    public void AddTextMenuItems(YailList menuItems) {
        customTextMenuItems.clear();
        int added = 0;
        if (menuItems != null) {
            for (Object o : menuItems.toArray()) {
                if (o == null)
                    continue;
                String s = o.toString().trim();
                if (!s.isEmpty() && !customTextMenuItems.contains(s)) {
                    customTextMenuItems.add(s);
                    added++;
                }
            }
        }
        TextMenuItemsAdded(added);
    }

    @SimpleFunction(description = "Add custom image menu items (replaces existing custom items).")
    public void AddImageMenuItems(YailList menuItems) {
        customImageMenuItems.clear();
        int added = 0;
        if (menuItems != null) {
            for (Object o : menuItems.toArray()) {
                if (o == null)
                    continue;
                String s = o.toString().trim();
                if (!s.isEmpty() && !customImageMenuItems.contains(s)) {
                    customImageMenuItems.add(s);
                    added++;
                }
            }
        }
        ImageMenuItemsAdded(added);
    }

    @SimpleFunction(description = "Clear custom text menu items.")
    public void ClearTextMenuItems() {
        customTextMenuItems.clear();
    }

    @SimpleFunction(description = "Clear custom image menu items.")
    public void ClearImageMenuItems() {
        customImageMenuItems.clear();
    }

    @SimpleFunction(description = "Get current text menu items as a list.")
    public YailList GetTextMenuItems() {
        return YailList.makeList(customTextMenuItems);
    }

    @SimpleFunction(description = "Get current image menu items as a list.")
    public YailList GetImageMenuItems() {
        return YailList.makeList(customImageMenuItems);
    }

    @SimpleFunction(description = "Get count of custom text menu items.")
    public int GetTextMenuItemsCount() {
        return customTextMenuItems.size();
    }

    @SimpleFunction(description = "Get count of custom image menu items.")
    public int GetImageMenuItemsCount() {
        return customImageMenuItems.size();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Events
    // ────────────────────────────────────────────────────────────────────────

    @SimpleEvent(description = "Called when image already exists and was not re-saved.")
    public void ImageAlreadyExists(String absolutePath, String fileName) {
        EventDispatcher.dispatchEvent(this, "ImageAlreadyExists", absolutePath, fileName);
    }

    @SimpleEvent(description = "Called when an image is saved successfully.")
    public void ImageSaved(String absolutePath, String fileName) {
        EventDispatcher.dispatchEvent(this, "ImageSaved", absolutePath, fileName);
    }

    @SimpleEvent(description = "Called when saving an image fails.")
    public void ImageSaveFailed(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ImageSaveFailed", errorMessage);
    }

    @SimpleEvent(description = "Triggered when a profile picture is clicked.")
    public void ProfilePictureClicked(String name, String avatarUrl) {
        EventDispatcher.dispatchEvent(this, "ProfilePictureClicked", name, avatarUrl);
    }

    @SimpleEvent(description = "Triggered when a message is selected.")
    public void MessageSelected(String message, int messageId) {
        EventDispatcher.dispatchEvent(this, "MessageSelected", message, messageId);
    }

    @SimpleEvent(description = "Triggered when a message is updated.")
    public void MessageUpdated(int messageId, String newMessage) {
        EventDispatcher.dispatchEvent(this, "MessageUpdated", messageId, newMessage);
    }

    @SimpleEvent(description = "Triggered when scroll to message completes.")
    public void MessageScrolledTo(int messageId) {
        EventDispatcher.dispatchEvent(this, "MessageScrolledTo", messageId);
    }

    @SimpleEvent(description = "Triggered when a text menu item is clicked.")
    public void TextMenuItemClicked(String itemText, String message, int messageId) {
        EventDispatcher.dispatchEvent(this, "TextMenuItemClicked", itemText, message, messageId);
    }

    @SimpleEvent(description = "Triggered when an image menu item is clicked.")
    public void ImageMenuItemClicked(String itemText, String imageUrl, int messageId) {
        EventDispatcher.dispatchEvent(this, "ImageMenuItemClicked", itemText, imageUrl, messageId);
    }

    @SimpleEvent(description = "Fired when text is successfully copied to clipboard.")
    public void ClipboardCopySuccess(String copiedText) {
        EventDispatcher.dispatchEvent(this, "ClipboardCopySuccess", copiedText);
    }

    @SimpleEvent(description = "Fired when copying to clipboard fails.")
    public void ClipboardCopyFailed(String error) {
        EventDispatcher.dispatchEvent(this, "ClipboardCopyFailed", error);
    }

    @SimpleEvent(description = "Triggered when selection is cleared.")
    public void SelectionCleared() {
        EventDispatcher.dispatchEvent(this, "SelectionCleared");
    }

    @SimpleEvent(description = "Triggered when text menu items are added.")
    public void TextMenuItemsAdded(int count) {
        EventDispatcher.dispatchEvent(this, "TextMenuItemsAdded", count);
    }

    @SimpleEvent(description = "Triggered when image menu items are added.")
    public void ImageMenuItemsAdded(int count) {
        EventDispatcher.dispatchEvent(this, "ImageMenuItemsAdded", count);
    }

    @SimpleEvent(description = "Fired when the user taps Forward in the context menu.")
    public void ForwardTriggered(int messageId, String message) {
        EventDispatcher.dispatchEvent(this, "ForwardTriggered", messageId, message);
    }

    @SimpleEvent(description = "Fired when the user taps Edit in the context menu (sent messages only).")
    public void EditRequested(int messageId, String currentMessage) {
        EventDispatcher.dispatchEvent(this, "EditRequested", messageId, currentMessage);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Designer + Setter/Getter Properties
    // ────────────────────────────────────────────────────────────────────────
    // (All existing v3.0 properties preserved; new ones appended below)

    @SimpleProperty(description = "Get avatar size in DP.")
    public int AvatarSize() {
        return avatarSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "40")
    @SimpleProperty(description = "Sets or gets AvatarSize.")
    public void AvatarSize(int v) {
        avatarSize = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get max width for text messages in DP.")
    public int TextMessageMaxWidth() {
        return textMessageMaxWidth;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty(description = "Sets or gets TextMessageMaxWidth.")
    public void TextMessageMaxWidth(int v) {
        textMessageMaxWidth = v;
        if (v == 0)
            useResponsiveWidth = true;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get max width for image messages in DP.")
    public int ImageMessageMaxWidth() {
        return imageMessageMaxWidth;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty(description = "Sets or gets ImageMessageMaxWidth.")
    public void ImageMessageMaxWidth(int v) {
        imageMessageMaxWidth = v == 0 ? 300 : v;
        if (v == 0)
            useResponsiveWidth = true;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets FullscreenImageBGColor.")
    public int FullscreenImageBGColor() {
        return fullscreenImageBGColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0C0C0C")
    @SimpleProperty(description = "Sets or gets FullscreenImageBGColor.")
    public void FullscreenImageBGColor(int v) {
        fullscreenImageBGColor = v;
    }

    @SimpleProperty(description = "Sets or gets AvatarBackgroundColor.")
    public int AvatarBackgroundColor() {
        return avatarBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFDDDDDD")
    @SimpleProperty(description = "Sets or gets AvatarBackgroundColor.")
    public void AvatarBackgroundColor(int v) {
        avatarBackgroundColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SentMessageTextColor.")
    public int SentMessageTextColor() {
        return sentMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty(description = "Sets or gets SentMessageTextColor.")
    public void SentMessageTextColor(int v) {
        sentMessageTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ReceivedMessageTextColor.")
    public int ReceivedMessageTextColor() {
        return receivedMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Sets or gets ReceivedMessageTextColor.")
    public void ReceivedMessageTextColor(int v) {
        receivedMessageTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SentMessageBackgroundColor.")
    public int SentMessageBackgroundColor() {
        return sentMessageBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0084FF")
    @SimpleProperty(description = "Sets or gets SentMessageBackgroundColor.")
    public void SentMessageBackgroundColor(int v) {
        sentMessageBackgroundColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ReceivedMessageBackgroundColor.")
    public int ReceivedMessageBackgroundColor() {
        return receivedMessageBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFF0F0F0")
    @SimpleProperty(description = "Sets or gets ReceivedMessageBackgroundColor.")
    public void ReceivedMessageBackgroundColor(int v) {
        receivedMessageBackgroundColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets MessageFontSize.")
    public int MessageFontSize() {
        return messageFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "16")
    @SimpleProperty(description = "Sets or gets MessageFontSize.")
    public void MessageFontSize(int v) {
        messageFontSize = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SystemMessageFontSize.")
    public int SystemMessageFontSize() {
        return systemMessageFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "14")
    @SimpleProperty(description = "Sets or gets SystemMessageFontSize.")
    public void SystemMessageFontSize(int v) {
        systemMessageFontSize = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SystemMessageTextColor.")
    public int SystemMessageTextColor() {
        return systemMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Sets or gets SystemMessageTextColor.")
    public void SystemMessageTextColor(int v) {
        systemMessageTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SelectedMessageBgColor.")
    public int SelectedMessageBgColor() {
        return selectedMessageBgColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H8CF9D3FF")
    @SimpleProperty(description = "Sets or gets SelectedMessageBgColor.")
    public void SelectedMessageBgColor(int v) {
        selectedMessageBgColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets TypingIndicatorTextColor.")
    public int TypingIndicatorTextColor() {
        return typingIndicatorTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Sets or gets TypingIndicatorTextColor.")
    public void TypingIndicatorTextColor(int v) {
        typingIndicatorTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets TimestampFontSize.")
    public int TimestampFontSize() {
        return timestampFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Sets or gets TimestampFontSize.")
    public void TimestampFontSize(int v) {
        timestampFontSize = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets TimestampTextColor.")
    public int TimestampTextColor() {
        return timestampTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Sets or gets TimestampTextColor.")
    public void TimestampTextColor(int v) {
        timestampTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SentStatusTextColor.")
    public int SentStatusTextColor() {
        return sentStatusTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0000FF")
    @SimpleProperty(description = "Sets or gets SentStatusTextColor.")
    public void SentStatusTextColor(int v) {
        sentStatusTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ReceivedStatusTextColor.")
    public int ReceivedStatusTextColor() {
        return receivedStatusTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFF00FF")
    @SimpleProperty(description = "Sets or gets ReceivedStatusTextColor.")
    public void ReceivedStatusTextColor(int v) {
        receivedStatusTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SentStatusText.")
    public String SentStatusText() {
        return sentStatusText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "✓✓")
    @SimpleProperty(description = "Sets or gets SentStatusText.")
    public void SentStatusText(String v) {
        sentStatusText = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ReceivedStatusText.")
    public String ReceivedStatusText() {
        return receivedStatusText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "🚀")
    @SimpleProperty(description = "Sets or gets ReceivedStatusText.")
    public void ReceivedStatusText(String v) {
        receivedStatusText = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SentNameTextColor.")
    public int SentNameTextColor() {
        return sentNameTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Sets or gets SentNameTextColor.")
    public void SentNameTextColor(int v) {
        sentNameTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ReceivedNameTextColor.")
    public int ReceivedNameTextColor() {
        return receivedNameTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Sets or gets ReceivedNameTextColor.")
    public void ReceivedNameTextColor(int v) {
        receivedNameTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets NameFontSize.")
    public int NameFontSize() {
        return nameFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Sets or gets NameFontSize.")
    public void NameFontSize(int v) {
        nameFontSize = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets MessageCornerRadius.")
    public float MessageCornerRadius() {
        return messageCornerRadius;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "20.0")
    @SimpleProperty(description = "Sets or gets MessageCornerRadius.")
    public void MessageCornerRadius(float v) {
        messageCornerRadius = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets MessageHorizontalPadding.")
    public int MessageHorizontalPadding() {
        return messageHorizontalPadding;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "16")
    @SimpleProperty(description = "Sets or gets MessageHorizontalPadding.")
    public void MessageHorizontalPadding(int v) {
        messageHorizontalPadding = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets MessageVerticalPadding.")
    public int MessageVerticalPadding() {
        return messageVerticalPadding;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Sets or gets MessageVerticalPadding.")
    public void MessageVerticalPadding(int v) {
        messageVerticalPadding = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ShowTimestamp.")
    public boolean ShowTimestamp() {
        return showTimestamp;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets ShowTimestamp.")
    public void ShowTimestamp(boolean v) {
        showTimestamp = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ShowReadStatus.")
    public boolean ShowReadStatus() {
        return showReadStatus;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets ShowReadStatus.")
    public void ShowReadStatus(boolean v) {
        showReadStatus = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets AutoLinkEnabledInChat.")
    public boolean AutoLinkEnabledInChat() {
        return autoLinkEnabledInChat;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets AutoLinkEnabledInChat.")
    public void AutoLinkEnabledInChat(boolean v) {
        autoLinkEnabledInChat = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ShowMetadataInsideBubble.")
    public boolean ShowMetadataInsideBubble() {
        return showMetadataInsideBubble;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Sets or gets ShowMetadataInsideBubble.")
    public void ShowMetadataInsideBubble(boolean v) {
        showMetadataInsideBubble = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets ImageWidthFixInTextImageMessage.")
    public boolean ImageWidthFixInTextImageMessage() {
        return imageFunctionWidthFix;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets ImageWidthFixInTextImageMessage.")
    public void ImageWidthFixInTextImageMessage(boolean v) {
        imageFunctionWidthFix = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets CustomFontFamily.")
    public String CustomFontFamily() {
        return customFontFamily;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET, defaultValue = "")
    @SimpleProperty(description = "Sets or gets CustomFontFamily.")
    public void CustomFontFamily(String v) {
        loadTypeface(v);
        customFontFamily = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets TypingIndicatorText.")
    public String TypingIndicatorText() {
        return typingIndicatorText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "Typing")
    @SimpleProperty(description = "Sets or gets TypingIndicatorText.")
    public void TypingIndicatorText(String v) {
        typingIndicatorText = v;
        if (typingIndicatorPosition >= 0 && typingIndicatorPosition < messageList.size()) {
            messageList.get(typingIndicatorPosition).message = v.isEmpty() ? "Typing" : v;
            if (chatAdapter != null)
                chatAdapter.notifyItemChanged(typingIndicatorPosition);
        }
    }

    @SimpleProperty(description = "Sets or gets SquareBubbleEdge.")
    public boolean SquareBubbleEdge() {
        return squareBubbleEdge;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Sets or gets SquareBubbleEdge.")
    public void SquareBubbleEdge(boolean v) {
        squareBubbleEdge = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Sets or gets SquareEdgeCornerRadius.")
    public float SquareEdgeCornerRadius() {
        return squareEdgeCornerRadius;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "8.0")
    @SimpleProperty(description = "Sets or gets SquareEdgeCornerRadius.")
    public void SquareEdgeCornerRadius(float v) {
        squareEdgeCornerRadius = v;
        refreshChatConfig();
    }

    // ── NEW Properties ───────────────────────────────────────────────────

    @SimpleProperty(description = "Enable rendering of HTML tags in message text.")
    public boolean HtmlEnabledInChat() {
        return htmlEnabledInChat;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Sets or gets HtmlEnabledInChat.")
    public void HtmlEnabledInChat(boolean v) {
        htmlEnabledInChat = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Enable Markdown rendering in message text (bold, italic, code, links, etc.).")
    public boolean MarkdownEnabledInChat() {
        return markdownEnabledInChat;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Sets or gets MarkdownEnabledInChat.")
    public void MarkdownEnabledInChat(boolean v) {
        markdownEnabledInChat = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Enable automatic link-preview cards for URLs in messages.")
    public boolean LinkPreviewEnabled() {
        return linkPreviewEnabled;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets LinkPreviewEnabled.")
    public void LinkPreviewEnabled(boolean v) {
        linkPreviewEnabled = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Background color for link-preview cards.")
    public int LinkPreviewBgColor() {
        return linkPreviewBgColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFF5F5F5")
    @SimpleProperty(description = "Sets or gets LinkPreviewBgColor.")
    public void LinkPreviewBgColor(int v) {
        linkPreviewBgColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Accent color for link-preview cards (top bar and site name).")
    public int LinkPreviewAccentColor() {
        return linkPreviewAccentColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0084FF")
    @SimpleProperty(description = "Sets or gets LinkPreviewAccentColor.")
    public void LinkPreviewAccentColor(int v) {
        linkPreviewAccentColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Show or hide the 'edited' label on updated messages.")
    public boolean ShowEditedLabel() {
        return showEditedLabel;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets ShowEditedLabel.")
    public void ShowEditedLabel(boolean v) {
        showEditedLabel = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Text used for the edited label (default: 'edited').")
    public String EditedLabelText() {
        return editedLabelText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "edited")
    @SimpleProperty(description = "Sets or gets EditedLabelText.")
    public void EditedLabelText(String v) {
        editedLabelText = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Color for the edited label.")
    public int EditedLabelColor() {
        return editedLabelColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Sets or gets EditedLabelColor.")
    public void EditedLabelColor(int v) {
        editedLabelColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Color of the star indicator shown in the metadata row.")
    public int StarredIndicatorColor() {
        return starredIndicatorColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFD700")
    @SimpleProperty(description = "Sets or gets StarredIndicatorColor.")
    public void StarredIndicatorColor(int v) {
        starredIndicatorColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Text/emoji shown as the star indicator (default: ★).")
    public String StarredIndicatorText() {
        return starredIndicatorText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "★")
    @SimpleProperty(description = "Sets or gets StarredIndicatorText.")
    public void StarredIndicatorText(String v) {
        starredIndicatorText = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Background color of the reply-quote strip on SENT messages.")
    public int SentReplyBubbleBgColor() {
        return sentReplyBubbleBgColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H44FFFFFF")
    @SimpleProperty(description = "Sets or gets SentReplyBubbleBgColor.")
    public void SentReplyBubbleBgColor(int v) {
        sentReplyBubbleBgColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Background color of the reply-quote strip on RECEIVED messages.")
    public int ReplyBubbleBgColor() {
        return replyBubbleBgColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H220084FF")
    @SimpleProperty(description = "Sets or gets ReplyBubbleBgColor.")
    public void ReplyBubbleBgColor(int v) {
        replyBubbleBgColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Accent color of the reply-quote left border.")
    public int ReplyAccentColor() {
        return replyAccentColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0084FF")
    @SimpleProperty(description = "Sets or gets ReplyAccentColor.")
    public void ReplyAccentColor(int v) {
        replyAccentColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "The color of the text preview inside the reply-quote.")
    public int ReplyPreviewTextColor() {
        return replyPreviewTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF444444")
    @SimpleProperty(description = "Sets or gets ReplyPreviewTextColor.")
    public void ReplyPreviewTextColor(int v) {
        replyPreviewTextColor = v;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Enable or disable swipe-to-reply gesture.")
    public boolean SwipeToReplyEnabled() {
        return swipeToReplyEnabled;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets SwipeToReplyEnabled.")
    public void SwipeToReplyEnabled(boolean v) {
        swipeToReplyEnabled = v;
        if (recyclerView != null)
            attachSwipeHelper();
    }

    @SimpleProperty(description = "Show or hide default context-menu items (Reply, Star, Copy, Delete, Forward, Edit).")
    public boolean ShowDefaultMenuItems() {
        return showDefaultMenuItems;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Sets or gets ShowDefaultMenuItems.")
    public void ShowDefaultMenuItems(boolean v) {
        showDefaultMenuItems = v;
        refreshChatConfig();
    }

    // ── Font loading (unchanged) ─────────────────────────────────────────────

    private void loadTypeface(String path) {
        if (path == null || path.trim().isEmpty()) {
            typeface = Typeface.DEFAULT;
            return;
        }
        try {
            if (isCompanion()) {
                String pkg = form.getPackageName();
                String resolved = Build.VERSION.SDK_INT > 28
                        ? "/storage/emulated/0/Android/data/" + pkg + "/files/assets/" + path
                        : "/storage/emulated/0/" + detectPlatform(pkg) + "/assets/" + path;
                File f = new File(resolved);
                typeface = f.exists() ? Typeface.createFromFile(f) : Typeface.DEFAULT;
            } else {
                typeface = Typeface.createFromAsset(form.$context().getAssets(), path);
            }
        } catch (Exception e) {
            typeface = Typeface.DEFAULT;
        }
    }

    private boolean isCompanion() {
        String p = form.getPackageName();
        return p.contains("makeroid") || p.contains("kodular") || p.contains("Niotron")
                || p.contains("Appzard") || p.contains("appinventor") || p.contains("androidbuilder");
    }

    private String detectPlatform(String p) {
        if (p.contains("makeroid"))
            return "Makeroid";
        if (p.contains("kodular"))
            return "Kodular";
        if (p.contains("Niotron"))
            return "Niotron";
        if (p.contains("Appzard"))
            return "Appzard";
        if (p.contains("androidbuilder"))
            return "AndroidBuilder";
        return "AppInventor";
    }

    // ── Swipe-dismiss helper (unchanged from v3.0) ───────────────────────────

    public static class OnSwipeTouchListener implements View.OnTouchListener {
        private final android.view.GestureDetector detector;

        public void onSwipeDown() {
        }

        public OnSwipeTouchListener(Context ctx) {
            detector = new android.view.GestureDetector(ctx,
                    new android.view.GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(android.view.MotionEvent e) {
                            return true;
                        }

                        @Override
                        public boolean onFling(android.view.MotionEvent e1,
                                android.view.MotionEvent e2,
                                float vX, float vY) {
                            float dy = e2.getY() - e1.getY();
                            if (Math.abs(dy) > 100 && Math.abs(vY) > 100 && dy > 0)
                                onSwipeDown();
                            return true;
                        }
                    });
        }

        @Override
        public boolean onTouch(View v, android.view.MotionEvent e) {
            return detector.onTouchEvent(e);
        }
    }

    @SimpleProperty(description = "Whether to show date headers in the chat.")
    public boolean ShowDateHeaders() {
        return showDateHeaders;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Enable or disable date headers.")
    public void ShowDateHeaders(boolean v) {
        showDateHeaders = v;
        refreshChatConfig();
    }
}