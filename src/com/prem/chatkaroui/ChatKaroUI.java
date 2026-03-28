package com.prem.chatkaroui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.LinearLayout;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.*;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.graphics.Color.parseColor;

@DesignerComponent(version = 2, versionName = "2.1", description = "ChatKaroUI is a customizable chat component with text, images and messages support. <br>"
        +
        "Made by: Arun Gupta <br>" +
        "<span><a href=\"https://community.appinventor.mit.edu/t/154865\" target=\"_blank\"><small><mark>Mit AI2 Community</mark></small></a></span> | "
        +
        "<span><a href=\"https://community.kodular.io/t/301309\" target=\"_blank\"><small><mark>Kodular Community</mark></small></a></span>", nonVisible = true, iconName = "icon.png", helpUrl = "https://www.telegram.me/Arungupta1526")
public class ChatKaroUI extends AndroidNonvisibleComponent implements Component, ImageLoader {

    // Add these with other instance variables
    private int nextMessageId = 1;
    private final SparseArray<String> messageTextsById = new SparseArray<>();
    private final SparseArray<Boolean> messageSentFlagsById = new SparseArray<>();
    private final SparseArray<Integer> messagePositionById = new SparseArray<>();
    // Add these properties with other configuration properties
    private boolean squareBubbleEdge = false;
    // private int squareEdgeCornerRadius = 8; // Smaller radius for square-ish look
    private float squareEdgeCornerRadius = 8f; // Smaller radius for square-ish look
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_KEY = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_DISPLAY = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat SDF_TIME = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat SDF_DATE_SHORT = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    // UI Components
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ChatAdapter chatAdapter;
    private ChatConfig chatConfig;
    private final List<MessageModel> messageList = new ArrayList<>();
    private int typingIndicatorPosition = -1;

    // Configuration properties with default values
    private String sentStatusText = "✓✓"; // Default: double check "&#x2714;&#x270C;"
    private String receivedStatusText = "🚀"; // Default: rocket "&#x1F680;"
    // private String fontFamily = "sans-serif";
    private String customFontFamily = "";
    private String typingIndicatorText = "";
    private int timestampTextColor = Color.GRAY;
    private int sentStatusTextColor = Color.BLUE;
    private int receivedStatusTextColor = Color.MAGENTA;
    private int sentMessageBackgroundColor = parseColor("#0084ff");
    private int receivedMessageBackgroundColor = parseColor("#f0f0f0");
    private int sentMessageTextColor = Color.WHITE;
    private int sentNameTextColor = Color.BLACK;
    private int receivedMessageTextColor = Color.BLACK;
    private int receivedNameTextColor = Color.BLACK;
    // private int toggleMesgSelBgColor = parseColor("#f9d3ff8c");
    private int selectedMessageBgColor = parseColor("#add9b5");
    private int typingIndicatorTextColor = Color.GRAY;
    private int systemMessageTextColor = Color.GRAY;
    private int fullscreenImageBGColor = parseColor("#0c0c0c");
    private int avatarBackgroundColor = Color.LTGRAY;
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
    private boolean showTimestamp = true;
    private boolean showReadStatus = true;
    private boolean showTypingIndicator = false;
    private boolean showMetadataInsideBubble = false;
    private boolean useResponsiveWidth = true;
    private final List<String> customImageMenuItems = new ArrayList<>();
    private final List<String> customTextMenuItems = new ArrayList<>();

    // Handlers and threading
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final List<Integer> selectedMessageIds = new ArrayList<>();
    private Typeface typeface;
    private Runnable typingRunnable;
    private boolean autoLinkEnabledInChat = true;

    // Image caching
    private final LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private boolean imageFunctionWidthFix = true;
    private AndroidViewComponent verticalArrangement;

    /**
     * Constructor for ChatKaroUI component
     *
     * @param container The parent container where the component will be placed
     */
    public ChatKaroUI(ComponentContainer container) {
        super(container.$form());
        this.context = container.$context();
    }

    /**
     * Initializes the chat UI within a VerticalArrangement
     *
     * @param arrangement The VerticalArrangement to initialize the chat in
     */
    @SimpleFunction(description = "Initialize the chat UI in a VerticalArrangement. " +
            "This must be called before adding any messages.")
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
                public void onMessageSelected(String message, int messageId) {
                    handleMessageSelectionToggle(message, messageId);
                }

                @Override
                public void onTextMenuItemClicked(String item, String message, int messageId) {
                    TextMenuItemClicked(item, message, messageId);
                }

                @Override
                public void onImageMenuItemClicked(String item, String imageUrl, int messageId) {
                    ImageMenuItemClicked(item, imageUrl, messageId);
                }

                @Override
                public void onProfilePictureClicked(String name, String avatarUrl) {
                    ProfilePictureClicked(name, avatarUrl);
                }

                @Override
                public void showTextOptionsMenu(View anchor, String message, int messageId) {
                    ChatKaroUI.this.showTextOptionsMenu(anchor, message, messageId);
                }

                @Override
                public void showImageOptionsMenu(View anchor, String imageUrl, ImageView imageView, int messageId) {
                    ChatKaroUI.this.showImageOptionsMenu(anchor, imageUrl, imageView, messageId);
                }

                @Override
                public void showFullscreenImage(Drawable drawable) {
                    ChatKaroUI.this.showFullscreenImage(drawable);
                }

