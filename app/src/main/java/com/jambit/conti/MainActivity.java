package com.jambit.conti;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.jambit.conti.ccss.CCSS;


public class MainActivity extends ActionBarActivity {

    private View appView;

    private Bitmap bitmap;

    private Canvas canvas;

    private CCSS ccss;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        appView = findViewById(R.id.app);
        appView.getViewTreeObserver().addOnPreDrawListener(new StartingPreDrawListener());

        handler = new Handler(Looper.getMainLooper());
    }

    private void updateMirror() {
        appView.draw(canvas);

        bitmap.copyPixelsToBuffer(ccss.getBuffer());
        ccss.commit();

        // schedule next update
        handler.post(new Runnable() {

            @Override
            public void run() {
                updateMirror();
            }
        });
    }

    private class ColorChanger implements View.OnTouchListener {

        private final int viewX;

        private final int viewY;

        private final int viewWidth;

        private final int viewHeight;

        private final Rect bounds;

        private ColorChanger(int viewX, int viewY, int viewWidth, int viewHeight) {
            this.viewX = viewX;
            this.viewY = viewY;
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;

            bounds = new Rect(viewX, viewY, viewX + viewWidth, viewY + viewHeight);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_MOVE:
                    float x = event.getRawX() - viewX;
                    float y = event.getRawY() - viewY;

                    if (bounds.contains((int) x, (int) y)) {
                        // rules:
                        // x axis changes R - left = 0, right = 255
                        // y axis changes G - down = 0, up = 255

                        float xRelative = x / viewWidth;
                        float yRelative = y / viewHeight;

                        int r = normalize((int) (xRelative * 255));
                        int g = normalize((int) (yRelative * 255));

                        int color = Color.argb(255, r, g, 255);
                        appView.setBackgroundColor(color);

                        return true;
                    }
            }

            return false;
        }

        private int normalize(int v) {
            return Math.min(255, Math.max(0, v));
        }
    }

    // the dimensions are known in the observer right before first drawing
    // use this event to start everything
    private class StartingPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

        @Override
        public boolean onPreDraw() {
            appView.getViewTreeObserver().removeOnPreDrawListener(this);

            int width = appView.getWidth();
            int height = appView.getHeight();

            int[] appViewLocation = new int[2];
            appView.getLocationOnScreen(appViewLocation);
            appView.setOnTouchListener(new ColorChanger(appViewLocation[0], appViewLocation[1], width, height));
            appView.setBackgroundColor(Color.WHITE);

            // prepare the master bitmap and canvas
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            // prepare the mirror bitmap to be updated
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            ImageView mirrorView = (ImageView) findViewById(R.id.mirror);
            mirrorView.setImageDrawable(bitmapDrawable);

            // 4 is ARGB - the generated images will be 32bit bitmaps
            ccss = new CCSS(width * height * 4, new BitmapDrawableMirrorScreen(bitmapDrawable));

            // start
            updateMirror();

            return true;
        }
    }
}
