package com.prem.chatkaroui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.VerticalArrangement;

import java.io.*;
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

@DesignerComponent(
        version = 1,
        versionName = "1.0",
        description = "ChatKaroUI is a customizable chat component with text, images and messages support. <br>" +
                "Made by: Arun Gupta <br>" +
                "<span><a href=\"https://community.appinventor.mit.edu/\" target=\"_blank\"><small><mark>Mit AI2 Community</mark></small></a></span> | " +
                "<span><a href=\"https://community.kodular.io/\" target=\"_blank\"><small><mark>Kodular Community</mark></small></a></span>",
        nonVisible = true,
        iconName = "icon.png",
        helpUrl = "https://www.telegram.me/Arungupta1526")
public class ChatKaroUI extends AndroidNonvisibleComponent implements Component {

    // UI Components
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private TextView typingIndicatorView;

    // Configuration properties with default values
    private String sentStatusText = "✓✓"; // Default: double check "&#x2714;&#x270C;"
    private String receivedStatusText = "🚀"; // Default: rocket "&#x1F680;"
//    private String fontFamily = "sans-serif";
    private String customFontFamily = "";
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
    private int selectedMessageBgColor = parseColor("#f9d3ff8c");
    private int typingIndicatorTextColor = Color.GRAY;
    private int systemMessageTextColor = Color.GRAY;
    private int fullscreenImageBGColor = parseColor("#0c0c0c");
    private int avatarBackgroundColor = Color.LTGRAY;
    private int timestampFontSize = 12;
    private int avatarSize = 40;
    private int messageMaxWidth = 250;
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
    private int useResponsiveWidthSize = 0;
    private boolean useResponsiveWidth = true;
    private boolean useResponsiveWidthForText = true;
    private final List<String> customMenuItems = new ArrayList<>();

    // Handlers and threading
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final List<View> selectedMessages = new ArrayList<>();
    private Typeface typeface;
    private Runnable typingRunnable;
    private boolean autoLinkEnabledInChat = true;

    // Image caching
    private final LruCache<String, Bitmap> imageCache = new LruCache<>(50);

    private boolean textFunctionWidthFix = false;
    private boolean imageFunctionWidthFix = true;

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
            // Create main chat container
            chatContainer = new LinearLayout(context);
            chatContainer.setOrientation(LinearLayout.VERTICAL);
            chatContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));

            // Create scroll view for chat
            scrollView = new ScrollView(context);
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
//            Initialized();
        } catch (Exception e) {
            // Fire initialization error event
//            InitializeError(e.getMessage());
            Log.e("ChatUI", "Initialization error: " + e.getMessage());
        }
    }

    // Event declarations
