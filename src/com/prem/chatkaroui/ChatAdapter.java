package com.prem.chatkaroui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableStringBuilder;
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
 * RecyclerView Adapter for ChatKaroUI v3.1.
 *
 * New in v3.1:
 * - Reply-quote bubble above message content
 * - Star (★) indicator in metadata row
 * - "edited" italic label in metadata row
 * - HTML rendering (Html.fromHtml) when cfg.htmlEnabledInChat
 * - Markdown rendering (MarkdownParser) when cfg.markdownEnabledInChat
 * - Link-preview card below message content
 * - Portrait / landscape: max-width recalculated from cfg.arrangementWidthPx
 * at bind-time (not at create-time) so rotation works without notify
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── Event callback interface ─────────────────────────────────────────────

    public interface EventCallback {
        void onMessageSelected(String message, int messageId);

        void onTextMenuItemClicked(String item, String message, int messageId);

        void onImageMenuItemClicked(String item, String imageUrl, int messageId);

        void onProfilePictureClicked(String name, String avatarUrl);

        void showTextOptionsMenu(View anchor, String message, int messageId, boolean isSent, boolean isStarred);

        void showImageOptionsMenu(View anchor, String imageUrl, ImageView imageView, int messageId);

        void showFullscreenImage(android.graphics.drawable.Drawable drawable);

        boolean isMultiSelectionActive();

        /** Called when the user taps the reply-quote strip inside a message. */
        void onReplyQuoteTapped(int replyToId);
    }

    // ────────────────────────────────────────────────────────────────────────

    private final Context context;
    private final List<MessageModel> messages;
    private final ChatConfig config;
    private final ImageLoader imageLoader;
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
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        MessageModel m = messages.get(position);
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

    // ── ViewHolder creation ──────────────────────────────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case MessageModel.TYPE_DATE_HEADER:
                return new DateHeaderVH(createDateHeaderView());
            case MessageModel.TYPE_SYSTEM:
                return new SystemMessageVH(createSystemMessageView());
            case MessageModel.TYPE_TYPING_INDICATOR:
                return new TypingVH(createTypingView());
            default:
                return new MessageVH(createMessageRootView());
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel model = messages.get(position);
        if (holder instanceof DateHeaderVH)
            ((DateHeaderVH) holder).bind(model);
        else if (holder instanceof SystemMessageVH)
            ((SystemMessageVH) holder).bind(model);
        else if (holder instanceof TypingVH)
            ((TypingVH) holder).bind(model);
        else if (holder instanceof MessageVH)
            ((MessageVH) holder).bind(model, eventCallback, imageLoader, config);
    }

    // ── Simple ViewHolders ───────────────────────────────────────────────────

    static class DateHeaderVH extends RecyclerView.ViewHolder {
        final TextView tv;

        DateHeaderVH(TextView v) {
            super(v);
            tv = v;
        }

        void bind(MessageModel m) {
            tv.setText(m.message);
        }
    }

    static class SystemMessageVH extends RecyclerView.ViewHolder {
        final TextView tv;

        SystemMessageVH(TextView v) {
            super(v);
            tv = v;
        }

        void bind(MessageModel m) {
            tv.setText(m.message);
        }
    }

    static class TypingVH extends RecyclerView.ViewHolder {
        final TextView tv;

        TypingVH(TextView v) {
            super(v);
            tv = v;
        }

        void bind(MessageModel m) {
            tv.setText(m.message);
        }
    }

    // ── Message ViewHolder ───────────────────────────────────────────────────


    static class MessageVH extends RecyclerView.ViewHolder {
        final LinearLayout rootLayout;
        boolean isInitialized = false;
        
        TextView nameView;
        ImageView avatarView;
        LinearLayout contentRow;
        LinearLayout bubbleWrapper;
        LinearLayout innerContent;
        
        LinearLayout replyStrip;
        TextView replySenderView;
        TextView replyPreviewView;
        
        TextView msgView;
        ImageView imgView;
        
        LinearLayout previewCard;
        TextView previewSiteView;
        TextView previewTitleView;
        TextView previewDescView;
        
        LinearLayout metaRow;
        TextView editedView;
        TextView timeView;
        TextView starView;
        TextView statusView;

        MessageVH(LinearLayout root) {
            super(root);
            rootLayout = root;
        }

        @SuppressWarnings("deprecation")
        void bind(MessageModel model, EventCallback cb,
                ImageLoader imageLoader, ChatConfig cfg) {

            boolean isSent = model.isSentType();
            Context ctx = rootLayout.getContext();

            if (!isInitialized) {
                initViews(ctx, isSent, cfg, model.viewType);
                isInitialized = true;
            }

            // ── Selection highlight ──────────────────────────────────────────
            if (model.isSelected) {
                android.graphics.drawable.GradientDrawable sel = new android.graphics.drawable.GradientDrawable();
                sel.setColor(cfg.selectedMessageBgColor);
                rootLayout.setBackground(sel);
            } else {
                rootLayout.setBackground(null);
            }

            // ── Sender name row ──────────────────────────────────────────────
            if (nameView != null) {
                if (model.senderName != null && !model.senderName.isEmpty()) {
                    nameView.setVisibility(View.VISIBLE);
                    nameView.setText(model.senderName);
                    nameView.setTextColor(isSent ? cfg.sentNameTextColor : cfg.receivedNameTextColor);
                } else {
                    nameView.setVisibility(View.GONE);
                }
            }

            // ── Avatar ───────────────────────────────────────────────────────
            if (avatarView != null) {
                final String name = model.senderName;
                final String url = model.avatarUrl;
                avatarView.setOnClickListener(v -> {
                    if (cb != null) cb.onProfilePictureClicked(name, url);
                });
                imageLoader.loadCircular(avatarView, model.avatarUrl, cfg.avatarSize);
            }

            // ── Bubble background ─────────────────────────────────────────────
            boolean hasImage = (model.viewType == MessageModel.TYPE_SENT_TEXT_IMAGE
                    || model.viewType == MessageModel.TYPE_RECEIVED_TEXT_IMAGE)
                    && model.imageUrl != null && !model.imageUrl.isEmpty();

            if (hasImage || cfg.showMetadataInsideBubble) {
                innerContent.setBackground(cfg.createBubbleDrawable(isSent));
            } else {
                innerContent.setBackground(null);
            }

            // ── 1. Reply quote strip ─────────────────────────────────────────
            if (model.hasReply()) {
                replyStrip.setVisibility(View.VISIBLE);
                replySenderView.setText(model.replyToSender != null
                        ? model.replyToSender
                        : (model.replyToIsSent ? "You" : ""));
                String preview = model.replyToText != null ? model.replyToText : "";
                if (preview.length() > 80) preview = preview.substring(0, 80) + "…";
                replyPreviewView.setText(preview);
                replyPreviewView.setTextColor(cfg.replyPreviewTextColor);
                
                final int replyId = model.replyToId;
                replyStrip.setOnClickListener(v -> {
                    if (cb != null) cb.onReplyQuoteTapped(replyId);
                });
            } else {
                replyStrip.setVisibility(View.GONE);
            }

            // ── 2. Text view ─────────────────────────────────────────────────
            if (model.message != null && !model.message.isEmpty()) {
                msgView.setVisibility(View.VISIBLE);
                msgView.setTextColor(isSent ? cfg.sentMessageTextColor : cfg.receivedMessageTextColor);
                
                // Recalculate max width for text
                int arrangementW = cfg.arrangementWidthPx > 0
                        ? cfg.arrangementWidthPx
                        : ctx.getResources().getDisplayMetrics().widthPixels;
                int defaultMaxPx = (int) (arrangementW * 0.8f);
                int maxWidthDp = hasImage ? cfg.imageMessageMaxWidth : cfg.textMessageMaxWidth;
                int finalMaxPx = (cfg.useResponsiveWidth && maxWidthDp == 0)
                        ? defaultMaxPx
                        : dpToPx(ctx, maxWidthDp);
                msgView.setMaxWidth(finalMaxPx);

                if (cfg.markdownEnabledInChat) {
                    msgView.setText(MarkdownParser.parse(model.message));
                    msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                } else if (cfg.htmlEnabledInChat) {
                    CharSequence html;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        html = android.text.Html.fromHtml(model.message, android.text.Html.FROM_HTML_MODE_COMPACT);
                    } else {
                        html = android.text.Html.fromHtml(model.message);
                    }
                    msgView.setText(html);
                    msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                } else {
                    msgView.setText(model.message);
                    if (cfg.autoLinkEnabledInChat) {
                        android.text.util.Linkify.addLinks(msgView, android.text.util.Linkify.WEB_URLS | android.text.util.Linkify.EMAIL_ADDRESSES);
                        java.util.regex.Pattern phone = java.util.regex.Pattern.compile("\\b\\d{10,}\\b");
                        java.util.regex.Matcher mat = phone.matcher(model.message);
                        if (mat.find()) android.text.util.Linkify.addLinks(msgView, phone, "tel:");
                        msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                    }
                }
                
                if (!hasImage && cfg.showMetadataOutBubble) {
                    msgView.setBackground(cfg.createBubbleDrawable(isSent));
                } else {
                    msgView.setBackground(null);
                }
            } else {
                msgView.setVisibility(View.GONE);
            }

            // ── 3. Image view ────────────────────────────────────────────────
            if (hasImage) {
                imgView.setVisibility(View.VISIBLE);
                imageLoader.loadWithPlaceholder(imgView, model.imageUrl, cfg.imageMessageMaxWidth);

                final String imgUrl = model.imageUrl;
                final int msgId = model.messageId;
                imgView.setOnClickListener(v -> {
                    android.graphics.drawable.Drawable d = imgView.getDrawable();
                    if (d != null && cb != null) cb.showFullscreenImage(d);
                });
                imgView.setOnLongClickListener(v -> {
                    if (cb != null) cb.showImageOptionsMenu(v, imgUrl, imgView, msgId);
                    return true;
                });
            } else {
                imgView.setVisibility(View.GONE);
            }

            // Ordering image and text dynamically based on model.messageOnTop
            if (msgView.getVisibility() == View.VISIBLE && imgView.getVisibility() == View.VISIBLE) {
                innerContent.removeView(msgView);
                innerContent.removeView(imgView);
                if (model.messageOnTop) {
                    innerContent.addView(msgView);
                    innerContent.addView(imgView);
                } else {
                    innerContent.addView(imgView);
                    innerContent.addView(msgView);
                }
            }

            // ── 4. Link preview card ─────────────────────────────────────────
            if (cfg.linkPreviewEnabled && model.hasLinkPreview()) {
                previewCard.setVisibility(View.VISIBLE);
                if (model.previewSiteName != null && !model.previewSiteName.isEmpty()) {
                    previewSiteView.setVisibility(View.VISIBLE);
                    previewSiteView.setText(model.previewSiteName);
                } else {
                    previewSiteView.setVisibility(View.GONE);
                }
                if (model.previewTitle != null && !model.previewTitle.isEmpty()) {
                    previewTitleView.setVisibility(View.VISIBLE);
                    previewTitleView.setText(model.previewTitle);
                } else {
                    previewTitleView.setVisibility(View.GONE);
                }
                if (model.previewDescription != null && !model.previewDescription.isEmpty()) {
                    previewDescView.setVisibility(View.VISIBLE);
                    previewDescView.setText(model.previewDescription);
                } else {
                    previewDescView.setVisibility(View.GONE);
                }
            } else {
                previewCard.setVisibility(View.GONE);
            }

            // ── 5. Metadata row ──────────────────────────────────────────────
            if (cfg.showEditedLabel && model.isEdited) {
                editedView.setVisibility(View.VISIBLE);
                editedView.setText(cfg.editedLabelText != null ? cfg.editedLabelText : "edited");
            } else {
                editedView.setVisibility(View.GONE);
            }

            if (cfg.showTimestamp && model.timestamp != null && !model.timestamp.isEmpty()) {
                timeView.setVisibility(View.VISIBLE);
                timeView.setText(model.timestamp);
                timeView.setTextColor(cfg.timestampTextColor);
            } else {
                timeView.setVisibility(View.GONE);
            }

            if (model.isStarred) {
                starView.setVisibility(View.VISIBLE);
                starView.setText(cfg.starredIndicatorText != null ? cfg.starredIndicatorText : "★");
                starView.setTextColor(cfg.starredIndicatorColor);
            } else {
                starView.setVisibility(View.GONE);
            }

            if (cfg.showReadStatus) {
                statusView.setVisibility(View.VISIBLE);
                statusView.setText(isSent ? cfg.sentStatusText : cfg.receivedStatusText);
                statusView.setTextColor(isSent ? cfg.sentStatusTextColor : cfg.receivedStatusTextColor);
            } else {
                statusView.setVisibility(View.GONE);
            }

            // Check where metadata row resides
            if (metaRow.getParent() != null) {
                ((ViewGroup) metaRow.getParent()).removeView(metaRow);
            }
            if (cfg.showMetadataInsideBubble) {
                innerContent.addView(metaRow);
            } else {
                bubbleWrapper.addView(metaRow);
            }

            // ── Long-click / click listeners ─────────────────────────────────
            final String msgText = model.message != null ? model.message : "";
            final int msgId = model.messageId;
            final boolean starred = model.isStarred;

            View.OnLongClickListener longClickListener = v -> {
                if (cb != null && msgId > 0) {
                    if (cb.isMultiSelectionActive()) {
                        cb.onMessageSelected(msgText, msgId);
                    } else {
                        cb.onMessageSelected(msgText, msgId);
                        v.post(() -> cb.showTextOptionsMenu(innerContent, msgText, msgId, isSent, starred));
                    }
                }
                return true;
            };

            View.OnClickListener clickListener = v -> {
                if (cb != null && msgId > 0 && cb.isMultiSelectionActive())
                    cb.onMessageSelected(msgText, msgId);
            };

            innerContent.setOnLongClickListener(longClickListener);
            innerContent.setOnClickListener(clickListener);
            bubbleWrapper.setOnLongClickListener(longClickListener);
            bubbleWrapper.setOnClickListener(clickListener);
            msgView.setOnLongClickListener(longClickListener);
            msgView.setOnClickListener(clickListener);
            replyStrip.setOnLongClickListener(longClickListener);
            
            replyStrip.setOnClickListener(v -> {
                if (cb != null && msgId > 0 && cb.isMultiSelectionActive()) {
                    cb.onMessageSelected(msgText, msgId);
                } else if (cb != null && model.replyToId > 0) {
                    cb.onReplyQuoteTapped(model.replyToId);
                }
            });

            rootLayout.setOnLongClickListener(longClickListener);
            rootLayout.setOnClickListener(clickListener);
        }

        private void initViews(Context ctx, boolean isSent, ChatConfig cfg, int viewType) {
            // Sender name setup
            nameView = new TextView(ctx);
            nameView.setTypeface(cfg.typeface != null ? cfg.typeface : Typeface.DEFAULT, Typeface.BOLD);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.nameFontSize);
            nameView.setGravity(isSent ? Gravity.END : Gravity.START);
            nameView.setPadding(dpToPx(ctx, isSent ? 0 : cfg.avatarSize / 2 + 8), 0, dpToPx(ctx, isSent ? cfg.avatarSize / 2 + 8 : 0), 0);
            rootLayout.addView(nameView);

            // Content row & Avatar
            contentRow = new LinearLayout(ctx);
            contentRow.setOrientation(LinearLayout.HORIZONTAL);
            contentRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            boolean hasAvatar = viewType == MessageModel.TYPE_SENT_AVATAR
                    || viewType == MessageModel.TYPE_RECEIVED_AVATAR
                    || viewType == MessageModel.TYPE_SENT_TEXT_IMAGE
                    || viewType == MessageModel.TYPE_RECEIVED_TEXT_IMAGE;

            if (hasAvatar) {
                avatarView = new ImageView(ctx);
                int avatarPx = dpToPx(ctx, cfg.avatarSize);
                LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(avatarPx, avatarPx);
                avp.setMargins(dpToPx(ctx, isSent ? 0 : 8), 0, dpToPx(ctx, isSent ? 8 : 0), 0);
                avatarView.setLayoutParams(avp);
                android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circle.setColor(cfg.avatarBackgroundColor);
                avatarView.setBackground(circle);
            }

            // Bubble wrapper
            bubbleWrapper = new LinearLayout(ctx);
            bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
            bubbleWrapper.setGravity(isSent ? Gravity.END : Gravity.START);
            LinearLayout.LayoutParams bwp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bwp.setMargins(dpToPx(ctx, 8), 0, dpToPx(ctx, 8), 0);
            bubbleWrapper.setLayoutParams(bwp);

            innerContent = new LinearLayout(ctx);
            innerContent.setOrientation(LinearLayout.VERTICAL);
            innerContent.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            int arrangementW = cfg.arrangementWidthPx > 0 ? cfg.arrangementWidthPx : ctx.getResources().getDisplayMetrics().widthPixels;
            int defaultMaxPx = (int) (arrangementW * 0.8f);
            int maxWidthDp = cfg.textMessageMaxWidth;
            int pmaxPx = (cfg.useResponsiveWidth && maxWidthDp == 0) ? defaultMaxPx : dpToPx(ctx, maxWidthDp);

            // Reply Strip
            replyStrip = new LinearLayout(ctx);
            replyStrip.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sp.setMargins(dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, 6), dpToPx(ctx, cfg.messageHorizontalPadding), 0);
            replyStrip.setLayoutParams(sp);
            replyStrip.setBackground(cfg.createReplyDrawable(isSent));
            replyStrip.setPadding(0, dpToPx(ctx, 4), dpToPx(ctx, 8), dpToPx(ctx, 4));
            
            View accent = new View(ctx);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dpToPx(ctx, 3), ViewGroup.LayoutParams.MATCH_PARENT);
            ap.setMargins(0, 0, dpToPx(ctx, 8), 0);
            accent.setLayoutParams(ap);
            accent.setBackgroundColor(cfg.replyAccentColor);
            replyStrip.addView(accent);

            LinearLayout textCol = new LinearLayout(ctx);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            replySenderView = new TextView(ctx);
            replySenderView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.nameFontSize);
            replySenderView.setTypeface(null, Typeface.BOLD);
            replySenderView.setTextColor(cfg.replyAccentColor);
            replySenderView.setMaxWidth(pmaxPx - dpToPx(ctx, 30));
            replySenderView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            replySenderView.setSingleLine(true);
            textCol.addView(replySenderView);
            
            replyPreviewView = new TextView(ctx);
            replyPreviewView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.messageFontSize - 2);
            replyPreviewView.setMaxLines(2);
            replyPreviewView.setMaxWidth(pmaxPx - dpToPx(ctx, 30));
            replyPreviewView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(replyPreviewView);
            replyStrip.addView(textCol);
            innerContent.addView(replyStrip);

            // Message text
            msgView = new TextView(ctx);
            msgView.setTypeface(cfg.typeface != null ? cfg.typeface : Typeface.DEFAULT);
            msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.messageFontSize);
            msgView.setPadding(dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding), dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding));
            msgView.setSingleLine(false);
            msgView.setHorizontallyScrolling(false);
            msgView.setMaxWidth(pmaxPx);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                msgView.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
            }
            msgView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            innerContent.addView(msgView);

            // Message image
            imgView = new ImageView(ctx);
            imgView.setPadding(dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding), dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding));
            int maxImgPx = dpToPx(ctx, cfg.imageMessageMaxWidth);
            LinearLayout.LayoutParams imgp = new LinearLayout.LayoutParams(maxImgPx, ViewGroup.LayoutParams.WRAP_CONTENT);
            imgp.gravity = Gravity.CENTER;
            imgView.setLayoutParams(imgp);
            imgView.setAdjustViewBounds(true);
            imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            innerContent.addView(imgView);

            // Link Preview Card
            previewCard = new LinearLayout(ctx);
            previewCard.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(pmaxPx, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.setMargins(dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, 4), dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding));
            previewCard.setLayoutParams(cp);
            previewCard.setBackground(cfg.createLinkPreviewDrawable());
            previewCard.setPadding(dpToPx(ctx, 10), dpToPx(ctx, 8), dpToPx(ctx, 10), dpToPx(ctx, 8));

            View topBar = new View(ctx);
            topBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 3)));
            topBar.setBackgroundColor(cfg.linkPreviewAccentColor);
            previewCard.addView(topBar);

            previewSiteView = new TextView(ctx);
            previewSiteView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.nameFontSize);
            previewSiteView.setTextColor(cfg.linkPreviewAccentColor);
            previewSiteView.setTypeface(null, Typeface.BOLD);
            previewSiteView.setPadding(0, dpToPx(ctx, 4), 0, 0);
            previewCard.addView(previewSiteView);

            previewTitleView = new TextView(ctx);
            previewTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.messageFontSize - 1);
            previewTitleView.setTypeface(null, Typeface.BOLD);
            previewTitleView.setTextColor(android.graphics.Color.BLACK);
            previewTitleView.setMaxLines(2);
            previewCard.addView(previewTitleView);

            previewDescView = new TextView(ctx);
            previewDescView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.messageFontSize - 3);
            previewDescView.setTextColor(android.graphics.Color.GRAY);
            previewDescView.setMaxLines(3);
            previewCard.addView(previewDescView);
            innerContent.addView(previewCard);

            bubbleWrapper.addView(innerContent);

            // Metadata Row
            metaRow = new LinearLayout(ctx);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            metaRow.setGravity(isSent ? Gravity.END : Gravity.START);

            editedView = new TextView(ctx);
            editedView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.timestampFontSize - 1);
            editedView.setTextColor(cfg.editedLabelColor);
            editedView.setTypeface(null, Typeface.ITALIC);
            editedView.setPadding(dpToPx(ctx, 4), dpToPx(ctx, 2), dpToPx(ctx, 4), dpToPx(ctx, 2));
            metaRow.addView(editedView);

            timeView = new TextView(ctx);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.timestampFontSize);
            timeView.setPadding(dpToPx(ctx, 8), dpToPx(ctx, 2), dpToPx(ctx, 4), dpToPx(ctx, 2));
            metaRow.addView(timeView);

            starView = new TextView(ctx);
            starView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.timestampFontSize);
            starView.setPadding(dpToPx(ctx, 2), dpToPx(ctx, 2), dpToPx(ctx, 4), dpToPx(ctx, 2));
            metaRow.addView(starView);

            statusView = new TextView(ctx);
            statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            statusView.setPadding(dpToPx(ctx, 4), dpToPx(ctx, 2), dpToPx(ctx, 8), dpToPx(ctx, 2));
            metaRow.addView(statusView);

            // Assemble row
            if (isSent) {
                contentRow.addView(bubbleWrapper);
                if (avatarView != null) contentRow.addView(avatarView);
                contentRow.setGravity(Gravity.END);
            } else {
                if (avatarView != null) contentRow.addView(avatarView);
                contentRow.addView(bubbleWrapper);
                contentRow.setGravity(Gravity.START);
            }
            rootLayout.addView(contentRow);
        }

        private static int dpToPx(Context ctx, int dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }

    // ── Factory: non-message views ───────────────────────────────────────────

    private TextView createDateHeaderView() {
        TextView tv = new TextView(context);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dpToPx(16), 0, dpToPx(8));
        tv.setTextColor(android.graphics.Color.parseColor("#FF6200"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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

    private LinearLayout createMessageRootView() {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        l.setPadding(0, dpToPx(4), 0, dpToPx(4));
        return l;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}