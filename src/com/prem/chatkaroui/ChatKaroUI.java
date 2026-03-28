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

import androidx.core.widget.NestedScrollView;

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
public class ChatKaroUI extends AndroidNonvisibleComponent implements Component {

    // Add these with other instance variables
    private int nextMessageId = 1;
    private final SparseArray<View> messageViewsById = new SparseArray<>();
    private final SparseArray<String> messageTextsById = new SparseArray<>();
    private final SparseArray<Boolean> messageSentFlagsById = new SparseArray<>();
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
    private LinearLayout chatContainer;
    private NestedScrollView scrollView;
    private TextView typingIndicatorView;

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
    private boolean showMetadataOutBubble = true;
    private boolean useResponsiveWidth = true;
    private boolean useResponsiveWidthForText = true;
    private final List<String> customImageMenuItems = new ArrayList<>();
    private final List<String> customTextMenuItems = new ArrayList<>();

    // Handlers and threading
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final List<View> selectedMessages = new ArrayList<>();
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

    private boolean textFunctionWidthFix = false;
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
            verticalArrangement = arrangement; // ✅ Store for later use

            // Create main chat container
            chatContainer = new LinearLayout(context);
            chatContainer.setOrientation(LinearLayout.VERTICAL);
            chatContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

            // Create scroll view for chat
            scrollView = new NestedScrollView(context);
            scrollView.addView(chatContainer);

            // Get the FrameLayout from the VerticalArrangement
            FrameLayout frameLayout = (FrameLayout) arrangement.getView();

            // Create a root container to hold both chat and emoji picker
            FrameLayout rootContainer = new FrameLayout(context);
            rootContainer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            // Add chat scroll view to root container
            rootContainer.addView(scrollView);

            // Add root container to the VerticalArrangement
            frameLayout.addView(rootContainer);