//    @SimpleEvent(description = "Triggered when chat initializes successfully")
//    public void Initialized() {
//        EventDispatcher.dispatchEvent(this, "Initialized");
//    }
//
//    @SimpleEvent(description = "Triggered when chat initialization fails with error message")
//    public void InitializeError(String errorMessage) {
//        EventDispatcher.dispatchEvent(this, "InitializeError", errorMessage);
//    }

    /**
     * Custom touch listener for swipe gestures
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {
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
        // addMessageWithDateCheck(timestamp); // <-- Add this first
        maybeAddDateHeader(timestamp); // <-- Add this first
        addSimpleMessageToUI(message, true, timestamp);
    }

    @SimpleFunction(description = "Receive a simple message without avatar or name")
    public void ReceiveSimple(String message, String timestamp) {
        maybeAddDateHeader(timestamp); // <-- Add this first
        addSimpleMessageToUI(message, false, timestamp);
    }

    @SimpleFunction(description = "Send a message with avatar and sender name")
    public void SendWithAvatar(String message, String avatarUrl, String senderName, String timestamp) {
        maybeAddDateHeader(timestamp); // <-- Add this first
        addAvatarMessageToUI(message, avatarUrl, true, senderName, timestamp);
    }

    @SimpleFunction(description = "Receive a message with avatar and sender name")
    public void ReceiveWithAvatar(String message, String avatarUrl, String receiverName, String timestamp) {
        maybeAddDateHeader(timestamp); // <-- Add this first
        addAvatarMessageToUI(message, avatarUrl, false, receiverName, timestamp);
    }

    @SimpleFunction(description = "Send a message with both text and image")
    public void SendTextImage(String message, String imageUrl, String avatarUrl, String senderName, String timestamp,
                              boolean messageOnTop) {
        maybeAddDateHeader(timestamp); // <-- Add this first
        addTextImageMessageToUI(message, imageUrl, avatarUrl, true, senderName, timestamp, messageOnTop);
    }

    @SimpleFunction(description = "Receive a message with both text and image")
    public void ReceiveTextImage(String message, String imageUrl, String avatarUrl, String receiverName,
                                 String timestamp, boolean messageOnTop) {
        maybeAddDateHeader(timestamp); // <-- Add this first
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
                LinearLayout messageLayout = createMessageLayout(isSent); // VERTICAL
                LinearLayout contentLayout = createContentRow(); // HORIZONTAL
                LinearLayout messageContainer = createMessageContainer(isSent); // VERTICAL

                // Create and add message bubble
                boolean fixTextWidth = textFunctionWidthFix;
                TextView messageView = createMessageView(message, isSent, textMessageMaxWidth, fixTextWidth);
                messageContainer.addView(messageView);

                // Add metadata (timestamp, status)
                LinearLayout metaLayout = createMetadataLayout(isSent, timestamp); // HORIZONTAL
                messageContainer.addView(metaLayout);

                contentLayout.addView(messageContainer);
                contentLayout.setGravity(isSent ? Gravity.END : Gravity.START);

                messageLayout.addView(contentLayout);
                chatContainer.addView(messageLayout);

                int index = chatContainer.indexOfChild(messageLayout);
//                bindSwipeListener(messageContainer, message, index + 1);

//                EnableLinkDetection(autoLinkEnabledInChat);

                // Scroll and animate
                scrollToBottom();
                setupLongClickListener(messageLayout, message);
                animateMessageAppearance(messageLayout);

            } catch (Exception e) {
                Log.e("ChatUI", "Error adding simple message: " + e.getMessage());
            }
        });
    }

//    private void bindSwipeListener(View targetView, String message, int index) {
//        targetView.setOnTouchListener(new OnSwipeTouchListener(context) {
//            @Override
//            public void onSwipeLeft() {
//                MessageSwiped(message, index, "left");
//            }
//
//            @Override
//            public void onSwipeRight() {
//                MessageSwiped(message, index, "right");
//            }
//
//            // @Override
//            // public void onSwipeDown() {
//            // Optional: fullscreen image dismiss or RTC drop
//            // }
//
//        });
//    }

//    @SimpleEvent(description = "Triggered when a message is swiped left or right")
//    public void MessageSwiped(String message, int index, String direction) {
//        EventDispatcher.dispatchEvent(this, "MessageSwiped", message, index, direction);
//    }

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
                LinearLayout messageLayout = createMessageLayout(isSent); // VERTICAL

                // Add sender/receiver name if provided
                if (name != null && !name.isEmpty()) {
                    TextView nameView = createNameView(name, isSent);
                    messageLayout.addView(nameView);
                }

                LinearLayout contentLayout = createContentRow(); // HORIZONTAL

                // Create avatar view
                ImageView avatarView = createAvatarView(name, avatarUrl, isSent);
                loadImage(avatarView, avatarUrl, avatarSize);

                LinearLayout messageContainer = createMessageContainer(isSent); // VERTICAL

                // Create message bubble
                boolean fixTextWidth = textFunctionWidthFix;
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
//                EnableLinkDetection(autoLinkEnabledInChat);

                // Finalize
                scrollToBottom();
                setupLongClickListener(messageLayout, message);
                animateMessageAppearance(messageLayout);
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
                LinearLayout messageLayout = createMessageLayout(isSent); // VERTICAL

                // Add sender name
                if (name != null && !name.isEmpty()) {
                    TextView nameView = createNameView(name, isSent);
                    messageLayout.addView(nameView);
                }

                LinearLayout contentLayout = createContentRow(); // HORIZONTAL

                // Create avatar
                ImageView avatarView = createAvatarView(name, avatarUrl, isSent);
                loadImage(avatarView, avatarUrl, avatarSize);

                LinearLayout messageContainer = createMessageContainer(isSent); // VERTICAL
                LinearLayout bubbleWrapper = createMessageContainer(isSent);

                // Create text/image container
                LinearLayout contentContainer = new LinearLayout(context);
                contentContainer.setOrientation(LinearLayout.VERTICAL);
                contentContainer.setBackground(createBubbleDrawable(isSent));
                contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                // Add content based on order preference
                boolean fixImageWidth = imageFunctionWidthFix;
                if (messageOnTop) {
                    if (!message.isEmpty())
                        contentContainer
                                .addView(createMessageView(message, isSent, imageMessageMaxWidth, fixImageWidth));
                    if (!imageUrl.isEmpty())
                        contentContainer.addView(createMessageImageView(imageUrl));
                } else {
                    if (!imageUrl.isEmpty())
                        contentContainer.addView(createMessageImageView(imageUrl));
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
//                EnableLinkDetection(autoLinkEnabledInChat);
                scrollToBottom();
                setupLongClickListener(messageLayout, message);

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
    private ImageView createMessageImageView(String imageUrl) {
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
        imageView.setOnClickListener(v -> showFullscreenImage(imageView.getDrawable()));
        imageView.setOnLongClickListener(v -> {
            showImageOptionsMenu(v, imageUrl, imageView);
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
     * Shows a context menu with options for an image
     *
     * @param anchor    The view to anchor the menu to
     * @param imageUrl  URL or path of the image
     * @param imageView The ImageView being interacted with
     */
    private void showImageOptionsMenu(View anchor, String imageUrl, ImageView imageView) {
        PopupMenu menu = new PopupMenu(context, anchor);

        // Default items
        final String reload = "Reload Image";
        final String save = "Save Image";
        final String share = "Share Image";

        menu.getMenu().add(reload);
        menu.getMenu().add(save);
        menu.getMenu().add(share);

        // Custom items
        for (String item : customMenuItems) {
            menu.getMenu().add(item);
        }

        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            // ImageMenuItemClicked(title, imageUrl);

            boolean handled = false;

            if (title.equals(reload)) {
                loadImageWithPlaceholder(imageView, imageUrl, imageMessageMaxWidth);
                handled = true;
            }

            ImageMenuItemClicked(title, imageUrl);
            return handled;
        });
        menu.show();
    }

    /**
     * Triggered when an image menu item is clicked.
     *
     * @param itemText The menu item that was selected
     * @param imageUrl URL or path of the image related to the selection
     */
    @SimpleEvent(description = "Triggered when image menu item is clicked")
    public void ImageMenuItemClicked(String itemText, String imageUrl) {
        EventDispatcher.dispatchEvent(this, "ImageMenuItemClicked", itemText, imageUrl);
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
        new Thread(() -> {
            String result;
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = new BufferedInputStream(connection.getInputStream());
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();

                if (bitmap != null) {

                    // Target: Android/data/<your.package>/files/chat-images/
                    File externalFilesDir = context.getExternalFilesDir(null); // app-private
                    File sharedDir = new File(externalFilesDir, "chat-images");
                    if (!sharedDir.exists())
                        sharedDir.mkdirs();

                    // only one formate png/jpg
                    // String fileName = "img_" + System.currentTimeMillis() + ".png";
                    // String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                    // File file = new File(sharedDir, fileName);
                    //
                    // FileOutputStream stream = new FileOutputStream(file);
                    // bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    // stream.close();

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
                        ImageAlreadyExists(file.getAbsolutePath(), fileName);
                        return;
                    }

                    FileOutputStream stream = new FileOutputStream(file);
                    bitmap.compress(compressFormat, 100, stream);
                    stream.close();

                    // 🔔 Fire event block
                    ImageSaved(file.getAbsolutePath(), fileName); // 🔔 Success callback
                } else {
                    ImageSaveFailed("Failed to decode image bitmap.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                ImageSaveFailed("Download or save error: " + e.getMessage());
            }
        }).start();
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
                ViewGroup.LayoutParams.WRAP_CONTENT, // width
                ViewGroup.LayoutParams.WRAP_CONTENT // height
        );
        // params.setMargins(8, 0, 8, 0);
        params.setMargins(dpToPx(8), 0, dpToPx(8), 0); // abhi

        if (showMetadataInsideBubble) {
            GradientDrawable bubble = createBubbleDrawable(isSent);
            messageBox.setBackground(bubble);
        }
        messageBox.setLayoutParams(params);

        return messageBox;
    }

    /**
     * Scrolls the chat to the bottom
     */
    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Sets up long click listener for message selection
     *
     * @param messageLayout The message view
     * @param message       The message text
     */
    private void setupLongClickListener(View messageLayout, final String message) {
        messageLayout.setOnLongClickListener(v -> {
            toggleMessageSelection(messageLayout, message);
            return true;
        });
    }

    /**
     * Toggles message selection state
     *
     * @param messageLayout The message view
     * @param message       The message text
     */
    private void toggleMessageSelection(View messageLayout, String message) {
        int index = chatContainer.indexOfChild(messageLayout);
        if (index == -1)
            return;

        if (selectedMessages.contains(messageLayout)) {
            selectedMessages.remove(messageLayout);
            messageLayout.setBackgroundColor(Color.TRANSPARENT);
        } else {
            GradientDrawable highlight = new GradientDrawable();
            highlight.setColor(selectedMessageBgColor);
            highlight.setCornerRadius(messageCornerRadius);
            messageLayout.setBackground(highlight);
            selectedMessages.add(messageLayout);
        }
        MessageSelected(message, index + 1); // 1-based index
    }

    @SimpleEvent(description = "Triggered when profile picture is clicked")
    public void ProfilePictureClicked(String name, String avatarUrl) {
        EventDispatcher.dispatchEvent(this, "ProfilePictureClicked", name, avatarUrl);
    }

    @SimpleEvent(description = "Triggered when message is selected")
    public void MessageSelected(String message, int index) {
        EventDispatcher.dispatchEvent(this, "MessageSelected", message, index);
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
        int screenWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        int defaultMaxPx = (int) (screenWidthPx * 0.8f);

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
//            if (showLinkPreviews) {
//                String url = extractFirstUrl(message);
//                if (url != null) {
//                    generateLinkPreview(url, messageContainer);
//                }
//            }

            // Extract and process first URL
//            String url = extractFirstUrl(message);
//            if (url != null && showLinkPreviews) {
//                generateLinkPreview(url, messageContainer, maxWidthDp); // Now properly passed
//            }
        }

        return messageView;
    }

    /**
     * Creates a bubble drawable for messages
     *
     * @param isSent True if message is sent by user, false if received
     * @return Configured GradientDrawable
     */
    private GradientDrawable createBubbleDrawable(boolean isSent) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(isSent ? sentMessageBackgroundColor : receivedMessageBackgroundColor);
        shape.setCornerRadius(messageCornerRadius);
        return shape;
    }

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
                ViewGroup.LayoutParams.MATCH_PARENT,
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
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
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

                StringBuilder dots = new StringBuilder("Typing");
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
     * Loads an image into an ImageView (for avatars)
     *
     * @param imageView  The ImageView to load into
     * @param imageUrl   URL or path of the image
     * @param avatarSize
     */
    // private void loadImage(final ImageView imageView, final String imageUrl, int
    // avatarSize) {
    // if (imageUrl == null || imageUrl.isEmpty()) {
    // imageView.setImageResource(android.R.drawable.ic_menu_report_image);
    // return;
    // }
    //
    // // Check in-memory cache first
    // Bitmap cachedBitmap = imageCache.get(imageUrl);
    // if (cachedBitmap != null) {
    // imageView.setImageBitmap(cachedBitmap);
    // return;
    // }
    //
    // imageExecutor.execute(() -> {
    // try {
    // Bitmap bitmap = null;
    //
    // if (imageUrl.startsWith("http")) {
    // URL url = new URL(imageUrl);
    // HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    // connection.setDoInput(true);
    // connection.connect();
    // InputStream input = connection.getInputStream();
    // bitmap = BitmapFactory.decodeStream(input);
    // } else {
    // File imageFile = new File(imageUrl);
    // if (imageFile.exists()) {
    // bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    // }
    // }
    //
    //// final Bitmap finalBitmap = bitmap;
    //// int avatarSizePx = dpToPx(40); // constant size
    // int avatarSizePx = dpToPx(avatarSize); // constant size
    //
    // final Bitmap finalBitmap = (bitmap != null && !bitmap.isRecycled())
    //// ? getCircularBitmap(bitmap, avatarSizePx)
    // ? getCircularBitmap(bitmap, avatarSizePx, true) // Enable red border
    // : null;
    // uiHandler.post(() -> {
    // if (finalBitmap != null) {
    // imageCache.put(imageUrl, finalBitmap);
    // imageView.setImageBitmap(finalBitmap);
    //
    // ViewGroup.LayoutParams params = imageView.getLayoutParams();
    // params.width = avatarSizePx;
    // params.height = avatarSizePx;
    // imageView.setLayoutParams(params);
    //
    //
    //// Bitmap circular = getCircularBitmap(finalBitmap, avatarSizePx);
    //// imageCache.put(imageUrl, circular); // cache cropped version
    //// imageView.setImageBitmap(circular);
    //
    // } else {
    // imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    // }
    // });
    // } catch (Exception e) {
    // uiHandler.post(() ->
    // imageView.setImageResource(android.R.drawable.ic_menu_report_image));
    // }
    // });
    // }

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
                    connection.connect();

                    // Safer resource handling
                    try (InputStream input = connection.getInputStream()) {
                        bitmap = BitmapFactory.decodeStream(input);
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
                        // ? getCircularBitmap(bitmap, avatarSizePx)
                        ? getCircularBitmap(bitmap, avatarSizePx, true) // Enable red border
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
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;

                InputStream is = openStream(url);
                BitmapFactory.decodeStream(is, null, opts);
                is.close();

                int widthPx = opts.outWidth;
                int heightPx = opts.outHeight;
                float ratio = (float) heightPx / widthPx;

                int targetWidthPx = dpToPx(maxDpWidth);
                int targetHeight = Math.round(targetWidthPx * ratio);

                opts.inJustDecodeBounds = false;
                opts.inSampleSize = calculateSampleSize(widthPx, targetWidthPx);

                is = openStream(url);
                Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
                is.close();

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
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setDoInput(true);
            conn.connect();
            return conn.getInputStream();
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

    /**
     * Recursively enables/disables link detection in TextViews
     *
     * @param parent The parent ViewGroup to search
     * @param enable True to enable link detection, false to disable
     */
    private void findTextViewsAndSetLinks(ViewGroup parent, boolean enable) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (enable) {
                    // Always linkify web URLs and email addresses
                    Linkify.addLinks(textView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

                    // Check for phone numbers with 10 or more digits
                    String text = textView.getText().toString();
                    Pattern phonePattern = Pattern.compile("\\b\\d{10,}\\b");
                    Matcher matcher = phonePattern.matcher(text);
                    if (matcher.find()) {
                        Linkify.addLinks(textView, phonePattern, "tel:");
                    }

                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    textView.setAutoLinkMask(0);
                    textView.setText(textView.getText().toString());
                    textView.setMovementMethod(null);
                }
            } else if (child instanceof ViewGroup) {
                findTextViewsAndSetLinks((ViewGroup) child, enable);
            }
        }
    }

    /**
     * Enables/disables link detection in all chat messages
     *
     * @param enable True to enable link detection, false to disable
     */
    private void EnableLinkDetection(boolean enable) {
        for (int i = 0; i < chatContainer.getChildCount(); i++) {
            View view = chatContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                findTextViewsAndSetLinks((ViewGroup) view, enable);
            }
        }
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
     * @param index 1-based index of the message to delete
     *              Usage: DeleteMessage(3) - deletes the third message
     */
    @SimpleFunction(description = "Delete message by position (1-based index)")
    public void DeleteMessage(int index) {
        // Check if index is valid (between 1 and total messages)
        if (index > 0 && index <= chatContainer.getChildCount()) {
            View message = chatContainer.getChildAt(index - 1); // Get the message view
            chatContainer.removeViewAt(index - 1); // Remove from UI
            selectedMessages.remove(message); // Remove from selection list
        }
    }

    /**
     * Clears all messages from the chat UI
     * Usage: Call when starting a new conversation
     */
    @SimpleFunction(description = "Clear all messages")
    public void ClearAllMessages() {
        chatContainer.removeAllViews(); // Remove all message views
        selectedMessages.clear(); // Clear selection list
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
        // Reset background color for all selected messages
        for (View view : selectedMessages) {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        selectedMessages.clear(); // Empty the selection list
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
        // Remove each selected view from the container
        for (View view : selectedMessages) {
            chatContainer.removeView(view);
        }
        selectedMessages.clear(); // Clear selection list
    }

    // ====================== IMAGE MENU MANAGEMENT ====================== //

    /**
     * Adds a custom item to the image context menu
     *
     * @param itemText The text to display in the menu
     *                 Usage: AddImageMenuItem("Save to Gallery")
     */
    @SimpleFunction(description = "Add custom item to image menu")
    public void AddImageMenuItem(String itemText) {
        // Add to list if not already present
        if (!customMenuItems.contains(itemText)) {
            customMenuItems.add(itemText);
        }
    }

    /**
     * Clears all custom image menu items
     * Usage: Call to reset to default menu items
     */
    @SimpleFunction(description = "Clear custom image menu items")
    public void ClearImageMenuItems() {
        customMenuItems.clear(); // Empty the custom items list
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
            // Return current date if no timestamp provided
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date());
        }
        try {
            // Split timestamp if it contains both date and time
            if (timestamp.contains(" ")) {
                return timestamp.split(" ")[0]; // Return date portion
            }
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date()); // Fallback to current date
        } catch (Exception e) {
            return ""; // Return empty string on error
        }
    }

    /**
     * Adds a date header view to the chat
     *
     * @param date The date string to display
     */
    // private void addDateHeader(String date) {
    // TextView dateHeader = new TextView(context);
    // dateHeader.setText(formatDateReadable(date)); // Format date nicely
    // dateHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    //// dateHeader.setTextColor(Color.GRAY);
    // dateHeader.setTypeface(null, Typeface.BOLD);
    //// dateHeader.setPadding(0, 16, 0, 8); // Vertical padding
    // dateHeader.setPadding(0, dpToPx(16), 0, dpToPx(8)); // Vertical padding in dp
    //
    // dateHeader.setTextColor(Color.parseColor("#FF6200EE")); // Purple

    //// dateHeader.setBackgroundResource(R.drawable.date_header_bg); // Custom
    //// shape
    // dateHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    //
    // chatContainer.addView(dateHeader); // Add to chat UI
    // }
    private void addDateHeader(String date) {
        TextView dateHeader = new TextView(context);
        dateHeader.setText(formatDateReadable(date));
        dateHeader.setTypeface(null, Typeface.BOLD);
        dateHeader.setPadding(0, dpToPx(16), 0, dpToPx(8));
        dateHeader.setTextColor(Color.parseColor("#FF6200EE"));
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date messageDate = sdf.parse(date);
            Date today = new Date();

            if (isSameDay(messageDate, today)) {
                return "Today"; // Special case for today
            }

            Date yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
            if (isSameDay(messageDate, yesterday)) {
                return "Yesterday"; // Special case for yesterday
            }

            // Default format for other dates
            return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(messageDate);
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
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return fmt.format(date1).equals(fmt.format(date2)); // Compare formatted dates
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
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
    }

    /**
     * Gets current date in dd-MM-yyyy format (e.g. "10-07-2025")
     *
     * @return Formatted date string
     *         Usage: String date = GetCurrentDate();
     */
    @SimpleFunction(description = "Get current date formatted as yyyy-MM-dd")
    public String GetCurrentDate() {
        return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
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
            // imageMessageMaxWidth = useResponsiveWidthSize;
        } else {
            useResponsiveWidth = false;
            useResponsiveWidthForText = true;
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
//        EnableLinkDetection(enable);
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

    @SimpleProperty(description = "Get whether metadata is shown inside bubble")
    public boolean ImageWidthFixInTextImageMessage() {
        return imageFunctionWidthFix;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty(description = "Show timestamp/status inside message bubble (true) or outside (false)")
    public void ImageWidthFixInTextImageMessage(boolean value) {
        imageFunctionWidthFix = value;
    }

    // new

    /**
     * Sets the custom font family for messages.
     * Provide the path to the font file (e.g., 'myfont.ttf').
     */

    @SimpleProperty(description = "Get status text for sent messages")
    public String CustomFontFamily() {
        return customFontFamily;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET, defaultValue = "")
    @SimpleProperty(description = "Sets the custom font family for messages. Provide the path to the font file (e.g., 'myfont.ttf').")
    public void CustomFontFamily(String typefacePath) {
        loadTypeface(typefacePath);
        customFontFamily = typefacePath;
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
        if (packageName.contains("makeroid")) return "Makeroid";
        if (packageName.contains("kodular")) return "Kodular";
        if (packageName.contains("Niotron")) return "Niotron";
        if (packageName.contains("Appzard")) return "Appzard";
        if (packageName.contains("androidbuilder")) return "AndroidBuilder";
        return "AppInventor";
    }

}