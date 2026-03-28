package com.prem.chatkaroui;

import android.widget.ImageView;

/**
 * Interface for image loading, implemented by ChatKaroUI.
 * Decouples ChatAdapter from the image loading internals.
 */
public interface ImageLoader {
    /** Load a circular/avatar image into the given ImageView */
    void loadCircular(ImageView imageView, String url, int sizeDp);

    /** Load a message image with placeholder */
    void loadWithPlaceholder(ImageView imageView, String url, int maxWidthDp);
}