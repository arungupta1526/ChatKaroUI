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

        void showTextOptionsMenu(View anchor, String message, int messageId, boolean isSent, boolean isStarred,
                String senderName, String avatarUrl);

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
            case MessageModel.TYPE_UNREAD_SEPARATOR:
                return new SystemMessageVH(createSystemMessageView());
            case MessageModel.TYPE_SENT_SIMPLE:
            case MessageModel.TYPE_SENT_AVATAR:
            case MessageModel.TYPE_SENT_TEXT_IMAGE:
                return new SentMessageVH(createMessageRootView(), config, viewType);
            case MessageModel.TYPE_RECEIVED_SIMPLE:
            case MessageModel.TYPE_RECEIVED_AVATAR:
            case MessageModel.TYPE_RECEIVED_TEXT_IMAGE:
                return new ReceivedMessageVH(createMessageRootView(), config, viewType);
            default:
                return new SentMessageVH(createMessageRootView(), config, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel model = messages.get(position);
        if (holder instanceof DateHeaderVH)
            ((DateHeaderVH) holder).bind(model);
        else if (holder instanceof SystemMessageVH)
            ((SystemMessageVH) holder).bind(model, config);
        else if (holder instanceof TypingVH)
            ((TypingVH) holder).bind(model);
        else if (holder instanceof BaseMessageVH)
            ((BaseMessageVH) holder).bind(model, eventCallback, imageLoader, config);
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

        // void bind(MessageModel m) {
        // tv.setText(m.message);
        // }

        void bind(MessageModel m, ChatConfig cfg) {
            tv.setText(m.message);
            tv.setTextColor(cfg.systemMessageTextColor);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.systemMessageFontSize);
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

    // ── Message ViewHolders (V3.3 Refactored) ───────────────────────────────────

    static abstract class BaseMessageVH extends RecyclerView.ViewHolder {
        protected final LinearLayout rootLayout;
        protected TextView nameView;
        protected ImageView avatarView;
        protected LinearLayout contentRow;
        protected LinearLayout bubbleWrapper;
        protected LinearLayout innerContent;
        protected LinearLayout replyStrip;
        protected TextView replySenderView;
        protected TextView replyPreviewView;
        protected TextView msgView;
        protected ImageView imgView;
        protected LinearLayout previewCard;
        protected TextView previewSiteView, previewTitleView, previewDescView;
        protected LinearLayout metaRow;
        protected TextView editedView, timeView, starView, statusView;

        BaseMessageVH(LinearLayout root) {
            super(root);
            this.rootLayout = root;
        }

        @SuppressWarnings("deprecation")
        void bind(MessageModel model, EventCallback cb, ImageLoader imageLoader, ChatConfig cfg) {
            boolean isSent = model.isSentType();
            Context ctx = rootLayout.getContext();

            // ── Selection highlight ──────────────────────────────────────────
            rootLayout.setBackgroundColor(model.isSelected ? cfg.selectedMessageBgColor : 0x00000000);

            // ── Sender Name ──────────────────────────────────────────────────
            if (nameView != null) {
                if (model.senderName != null && !model.senderName.isEmpty()) {
                    nameView.setVisibility(View.VISIBLE);
                    nameView.setText(model.senderName);
                    nameView.setTextColor(isSent ? cfg.sentNameTextColor : cfg.receivedNameTextColor);
                    nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, cfg.nameFontSize);
                } else {
                    nameView.setVisibility(View.GONE);
                }
            }

            // ── Avatar ───────────────────────────────────────────────────────
            if (avatarView != null) {
                final String name = model.senderName;
                final String url = model.avatarUrl;
                avatarView.setOnClickListener(v -> {
                    if (cb != null)
                        cb.onProfilePictureClicked(name, url);
                });
                imageLoader.loadCircular(avatarView, url, cfg.avatarSize);
            }

            // ── Bubble & Content ─────────────────────────────────────────────
            boolean hasImage = (model.imageUrl != null && !model.imageUrl.isEmpty());
            if (hasImage || cfg.showMetadataInsideBubble) {
                innerContent.setBackground(cfg.createBubbleDrawable(isSent));
            } else {
                innerContent.setBackground(null);
            }

            // ── Reply Quote ──────────────────────────────────────────────────
            if (model.hasReply()) {
                replyStrip.setVisibility(View.VISIBLE);
                replySenderView.setText(
                        model.replyToSender != null ? model.replyToSender : (model.replyToIsSent ? "You" : ""));
                String preview = model.replyToText != null ? model.replyToText : "";
                if (preview.length() > 80)
                    preview = preview.substring(0, 80) + "…";
                replyPreviewView.setText(preview);
                replyPreviewView.setTextColor(cfg.replyPreviewTextColor);
                replyStrip.setBackground(cfg.createReplyDrawable(isSent));

                final int replyId = model.replyToId;
                replyStrip.setOnClickListener(v -> {
                    if (cb != null && model.messageId > 0 && cb.isMultiSelectionActive()) {
                        cb.onMessageSelected(model.message, model.messageId);
                    } else if (cb != null && replyId > 0) {
                        cb.onReplyQuoteTapped(replyId);
                    }
                });
            } else {
                replyStrip.setVisibility(View.GONE);
            }

            // ── Message Text & Styling ──────────────────────────────────────
            if (model.message != null && !model.message.isEmpty()) {
                msgView.setVisibility(View.VISIBLE);
                msgView.setTextColor(isSent ? cfg.sentMessageTextColor : cfg.receivedMessageTextColor);

                int arrangementW = cfg.arrangementWidthPx > 0 ? cfg.arrangementWidthPx
                        : ctx.getResources().getDisplayMetrics().widthPixels;
                int defaultMaxPx = (int) (arrangementW * 0.8f);
                int maxWidthDp = hasImage ? cfg.imageMessageMaxWidth : cfg.textMessageMaxWidth;
                int finalMaxPx = (cfg.useResponsiveWidth && maxWidthDp == 0) ? defaultMaxPx : dpToPx(ctx, maxWidthDp);
                msgView.setMaxWidth(finalMaxPx);

                if (cfg.markdownEnabledInChat) {
                    msgView.setText(MarkdownParser.parse(model.message));
                    msgView.setMovementMethod(LinkMovementMethod.getInstance());
                } else if (cfg.htmlEnabledInChat) {
                    msgView.setText(
                            Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(model.message, Html.FROM_HTML_MODE_COMPACT)
                                    : Html.fromHtml(model.message));
                    msgView.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    msgView.setText(model.message);
                    if (cfg.autoLinkEnabledInChat) {
                        Linkify.addLinks(msgView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                        Linkify.addLinks(msgView, java.util.regex.Pattern.compile("\\b\\d{10,}\\b"), "tel:");
                        msgView.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                }
                msgView.setBackground(!hasImage && cfg.showMetadataOutBubble ? cfg.createBubbleDrawable(isSent) : null);
            } else {
                msgView.setVisibility(View.GONE);
            }

            // ── Image Layout ─────────────────────────────────────────────────
            if (hasImage) {
                imgView.setVisibility(View.VISIBLE);
                imageLoader.loadWithPlaceholder(imgView, model.imageUrl, cfg.imageMessageMaxWidth);
                final String imgUrl = model.imageUrl;
                final int msgId = model.messageId;
                imgView.setOnClickListener(v -> {
                    if (imgView.getDrawable() != null && cb != null)
                        cb.showFullscreenImage(imgView.getDrawable());
                });
                imgView.setOnLongClickListener(v -> {
                    if (cb != null)
                        cb.showImageOptionsMenu(v, imgUrl, imgView, msgId);
                    return true;
                });

                // Text vs Image Ordering
                if (msgView.getVisibility() == View.VISIBLE) {
                    int msgIdx = innerContent.indexOfChild(msgView);
                    int imgIdx = innerContent.indexOfChild(imgView);
                    boolean needsSwap = model.messageOnTop ? (msgIdx > imgIdx) : (imgIdx > msgIdx);
                    if (needsSwap) {
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
                }
            } else {
                imgView.setVisibility(View.GONE);
            }

            // ── Link Preview ─────────────────────────────────────────────────
            if (cfg.linkPreviewEnabled && model.hasLinkPreview()) {
                previewCard.setVisibility(View.VISIBLE);
                previewSiteView.setText(model.previewSiteName);
                previewSiteView.setVisibility(model.previewSiteName.isEmpty() ? View.GONE : View.VISIBLE);
                previewTitleView.setText(model.previewTitle);
                previewTitleView.setVisibility(model.previewTitle.isEmpty() ? View.GONE : View.VISIBLE);
                previewDescView.setText(model.previewDescription);
                previewDescView.setVisibility(model.previewDescription.isEmpty() ? View.GONE : View.VISIBLE);
                previewCard.setBackground(cfg.createLinkPreviewDrawable());
            } else {
                previewCard.setVisibility(View.GONE);
            }

            // ── Metadata Row ──────────────────────────────────────────────────
            editedView.setVisibility(cfg.showEditedLabel && model.isEdited ? View.VISIBLE : View.GONE);
            editedView.setText(cfg.editedLabelText);
            timeView.setVisibility(
                    cfg.showTimestamp && model.timestamp != null && !model.timestamp.isEmpty() ? View.VISIBLE
                            : View.GONE);
            timeView.setText(model.timestamp);
            starView.setVisibility(model.isStarred ? View.VISIBLE : View.GONE);
            starView.setText(cfg.starredIndicatorText);
            starView.setTextColor(cfg.starredIndicatorColor);
            statusView.setVisibility(cfg.showReadStatus ? View.VISIBLE : View.GONE);
            statusView.setText(isSent ? cfg.sentStatusText : cfg.receivedStatusText);
            statusView.setTextColor(isSent ? cfg.sentStatusTextColor : cfg.receivedStatusTextColor);

            // Re-parent Metadata Row if needed
            ViewGroup currentMetaParent = (ViewGroup) metaRow.getParent();
            ViewGroup targetMetaParent = cfg.showMetadataInsideBubble ? innerContent : bubbleWrapper;
            if (currentMetaParent != targetMetaParent) {
                if (currentMetaParent != null)
                    currentMetaParent.removeView(metaRow);
                targetMetaParent.addView(metaRow);
            }
            if (cfg.showMetadataInsideBubble) {
                LinearLayout.LayoutParams mlp = (LinearLayout.LayoutParams) metaRow.getLayoutParams();
                mlp.gravity = isSent ? Gravity.END : Gravity.START;
                metaRow.setLayoutParams(mlp);
            }

            // ── Interaction Listeners ─────────────────────────────────────────
            final String msgText = model.message != null ? model.message : "";
            final int mId = model.messageId;
            final boolean starred = model.isStarred;

            View.OnLongClickListener lc = v -> {
                if (cb != null && mId > 0) {
                    cb.onMessageSelected(msgText, mId);
                    if (!cb.isMultiSelectionActive()) {
                        v.post(() -> cb.showTextOptionsMenu(innerContent, msgText, mId, isSent, starred,
                                model.senderName, model.avatarUrl));
                    }
                }
                return true;
            };
            View.OnClickListener cl = v -> {
                if (cb != null && mId > 0 && cb.isMultiSelectionActive())
                    cb.onMessageSelected(msgText, mId);
            };

            innerContent.setOnLongClickListener(lc);
            innerContent.setOnClickListener(cl);
            bubbleWrapper.setOnLongClickListener(lc);
            bubbleWrapper.setOnClickListener(cl);
            msgView.setOnLongClickListener(lc);
            msgView.setOnClickListener(cl);
            rootLayout.setOnLongClickListener(lc);
            rootLayout.setOnClickListener(cl);
        }

        protected void initSkeleton(Context ctx, ChatConfig cfg, int viewType, boolean isSent) {
            // Skeleton Creation (Fixed properties only)
            nameView = new TextView(ctx);
            nameView.setTypeface(null, Typeface.BOLD);
            nameView.setGravity(isSent ? Gravity.END : Gravity.START);
            rootLayout.addView(nameView);

            contentRow = new LinearLayout(ctx);
            contentRow.setOrientation(LinearLayout.HORIZONTAL);
            contentRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            contentRow.setGravity(isSent ? Gravity.END : Gravity.START);

            boolean hasAvatarPool = viewType != MessageModel.TYPE_SENT_SIMPLE
                    && viewType != MessageModel.TYPE_RECEIVED_SIMPLE;
            if (hasAvatarPool) {
                avatarView = new ImageView(ctx);
                int avPx = dpToPx(ctx, cfg.avatarSize);
                LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(avPx, avPx);
                avp.setMargins(dpToPx(ctx, isSent ? 0 : 8), 0, dpToPx(ctx, isSent ? 8 : 0), 0);
                avatarView.setLayoutParams(avp);
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(cfg.avatarBackgroundColor);
                avatarView.setBackground(circle);
            }

            bubbleWrapper = new LinearLayout(ctx);
            bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
            bubbleWrapper.setGravity(isSent ? Gravity.END : Gravity.START);
            LinearLayout.LayoutParams bwp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bwp.setMargins(dpToPx(ctx, 8), 0, dpToPx(ctx, 8), 0);
            bubbleWrapper.setLayoutParams(bwp);

            innerContent = new LinearLayout(ctx);
            innerContent.setOrientation(LinearLayout.VERTICAL);
            innerContent.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            // Reply Quote
            replyStrip = new LinearLayout(ctx);
            replyStrip.setOrientation(LinearLayout.HORIZONTAL);
            replyStrip.setPadding(0, dpToPx(ctx, 4), dpToPx(ctx, 8), dpToPx(ctx, 4));
            View accent = new View(ctx);
            accent.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(ctx, 3), ViewGroup.LayoutParams.MATCH_PARENT));
            accent.setBackgroundColor(cfg.replyAccentColor);
            replyStrip.addView(accent);
            LinearLayout textCol = new LinearLayout(ctx);
            textCol.setOrientation(LinearLayout.VERTICAL);
            replySenderView = new TextView(ctx);
            replySenderView.setTypeface(null, Typeface.BOLD);
            replySenderView.setTextColor(cfg.replyAccentColor);
            replySenderView.setSingleLine(true);
            replyPreviewView = new TextView(ctx);
            replyPreviewView.setMaxLines(2);
            textCol.addView(replySenderView);
            textCol.addView(replyPreviewView);
            replyStrip.addView(textCol);
            innerContent.addView(replyStrip);

            msgView = new TextView(ctx);
            msgView.setPadding(dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding),
                    dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding));
            if (Build.VERSION.SDK_INT >= 23)
                msgView.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            innerContent.addView(msgView);

            imgView = new ImageView(ctx);
            imgView.setPadding(dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding),
                    dpToPx(ctx, cfg.messageHorizontalPadding), dpToPx(ctx, cfg.messageVerticalPadding));
            imgView.setAdjustViewBounds(true);
            imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            innerContent.addView(imgView);

            // Preview Card
            previewCard = new LinearLayout(ctx);
            previewCard.setOrientation(LinearLayout.VERTICAL);
            previewCard.setPadding(dpToPx(ctx, 10), dpToPx(ctx, 8), dpToPx(ctx, 10), dpToPx(ctx, 8));
            View topBar = new View(ctx);
            topBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 3)));
            topBar.setBackgroundColor(cfg.linkPreviewAccentColor);
            previewCard.addView(topBar);
            previewSiteView = new TextView(ctx);
            previewSiteView.setTypeface(null, Typeface.BOLD);
            previewCard.addView(previewSiteView);
            previewTitleView = new TextView(ctx);
            previewTitleView.setTypeface(null, Typeface.BOLD);
            previewTitleView.setTextColor(Color.BLACK);
            previewCard.addView(previewTitleView);
            previewDescView = new TextView(ctx);
            previewDescView.setTextColor(Color.GRAY);
            previewCard.addView(previewDescView);
            innerContent.addView(previewCard);

            bubbleWrapper.addView(innerContent);

            // Metadata Row
            metaRow = new LinearLayout(ctx);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setGravity(isSent ? Gravity.END : Gravity.START);
            editedView = new TextView(ctx);
            editedView.setTypeface(null, Typeface.ITALIC);
            metaRow.addView(editedView);
            timeView = new TextView(ctx);
            metaRow.addView(timeView);
            starView = new TextView(ctx);
            metaRow.addView(starView);
            statusView = new TextView(ctx);
            statusView.setTextSize(12);
            metaRow.addView(statusView);

            // Assemble into Content Row
            if (isSent) {
                contentRow.addView(bubbleWrapper);
                if (avatarView != null)
                    contentRow.addView(avatarView);
            } else {
                if (avatarView != null)
                    contentRow.addView(avatarView);
                contentRow.addView(bubbleWrapper);
            }
            rootLayout.addView(contentRow);
        }

        protected static int dpToPx(Context ctx, float dp) {
            return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
        }
    }

    static class SentMessageVH extends BaseMessageVH {
        SentMessageVH(LinearLayout root, ChatConfig cfg, int viewType) {
            super(root);
            initSkeleton(root.getContext(), cfg, viewType, true);
        }
    }

    static class ReceivedMessageVH extends BaseMessageVH {
        ReceivedMessageVH(LinearLayout root, ChatConfig cfg, int viewType) {
            super(root);
            initSkeleton(root.getContext(), cfg, viewType, false);
        }
    }

    // ── Factory: non-message views ───────────────────────────────────────────

    private TextView createDateHeaderView() {
        TextView tv = new TextView(context);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dpToPx(16), 0, dpToPx(8));
        tv.setTextColor(0xFFFF6200);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private TextView createSystemMessageView() {
        TextView tv = new TextView(context);
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