                @Override
                public boolean isMultiSelectionActive() {
                    return IsMultiSelectionActive();
                }
            });
            recyclerView.setAdapter(chatAdapter);

            FrameLayout frameLayout = (FrameLayout) arrangement.getView();
            FrameLayout rootContainer = new FrameLayout(context);
            rootContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            rootContainer.addView(recyclerView);
            frameLayout.addView(rootContainer);
        } catch (Exception e) {
            Log.e("ChatUI", "Initialization error: " + e.getMessage());
        }
    }

    private ChatConfig buildConfig() {
        ChatConfig config = new ChatConfig();
        config.sentMessageBackgroundColor = sentMessageBackgroundColor;
        config.receivedMessageBackgroundColor = receivedMessageBackgroundColor;
        config.sentMessageTextColor = sentMessageTextColor;
        config.receivedMessageTextColor = receivedMessageTextColor;
        config.sentNameTextColor = sentNameTextColor;
        config.receivedNameTextColor = receivedNameTextColor;
        config.selectedMessageBgColor = selectedMessageBgColor;
        config.timestampTextColor = timestampTextColor;
        config.sentStatusTextColor = sentStatusTextColor;
        config.receivedStatusTextColor = receivedStatusTextColor;
        config.systemMessageTextColor = systemMessageTextColor;
        config.typingIndicatorTextColor = typingIndicatorTextColor;
        config.avatarBackgroundColor = avatarBackgroundColor;
        config.messageFontSize = messageFontSize;
        config.systemMessageFontSize = systemMessageFontSize;
        config.nameFontSize = nameFontSize;
        config.timestampFontSize = timestampFontSize;
        config.avatarSize = avatarSize;
        config.textMessageMaxWidth = textMessageMaxWidth;
        config.imageMessageMaxWidth = imageMessageMaxWidth;
        config.messageHorizontalPadding = messageHorizontalPadding;
        config.messageVerticalPadding = messageVerticalPadding;
        config.messageCornerRadius = messageCornerRadius;
        config.squareEdgeCornerRadius = squareEdgeCornerRadius;
        config.showTimestamp = showTimestamp;
        config.showReadStatus = showReadStatus;
        config.showMetadataInsideBubble = showMetadataInsideBubble;
        config.showMetadataOutBubble = !showMetadataInsideBubble;
        config.squareBubbleEdge = squareBubbleEdge;
        config.autoLinkEnabledInChat = autoLinkEnabledInChat;
        config.useResponsiveWidth = useResponsiveWidth;
        config.imageFunctionWidthFix = imageFunctionWidthFix;
        config.sentStatusText = sentStatusText;
        config.receivedStatusText = receivedStatusText;
        config.typeface = typeface;
        config.arrangementWidthPx = ArrangementWidthPx();
        return config;
    }

    private void refreshChatConfig() {
        if (chatConfig == null) {
            return;
        }
        ChatConfig updated = buildConfig();
        chatConfig.sentMessageBackgroundColor = updated.sentMessageBackgroundColor;
        chatConfig.receivedMessageBackgroundColor = updated.receivedMessageBackgroundColor;
        chatConfig.sentMessageTextColor = updated.sentMessageTextColor;
        chatConfig.receivedMessageTextColor = updated.receivedMessageTextColor;
        chatConfig.sentNameTextColor = updated.sentNameTextColor;
        chatConfig.receivedNameTextColor = updated.receivedNameTextColor;
        chatConfig.selectedMessageBgColor = updated.selectedMessageBgColor;
        chatConfig.timestampTextColor = updated.timestampTextColor;
        chatConfig.sentStatusTextColor = updated.sentStatusTextColor;
        chatConfig.receivedStatusTextColor = updated.receivedStatusTextColor;
        chatConfig.systemMessageTextColor = updated.systemMessageTextColor;
        chatConfig.typingIndicatorTextColor = updated.typingIndicatorTextColor;
        chatConfig.avatarBackgroundColor = updated.avatarBackgroundColor;
        chatConfig.messageFontSize = updated.messageFontSize;
        chatConfig.systemMessageFontSize = updated.systemMessageFontSize;
        chatConfig.nameFontSize = updated.nameFontSize;
        chatConfig.timestampFontSize = updated.timestampFontSize;
        chatConfig.avatarSize = updated.avatarSize;
        chatConfig.textMessageMaxWidth = updated.textMessageMaxWidth;
        chatConfig.imageMessageMaxWidth = updated.imageMessageMaxWidth;
        chatConfig.messageHorizontalPadding = updated.messageHorizontalPadding;
        chatConfig.messageVerticalPadding = updated.messageVerticalPadding;
        chatConfig.messageCornerRadius = updated.messageCornerRadius;
        chatConfig.squareEdgeCornerRadius = updated.squareEdgeCornerRadius;
        chatConfig.showTimestamp = updated.showTimestamp;
        chatConfig.showReadStatus = updated.showReadStatus;
        chatConfig.showMetadataInsideBubble = updated.showMetadataInsideBubble;
        chatConfig.showMetadataOutBubble = updated.showMetadataOutBubble;
        chatConfig.squareBubbleEdge = updated.squareBubbleEdge;
        chatConfig.autoLinkEnabledInChat = updated.autoLinkEnabledInChat;
        chatConfig.useResponsiveWidth = updated.useResponsiveWidth;
        chatConfig.imageFunctionWidthFix = updated.imageFunctionWidthFix;
        chatConfig.sentStatusText = updated.sentStatusText;
        chatConfig.receivedStatusText = updated.receivedStatusText;
        chatConfig.typeface = updated.typeface;
        chatConfig.arrangementWidthPx = updated.arrangementWidthPx;

        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }

    // Event declarations
    // @SimpleEvent(description = "Triggered when chat initializes successfully")
    // public void Initialized() {
    // EventDispatcher.dispatchEvent(this, "Initialized");
    // }
    //
    // @SimpleEvent(description = "Triggered when chat initialization fails with
    // error message")
    // public void InitializeError(String errorMessage) {
    // EventDispatcher.dispatchEvent(this, "InitializeError", errorMessage);
    // }

    /**
     * Custom touch listener for swipe gestures
     */
    public static class OnSwipeTouchListener implements View.OnTouchListener {
        private final GestureDetector detector;

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public OnSwipeTouchListener(Context ctx) {
            detector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 100;
                private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                // public void onSwipeLeft() {}
                // public void onSwipeRight() {}
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2,
                        float velocityX, float velocityY) {
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                            Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0)
                            onSwipeDown();
                        return true;
                    }
                    // abhi 14/07/25
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0)
                            onSwipeRight();
                        else
                            onSwipeLeft();
                        return true;
                    }
                    // end
                    return false;
                }
            });
        }

        public void onSwipeDown() {
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return detector.onTouchEvent(event);
        }
    }

    // Message sending/receiving functions
    @SimpleFunction(description = "Send a simple message without avatar or name")
    public void SendSimple(String message, String timestamp) {
        final int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_SENT_SIMPLE,
                message, "", "", timestamp));
    }

    @SimpleFunction(description = "Receive a simple message without avatar or name")
    public void ReceiveSimple(String message, String timestamp) {
        final int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_RECEIVED_SIMPLE,
                message, "", "", timestamp));
    }

    @SimpleFunction(description = "Send a message with avatar and sender name")
    public void SendWithAvatar(String message, String avatarUrl, String senderName, String timestamp) {
        final int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_SENT_AVATAR,
                message, avatarUrl, senderName, timestamp));
    }

    @SimpleFunction(description = "Receive a message with avatar and sender name")
    public void ReceiveWithAvatar(String message, String avatarUrl, String receiverName, String timestamp) {
        final int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_RECEIVED_AVATAR,
                message, avatarUrl, receiverName, timestamp));
    }

    @SimpleFunction(description = "Send a message with both text and image")
    public void SendTextImage(String message, String imageUrl, String avatarUrl, String senderName, String timestamp,
            boolean messageOnTop) {
        final int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_SENT_TEXT_IMAGE,
                message, imageUrl, avatarUrl, senderName, timestamp, messageOnTop));
    }

    @SimpleFunction(description = "Receive a message with both text and image")
    public void ReceiveTextImage(String message, String imageUrl, String avatarUrl, String receiverName,
            String timestamp, boolean messageOnTop) {
        final int id = nextMessageId++;
        addMessageInternal(new MessageModel(id, MessageModel.TYPE_RECEIVED_TEXT_IMAGE,
                message, imageUrl, avatarUrl, receiverName, timestamp, messageOnTop));
    }

    /**
     * Adds a system systemMessage (like "User joined") to the chat
     *
     * @param message The systemMessage text to display
     */
    @SimpleFunction(description = "Add system systemMessage (e.g., 'User joined')")
    public void AddSystemMessage(String message) {
        uiHandler.post(() -> {
            messageList.add(new MessageModel(MessageModel.TYPE_SYSTEM, message));
            if (chatAdapter != null) {
                chatAdapter.notifyItemInserted(messageList.size() - 1);
            }
            scrollToBottom();
        });
    }

    private void addMessageInternal(MessageModel model) {
        uiHandler.post(() -> {
            if (model.messageId > 0) {
                String date = extractDateFrom(model.timestamp);
                if (!date.equals(lastMessageDate)) {
                    String displayDate = formatDateReadable(date);
                    messageList.add(new MessageModel(MessageModel.TYPE_DATE_HEADER, displayDate));
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                    }
                    lastMessageDate = date;
                }
            }

            if (model.messageId > 0) {
                messageTextsById.put(model.messageId, model.message != null ? model.message : "");
                messageSentFlagsById.put(model.messageId,
                        model.viewType == MessageModel.TYPE_SENT_SIMPLE
                                || model.viewType == MessageModel.TYPE_SENT_AVATAR
                                || model.viewType == MessageModel.TYPE_SENT_TEXT_IMAGE);
            }

            messageList.add(model);
            int insertedPos = messageList.size() - 1;
            if (model.messageId > 0) {
                messagePositionById.put(model.messageId, insertedPos);
            }
            if (chatAdapter != null) {
                chatAdapter.notifyItemInserted(insertedPos);
            }
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        if (recyclerView == null || layoutManager == null || messageList.isEmpty()) {
            return;
        }
        recyclerView.post(() -> layoutManager.scrollToPositionWithOffset(messageList.size() - 1, 0));
    }

    /**
     * Shows an image in fullscreen mode
     *
     * @param drawable The Drawable to display in fullscreen
     */
    private void showFullscreenImage(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        Dialog dialog = new Dialog(context,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        ImageView fullImageView = new ImageView(context);
        fullImageView.setImageDrawable(drawable);
        fullImageView.setBackgroundColor(fullscreenImageBGColor);
        fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // fullImageView.setOnClickListener(view -> dialog.dismiss());

        // Add swipe-down to dismiss
        fullImageView.setOnTouchListener(new OnSwipeTouchListener(context) {
            @Override
            public void onSwipeDown() {
                dialog.dismiss();
            }
        });

        dialog.setContentView(fullImageView);
        dialog.show();
    }

    /**
     * Downloads an image from the URL, saves it to shared app folder as PNG or JPG.
     * Fires ImageSaved or ImageSaveFailed events.
     *
     * @param imageUrl The direct URL of the image to download
     * @param format   Desired format of the saved image ("png" or "jpg")
     */
    @SimpleFunction(description = "Downloads an image from the URL, saves it to chat-images folder under ASD as PNG or JPG. Fires ImageSaved, ImageAlreadyExists or ImageSaveFailed.")
    public void DownloadImage(String imageUrl, String format) {
        imageExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.connect();

                Bitmap bitmap;
                try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                    bitmap = BitmapFactory.decodeStream(input);
                }

                if (bitmap != null) {

                    // Target: Android/data/<your.package>/files/chat-images/
                    File externalFilesDir = context.getExternalFilesDir(null); // app-private
                    File sharedDir = new File(externalFilesDir, "chat-images");
                    if (!sharedDir.exists())
                        sharedDir.mkdirs();

                    // Determine format and extension
                    String extension = format.equalsIgnoreCase("jpg") ? "jpg" : "png";
                    Bitmap.CompressFormat compressFormat = extension.equals("jpg") ? Bitmap.CompressFormat.JPEG
                            : Bitmap.CompressFormat.PNG;

                    // 📛 Filename derived from URL path
                    String baseName = new File(url.getPath()).getName();

                    // If baseName is empty or purely numeric, fallback to hash
                    if (baseName.isEmpty() || baseName.matches("^\\d+$")) {
                        baseName = "url_" + Math.abs(imageUrl.hashCode()); // or use MD5 if desired
                    }

                    // Sanitize the final name
                    baseName = baseName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");

                    String fileName = "img_" + baseName + "." + extension;

                    File file = new File(sharedDir, fileName);

                    if (file.exists()) {
                        String absPath = file.getAbsolutePath();
                        String fn = fileName;
                        uiHandler.post(() -> ImageAlreadyExists(absPath, fn));
                        return;
                    }

                    try (FileOutputStream stream = new FileOutputStream(file)) {
                        bitmap.compress(compressFormat, 100, stream);
                    }

                    String absPath = file.getAbsolutePath();
                    String fn = fileName;
                    uiHandler.post(() -> ImageSaved(absPath, fn));
                } else {
                    uiHandler.post(() -> ImageSaveFailed("Failed to decode image bitmap."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String errMsg = e.getMessage();
                uiHandler.post(() -> ImageSaveFailed("Download or save error: " + errMsg));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Called when image already exists and was not re-saved.
     *
     * @param absolutePath Full path of the existing image
     * @param fileName     File name of the existing image
     */
    @SimpleEvent(description = "Called when the image already exists and was not re-saved.")
    public void ImageAlreadyExists(String absolutePath, String fileName) {
        EventDispatcher.dispatchEvent(this, "ImageAlreadyExists", absolutePath, fileName);
    }

    /**
     * Called when an image is saved successfully.
     *
     * @param absolutePath Full file system path to the saved image
     * @param fileName     The name of the saved image file
     */
    @SimpleEvent(description = "Called when an image is saved successfully. Returns absolute path and file name.")
    public void ImageSaved(String absolutePath, String fileName) {
        EventDispatcher.dispatchEvent(this, "ImageSaved", absolutePath, fileName);
    }

    /**
     * Called when saving an image fails.
     *
     * @param errorMessage The error message explaining the failure
     */
    @SimpleEvent(description = "Called when saving an image fails. Returns the error message.")
    public void ImageSaveFailed(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ImageSaveFailed", errorMessage);
    }

    @SimpleEvent(description = "Triggered when profile picture is clicked")
    public void ProfilePictureClicked(String name, String avatarUrl) {
        EventDispatcher.dispatchEvent(this, "ProfilePictureClicked", name, avatarUrl);
    }

    @SimpleFunction(description = "Returns the width in pixels of the VerticalArrangement passed to Initialize, or screen width as fallback.")
    public int ArrangementWidthPx() {
        if (verticalArrangement != null && verticalArrangement.getView().getWidth() > 0) {
            return verticalArrangement.getView().getWidth();
        } else {
            return context.getResources().getDisplayMetrics().widthPixels;
        }
    }

    @SimpleFunction(description = "Returns the screen width in pixels.")
    public int ScreenWidthPx() {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * Loads an image into the given ImageView, optionally cropping it as a circular
     * avatar.
     * Supports local file paths or remote URLs. Applies caching and scales to
     * desired avatar size.
     *
     * @param imageView  The ImageView to display the image in
     * @param imageUrl   The URL or file path of the image
     * @param avatarSize Desired avatar size in dp (e.g., 40 for 40dp x 40dp)
     */
    private void loadImage(final ImageView imageView, final String imageUrl, int avatarSize) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            // Default fallback image if URL is missing
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }

        // Check cached version first
        Bitmap cachedBitmap = imageCache.get(imageUrl + "_" + avatarSize);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        imageExecutor.execute(() -> {
            try {
                Bitmap bitmap = null;

                if (imageUrl.startsWith("http")) {
                    // Load from web
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);
                    connection.connect();

                    // Safer resource handling
                    try (InputStream input = connection.getInputStream()) {
                        bitmap = BitmapFactory.decodeStream(input);
                    } finally {
                        connection.disconnect();
                    }

                } else {
                    // Load from local file
                    File imageFile = new File(imageUrl);
                    if (imageFile.exists()) {
                        bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    }
                }

                // Convert to circular cropped bitmap if valid
                int avatarSizePx = dpToPx(avatarSize);
                final Bitmap finalBitmap = (bitmap != null && !bitmap.isRecycled())
                        ? getCircularBitmap(bitmap, avatarSizePx, false)
                        : null;

                uiHandler.post(() -> {
                    if (finalBitmap != null) {
                        imageCache.put(imageUrl + "_" + avatarSize, finalBitmap);
                        imageView.setImageBitmap(finalBitmap);

                        // Apply dimensions
                        ViewGroup.LayoutParams params = imageView.getLayoutParams();
                        params.width = avatarSizePx;
                        params.height = avatarSizePx;
                        imageView.setLayoutParams(params);
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                });

            } catch (Exception e) {
                uiHandler.post(() -> imageView.setImageResource(android.R.drawable.ic_menu_report_image));
            }
        });
    }

    @Override
    public void loadCircular(ImageView imageView, String url, int sizeDp) {
        loadImage(imageView, url, sizeDp);
    }

    /**
     * Crops a bitmap to a centered square, scales it to target size, and applies
     * circular masking.
     * Optionally adds a red debug stroke around the avatar.
     *
     * @param source      Original bitmap
     * @param sizePx      Desired avatar size in pixels
     * @param debugBorder Whether to draw a red border for debugging purposes
     * @return Circular cropped bitmap
     */

    // public Bitmap getCircularBitmap(Bitmap source, int sizePx) {
    public Bitmap getCircularBitmap(Bitmap source, int sizePx, boolean debugBorder) {
        // Center-crop to square first
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap cropped = Bitmap.createBitmap(source, x, y, size, size);

        // Scale to desired output size (40dp)
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, sizePx, sizePx, true);
        if (scaled != cropped) {
            cropped.recycle();
        }

        // Create circular mask
        Bitmap output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaled, 0, 0, paint);

        if (debugBorder) {
            // Optional red stroke around avatar
            paint.setXfermode(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(2);
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1, paint);
        }

        return output;
    }

    /**
     * Loads an image with placeholder into an ImageView (for message images)
     *
     * @param imageView  The ImageView to load into
     * @param url        URL or path of the image
     * @param maxDpWidth Maximum width in dp
     */
    public void loadImageWithPlaceholder(final ImageView imageView, final String url, final int maxDpWidth) {
        if (url == null || url.isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            imageView.setAlpha(1f);
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
                byte[] imageBytes;
                try (InputStream is = openStream(url);
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int read;
                    while ((read = is.read(chunk)) != -1) {
                        buffer.write(chunk, 0, read);
                    }
                    imageBytes = buffer.toByteArray();
                }

                BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                boundsOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, boundsOptions);

                int widthPx = boundsOptions.outWidth;
                int heightPx = boundsOptions.outHeight;
                float ratio = widthPx > 0 ? (float) heightPx / widthPx : 1f;

                int targetWidthPx = dpToPx(maxDpWidth);
                int targetHeight = Math.max(1, Math.round(targetWidthPx * ratio));

                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                decodeOptions.inSampleSize = calculateSampleSize(widthPx > 0 ? widthPx : targetWidthPx, targetWidthPx);

                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, decodeOptions);

                final Bitmap finalBitmap = bitmap;
                final int finalHeight = targetHeight;

                uiHandler.post(() -> {
                    if (finalBitmap != null) {
                        ViewGroup.LayoutParams params = imageView.getLayoutParams();
                        params.width = targetWidthPx;
                        params.height = finalHeight;
                        imageView.setLayoutParams(params);

                        imageCache.put(url, finalBitmap);
                        imageView.setImageBitmap(finalBitmap);
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

    @Override
    public void loadWithPlaceholder(ImageView imageView, String url, int maxWidthDp) {
        loadImageWithPlaceholder(imageView, url, maxWidthDp);
    }

    /**
     * Calculates sample size for bitmap loading
     *
     * @param originalWidth Original image width
     * @param targetWidth   Target width
     * @return Sample size
     */
    private int calculateSampleSize(int originalWidth, int targetWidth) {
        int sampleSize = 1;
        if (originalWidth > targetWidth) {
            final int halfWidth = originalWidth / 2;
            while ((halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }

    /**
     * Opens an input stream for an image URL or file path
     *
     * @param url URL or path of the image
     * @return InputStream for the image
     * @throws Exception If stream cannot be opened
     */
    private InputStream openStream(String url) throws Exception {
        if (url.startsWith("http")) {
            URL u = new URL(url);
            final HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.connect();
            return new FilterInputStream(conn.getInputStream()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        conn.disconnect();
                    }
                }
            };
        } else {
            File f = new File(url);
            if (!f.exists())
                throw new Exception("File not found: " + url);
            return new FileInputStream(f);
        }
    }

    /**
     * Converts dp to pixels
     *
     * @param dp Value in dp
     * @return Value in pixels
     */
    private int dpToPx(int dp) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return Math.round(dp * dm.density);
    }

    @SimpleFunction(description = "Reset max width to 80% of screen width")
    public void ResetTextMessageMaxWidth() {
        useResponsiveWidth = true;
        textMessageMaxWidth = 0;
        refreshChatConfig();
    }

    // ====================== TYPING INDICATORS ====================== //
    @SimpleFunction(description = "Show typing indicator")
    public void ShowTypingIndicator() {
        showTypingIndicator = true;
        uiHandler.post(() -> {
            if (typingIndicatorPosition >= 0) {
                return;
            }
            MessageModel typingModel = new MessageModel(MessageModel.TYPE_TYPING_INDICATOR,
                    typingIndicatorText.isEmpty() ? "Typing" : typingIndicatorText);
            messageList.add(typingModel);
            typingIndicatorPosition = messageList.size() - 1;
            if (chatAdapter != null) {
                chatAdapter.notifyItemInserted(typingIndicatorPosition);
            }
            scrollToBottom();
            startTypingAnimation();
        });
    }

    @SimpleFunction(description = "Hide typing indicator")
    public void HideTypingIndicator() {
        showTypingIndicator = false;
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        uiHandler.post(() -> {
            if (typingIndicatorPosition < 0) {
                return;
            }
            messageList.remove(typingIndicatorPosition);
            if (chatAdapter != null) {
                chatAdapter.notifyItemRemoved(typingIndicatorPosition);
            }
            typingIndicatorPosition = -1;
            rebuildMessagePositions();
        });
    }

    private void startTypingAnimation() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        typingRunnable = new Runnable() {
            private int dotCount = 0;

            @Override
            public void run() {
                if (!showTypingIndicator || typingIndicatorPosition < 0) {
                    return;
                }
                StringBuilder dots = new StringBuilder(
                        typingIndicatorText.isEmpty() ? "Typing" : typingIndicatorText);
                for (int i = 0; i < dotCount; i++) {
                    dots.append('.');
                }
                if (typingIndicatorPosition < messageList.size()) {
                    messageList.get(typingIndicatorPosition).message = dots.toString();
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemChanged(typingIndicatorPosition);
                    }
                }
                scrollToBottom();
                dotCount = (dotCount + 1) % 4;
                typingHandler.postDelayed(this, 500);
            }
        };
        typingHandler.post(typingRunnable);
    }

    private void handleMessageSelectionToggle(String message, int messageId) {
        int position = findMessagePositionById(messageId);
        if (position < 0) {
            return;
        }
        MessageModel model = messageList.get(position);
        model.isSelected = !model.isSelected;

        if (model.isSelected) {
            if (!selectedMessageIds.contains(messageId)) {
                selectedMessageIds.add(messageId);
            }
            MessageSelected(message, messageId);
        } else {
            selectedMessageIds.remove((Integer) messageId);
            if (selectedMessageIds.isEmpty()) {
                SelectionCleared();
            }
        }

        if (chatAdapter != null) {
            chatAdapter.notifyItemChanged(position);
        }
    }

    private int findMessagePositionById(int messageId) {
        Integer position = messagePositionById.get(messageId);
        if (position != null && position >= 0 && position < messageList.size()
                && messageList.get(position).messageId == messageId) {
            return position;
        }
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
            MessageModel model = messageList.get(i);
            if (model.messageId > 0) {
                messagePositionById.put(model.messageId, i);
                latestDate = extractDateFrom(model.timestamp);
            }
        }
        lastMessageDate = latestDate;
        if (typingIndicatorPosition >= messageList.size()) {
            typingIndicatorPosition = -1;
        } else if (typingIndicatorPosition >= 0
                && messageList.get(typingIndicatorPosition).viewType != MessageModel.TYPE_TYPING_INDICATOR) {
            typingIndicatorPosition = -1;
        }
    }

    private void removeOrphanDateHeaderIfNeeded(int removedPosition) {
        int headerPos = removedPosition - 1;
        if (headerPos < 0 || headerPos >= messageList.size()) {
            return;
        }
        MessageModel candidate = messageList.get(headerPos);
        if (candidate.viewType != MessageModel.TYPE_DATE_HEADER) {
            return;
        }
        boolean nextIsRealMessage = headerPos + 1 < messageList.size()
                && messageList.get(headerPos + 1).messageId > 0;
        if (!nextIsRealMessage) {
            messageList.remove(headerPos);
            if (chatAdapter != null) {
                chatAdapter.notifyItemRemoved(headerPos);
            }
            rebuildMessagePositions();
        }
        if (messageList.isEmpty()) {
            lastMessageDate = "";
        }
    }

    // ====================== MESSAGE MANAGEMENT ====================== //
    @SimpleFunction(description = "Delete message by ID")
    public void DeleteMessageById(int messageId) {
        uiHandler.post(() -> {
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if (messageList.get(i).messageId == messageId) {
                    messageList.remove(i);
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemRemoved(i);
                    }
                    selectedMessageIds.remove((Integer) messageId);
                    messagePositionById.remove(messageId);
                    rebuildMessagePositions();
                    removeOrphanDateHeaderIfNeeded(i);
                    break;
                }
            }
            if (!MessageExists(messageId)) {
                messageTextsById.remove(messageId);
                messageSentFlagsById.remove(messageId);
            }
        });
    }

    @SimpleFunction(description = "Update an existing message by ID")
    public void UpdateMessageById(int messageId, String newMessage) {
        uiHandler.post(() -> {
            for (int i = 0; i < messageList.size(); i++) {
                MessageModel model = messageList.get(i);
                if (model.messageId == messageId) {
                    model.message = newMessage;
                    messageTextsById.put(messageId, newMessage);
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemChanged(i);
                    }
                    MessageUpdated(messageId, newMessage);
                    break;
                }
            }
        });
    }

    @SimpleFunction(description = "Get message text by ID")
    public String GetMessageTextById(int messageId) {
        String message = messageTextsById.get(messageId);
        return message != null ? message : "";
    }

    @SimpleFunction(description = "Scroll to a specific message by ID")
    public void GotoMessageById(int messageId) {
        uiHandler.post(() -> {
            for (int i = 0; i < messageList.size(); i++) {
                if (messageList.get(i).messageId == messageId) {
                    if (recyclerView != null) {
                        recyclerView.smoothScrollToPosition(i);
                    }
                    MessageScrolledTo(messageId);
                    break;
                }
            }
        });
    }

    @SimpleFunction(description = "Check if a message ID exists")
    public boolean MessageExists(int messageId) {
        for (MessageModel model : messageList) {
            if (model.messageId == messageId) {
                return true;
            }
        }
        return false;
    }

    // @SimpleFunction(description = "Get all message IDs currently in chat")
    // public List<Integer> GetAllMessageIds() {
    // List<Integer> ids = new ArrayList<>();
    // for (int i = 0; i < messageViewsById.size(); i++) {
    // ids.add(messageViewsById.keyAt(i));
    // }
    // return ids;
    // }

    @SimpleFunction(description = "Get all message IDs (excluding date headers and system messages)")
    public YailList GetAllMessageIds() {
        List<Integer> allIds = new ArrayList<>();
        for (MessageModel model : messageList) {
            if (model.messageId > 0) {
                allIds.add(model.messageId);
            }
        }
        return YailList.makeList(allIds);
    }

    @SimpleFunction(description = "Get whether a message was sent (true) or received (false) by ID")
    public boolean IsMessageSent(int messageId) {
        Boolean isSent = messageSentFlagsById.get(messageId);
        return isSent != null ? isSent : false;
    }

    @SimpleEvent(description = "Triggered when a message is selected")
    public void MessageSelected(String message, int messageId) {
        EventDispatcher.dispatchEvent(this, "MessageSelected", message, messageId);
    }

    @SimpleEvent(description = "Triggered when a message is updated")
    public void MessageUpdated(int messageId, String newMessage) {
        EventDispatcher.dispatchEvent(this, "MessageUpdated", messageId, newMessage);
    }

    @SimpleEvent(description = "Triggered when scroll to message is completed")
    public void MessageScrolledTo(int messageId) {
        EventDispatcher.dispatchEvent(this, "MessageScrolledTo", messageId);
    }

    @SimpleEvent(description = "Triggered when text menu item is clicked")
    public void TextMenuItemClicked(String itemText, String message, int messageId) {
        EventDispatcher.dispatchEvent(this, "TextMenuItemClicked", itemText, message, messageId);
    }

    @SimpleEvent(description = "Triggered when image menu item is clicked")
    public void ImageMenuItemClicked(String itemText, String imageUrl, int messageId) {
        EventDispatcher.dispatchEvent(this, "ImageMenuItemClicked", itemText, imageUrl, messageId);
    }

    @SimpleFunction(description = "Clean up internal ID tracking (useful after clearing chat)")
    public void CleanupIdTracking() {
        messageTextsById.clear();
        messageSentFlagsById.clear();
        messagePositionById.clear();
        selectedMessageIds.clear();
        nextMessageId = 1; // Reset ID counter
    }

    @SimpleFunction(description = "Clear all messages")
    public void ClearAllMessages() {
        uiHandler.post(() -> {
            int size = messageList.size();
            messageList.clear();
            if (chatAdapter != null && size > 0) {
                chatAdapter.notifyItemRangeRemoved(0, size);
            }
            selectedMessageIds.clear();
            messageTextsById.clear();
            messageSentFlagsById.clear();
            messagePositionById.clear();
            nextMessageId = 1;
            lastMessageDate = "";
            typingIndicatorPosition = -1;
            if (typingRunnable != null) {
                typingHandler.removeCallbacks(typingRunnable);
            }
        });
    }

    @SimpleFunction(description = "Get total message count")
    public int GetMessageCount() {
        int count = 0;
        for (MessageModel model : messageList) {
            if (model.messageId > 0
                    && model.viewType != MessageModel.TYPE_DATE_HEADER
                    && model.viewType != MessageModel.TYPE_SYSTEM
                    && model.viewType != MessageModel.TYPE_TYPING_INDICATOR) {
                count++;
            }
        }
        return count;
    }

    // ====================== SELECTION MANAGEMENT ====================== //
    @SimpleFunction(description = "Clear message selections")
    public void ClearSelection() {
        uiHandler.post(() -> {
            List<Integer> toUpdate = new ArrayList<>(selectedMessageIds);
            selectedMessageIds.clear();
            for (int messageId : toUpdate) {
                int position = findMessagePositionById(messageId);
                if (position >= 0) {
                    messageList.get(position).isSelected = false;
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemChanged(position);
                    }
                }
            }
            SelectionCleared();
        });
    }

    @SimpleFunction(description = "Get number of selected messages")
    public int GetSelectedCount() {
        return selectedMessageIds.size();
    }

    @SimpleFunction(description = "Delete selected messages")
    public void DeleteSelectedMessages() {
        uiHandler.post(() -> {
            List<Integer> toDelete = new ArrayList<>(selectedMessageIds);
            selectedMessageIds.clear();
            for (int messageId : toDelete) {
                DeleteMessageById(messageId);
            }
        });
    }

    // ====================== IMAGE MENU MANAGEMENT ====================== //

    /**
     * Clears all custom image menu items
     * Usage: Call to reset to default menu items
     */
    @SimpleFunction(description = "Clear custom image menu items")
    public void ClearImageMenuItems() {
        customImageMenuItems.clear(); // Empty the custom items list
    }

    /**
     * Clears all custom text menu items
     * Usage: Call to reset to default menu items
     */
    @SimpleFunction(description = "Clear custom text menu items")
    public void ClearTextMenuItems() {
        customTextMenuItems.clear(); // Empty the custom items list
    }

    // ====================== DATE HEADER FUNCTIONALITY ====================== //

    private String lastMessageDate = ""; // Tracks last displayed date
    // private SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd",
    // Locale.getDefault());
    // private SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM d, yyyy",
    // Locale.getDefault());

    /**
     * Resets date tracking for new conversations
     * Usage: Call when starting a new chat session
     */
    @SimpleFunction(description = "Reset date tracking for new conversations")
    public void ResetDateTracking() {
        lastMessageDate = ""; // Clear the tracked date
    }

    /**
     * Extracts just the date portion from a timestamp string
     *
     * @param timestamp Full timestamp string
     * @return Just the date portion (yyyy-MM-dd format)
     */
    private String extractDateFrom(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return formatDate(new Date());
        }
        try {
            if (timestamp.contains(" ")) {
                return timestamp.split(" ")[0];
            }
            if (timestamp.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return timestamp;
            }
            return formatDate(new Date());
        } catch (Exception e) {
            return formatDate(new Date());
        }
    }

    /**
     * Formats a date string into a more readable format
     *
     * @param date Raw date string (yyyy-MM-dd)
     * @return Friendly date string (Today/Yesterday/January 1, 2023)
     */
    private String formatDateReadable(String date) {
        try {
            Date messageDate = parseStorageDate(date);
            Date today = new Date();

            if (isSameDay(messageDate, today)) {
                return "Today"; // Special case for today
            }

            Date yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
            if (isSameDay(messageDate, yesterday)) {
                return "Yesterday"; // Special case for yesterday
            }

            // Default format for other dates
            return formatDisplayDate(messageDate);
        } catch (Exception e) {
            return date; // Return original if parsing fails
        }
    }

    /**
     * Checks if two Date objects represent the same calendar day
     *
     * @param date1 First date to compare
     * @param date2 Second date to compare
     * @return true if both dates are the same day
     */
    private boolean isSameDay(Date date1, Date date2) {
        return formatDateKey(date1).equals(formatDateKey(date2));
    }

    // ====================== UTILITY FUNCTIONS ====================== //

    /**
     * Generates a YouTube thumbnail URL from a video URL or ID
     *
     * @param youTubeURL Full YouTube URL or just video ID
     * @return URL of the thumbnail image
     *         Usage: String thumb =
     *         GetYouTubeThumbnail("https://youtu.be/dQw4w9WgXcQ");
     */
    @SimpleFunction(description = "Get YouTube thumbnail URL from a full URL or video ID.")
    public String GetYouTubeThumbnail(String youTubeURL) {
        String videoId = extractVideoId(youTubeURL);
        if (videoId.isEmpty()) {
            return "Invalid YouTube URL or ID."; // Error message
        }
        // Return medium quality thumbnail URL
        return "https://img.youtube.com/vi/" + videoId + "/" + "mqdefault" + ".jpg";
    }

    /**
     * Extracts the video ID from various YouTube URL formats
     *
     * @param input YouTube URL or video ID
     * @return Extracted video ID or empty string if invalid
     */
    private String extractVideoId(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ""; // Empty input
        }

        // Check if input is already a video ID (11 characters)
        if (!input.contains("http") && input.length() == 11) {
            return input;
        }

        try {
            Uri uri = Uri.parse(input);
            String videoId = uri.getQueryParameter("v"); // Standard ?v=ID format
            if (videoId != null)
                return videoId;

            // Handle youtu.be/ID or /embed/ID formats
            List<String> pathSegments = uri.getPathSegments();
            if (!pathSegments.isEmpty()) {
                return pathSegments.get(pathSegments.size() - 1);
            }
        } catch (Exception e) {
            return ""; // Parsing failed
        }

        return ""; // No ID found
    }

    /**
     * Gets current time in hh:mm a format (e.g. "02:30 PM")
     *
     * @return Formatted time string
     *         Usage: String time = GetCurrentTime();
     */
    @SimpleFunction(description = "Get current timestamp formatted as hh:mm a")
    public String GetCurrentTime() {
        return formatCurrentTime();
    }

    /**
     * Gets current date in dd-MM-yyyy format (e.g. "10-07-2025")
     *
     * @return Formatted date string
     *         Usage: String date = GetCurrentDate();
     */
    @SimpleFunction(description = "Get current date formatted as dd-MM-yyyy")
    public String GetCurrentDate() {
        return formatShortDate(new Date());
    }

    @SimpleFunction(description = "Copy text to clipboard")
    public void CopyToClipboard(String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("ChatMessage", text);
            clipboard.setPrimaryClip(clip);
            ClipboardCopySuccess(text);
        } catch (Exception e) {
            ClipboardCopyFailed(e.getMessage());
        }
    }

    @SimpleEvent(description = "Fires when text is successfully copied to clipboard")
    public void ClipboardCopySuccess(String copiedText) {
        EventDispatcher.dispatchEvent(this, "ClipboardCopySuccess", copiedText);
    }

    @SimpleEvent(description = "Fires when copying to clipboard fails")
    public void ClipboardCopyFailed(String error) {
        EventDispatcher.dispatchEvent(this, "ClipboardCopyFailed", error);
    }

    // Properties with getters and setters

    @SimpleProperty(description = "Get avatar size in DP")
    public int AvatarSize() {
        return avatarSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "40")
    @SimpleProperty(description = "Set avatar size in DP")
    public void AvatarSize(int size) {
        avatarSize = size;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get max width for text messages in DP")
    public int TextMessageMaxWidth() {
        return textMessageMaxWidth;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty(description = "Set custom max width (in dp) for text messages. Set '0' for use max width to 80% of screen width")
    public void TextMessageMaxWidth(int widthDp) {
        textMessageMaxWidth = widthDp;
        if (widthDp == 0) {
            useResponsiveWidth = true;
        }
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get max width for image messages in DP")
    public int ImageMessageMaxWidth() {
        return imageMessageMaxWidth;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty(description = "Set max width (in dp) for image messages. Set '0' for use max width to 80% of screen width")
    public void ImageMessageMaxWidth(int widthDp) {
        imageMessageMaxWidth = widthDp == 0 ? 300 : widthDp;
        if (widthDp == 0) {
            useResponsiveWidth = true;
        }
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get background color for fullscreen image viewer")
    public int FullscreenImageBGColor() {
        return fullscreenImageBGColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0C0C0C")
    @SimpleProperty(description = "Background color for fullscreen image viewer")
    public void FullscreenImageBGColor(int color) {
        fullscreenImageBGColor = color;
    }

    @SimpleProperty(description = "Get background color for avatars")
    public int AvatarBackgroundColor() {
        return avatarBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFDDDDDD")
    @SimpleProperty(description = "Background color for avatars")
    public void AvatarBackgroundColor(int color) {
        avatarBackgroundColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for sent messages")
    public int SentMessageTextColor() {
        return sentMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty(description = "Text color for sent messages")
    public void SentMessageTextColor(int color) {
        sentMessageTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for received messages")
    public int ReceivedMessageTextColor() {
        return receivedMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Text color for received messages")
    public void ReceivedMessageTextColor(int color) {
        receivedMessageTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get background color for sent messages")
    public int SentMessageBackgroundColor() {
        return sentMessageBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0084FF")
    @SimpleProperty(description = "Background color for sent messages")
    public void SentMessageBackgroundColor(int color) {
        sentMessageBackgroundColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get background color for received messages")
    public int ReceivedMessageBackgroundColor() {
        return receivedMessageBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFF0F0F0")
    @SimpleProperty(description = "Background color for received messages")
    public void ReceivedMessageBackgroundColor(int color) {
        receivedMessageBackgroundColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get font size for messages")
    public int MessageFontSize() {
        return messageFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "16")
    @SimpleProperty(description = "Font size for messages")
    public void MessageFontSize(int size) {
        messageFontSize = size;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get font size for messages")
    public int SystemMessageFontSize() {
        return systemMessageFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "14")
    @SimpleProperty(description = "Font size for messages")
    public void SystemMessageFontSize(int size) {
        systemMessageFontSize = size;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for system systemMessages")
    public int SystemMessageTextColor() {
        return systemMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Text color for system systemMessages")
    public void SystemMessageTextColor(int color) {
        systemMessageTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get background color for message selection")
    public int SelectedMessageBgColor() {
        return selectedMessageBgColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H8CF9D3FF")
    @SimpleProperty(description = "Background color for message selection")
    public void SelectedMessageBgColor(int color) {
        selectedMessageBgColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for typing indicator")
    public int TypingIndicatorTextColor() {
        return typingIndicatorTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Text color for typing indicator")
    public void TypingIndicatorTextColor(int color) {
        typingIndicatorTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text size for timestamps")
    public int TimestampFontSize() {
        return timestampFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Text size for timestamps")
    public void TimestampFontSize(int size) {
        timestampFontSize = size;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for timestamps")
    public int TimestampTextColor() {
        return timestampTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Text color for timestamps")
    public void TimestampTextColor(int color) {
        timestampTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for sent status")
    public int SentStatusTextColor() {
        return sentStatusTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0000FF")
    @SimpleProperty(description = "Text color for sent status")
    public void SentStatusTextColor(int color) {
        sentStatusTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for received status")
    public int ReceivedStatusTextColor() {
        return receivedStatusTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFF00FF")
    @SimpleProperty(description = "Text color for received status")
    public void ReceivedStatusTextColor(int color) {
        receivedStatusTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get status text for sent messages")
    public String SentStatusText() {
        return sentStatusText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "✓✓")
    @SimpleProperty(description = "Status text for sent messages (supports emoji)")
    public void SentStatusText(String text) {
        sentStatusText = text;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get status text for received messages")
    public String ReceivedStatusText() {
        return receivedStatusText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "🚀")
    @SimpleProperty(description = "Status text for received messages (supports emoji)")
    public void ReceivedStatusText(String text) {
        receivedStatusText = text;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for sender names in sent messages")
    public int SentNameTextColor() {
        return sentNameTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Text color for sender names in sent messages")
    public void SentNameTextColor(int color) {
        sentNameTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get text color for sender names in received messages")
    public int ReceivedNameTextColor() {
        return receivedNameTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Text color for sender names in received messages")
    public void ReceivedNameTextColor(int color) {
        receivedNameTextColor = color;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get font size for sender names")
    public int NameFontSize() {
        return nameFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Font size for sender names")
    public void NameFontSize(int size) {
        nameFontSize = size;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get corner radius for message bubbles")
    public float MessageCornerRadius() {
        return messageCornerRadius;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "20.0")
    @SimpleProperty(description = "Corner radius for message bubbles")
    public void MessageCornerRadius(float radius) {
        messageCornerRadius = radius;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get horizontal padding inside message bubbles")
    public int MessageHorizontalPadding() {
        return messageHorizontalPadding;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "16")
    @SimpleProperty(description = "Horizontal padding inside message bubbles")
    public void MessageHorizontalPadding(int padding) {
        messageHorizontalPadding = padding;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get vertical padding inside message bubbles")
    public int MessageVerticalPadding() {
        return messageVerticalPadding;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Vertical padding inside message bubbles")
    public void MessageVerticalPadding(int padding) {
        messageVerticalPadding = padding;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get whether timestamps are shown")
    public boolean ShowTimestamp() {
        return showTimestamp;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Show/hide message timestamps")
    public void ShowTimestamp(boolean show) {
        showTimestamp = show;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get whether read status indicators are shown")
    public boolean ShowReadStatus() {
        return showReadStatus;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Show/hide read status indicators")
    public void ShowReadStatus(boolean show) {
        showReadStatus = show;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get the current state of link detection")
    public boolean AutoLinkEnabledInChat() {
        return autoLinkEnabledInChat;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Set whether link detection is enabled or not")
    public void AutoLinkEnabledInChat(boolean enable) {
        autoLinkEnabledInChat = enable;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get whether metadata is shown inside bubble")
    public boolean ShowMetadataInsideBubble() {
        return showMetadataInsideBubble;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Show timestamp/status inside message bubble (true) or outside (false)")
    public void ShowMetadataInsideBubble(boolean value) {
        this.showMetadataInsideBubble = value;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get whether image width is fixed in text-image messages.")
    public boolean ImageWidthFixInTextImageMessage() {
        return imageFunctionWidthFix;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "When true, uses the fixed ImageMessageMaxWidth for combined text+image messages instead of responsive width.")
    public void ImageWidthFixInTextImageMessage(boolean value) {
        imageFunctionWidthFix = value;
        refreshChatConfig();
    }

    // new

    /**
     * Sets the custom font family for messages.
     * Provide the path to the font file (e.g., 'myfont.ttf').
     */

    @SimpleProperty(description = "Get the current custom font family path.")
    public String CustomFontFamily() {
        return customFontFamily;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET, defaultValue = "")
    @SimpleProperty(description = "Sets the custom font family for messages. Provide the path to the font file (e.g., 'myfont.ttf').")
    public void CustomFontFamily(String typefacePath) {
        loadTypeface(typefacePath);
        customFontFamily = typefacePath;
        refreshChatConfig();
    }

    @SimpleProperty(description = "Get typingIndicator Text messages")
    public String TypingIndicatorText() {
        return typingIndicatorText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "Typing")
    @SimpleProperty(description = "Set typingIndicator Text messages")
    public void TypingIndicatorText(String text) {
        typingIndicatorText = text;
        if (typingIndicatorPosition >= 0 && typingIndicatorPosition < messageList.size()) {
            messageList.get(typingIndicatorPosition).message = text;
            if (chatAdapter != null) {
                chatAdapter.notifyItemChanged(typingIndicatorPosition);
            }
        }
    }

    /**
     * Loads the typeface from assets or external path depending on mode
     *
     * @param typefacePath Path to the font file
     */
    private void loadTypeface(String typefacePath) {
        if (typefacePath == null || typefacePath.trim().isEmpty()) {
            Log.w("FontLoader", "Typeface path is empty or null.");
            typeface = Typeface.DEFAULT;
            return;
        }

        try {
            if (isCompanion()) {
                String packageName = form.getPackageName();
                String platform = detectPlatform(packageName);

                String resolvedPath = android.os.Build.VERSION.SDK_INT > 28
                        ? "/storage/emulated/0/Android/data/" + packageName + "/files/assets/" + typefacePath
                        : "/storage/emulated/0/" + platform + "/assets/" + typefacePath;

                File fontFile = new File(resolvedPath);
                if (fontFile.exists()) {
                    typeface = Typeface.createFromFile(fontFile);
                } else {
                    Log.w("FontLoader", "Font file not found: " + resolvedPath);
                    typeface = Typeface.DEFAULT;
                }
            } else {
                typeface = Typeface.createFromAsset(form.$context().getAssets(), typefacePath);
            }
        } catch (Exception e) {
            Log.e("FontLoader", "Error loading typeface from path: " + typefacePath, e);
            typeface = Typeface.DEFAULT;
        }
    }

    /**
     * Checks if running in App Inventor companion
     *
     * @return True if running in companion, false otherwise
     */
    private boolean isCompanion() {
        String packageName = form.getPackageName(); // Standardized usage
        return packageName.contains("makeroid") ||
                packageName.contains("kodular") ||
                packageName.contains("Niotron") ||
                packageName.contains("Appzard") ||
                packageName.contains("appinventor") ||
                packageName.contains("androidbuilder");
    }

    /**
     * Detects the platform name based on package name
     *
     * @param packageName The app's package name
     * @return Platform name string
     */
    private String detectPlatform(String packageName) {
        if (packageName.contains("makeroid"))
            return "Makeroid";
        if (packageName.contains("kodular"))
            return "Kodular";
        if (packageName.contains("Niotron"))
            return "Niotron";
        if (packageName.contains("Appzard"))
            return "Appzard";
        if (packageName.contains("androidbuilder"))
            return "AndroidBuilder";
        return "AppInventor";
    }

    // Properties for square bubble edge
    @SimpleProperty(description = "Get whether square bubble edges are enabled")
    public boolean SquareBubbleEdge() {
        return squareBubbleEdge;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Enable WhatsApp-style square bubble edges")
    public void SquareBubbleEdge(boolean enabled) {
        squareBubbleEdge = enabled;
        refreshChatConfig();
    }

    // @SimpleProperty(description = "Get square edge corner radius")
    // public int SquareEdgeCornerRadius() {
    // return squareEdgeCornerRadius;
    // }

    @SimpleProperty(description = "Get square edge corner radius")
    public float SquareEdgeCornerRadius() {
        return squareEdgeCornerRadius;
    }

    // @DesignerProperty(editorType =
    // PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "8")
    // @SimpleProperty(description = "Corner radius for square edge bubbles")
    // public void SquareEdgeCornerRadius(int radius) {
    // squareEdgeCornerRadius = radius;
    // }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "8.0")
    @SimpleProperty(description = "Corner radius for square edge bubbles")
    public void SquareEdgeCornerRadius(float radius) {
        squareEdgeCornerRadius = radius;
        refreshChatConfig();
    }

    @SimpleFunction(description = "Check if multi-selection mode is active")
    public boolean IsMultiSelectionActive() {
        return !selectedMessageIds.isEmpty();
    }

    // Enhanced clear selection with animation
    // @SimpleFunction(description = "Clear message selections with animation")
    // public void ClearSelectionAnimated() {
    // uiHandler.post(() -> {
    // for (View view : selectedMessages) {
    // view.animate()
    // .alpha(0.7f)
    // .alpha(1f)
    // .setDuration(200)
    // .start();
    // view.setBackgroundColor(Color.TRANSPARENT);
    // }
    // // Reset background color for all selected messages

    //// for (View view : selectedMessages) {
    //// view.setBackgroundColor(Color.TRANSPARENT);
    //// }
    // selectedMessages.clear();
    // SelectionCleared();
    // });
    // }
    @SimpleEvent(description = "Triggered when selection is cleared")
    public void SelectionCleared() {
        EventDispatcher.dispatchEvent(this, "SelectionCleared");
    }

    // Events for feedback
    // @SimpleEvent(description = "Triggered when text menu items are set")
    // public void TextMenuItemsSet(int count) {
    // EventDispatcher.dispatchEvent(this, "TextMenuItemsSet", count);
    // }
    //
    // @SimpleEvent(description = "Triggered when image menu items are set")
    // public void ImageMenuItemsSet(int count) {
    // EventDispatcher.dispatchEvent(this, "ImageMenuItemsSet", count);
    // }

    @SimpleEvent(description = "Triggered when text menu items are added")
    public void TextMenuItemsAdded(int count) {
        EventDispatcher.dispatchEvent(this, "TextMenuItemsAdded", count);
    }

    @SimpleEvent(description = "Triggered when image menu items are added")
    public void ImageMenuItemsAdded(int count) {
        EventDispatcher.dispatchEvent(this, "ImageMenuItemsAdded", count);
    }

    // 7
    // @SimpleFunction(description = "Get current text menu items as list")
    // public List<String> GetTextMenuItems() {
    // return new ArrayList<>(customTextMenuItems);
    // }

    // @SimpleFunction(description = "Get current image menu items as list")
    // public List<String> GetImageMenuItems() {
    // return new ArrayList<>(customImageMenuItems);
    // }

    // 8
    private void showTextOptionsMenu(View anchor, final String message, final int messageId) {
        PopupMenu menu = new PopupMenu(context, anchor);

        // Default items
        final String copy = "Copy";
        final String delete = "Delete";

        menu.getMenu().add(copy);
        menu.getMenu().add(delete);

        // Add custom items from the list
        for (String item : customTextMenuItems) {
            menu.getMenu().add(item);
        }

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            // Handle default items
            if (title.equals(copy)) {
                CopyToClipboard(message);
                return true;
            } else if (title.equals(delete)) {
                DeleteMessageById(messageId);
                return true;
            }

            // Fire event for custom items
            TextMenuItemClicked(title, message, messageId);
            return true;
        });
        menu.show();
    }

    private void showImageOptionsMenu(View anchor, String imageUrl, ImageView imageView, final int messageId) {
        PopupMenu menu = new PopupMenu(context, anchor);

        // Default items
        final String reload = "Reload Image";

        menu.getMenu().add(reload);

        // Custom items from the list
        for (String item : customImageMenuItems) {
            menu.getMenu().add(item);
        }

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();

            if (title.equals(reload)) {
                loadImageWithPlaceholder(imageView, imageUrl, imageMessageMaxWidth);
                return true;
            }

            ImageMenuItemClicked(title, imageUrl, messageId);
            return true;
        });
        menu.show();
    }

    // @SimpleEvent(description = "Triggered when edit message is clicked")
    // public void EditMessageClicked(int messageIndex, String currentMessage) {
    // EventDispatcher.dispatchEvent(this, "EditMessageClicked", messageIndex,
    // currentMessage);
    // }

    // 28/09/25

    @SimpleFunction(description = "Set text menu items from a YailList, replacing any existing items. Use ClearTextMenuItems first if appending is needed.")
    public void AddTextMenuItems(YailList menuItems) {
        customTextMenuItems.clear();
        if (menuItems != null) {
            int addedCount = 0;
            // Convert YailList to Object[] for iteration
            Object[] itemsArray = menuItems.toArray();
            for (Object item : itemsArray) {
                if (item != null) {
                    String itemStr = item.toString().trim();
                    if (!itemStr.isEmpty() && !customTextMenuItems.contains(itemStr)) {
                        customTextMenuItems.add(itemStr);
                        addedCount++;
                    }
                }
            }
            TextMenuItemsAdded(addedCount);
        } else {
            TextMenuItemsAdded(0);
        }
    }

    @SimpleFunction(description = "Set image menu items from a YailList, replacing any existing items. Use ClearImageMenuItems first if appending is needed.")
    public void AddImageMenuItems(YailList menuItems) {
        customImageMenuItems.clear();
        if (menuItems != null) {
            int addedCount = 0;
            Object[] itemsArray = menuItems.toArray();
            for (Object item : itemsArray) {
                if (item != null) {
                    String itemStr = item.toString().trim();
                    if (!itemStr.isEmpty() && !customImageMenuItems.contains(itemStr)) {
                        customImageMenuItems.add(itemStr);
                        addedCount++;
                    }
                }
            }
            ImageMenuItemsAdded(addedCount);
        } else {
            ImageMenuItemsAdded(0);
        }
    }

    @SimpleFunction(description = "Get count of text menu items")
    public int GetTextMenuItemsCount() {
        return customTextMenuItems.size();
    }

    @SimpleFunction(description = "Get count of image menu items")
    public int GetImageMenuItemsCount() {
        return customImageMenuItems.size();
    }

    @SimpleFunction(description = "Get current text menu items as YailList")
    public YailList GetTextMenuItems() {
        // Convert ArrayList to YailList
        return YailList.makeList(customTextMenuItems);
    }

    @SimpleFunction(description = "Get current image menu items as YailList")
    public YailList GetImageMenuItems() {
        // Convert ArrayList to YailList
        return YailList.makeList(customImageMenuItems);
    }

    @SimpleFunction(description = "Get the first (oldest) active message ID in the chat. Returns 0 if no messages.")
    public int GetFirstMessageId() {
        for (MessageModel model : messageList) {
            if (model.messageId > 0) {
                return model.messageId;
            }
        }
        return 0;
    }

    @SimpleFunction(description = "Get the last (newest) active message ID in the chat. Returns 0 if no messages.")
    public int GetLastMessageId() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i).messageId > 0) {
                return messageList.get(i).messageId;
            }
        }
        return 0;
    }

    private String formatCurrentTime() {
        synchronized (SDF_TIME) {
            return SDF_TIME.format(new Date());
        }
    }

    private String formatShortDate(Date date) {
        synchronized (SDF_DATE_SHORT) {
            return SDF_DATE_SHORT.format(date);
        }
    }

    private String formatDate(Date date) {
        synchronized (SDF_DATE) {
            return SDF_DATE.format(date);
        }
    }

    private String formatDisplayDate(Date date) {
        synchronized (SDF_DATE_DISPLAY) {
            return SDF_DATE_DISPLAY.format(date);
        }
    }

    private String formatDateKey(Date date) {
        synchronized (SDF_DATE_KEY) {
            return SDF_DATE_KEY.format(date);
        }
    }

    private Date parseStorageDate(String date) throws java.text.ParseException {
        synchronized (SDF_DATE) {
            return SDF_DATE.parse(date);
        }
    }
}
