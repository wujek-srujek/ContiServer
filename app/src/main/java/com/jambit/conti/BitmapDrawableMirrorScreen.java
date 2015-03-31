package com.jambit.conti;


import android.graphics.drawable.BitmapDrawable;

import com.jambit.conti.ccss.MirrorScreen;

import java.nio.ByteBuffer;


public class BitmapDrawableMirrorScreen implements MirrorScreen {

    private final BitmapDrawable bitmapDrawable;

    public BitmapDrawableMirrorScreen(BitmapDrawable bitmapDrawable) {
        this.bitmapDrawable = bitmapDrawable;
    }

    @Override
    public void update(ByteBuffer buffer) {
        bitmapDrawable.getBitmap().copyPixelsFromBuffer(buffer);

        // force redraw of the drawable - without it, no updates will be visible
        bitmapDrawable.invalidateSelf();
    }
}