            // Fire initialization success event
            // Initialized();
        } catch (Exception e) {
            // Fire initialization error event
            // InitializeError(e.getMessage());
            Log.e("ChatUI", "Initialization error: " + e.getMessage());
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
        addSimpleMessageToUI(message, true, timestamp);
    }

    @SimpleFunction(description = "Receive a simple message without avatar or name")
    public void ReceiveSimple(String message, String timestamp) {
        addSimpleMessageToUI(message, false, timestamp);
    }

    @SimpleFunction(description = "Send a message with avatar and sender name")
    public void SendWithAvatar(String message, String avatarUrl, String senderName, String timestamp) {
        addAvatarMessageToUI(message, avatarUrl, true, senderName, timestamp);
    }

    @SimpleFunction(description = "Receive a message with avatar and sender name")
    public void ReceiveWithAvatar(String message, String avatarUrl, String receiverName, String timestamp) {
        addAvatarMessageToUI(message, avatarUrl, false, receiverName, timestamp);
    }

    @SimpleFunction(description = "Send a message with both text and image")
    public void SendTextImage(String message, String imageUrl, String avatarUrl, String senderName, String timestamp,
            boolean messageOnTop) {
        addTextImageMessageToUI(message, imageUrl, avatarUrl, true, senderName, timestamp, messageOnTop);
    }

    @SimpleFunction(description = "Receive a message with both text and image")
    public void ReceiveTextImage(String message, String imageUrl, String avatarUrl, String receiverName,
            String timestamp, boolean messageOnTop) {
        addTextImageMessageToUI(message, imageUrl, avatarUrl, false, receiverName, timestamp, messageOnTop);
    }

    /**
     * Adds a system systemMessage (like "User joined") to the chat
     *
     * @param message The systemMessage text to display
     */
    @SimpleFunction(description = "Add system systemMessage (e.g., 'User joined')")
    public void AddSystemMessage(String message) {
        uiHandler.post(() -> {
            TextView systemMessageView = new TextView(context);
            systemMessageView.setText(message);
            systemMessageView.setTextColor(systemMessageTextColor);
            systemMessageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, systemMessageFontSize);
            systemMessageView.setGravity(Gravity.CENTER);

            systemMessageView.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

            chatContainer.addView(systemMessageView);
            scrollToBottom();
        });
    }

    /**
     * Adds a simple message (without avatar or name) to the UI
     *
     * @param message   The message text
     * @param isSent    True if message is sent by user, false if received
     * @param timestamp The timestamp to display
     */
    private void addSimpleMessageToUI(String message, boolean isSent, String timestamp) {
        uiHandler.post(() -> {
            try {
                maybeAddDateHeader(timestamp);
                final int messageId = nextMessageId++;

                LinearLayout messageLayout = createMessageLayout(isSent); // VERTICAL

                LinearLayout contentLayout = createContentRow(); // HORIZONTAL
                boolean fixTextWidth = textFunctionWidthFix;
                LinearLayout messageContainer = createMessageContainer(isSent);

                // Create and add message bubble
                TextView messageView = createMessageView(message, isSent, textMessageMaxWidth, fixTextWidth);
                messageContainer.addView(messageView);

                // Add metadata (timestamp, status)
                LinearLayout metaLayout = createMetadataLayout(isSent, timestamp); // HORIZONTAL
                messageContainer.addView(metaLayout);

                contentLayout.addView(messageContainer);
                contentLayout.setGravity(isSent ? Gravity.END : Gravity.START);

                messageLayout.addView(contentLayout);
                chatContainer.addView(messageLayout);

                // Scroll and animate
                scrollToBottom();
                animateMessageAppearance(messageLayout);
                setupLongClickListener(messageLayout, message, messageId, isSent);

                // Store message references by ID
                messageViewsById.put(messageId, messageLayout);
                messageTextsById.put(messageId, message);
                messageSentFlagsById.put(messageId, isSent);

                // Update long click listener to use ID
                // setupLongClickListener(messageLayout, message, messageId, isSent);

            } catch (Exception e) {
                Log.e("ChatUI", "Error adding simple message: " + e.getMessage());
            }
        });
    }

    /**
     * Adds a message with avatar to the UI
     *
     * @param message   The message text
     * @param avatarUrl URL or path to avatar image
     * @param isSent    True if message is sent by user, false if received
     * @param name      The sender/receiver name
     * @param timestamp The timestamp to display
     */
    private void addAvatarMessageToUI(String message, String avatarUrl, boolean isSent, String name, String timestamp) {
        uiHandler.post(() -> {
            try {
                maybeAddDateHeader(timestamp);
                final int messageId = nextMessageId++;

                LinearLayout messageLayout = createMessageLayout(isSent);

                // Add sender/receiver name if provided
                if (name != null && !name.isEmpty()) {
                    TextView nameView = createNameView(name, isSent);
                    messageLayout.addView(nameView);
                }

                LinearLayout contentLayout = createContentRow(); // HORIZONTAL

                // Create avatar view
                ImageView avatarView = createAvatarView(name, avatarUrl, isSent);
                loadImage(avatarView, avatarUrl, avatarSize);

                boolean fixTextWidth = textFunctionWidthFix;
                LinearLayout messageContainer = createMessageContainer(isSent);

                // Create message bubble
                TextView messageView = createMessageView(message, isSent, textMessageMaxWidth, fixTextWidth);
                messageContainer.addView(messageView);

                // Add timestamp and delivery info
                LinearLayout metaLayout = createMetadataLayout(isSent, timestamp);
                messageContainer.addView(metaLayout);

                // Add views in correct order based on sender/receiver side
                if (isSent) {
                    contentLayout.addView(messageContainer);
                    contentLayout.addView(avatarView);
                    contentLayout.setGravity(Gravity.END);
                } else {
                    contentLayout.addView(avatarView);
                    contentLayout.addView(messageContainer);
                    contentLayout.setGravity(Gravity.START);
                }

                // Compose final layout
                messageLayout.addView(contentLayout);
                chatContainer.addView(messageLayout);
                // Finalize
                scrollToBottom();
                animateMessageAppearance(messageLayout);
                setupLongClickListener(messageLayout, message, messageId, isSent);

                // Store references
                messageViewsById.put(messageId, messageLayout);
                messageTextsById.put(messageId, message);
                messageSentFlagsById.put(messageId, isSent);

            } catch (Exception e) {
                Log.e("ChatUI", "Error adding message: " + e.getMessage());
            }
        });
    }

    /**
     * Adds a message with both text and image to the UI
     *
     * @param message      The message text
     * @param imageUrl     URL or path to the image
     * @param avatarUrl    URL or path to avatar image
     * @param isSent       True if message is sent by user, false if received
     * @param name         The sender/receiver name
     * @param timestamp    The timestamp to display
     * @param messageOnTop True to show text above image, false to show image above
     *                     text
     */
    private void addTextImageMessageToUI(String message, String imageUrl, String avatarUrl,
            boolean isSent, String name, String timestamp,
            boolean messageOnTop) {
        uiHandler.post(() -> {
            try {
                maybeAddDateHeader(timestamp);
                final int messageId = nextMessageId++;

                LinearLayout messageLayout = createMessageLayout(isSent);

                // Add sender name
                if (name != null && !name.isEmpty()) {
                    TextView nameView = createNameView(name, isSent);
                    messageLayout.addView(nameView);
                }

                LinearLayout contentLayout = createContentRow(); // HORIZONTAL

                // Create avatar
                ImageView avatarView = createAvatarView(name, avatarUrl, isSent);
                loadImage(avatarView, avatarUrl, avatarSize);

                boolean fixImageWidth = imageFunctionWidthFix;

                LinearLayout bubbleWrapper = createMessageContainer(isSent);

                // Create text/image container
                LinearLayout contentContainer = new LinearLayout(context);
                contentContainer.setOrientation(LinearLayout.VERTICAL);
                contentContainer.setBackground(createBubbleDrawable(isSent));
                contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                // Add content based on order preference
                // boolean fixImageWidth = imageFunctionWidthFix;
                if (messageOnTop) {
                    if (!message.isEmpty())
                        contentContainer
                                .addView(createMessageView(message, isSent, imageMessageMaxWidth, fixImageWidth));
                    if (!imageUrl.isEmpty())
                        contentContainer.addView(createMessageImageView(imageUrl, messageId));
                } else {
                    if (!imageUrl.isEmpty())
                        contentContainer.addView(createMessageImageView(imageUrl, messageId));
                    if (!message.isEmpty())
                        contentContainer
                                .addView(createMessageView(message, isSent, imageMessageMaxWidth, fixImageWidth));
                }

                bubbleWrapper.addView(contentContainer);

                // Add metadata (timestamp + status)
                LinearLayout metaLayout = createMetadataLayout(isSent, timestamp);
                bubbleWrapper.addView(metaLayout);

                // Arrange avatar and bubble
                if (isSent) {
                    contentLayout.addView(bubbleWrapper);
                    contentLayout.addView(avatarView);
                    contentLayout.setGravity(Gravity.END);
                } else {
                    contentLayout.addView(avatarView);
                    contentLayout.addView(bubbleWrapper);
                    contentLayout.setGravity(Gravity.START);
                }

                messageLayout.addView(contentLayout);
                animateMessageAppearance(messageLayout);
                chatContainer.addView(messageLayout);
                scrollToBottom();
                setupLongClickListener(messageLayout, message, messageId, isSent);

                // Store references
                messageViewsById.put(messageId, messageLayout);
                messageTextsById.put(messageId, message);
                messageSentFlagsById.put(messageId, isSent);

            } catch (Exception e) {
                Log.e("ChatUI", "Error adding image message: " + e.getMessage());
            }
        });
    }

    /**
     * Creates an ImageView for a message image
     *
     * @param imageUrl URL or path to the image
     * @return Configured ImageView
     */
    private ImageView createMessageImageView(String imageUrl, final int messageId) {
        ImageView imageView = new ImageView(context);
        // abhi

        imageView.setPadding(
                dpToPx(messageHorizontalPadding),
                dpToPx(messageVerticalPadding),
                dpToPx(messageHorizontalPadding),
                dpToPx(messageVerticalPadding));

        int maxWidthPx = dpToPx(imageMessageMaxWidth);

        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                maxWidthPx,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER; // view inside parent
        imageView.setLayoutParams(params);

        // Maintain aspect ratio
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Load image
        loadImageWithPlaceholder(imageView, imageUrl, imageMessageMaxWidth);

        // Add interactions
        imageView.setOnClickListener(v -> {
            Drawable drawable = imageView.getDrawable();
            if (drawable != null) {
                showFullscreenImage(drawable);
            }
        });
        imageView.setOnLongClickListener(v -> {
            showImageOptionsMenu(v, imageUrl, imageView, messageId);
            return true;
        });

        return imageView;
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

    /**
     * Creates the vertical wrapper layout for a chat message
     *
     * @param isSent True if message is sent by user, false if received
     * @return Configured LinearLayout
     */
    private LinearLayout createMessageLayout(boolean isSent) {
        LinearLayout layout = new LinearLayout(context);
        layout.setTag(isSent ? "sent_message" : "received_message");
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // width
                ViewGroup.LayoutParams.WRAP_CONTENT // height
        ));
        layout.setPadding(0, dpToPx(4), 0, dpToPx(4)); // Vertical padding in dp
        return layout;
    }

    /**
     * Creates a horizontal row for message content
     *
     * @return Configured LinearLayout
     */
    private LinearLayout createContentRow() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    /**
     * Creates a container for message content
     *
     * @param isSent True if message is sent by user, false if received
     * @return Configured LinearLayout
     */
    private LinearLayout createMessageContainer(boolean isSent) {
        LinearLayout messageBox = new LinearLayout(context);
        messageBox.setOrientation(LinearLayout.VERTICAL);
        messageBox.setGravity(isSent ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                // ViewGroup.LayoutParams.MATCH_PARENT, // width // try 02/08/25 for date/time
                // proper show
                ViewGroup.LayoutParams.WRAP_CONTENT, // width // original
                ViewGroup.LayoutParams.WRAP_CONTENT // height
        );
        // params.setMargins(8, 0, 8, 0);
        params.setMargins(dpToPx(8), 0, dpToPx(8), 0); // abhi

        if (showMetadataInsideBubble) {
            GradientDrawable bubble = createBubbleDrawable(isSent);
            messageBox.setBackground(bubble);
        }
        messageBox.setLayoutParams(params);

        // Make the entire container clickable
        // messageBox.setClickable(true);
        // messageBox.setFocusable(true);

        // setupLongClickListener(messageBox, message, messageId, isSent);

        return messageBox;
    }

    /**
     * Scrolls the chat to the bottom
     */
    private void scrollToBottom() {
        scrollView.post(() -> scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN)));
    }

    @SimpleEvent(description = "Triggered when profile picture is clicked")
    public void ProfilePictureClicked(String name, String avatarUrl) {
        EventDispatcher.dispatchEvent(this, "ProfilePictureClicked", name, avatarUrl);
    }

    /**
     * Animates the appearance of a message
     *
     * @param view The message view to animate
     */
    private void animateMessageAppearance(View view) {
        view.setAlpha(0f);
        view.setTranslationY(50);
        view.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(500)
                .start();
    }

    /**
     * Creates a view for sender/receiver name
     *
     * @param name   The name to display
     * @param isSent True if message is sent by user, false if received
     * @return Configured TextView
     */
    private TextView createNameView(String name, boolean isSent) {
        TextView nameView = new TextView(context);
        nameView.setText(name);
        nameView.setTag("name_view");
        nameView.setTextColor(isSent ? sentNameTextColor : receivedNameTextColor);
        nameView.setTypeface(typeface != null ? typeface : Typeface.DEFAULT_BOLD, Typeface.BOLD);
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameFontSize);
        nameView.setPadding(
                dpToPx(isSent ? 0 : avatarSize / 2 + 8),
                dpToPx(0),
                dpToPx(isSent ? avatarSize / 2 + 8 : 0),
                dpToPx(0));
        nameView.setGravity(isSent ? Gravity.END : Gravity.START);
        return nameView;
    }

    /**
     * Creates an avatar view
     *
     * @param name      The name to associate with the avatar
     * @param avatarUrl The avatarUrl to associate with the avatar
     * @param isSent    The isSent to associate with the avatar
     * @return Configured ImageView
     */
    private ImageView createAvatarView(final String name, String avatarUrl, boolean isSent) {
        ImageView avatarView = new ImageView(context);
        int avatarSizePx = dpToPx(avatarSize); // reuse dpToPx from ChatKaroUI

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                avatarSizePx,
                avatarSizePx);
        if (isSent) {
            params.setMargins(0, 0, dpToPx(8), 0);
        } else {
            params.setMargins(dpToPx(8), 0, 0, 0);
        }

        avatarView.setLayoutParams(params);

        // Make avatar circular
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(avatarBackgroundColor);
        avatarView.setBackground(circle);

        // Add click handler
        avatarView.setOnClickListener(v -> ProfilePictureClicked(name, avatarUrl));
        return avatarView;
    }

    /**
     * Creates a view for message text
     *
     * @param message    The message text
     * @param isSent     True if message is sent by user, false if received
     * @param maxWidthDp Maximum width in dp
     * @return Configured TextView
     */
    private TextView createMessageView(String message, boolean isSent, int maxWidthDp, boolean imageWidthIsFix) {
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTag("message_content");
        messageView.setTextColor(isSent ? sentMessageTextColor : receivedMessageTextColor);
        messageView.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, messageFontSize);
        messageView.setPadding(
                dpToPx(messageHorizontalPadding),
                dpToPx(messageVerticalPadding),
                dpToPx(messageHorizontalPadding),
                dpToPx(messageVerticalPadding));

        if (showMetadataOutBubble) {
            GradientDrawable bubble = createBubbleDrawable(isSent);
            messageView.setBackground(bubble);
        }

        // Calculate max width
        // int screenWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        // int defaultMaxPx = (int) (screenWidthPx * 0.8f);

        // add on 03/09/25

        // ✅ Use VerticalArrangement width instead of screen width
        int arrangementWidthPx = 0;
        if (verticalArrangement != null && verticalArrangement.getView().getWidth() > 0) {
            arrangementWidthPx = verticalArrangement.getView().getWidth();
        } else {
            // Fallback if width is not yet measured
            arrangementWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        }

        int defaultMaxPx = (int) (arrangementWidthPx * 0.8f); // Already in px

        int finalMaxPx;
        if (useResponsiveWidth) {
            if (imageWidthIsFix) {
                finalMaxPx = dpToPx(maxWidthDp);
            } else {
                finalMaxPx = defaultMaxPx;
            }
        } else {
            if (useResponsiveWidthForText) {
                finalMaxPx = defaultMaxPx;
            } else
                finalMaxPx = dpToPx(maxWidthDp);
        }

        messageView.setMaxWidth(finalMaxPx);

        // Enable line wrapping + formatting improvements
        messageView.setSingleLine(false);
        messageView.setHorizontallyScrolling(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            messageView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        }

        // Set layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        messageView.setLayoutParams(params);

        // add on 31/08/25
        // Auto-detect URLs
        if (autoLinkEnabledInChat) {
            Linkify.addLinks(messageView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            // Check for phone numbers with 10 or more digits
            String text = messageView.getText().toString();
            Pattern phonePattern = Pattern.compile("\\b\\d{10,}\\b");
            Matcher matcher = phonePattern.matcher(text);
            if (matcher.find()) {
                Linkify.addLinks(messageView, phonePattern, "tel:");
            }
            messageView.setMovementMethod(LinkMovementMethod.getInstance());

            // Extract first URL for preview
            // if (showLinkPreviews) {
            // String url = extractFirstUrl(message);
            // if (url != null) {
            // generateLinkPreview(url, messageContainer);
            // }
            // }

            // Extract and process first URL
            // String url = extractFirstUrl(message);
            // if (url != null && showLinkPreviews) {
            // generateLinkPreview(url, messageContainer, maxWidthDp); // Now properly
            // passed
            // }
        }

        return messageView;
    }

    // @SimpleFunction(description = "Returns the Android API version of the
    // device.")
    // public int GetApiVersion() {
    // return Build.VERSION.SDK_INT;
    // }
    @SimpleFunction(description = "Returns the width in pixels of the VerticalArrangement passed to Initialize, or screen width as fallback.")
    public int ArrangementWidthPx() {
        // Build.VERSION.SDK_INT;
        // ✅ Use VerticalArrangement width instead of screen width
        // int arrangementWidthPx = 0;
        //// int arrangementWidthPx;
        // if (verticalArrangement != null && verticalArrangement.getView().getWidth() >
        // 0) {
        // arrangementWidthPx = verticalArrangement.getView().getWidth();
        // } else {
        // // Fallback if width is not yet measured
        // arrangementWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        // }
        // return arrangementWidthPx;

        // int arrangementWidthPx = 0;
        // int arrangementWidthPx;
        if (verticalArrangement != null && verticalArrangement.getView().getWidth() > 0) {
            return verticalArrangement.getView().getWidth();
        } else {
            // Fallback if width is not yet measured
            return context.getResources().getDisplayMetrics().widthPixels;
        }
    }

    @SimpleFunction(description = "Returns the screen width in pixels.")
    public int ScreenWidthPx() {
        // Build.VERSION.SDK_INT;
        // ✅ Use screen width
        // int screenWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        // return screenWidthPx;
        return context.getResources().getDisplayMetrics().widthPixels;

    }

    /**
     * Creates a bubble drawable for messages
     *
     * @param isSent True if message is sent by user, false if received
     * @return Configured GradientDrawable
     */
    // private GradientDrawable createBubbleDrawable(boolean isSent) {
    // GradientDrawable shape = new GradientDrawable();
    // shape.setColor(isSent ? sentMessageBackgroundColor :
    // receivedMessageBackgroundColor);
    // shape.setCornerRadius(messageCornerRadius);
    // return shape;
    // }

    /**
     * Creates a layout for message metadata (timestamp, status)
     *
     * @param isSent    True if message is sent by user, false if received
     * @param timestamp The timestamp to display
     * @return Configured LinearLayout
     */
    private LinearLayout createMetadataLayout(boolean isSent, String timestamp) {
        LinearLayout metaLayout = new LinearLayout(context);
        metaLayout.setOrientation(LinearLayout.HORIZONTAL);
        metaLayout.setLayoutParams(new LinearLayout.LayoutParams(
                // ViewGroup.LayoutParams.MATCH_PARENT, // 03/09 original
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        metaLayout.setGravity(isSent ? Gravity.END : Gravity.START);

        if (showTimestamp) {
            metaLayout.addView(createTimeView(timestamp));
        }

        if (showReadStatus) {
            metaLayout.addView(createStatusView(isSent));
        }

        return metaLayout;
    }

    /**
     * Creates a view for timestamp
     *
     * @param timestamp The timestamp to display
     * @return Configured TextView
     */
    private TextView createTimeView(String timestamp) {
        TextView timeView = new TextView(context);
        timeView.setText(timestamp.isEmpty() ? getCurrentTime() : timestamp);
        timeView.setTag("timestamp_view");
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, timestampFontSize);
        timeView.setTextColor(timestampTextColor);
        // timeView.setPadding(8, 2, 8, 2);
        timeView.setPadding(dpToPx(8), dpToPx(2), dpToPx(4), dpToPx(2));
        return timeView;
    }

    /**
     * Creates a view for message status
     *
     * @param isSent True if message is sent by user, false if received
     * @return Configured TextView
     */
    private TextView createStatusView(boolean isSent) {
        TextView statusView = new TextView(context);
        statusView.setText(isSent ? sentStatusText : receivedStatusText);
        statusView.setTag("status_view");
        statusView.setTextColor(isSent ? sentStatusTextColor : receivedStatusTextColor);
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        // statusView.setPadding(8, 2, 8, 2);
        statusView.setPadding(dpToPx(4), dpToPx(2), dpToPx(8), dpToPx(2));
        return statusView;
    }

    /**
     * Gets current time in hh:mm a format
     *
     * @return Formatted time string
     */
    private String getCurrentTime() {
        return formatCurrentTime();
    }

    // ===== TYPING INDICATOR IMPLEMENTATION =====
    private void addTypingIndicator() {
        if (typingIndicatorView != null)
            return;

        typingIndicatorView = new TextView(context);
        typingIndicatorView.setTextColor(typingIndicatorTextColor);
        // typingIndicatorView.setPadding(16, 8, 16, 8);
        typingIndicatorView.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        chatContainer.addView(typingIndicatorView);

        startTypingAnimation();
    }

    private void startTypingAnimation() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }

        typingRunnable = new Runnable() {
            private int dotCount = 0;

            @Override
            public void run() {
                if (!showTypingIndicator)
                    return;

                // StringBuilder dots = new StringBuilder("Typing");
                StringBuilder dots = new StringBuilder(!typingIndicatorText.isEmpty() ? typingIndicatorText : "Typing");
                for (int i = 0; i < dotCount; i++)
                    dots.append('.');

                typingIndicatorView.setText(dots);
                scrollToBottom();

                dotCount = (dotCount + 1) % 4;
                typingHandler.postDelayed(this, 500);
            }
        };
        typingHandler.post(typingRunnable);
    }

    private void removeTypingIndicator() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }
        if (typingIndicatorView != null) {
            chatContainer.removeView(typingIndicatorView);
            typingIndicatorView = null;
        }
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
    }

    // ====================== TYPING INDICATORS ====================== //

    /**
     * Shows a typing indicator in the chat UI to indicate someone is typing
     * Usage: Call when you want to show "X is typing..." in the chat
     */
    @SimpleFunction(description = "Show typing indicator")
    public void ShowTypingIndicator() {
        showTypingIndicator = true; // Set flag to true
        addTypingIndicator(); // Add the visual indicator to UI
    }

    /**
     * Hides the typing indicator from the chat UI
     * Usage: Call when typing has stopped or message is sent
     */
    @SimpleFunction(description = "Hide typing indicator")
    public void HideTypingIndicator() {
        showTypingIndicator = false; // Set flag to false
        removeTypingIndicator(); // Remove the visual indicator
    }

    // ====================== MESSAGE MANAGEMENT ====================== //

    /**
     * Deletes a specific message by its position in the chat
     *
     * @param messageId 1-based index of the message to delete
     *                  Usage: DeleteMessage(3) - deletes the third message
     */
    @SimpleFunction(description = "Delete message by ID")
    public void DeleteMessageById(int messageId) {
        uiHandler.post(() -> {
            View messageView = messageViewsById.get(messageId);
            if (messageView != null && messageView.getParent() != null) {
                chatContainer.removeView(messageView);
                messageViewsById.remove(messageId);
                messageTextsById.remove(messageId);
                messageSentFlagsById.remove(messageId);
                selectedMessages.remove(messageView);
            }
        });
    }

    @SimpleFunction(description = "Update an existing message by ID")
    public void UpdateMessageById(int messageId, String newMessage) {
        uiHandler.post(() -> {
            View messageLayout = messageViewsById.get(messageId);
            if (messageLayout != null) {
                TextView messageView = findMessageContentTextView((ViewGroup) messageLayout);
                if (messageView != null) {
                    messageView.setText(newMessage);
                    messageTextsById.put(messageId, newMessage);
                    MessageUpdated(messageId, newMessage);
                }
            }
        });
    }

    // Update the find method to use tags
    private TextView findMessageContentTextView(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if ("message_content".equals(textView.getTag())) {
                    return textView;
                }
            } else if (child instanceof ViewGroup) {
                TextView result = findMessageContentTextView((ViewGroup) child);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    @SimpleFunction(description = "Get message text by ID")
    public String GetMessageTextById(int messageId) {
        String message = messageTextsById.get(messageId);
        return message != null ? message : "";
    }

    @SimpleFunction(description = "Scroll to a specific message by ID")
    public void GotoMessageById(int messageId) {
        View messageView = messageViewsById.get(messageId);
        if (messageView == null) {
            return;
        }
        messageView.post(() -> {
            int[] location = new int[2];
            int[] scrollLocation = new int[2];
            messageView.getLocationOnScreen(location);
            scrollView.getLocationOnScreen(scrollLocation);
            int offset = location[1] - scrollLocation[1] + scrollView.getScrollY();
            scrollView.smoothScrollTo(0, offset);
            MessageScrolledTo(messageId);
        });
    }

    @SimpleFunction(description = "Check if a message ID exists")
    public boolean MessageExists(int messageId) {
        return messageViewsById.get(messageId) != null;
    }

    // @SimpleFunction(description = "Get all message IDs currently in chat")
    // public List<Integer> GetAllMessageIds() {
    // List<Integer> ids = new ArrayList<>();
    // for (int i = 0; i < messageViewsById.size(); i++) {
    // ids.add(messageViewsById.keyAt(i));
    // }
    // return ids;
    // }

    @SimpleFunction(description = "Get all message IDs (including inactive ones)")
    public YailList GetAllMessageIds() {
        List<Integer> allIds = new ArrayList<>();
        if (messageViewsById != null) {
            for (int i = 0; i < messageViewsById.size(); i++) {
                allIds.add(messageViewsById.keyAt(i));
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
        messageViewsById.clear();
        messageTextsById.clear();
        messageSentFlagsById.clear();
        nextMessageId = 1; // Reset ID counter
    }

    /**
     * Clears all messages from the chat UI
     * Usage: Call when starting a new conversation
     */
    @SimpleFunction(description = "Clear all messages")
    public void ClearAllMessages() {
        uiHandler.post(() -> {
            chatContainer.removeAllViews();
            selectedMessages.clear();
            messageViewsById.clear();
            messageTextsById.clear();
            messageSentFlagsById.clear();
            nextMessageId = 1;
            lastMessageDate = "";
        });
    }

    /**
     * Gets the total number of messages currently displayed
     *
     * @return Count of visible messages
     *         Usage: int count = GetMessageCount();
     */
    @SimpleFunction(description = "Get total message count")
    public int GetMessageCount() {
        return chatContainer.getChildCount(); // Return number of child views
    }

    // ====================== SELECTION MANAGEMENT ====================== //

    /**
     * Clears all currently selected messages
     * Usage: Call when canceling a multi-select operation
     */
    @SimpleFunction(description = "Clear message selections")
    public void ClearSelection() {
        uiHandler.post(() -> {
            for (View view : selectedMessages) {
                clearSelectionHighlight(view);
            }
            selectedMessages.clear();
            SelectionCleared();
        });
    }

    /**
     * Gets the number of currently selected messages
     *
     * @return Count of selected messages
     *         Usage: int selected = GetSelectedCount();
     */
    @SimpleFunction(description = "Get number of selected messages")
    public int GetSelectedCount() {
        return selectedMessages.size(); // Return size of selection list
    }

    /**
     * Deletes all currently selected messages
     * Usage: Call after selecting multiple messages to delete
     */
    @SimpleFunction(description = "Delete selected messages")
    public void DeleteSelectedMessages() {
        List<View> toDelete = new ArrayList<>(selectedMessages);
        selectedMessages.clear();
        uiHandler.post(() -> {
            for (View view : toDelete) {
                chatContainer.removeView(view);
                removeMessageTrackingForView(view);
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
     * Helper method to conditionally add date headers between messages
     *
     * @param timestamp The timestamp of the current message
     */
    private void maybeAddDateHeader(String timestamp) {
        if (!showTimestamp)
            return; // Skip if timestamps are disabled

        String currentDate = extractDateFrom(timestamp);
        if (!currentDate.equals(lastMessageDate)) {
            addDateHeader(currentDate); // Add header if date changed
            lastMessageDate = currentDate; // Update tracked date
        }
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
            // Split timestamp if it contains both date and time
            if (timestamp.contains(" ")) {
                return timestamp.split(" ")[0]; // Return date portion
            }
            return formatDate(new Date());
        } catch (Exception e) {
            return ""; // Return empty string on error
        }
    }

    /**
     * Adds a date header view to the chat
     *
     * @param date The date string to display
     */
    private void addDateHeader(String date) {
        TextView dateHeader = new TextView(context);
        dateHeader.setText(formatDateReadable(date));
        dateHeader.setTypeface(null, Typeface.BOLD);
        dateHeader.setPadding(0, dpToPx(16), 0, dpToPx(8));
        dateHeader.setTextColor(Color.parseColor("#FF6200"));
        dateHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        dateHeader.setGravity(Gravity.CENTER); // text inside view

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER; // view inside parent

        dateHeader.setLayoutParams(params);
        chatContainer.addView(dateHeader);
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
        } else {
            useResponsiveWidth = false;
            useResponsiveWidthForText = false;
        }
    }

    @SimpleProperty(description = "Get max width for image messages in DP")
    public int ImageMessageMaxWidth() {
        return imageMessageMaxWidth;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "0")
    @SimpleProperty(description = "Set max width (in dp) for image messages. Set '0' for use max width to 80% of screen width")
    public void ImageMessageMaxWidth(int widthDp) {
        imageMessageMaxWidth = widthDp;
        if (widthDp == 0) {
            useResponsiveWidth = true;
            imageMessageMaxWidth = 300;
        } else {
            useResponsiveWidth = false;
            useResponsiveWidthForText = false; // 03/09
            if (textMessageMaxWidth == 0) {
                float density = context.getResources().getDisplayMetrics().density;
                textMessageMaxWidth = (int) ((ArrangementWidthPx() * 0.8f) / density);
            }
        }
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
    }

    @SimpleProperty(description = "Get text color for sent messages")
    public int SentMessageTextColor() {
        return sentMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFFFFFF")
    @SimpleProperty(description = "Text color for sent messages")
    public void SentMessageTextColor(int color) {
        sentMessageTextColor = color;
    }

    @SimpleProperty(description = "Get text color for received messages")
    public int ReceivedMessageTextColor() {
        return receivedMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Text color for received messages")
    public void ReceivedMessageTextColor(int color) {
        receivedMessageTextColor = color;
    }

    @SimpleProperty(description = "Get background color for sent messages")
    public int SentMessageBackgroundColor() {
        return sentMessageBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0084FF")
    @SimpleProperty(description = "Background color for sent messages")
    public void SentMessageBackgroundColor(int color) {
        sentMessageBackgroundColor = color;
    }

    @SimpleProperty(description = "Get background color for received messages")
    public int ReceivedMessageBackgroundColor() {
        return receivedMessageBackgroundColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFF0F0F0")
    @SimpleProperty(description = "Background color for received messages")
    public void ReceivedMessageBackgroundColor(int color) {
        receivedMessageBackgroundColor = color;
    }

    @SimpleProperty(description = "Get font size for messages")
    public int MessageFontSize() {
        return messageFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "16")
    @SimpleProperty(description = "Font size for messages")
    public void MessageFontSize(int size) {
        messageFontSize = size;
    }

    @SimpleProperty(description = "Get font size for messages")
    public int SystemMessageFontSize() {
        return systemMessageFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "14")
    @SimpleProperty(description = "Font size for messages")
    public void SystemMessageFontSize(int size) {
        systemMessageFontSize = size;
    }

    @SimpleProperty(description = "Get text color for system systemMessages")
    public int SystemMessageTextColor() {
        return systemMessageTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Text color for system systemMessages")
    public void SystemMessageTextColor(int color) {
        systemMessageTextColor = color;
    }

    @SimpleProperty(description = "Get background color for message selection")
    public int SelectedMessageBgColor() {
        return selectedMessageBgColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&H8CF9D3FF")
    @SimpleProperty(description = "Background color for message selection")
    public void SelectedMessageBgColor(int color) {
        selectedMessageBgColor = color;
    }

    @SimpleProperty(description = "Get text color for typing indicator")
    public int TypingIndicatorTextColor() {
        return typingIndicatorTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Text color for typing indicator")
    public void TypingIndicatorTextColor(int color) {
        typingIndicatorTextColor = color;
    }

    @SimpleProperty(description = "Get text size for timestamps")
    public int TimestampFontSize() {
        return timestampFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Text size for timestamps")
    public void TimestampFontSize(int size) {
        timestampFontSize = size;
    }

    @SimpleProperty(description = "Get text color for timestamps")
    public int TimestampTextColor() {
        return timestampTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF888888")
    @SimpleProperty(description = "Text color for timestamps")
    public void TimestampTextColor(int color) {
        timestampTextColor = color;
    }

    @SimpleProperty(description = "Get text color for sent status")
    public int SentStatusTextColor() {
        return sentStatusTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF0000FF")
    @SimpleProperty(description = "Text color for sent status")
    public void SentStatusTextColor(int color) {
        sentStatusTextColor = color;
    }

    @SimpleProperty(description = "Get text color for received status")
    public int ReceivedStatusTextColor() {
        return receivedStatusTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFFFF00FF")
    @SimpleProperty(description = "Text color for received status")
    public void ReceivedStatusTextColor(int color) {
        receivedStatusTextColor = color;
    }

    @SimpleProperty(description = "Get status text for sent messages")
    public String SentStatusText() {
        return sentStatusText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "✓✓")
    @SimpleProperty(description = "Status text for sent messages (supports emoji)")
    public void SentStatusText(String text) {
        sentStatusText = text;
    }

    @SimpleProperty(description = "Get status text for received messages")
    public String ReceivedStatusText() {
        return receivedStatusText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "🚀")
    @SimpleProperty(description = "Status text for received messages (supports emoji)")
    public void ReceivedStatusText(String text) {
        receivedStatusText = text;
    }

    @SimpleProperty(description = "Get text color for sender names in sent messages")
    public int SentNameTextColor() {
        return sentNameTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Text color for sender names in sent messages")
    public void SentNameTextColor(int color) {
        sentNameTextColor = color;
    }

    @SimpleProperty(description = "Get text color for sender names in received messages")
    public int ReceivedNameTextColor() {
        return receivedNameTextColor;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR, defaultValue = "&HFF000000")
    @SimpleProperty(description = "Text color for sender names in received messages")
    public void ReceivedNameTextColor(int color) {
        receivedNameTextColor = color;
    }

    @SimpleProperty(description = "Get font size for sender names")
    public int NameFontSize() {
        return nameFontSize;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Font size for sender names")
    public void NameFontSize(int size) {
        nameFontSize = size;
    }

    @SimpleProperty(description = "Get corner radius for message bubbles")
    public float MessageCornerRadius() {
        return messageCornerRadius;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "20.0")
    @SimpleProperty(description = "Corner radius for message bubbles")
    public void MessageCornerRadius(float radius) {
        messageCornerRadius = radius;
    }

    @SimpleProperty(description = "Get horizontal padding inside message bubbles")
    public int MessageHorizontalPadding() {
        return messageHorizontalPadding;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "16")
    @SimpleProperty(description = "Horizontal padding inside message bubbles")
    public void MessageHorizontalPadding(int padding) {
        messageHorizontalPadding = padding;
    }

    @SimpleProperty(description = "Get vertical padding inside message bubbles")
    public int MessageVerticalPadding() {
        return messageVerticalPadding;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "12")
    @SimpleProperty(description = "Vertical padding inside message bubbles")
    public void MessageVerticalPadding(int padding) {
        messageVerticalPadding = padding;
    }

    @SimpleProperty(description = "Get whether timestamps are shown")
    public boolean ShowTimestamp() {
        return showTimestamp;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Show/hide message timestamps")
    public void ShowTimestamp(boolean show) {
        showTimestamp = show;
    }

    @SimpleProperty(description = "Get whether read status indicators are shown")
    public boolean ShowReadStatus() {
        return showReadStatus;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Show/hide read status indicators")
    public void ShowReadStatus(boolean show) {
        showReadStatus = show;
    }

    @SimpleProperty(description = "Get the current state of link detection")
    public boolean AutoLinkEnabledInChat() {
        return autoLinkEnabledInChat;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Set whether link detection is enabled or not")
    public void AutoLinkEnabledInChat(boolean enable) {
        autoLinkEnabledInChat = enable;
    }

    @SimpleProperty(description = "Get whether metadata is shown inside bubble")
    public boolean ShowMetadataInsideBubble() {
        return showMetadataInsideBubble;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty(description = "Show timestamp/status inside message bubble (true) or outside (false)")
    public void ShowMetadataInsideBubble(boolean value) {
        this.showMetadataInsideBubble = value;
        if (value) {
            showMetadataOutBubble = false;
        } else {
            showMetadataOutBubble = true;
        }
    }

    @SimpleProperty(description = "Get whether image width is fixed in text-image messages.")
    public boolean ImageWidthFixInTextImageMessage() {
        return imageFunctionWidthFix;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "When true, uses the fixed ImageMessageMaxWidth for combined text+image messages instead of responsive width.")
    public void ImageWidthFixInTextImageMessage(boolean value) {
        imageFunctionWidthFix = value;
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
    }

    @SimpleProperty(description = "Get typingIndicator Text messages")
    public String TypingIndicatorText() {
        return typingIndicatorText;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "Typing")
    @SimpleProperty(description = "Set typingIndicator Text messages")
    public void TypingIndicatorText(String text) {
        typingIndicatorText = text;
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

    // 2
    // Add this method to create WhatsApp-style bubbles
    private GradientDrawable createWhatsAppBubbleDrawable(boolean isSent) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(isSent ? sentMessageBackgroundColor : receivedMessageBackgroundColor);

        if (squareBubbleEdge) {
            // WhatsApp-style: smaller radius with one flat edge
            float[] radii = new float[8];
            if (isSent) {
                // Sent message: rounded top-left, bottom-left, bottom-right; flat top-right
                radii[0] = messageCornerRadius; // top-left
                radii[1] = messageCornerRadius;
                radii[2] = squareEdgeCornerRadius; // top-right (smaller)
                radii[3] = squareEdgeCornerRadius;
                radii[4] = messageCornerRadius; // bottom-right
                radii[5] = messageCornerRadius;
                radii[6] = messageCornerRadius; // bottom-left
                radii[7] = messageCornerRadius;
            } else {
                // Received message: rounded top-right, bottom-left, bottom-right; flat top-left
                radii[0] = squareEdgeCornerRadius; // top-left (smaller)
                radii[1] = squareEdgeCornerRadius;
                radii[2] = messageCornerRadius; // top-right
                radii[3] = messageCornerRadius;
                radii[4] = messageCornerRadius; // bottom-right
                radii[5] = messageCornerRadius;
                radii[6] = messageCornerRadius; // bottom-left
                radii[7] = messageCornerRadius;
            }
            shape.setCornerRadii(radii);
        } else {
            // Regular rounded bubbles
            shape.setCornerRadius(messageCornerRadius);
        }

        return shape;
    }

    // Update the createBubbleDrawable method to use the new style
    private GradientDrawable createBubbleDrawable(boolean isSent) {
        if (squareBubbleEdge) {
            return createWhatsAppBubbleDrawable(isSent);
        }

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(isSent ? sentMessageBackgroundColor : receivedMessageBackgroundColor);
        shape.setCornerRadius(messageCornerRadius);
        return shape;

        // return null;
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
    }

    /**
     * Toggles message selection state
     *
     * @param messageLayout The message view
     * @param message       The message text
     */
    private void toggleMessageSelection(View messageLayout, String message, final int messageId, boolean isSent) {
        if (selectedMessages.contains(messageLayout)) {
            selectedMessages.remove(messageLayout);
            clearSelectionHighlight(messageLayout);
        } else {
            GradientDrawable highlight = new GradientDrawable();
            highlight.setColor(selectedMessageBgColor);
            highlight.setCornerRadius(messageCornerRadius);
            messageLayout.setBackground(highlight);
            selectedMessages.add(messageLayout);

            MessageSelected(message, messageId); // Now using ID instead of index
        }
        // MessageSelected(message, index + 1); // 1-based index
        // MessageSelected(message, messageId); // Now using ID instead of index

        // Provide haptic feedback for better UX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            performHapticFeedback(messageLayout);
        }
    }

    private void performHapticFeedback(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    private void clearSelectionHighlight(View view) {
        view.setBackgroundColor(Color.TRANSPARENT);
    }

    // Add method to check if multi-selection is active
    @SimpleFunction(description = "Check if multi-selection mode is active")
    public boolean IsMultiSelectionActive() {
        return !selectedMessages.isEmpty();
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

    private void setupLongClickListener(View messageLayout, final String message, final int messageId, boolean isSent) {
        messageLayout.setOnLongClickListener(v -> {
            toggleMessageSelection(messageLayout, message, messageId, isSent);

            // Show context menu on long press
            showTextOptionsMenu(v, message, messageId);
            return true;
        });

        messageLayout.setOnClickListener(v -> {
            if (!selectedMessages.isEmpty()) {
                toggleMessageSelection(messageLayout, message, messageId, isSent);
            }
        });
    }

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
        try {
            if (messageViewsById == null || messageViewsById.size() == 0) {
                return 0;
            }

            // Ensure we have at least one valid message
            for (int i = 0; i < messageViewsById.size(); i++) {
                int key = messageViewsById.keyAt(i);
                View messageView = messageViewsById.get(key);
                if (messageView != null && messageView.getParent() != null) {
                    return key; // Return first valid message ID
                }
            }
            return 0; // No valid messages found
        } catch (Exception e) {
            Log.e("ChatUI", "Error getting first message ID: " + e.getMessage());
            return 0;
        }
    }

    @SimpleFunction(description = "Get the last (newest) active message ID in the chat. Returns 0 if no messages.")
    public int GetLastMessageId() {
        try {
            if (messageViewsById == null || messageViewsById.size() == 0) {
                return 0;
            }

            // Start from the end and find the last valid message
            for (int i = messageViewsById.size() - 1; i >= 0; i--) {
                int key = messageViewsById.keyAt(i);
                View messageView = messageViewsById.get(key);
                if (messageView != null && messageView.getParent() != null) {
                    return key; // Return last valid message ID
                }
            }
            return 0; // No valid messages found
        } catch (Exception e) {
            Log.e("ChatUI", "Error getting last message ID: " + e.getMessage());
            return 0;
        }
    }

    private void removeMessageTrackingForView(View view) {
        for (int i = messageViewsById.size() - 1; i >= 0; i--) {
            int key = messageViewsById.keyAt(i);
            if (messageViewsById.valueAt(i) == view) {
                messageViewsById.remove(key);
                messageTextsById.remove(key);
                messageSentFlagsById.remove(key);
            }
        }
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
