package com.prem.chatkaroui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * ItemTouchHelper.SimpleCallback that enables WhatsApp-style swipe-to-reply.
 *
 * Behaviour:
 *   - Any message item can be swiped RIGHT (received) or LEFT (sent).
 *   - A circular reply icon slides in proportionally to the swipe distance.
 *   - At 30 % of item width the item snaps back and fires {@link ReplyCallback}.
 *   - The icon grows from 0 → full size as the user swipes, giving tactile feedback.
 *   - Date-headers, system messages and typing indicators are NOT swipeable.
 *
 * Portrait / Landscape safe: uses item width at draw-time so it adapts
 * automatically to any screen orientation.
 */
public class SwipeReplyCallback extends ItemTouchHelper.SimpleCallback {

    public interface ReplyCallback {
        void onReply(int adapterPosition);
    }

    // ── Trigger threshold (fraction of item width) ───────────────────────────
    private static final float TRIGGER_FRACTION = 0.30f;
    // Maximum icon travel as fraction of item width
    private static final float MAX_ICON_TRAVEL  = 0.25f;

    private final ReplyCallback replyCallback;
    private final Paint circlePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<MessageModel> messageList;
    private final RecyclerView.Adapter<?> adapter;

    // Track which position was triggered so we don't re-fire on residual draw
    private long triggeredPosition = RecyclerView.NO_ID;

    public SwipeReplyCallback(List<MessageModel> messageList, RecyclerView.Adapter<?> adapter, ReplyCallback replyCallback) {
        // No drag, swipe LEFT + RIGHT
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.messageList   = messageList;
        this.adapter       = adapter;
        this.replyCallback = replyCallback;

        circlePaint.setColor(0xFFE0E0E0);
        circlePaint.setStyle(Paint.Style.FILL);

        arrowPaint.setColor(0xFF555555);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(dpToPx(2.5f));
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    // ── Swipe eligibility ────────────────────────────────────────────────────

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {
        int pos = viewHolder.getAdapterPosition();
        if (pos < 0 || pos >= messageList.size()) return 0;
        MessageModel m = messageList.get(pos);
        // Only real message items are swipeable
        int type = m.viewType;
        if (type == MessageModel.TYPE_DATE_HEADER
         || type == MessageModel.TYPE_SYSTEM
         || type == MessageModel.TYPE_TYPING_INDICATOR) {
            return 0;
        }
        // Sent messages swipe LEFT, received swipe RIGHT
        return m.isSentType() ? ItemTouchHelper.LEFT : ItemTouchHelper.RIGHT;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return TRIGGER_FRACTION;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        // Make it harder to accidentally trigger via velocity alone
        return defaultValue * 4f;
    }

    // ── Prevent actual removal — we only want the animation ─────────────────

    @Override
    public boolean onMove(@NonNull RecyclerView rv,
                          @NonNull RecyclerView.ViewHolder v,
                          @NonNull RecyclerView.ViewHolder t) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        if (pos >= 0 && pos != triggeredPosition) {
            triggeredPosition = pos;
            if (replyCallback != null) replyCallback.onReply(pos);
        }
        // Snap back — do NOT remove the item
        if (adapter != null) {
            adapter.notifyItemChanged(pos);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView rv,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(rv, viewHolder);
        triggeredPosition = RecyclerView.NO_ID;
        // Ensure view is fully reset to avoid ghost offset
        viewHolder.itemView.setTranslationX(0f);
    }

    // ── Custom draw: sliding reply-icon ─────────────────────────────────────

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState,
                            boolean isCurrentlyActive) {

        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        View item = viewHolder.itemView;
        float itemWidth  = item.getWidth();
        if (itemWidth == 0) return;
        float absDx      = Math.abs(dX);
        float fraction   = Math.min(absDx / itemWidth, MAX_ICON_TRAVEL);

        // Icon grows from 0 to full size at trigger threshold
        float growFraction = Math.min(fraction / TRIGGER_FRACTION, 1f);
        float iconRadius = dpToPx(18) * growFraction;   // max radius 18 dp
        float iconCenterY = item.getTop() + item.getHeight() / 2f;

        if (dX > 0) {
            // Received message swiped RIGHT → icon appears on left edge
            float iconCenterX = item.getLeft() + dpToPx(24) * fraction / MAX_ICON_TRAVEL;
            drawReplyIcon(c, iconCenterX, iconCenterY, iconRadius, false);
        } else if (dX < 0) {
            // Sent message swiped LEFT → icon appears on right edge
            float iconCenterX = item.getRight() - dpToPx(24) * fraction / MAX_ICON_TRAVEL;
            drawReplyIcon(c, iconCenterX, iconCenterY, iconRadius, true);
        }

        // Fire callback early (at trigger fraction) while still dragging
        if (isCurrentlyActive) {
            int pos = viewHolder.getAdapterPosition();
            if (fraction >= TRIGGER_FRACTION && pos != triggeredPosition) {
                triggeredPosition = pos;
                if (replyCallback != null) replyCallback.onReply(pos);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    // ── Icon drawing ─────────────────────────────────────────────────────────

    /**
     * Draws a circular background with a reply-arrow inside.
     *
     * @param flipped true = arrow points left (for sent messages swiped left)
     */
    private void drawReplyIcon(Canvas c, float cx, float cy,
                               float radius, boolean flipped) {
        if (radius < 2) return;

        // Circle background
        c.drawCircle(cx, cy, radius, circlePaint);

        if (radius < 6) return; // too small to draw arrow legibly

        // Scale arrow proportionally to circle
        float ar = radius * 0.55f;  // arrow "radius" (half-size)
        arrowPaint.setStrokeWidth(radius * 0.14f);

        c.save();
        if (flipped) {
            // Mirror horizontally around cx
            c.scale(-1f, 1f, cx, cy);
        }

        // Reply arrow: curved hook + arrowhead
        //   Looks like ↩ (return/reply symbol)
        Path path = new Path();

        // Curved tail (arc from right to left, curving upward)
        RectF arc = new RectF(cx - ar, cy - ar * 0.6f, cx + ar, cy + ar * 0.8f);
        path.arcTo(arc, 0f, -180f, true);

        // Arrowhead at the tail end
        float tipX = cx - ar;
        float tipY = cy + ar * 0.1f;
        path.moveTo(tipX - ar * 0.35f, tipY - ar * 0.35f);
        path.lineTo(tipX, tipY);
        path.lineTo(tipX - ar * 0.35f, tipY + ar * 0.35f);

        c.drawPath(path, arrowPaint);
        c.restore();
    }

    // ────────────────────────────────────────────────────────────────────────

    private static float dpToPx(float dp) {
        return dp * android.content.res.Resources.getSystem()
                        .getDisplayMetrics().density;
    }
}
