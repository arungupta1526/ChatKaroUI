package com.prem.chatkaroui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RecyclerView Adapter for ChatKaroUI.
 * Holds MessageModel data list — no View references stored.
 * ViewHolders are recycled automatically by RecyclerView.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<MessageModel> messages;
    private final ChatConfig config; // all styling config from ChatKaroUI
    private final ImageLoader imageLoader; // image loading callback into ChatKaroUI

    // Callbacks into ChatKaroUI for events
    public interface EventCallback {
        void onMessageSelected(String message, int messageId);

        void onTextMenuItemClicked(String item, String message, int messageId);

        void onImageMenuItemClicked(String item, String imageUrl, int messageId);

        void onProfilePictureClicked(String name, String avatarUrl);

        void showTextOptionsMenu(View anchor, String message, int messageId);

        void showImageOptionsMenu(View anchor, String imageUrl, ImageView imageView, int messageId);

        void showFullscreenImage(android.graphics.drawable.Drawable drawable);

        boolean isMultiSelectionActive();
    }

    private EventCallback eventCallback;

    public void setEventCallback(EventCallback cb) {
        this.eventCallback = cb;
    }

    public ChatAdapter(Context context, List<MessageModel> messages,
            ChatConfig config, ImageLoader imageLoader) {
        this.context = context;
        this.messages = messages;
        this.config = config;
        this.imageLoader = imageLoader;
        setHasStableIds(true); // enables efficient animations
    }

    @Override
    public long getItemId(int position) {
        MessageModel m = messages.get(position);
        // For date headers/typing use position-based negative ID
        return m.messageId >= 0 ? m.messageId : -(position + 1L);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case MessageModel.TYPE_DATE_HEADER:
                return new DateHeaderViewHolder(createDateHeaderView());
            case MessageModel.TYPE_SYSTEM:
                return new SystemMessageViewHolder(createSystemMessageView());
            case MessageModel.TYPE_TYPING_INDICATOR:
                return new TypingViewHolder(createTypingView());
            default:
                return new MessageViewHolder(createMessageRootView(viewType));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel model = messages.get(position);
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(model);
        } else if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(model);
        } else if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).bind(model);
        } else if (holder instanceof MessageViewHolder) {
            ((MessageViewHolder) holder).bind(model, eventCallback, imageLoader, config);
        }
    }

    // ── ViewHolder: Date Header ──────────────────────────────────────────────

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        DateHeaderViewHolder(TextView v) {
            super(v);
            textView = v;
        }

        void bind(MessageModel model) {
            textView.setText(model.message);
        }
    }

    // ── ViewHolder: System Message ───────────────────────────────────────────

    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        SystemMessageViewHolder(TextView v) {
            super(v);
            textView = v;
        }

        void bind(MessageModel model) {
            textView.setText(model.message);
        }
    }

    // ── ViewHolder: Typing Indicator ─────────────────────────────────────────

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        TypingViewHolder(TextView v) {
            super(v);
            textView = v;
        }

        void bind(MessageModel model) {
            textView.setText(model.message);
        }
    }

    // ── ViewHolder: Message (all chat message types) ─────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout rootLayout; // entire message row
        LinearLayout contentRow; // horizontal: avatar + bubble
        LinearLayout bubbleContainer; // the message bubble
        TextView messageTextView;
        TextView nameTextView;
        ImageView avatarImageView;
        ImageView messageImageView;
        TextView timestampView;
        TextView statusView;

        MessageViewHolder(LinearLayout root) {
            super(root);
            rootLayout = root;
        }

        void bind(MessageModel model, EventCallback cb, ImageLoader imageLoader, ChatConfig cfg) {
            boolean isSent = (model.viewType == MessageModel.TYPE_SENT_SIMPLE
                    || model.viewType == MessageModel.TYPE_SENT_AVATAR
                    || model.viewType == MessageModel.TYPE_SENT_TEXT_IMAGE);

            // Rebuild view content for this ViewHolder
            rootLayout.removeAllViews();
            rootLayout.setTag(isSent ? "sent_message" : "received_message");

            // Apply selection highlight
            if (model.isSelected) {
                GradientDrawable sel = new GradientDrawable();
                sel.setColor(cfg.selectedMessageBgColor);
                sel.setCornerRadius(cfg.messageCornerRadius);
                rootLayout.setBackground(sel);
            } else {
                rootLayout.setBackground(null);
            }

            Context ctx = rootLayout.getContext();

            // ── Name row (above bubble) ──
            if (model.senderName != null && !model.senderName.isEmpty()) {
                TextView nameView = new TextView(ctx);
                nameView.setText(model.senderName);
                nameView.setTextColor(isSent ? cfg.sentNameTextColor : cfg.receivedNameTextColor);
                nameView.setTypeface(cfg.typeface != null ? cfg.typeface : Typeface.DEFAULT_BOLD, Typeface.BOLD);
                nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.nameFontSize);
                nameView.setGravity(isSent ? Gravity.END : Gravity.START);
                int avatarHalf = dpToPx(ctx, cfg.avatarSize / 2 + 8);
                nameView.setPadding(
                        dpToPx(ctx, isSent ? 0 : cfg.avatarSize / 2 + 8), 0,
                        dpToPx(ctx, isSent ? cfg.avatarSize / 2 + 8 : 0), 0);
                rootLayout.addView(nameView);
                nameTextView = nameView;
            }

            // ── Content row (horizontal: avatar + bubble) ──
            LinearLayout contentRow = new LinearLayout(ctx);
            contentRow.setOrientation(LinearLayout.HORIZONTAL);
            contentRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            // ── Avatar ──
            boolean hasAvatar = (model.viewType == MessageModel.TYPE_SENT_AVATAR
                    || model.viewType == MessageModel.TYPE_RECEIVED_AVATAR
                    || model.viewType == MessageModel.TYPE_SENT_TEXT_IMAGE
                    || model.viewType == MessageModel.TYPE_RECEIVED_TEXT_IMAGE);

            ImageView avatarView = null;
            if (hasAvatar) {
                avatarView = new ImageView(ctx);
                int avatarSizePx = dpToPx(ctx, cfg.avatarSize);
                LinearLayout.LayoutParams avParams = new LinearLayout.LayoutParams(avatarSizePx, avatarSizePx);
                avParams.setMargins(
                        dpToPx(ctx, isSent ? 0 : 8), 0,
                        dpToPx(ctx, isSent ? 8 : 0), 0);
                avatarView.setLayoutParams(avParams);
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(cfg.avatarBackgroundColor);
                avatarView.setBackground(circle);
                final String name = model.senderName;
                final String url = model.avatarUrl;
                avatarView.setOnClickListener(v -> {
                    if (cb != null)
                        cb.onProfilePictureClicked(name, url);
                });
                imageLoader.loadCircular(avatarView, model.avatarUrl, cfg.avatarSize);
                avatarImageView = avatarView;
            }

            // ── Bubble wrapper ──
            LinearLayout bubbleWrapper = new LinearLayout(ctx);
            bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
            bubbleWrapper.setGravity(isSent ? Gravity.END : Gravity.START);
            LinearLayout.LayoutParams bwParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bwParams.setMargins(dpToPx(ctx, 8), 0, dpToPx(ctx, 8), 0);
            bubbleWrapper.setLayoutParams(bwParams);

            // ── Inner content (text + optional image) ──
            LinearLayout innerContent = new LinearLayout(ctx);
            innerContent.setOrientation(LinearLayout.VERTICAL);
            innerContent.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            boolean hasImage = (model.viewType == MessageModel.TYPE_SENT_TEXT_IMAGE
                    || model.viewType == MessageModel.TYPE_RECEIVED_TEXT_IMAGE)
                    && model.imageUrl != null && !model.imageUrl.isEmpty();

            if (hasImage || cfg.showMetadataInsideBubble) {
                // Image messages and inside-bubble metadata need a shared bubble background.
                innerContent.setBackground(cfg.createBubbleDrawable(isSent));
            }

            // ── Text view ──
            if (model.message != null && !model.message.isEmpty()) {
                TextView msgView = new TextView(ctx);
                msgView.setTag("message_content");
                msgView.setText(model.message);
                msgView.setTextColor(isSent ? cfg.sentMessageTextColor : cfg.receivedMessageTextColor);
                msgView.setTypeface(cfg.typeface != null ? cfg.typeface : Typeface.DEFAULT);
                msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.messageFontSize);
                msgView.setPadding(
                        dpToPx(ctx, cfg.messageHorizontalPadding),
                        dpToPx(ctx, cfg.messageVerticalPadding),
                        dpToPx(ctx, cfg.messageHorizontalPadding),
                        dpToPx(ctx, cfg.messageVerticalPadding));

                if (!hasImage && cfg.showMetadataOutBubble) {
                    msgView.setBackground(cfg.createBubbleDrawable(isSent));
                }

                // Max width
                int arrangementWidthPx = cfg.arrangementWidthPx > 0
                        ? cfg.arrangementWidthPx
                        : ctx.getResources().getDisplayMetrics().widthPixels;
                int defaultMaxPx = (int) (arrangementWidthPx * 0.8f);
                int maxWidthDp = hasImage ? cfg.imageMessageMaxWidth : cfg.textMessageMaxWidth;
                int finalMaxPx = (cfg.useResponsiveWidth && maxWidthDp == 0)
                        ? defaultMaxPx
                        : dpToPx(ctx, maxWidthDp);
                msgView.setMaxWidth(finalMaxPx);
                msgView.setSingleLine(false);
                msgView.setHorizontallyScrolling(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    msgView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
                }
                msgView.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                if (cfg.autoLinkEnabledInChat) {
                    Linkify.addLinks(msgView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                    Pattern phonePattern = Pattern.compile("\\b\\d{10,}\\b");
                    Matcher matcher = phonePattern.matcher(model.message);
                    if (matcher.find())
                        Linkify.addLinks(msgView, phonePattern, "tel:");
                    msgView.setMovementMethod(LinkMovementMethod.getInstance());
                }
                messageTextView = msgView;

                if (hasImage) {
                    if (model.messageOnTop)
                        innerContent.addView(msgView);
                } else {
                    innerContent.addView(msgView);
                }
            }

            // ── Image view ──
            if (hasImage) {
                ImageView imgView = new ImageView(ctx);
                imgView.setPadding(
                        dpToPx(ctx, cfg.messageHorizontalPadding),
                        dpToPx(ctx, cfg.messageVerticalPadding),
                        dpToPx(ctx, cfg.messageHorizontalPadding),
                        dpToPx(ctx, cfg.messageVerticalPadding));
                int maxWidthPx = dpToPx(ctx, cfg.imageMessageMaxWidth);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                        maxWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT);
                imgParams.gravity = Gravity.CENTER;
                imgView.setLayoutParams(imgParams);
                imgView.setAdjustViewBounds(true);
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageLoader.loadWithPlaceholder(imgView, model.imageUrl, cfg.imageMessageMaxWidth);

                final String imgUrl = model.imageUrl;
                final int msgId = model.messageId;
                imgView.setOnClickListener(v -> {
                    android.graphics.drawable.Drawable d = imgView.getDrawable();
                    if (d != null && cb != null)
                        cb.showFullscreenImage(d);
                });
                imgView.setOnLongClickListener(v -> {
                    if (cb != null)
                        cb.showImageOptionsMenu(v, imgUrl, imgView, msgId);
                    return true;
                });
                messageImageView = imgView;

                if (!model.messageOnTop && messageTextView != null) {
                    innerContent.addView(imgView);
                    innerContent.addView(messageTextView);
                } else {
                    innerContent.addView(imgView);
                }
            }

            bubbleWrapper.addView(innerContent);

            // ── Metadata (timestamp + status) ──
            LinearLayout metaLayout = new LinearLayout(ctx);
            metaLayout.setOrientation(LinearLayout.HORIZONTAL);
            metaLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            metaLayout.setGravity(isSent ? Gravity.END : Gravity.START);

            if (cfg.showTimestamp && model.timestamp != null && !model.timestamp.isEmpty()) {
                TextView timeView = new TextView(ctx);
                timeView.setText(model.timestamp);
                timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.timestampFontSize);
                timeView.setTextColor(cfg.timestampTextColor);
                timeView.setPadding(dpToPx(ctx, 8), dpToPx(ctx, 2), dpToPx(ctx, 4), dpToPx(ctx, 2));
                metaLayout.addView(timeView);
                timestampView = timeView;
            }

            if (cfg.showReadStatus) {
                TextView statusView = new TextView(ctx);
                statusView.setText(isSent ? cfg.sentStatusText : cfg.receivedStatusText);
                statusView.setTextColor(isSent ? cfg.sentStatusTextColor : cfg.receivedStatusTextColor);
                statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                statusView.setPadding(dpToPx(ctx, 4), dpToPx(ctx, 2), dpToPx(ctx, 8), dpToPx(ctx, 2));
                metaLayout.addView(statusView);
                this.statusView = statusView;
            }

            if (cfg.showMetadataInsideBubble) {
                innerContent.addView(metaLayout);
            } else {
                bubbleWrapper.addView(metaLayout);
            }

            // ── Assemble content row ──
            if (isSent) {
                contentRow.addView(bubbleWrapper);
                if (avatarView != null)
                    contentRow.addView(avatarView);
                contentRow.setGravity(Gravity.END);
            } else {
                if (avatarView != null)
                    contentRow.addView(avatarView);
                contentRow.addView(bubbleWrapper);
                contentRow.setGravity(Gravity.START);
            }

            rootLayout.addView(contentRow);
            bubbleContainer = bubbleWrapper;

            // ── Long click / click listeners ──
            final String msgText = model.message != null ? model.message : "";
            final int msgId = model.messageId;
            innerContent.setOnLongClickListener(v -> {
                if (cb != null && msgId > 0) {
                    cb.onMessageSelected(msgText, msgId);
                    cb.showTextOptionsMenu(v, msgText, msgId);
                }
                return true;
            });
            innerContent.setOnClickListener(v -> {
                if (cb != null && msgId > 0 && cb.isMultiSelectionActive()) {
                    cb.onMessageSelected(msgText, msgId);
                }
            });
        }

        private static int dpToPx(Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }

    // ── Factory methods for non-message views ────────────────────────────────

    private TextView createDateHeaderView() {
        TextView tv = new TextView(context);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dpToPx(16), 0, dpToPx(8));
        tv.setTextColor(android.graphics.Color.parseColor("#FF6200"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        tv.setLayoutParams(params);
        return tv;
    }

    private TextView createSystemMessageView() {
        TextView tv = new TextView(context);
        tv.setTextColor(config.systemMessageTextColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.systemMessageFontSize);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private TextView createTypingView() {
        TextView tv = new TextView(context);
        tv.setTextColor(config.typingIndicatorTextColor);
        tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private LinearLayout createMessageRootView(int viewType) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.setPadding(0, dpToPx(4), 0, dpToPx(4));
        return layout;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
