package com.jambit.conti.server;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jambit.conti.server.ccss.CCSS;
import com.jambit.conti.server.ccss.TouchListener;


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

    // the dimensions are known in the observer right before first drawing
    // use this event to start everything
    private class StartingPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

        @Override
        public boolean onPreDraw() {
            appView.getViewTreeObserver().removeOnPreDrawListener(this);

            int width = appView.getWidth();
            int height = appView.getHeight();

            appView.setOnTouchListener(new ColorChanger(width, height));
            appView.setBackgroundColor(Color.WHITE);

            final WebView webView = (WebView) findViewById(R.id.web);
            webView.setWebViewClient(new WebViewClient() {

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);

                    return true;
                }
            });
            webView.loadUrl("http://www.google.com");

            findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    webView.setVisibility(webView.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                }
            });

            // prepare the master bitmap and canvas
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            // 4 is ARGB - the generated images will be 32bit bitmaps
            ccss = new CCSS(width * height * 4);
            ccss.setTouchListener(new LocalTouchDispatcher(appView));

            // move the app off-screen - the real app should not be shown
//            appView.setX(-width);

            // start
            updateMirror();

            return true;
        }
    }

    private class ColorChanger implements View.OnTouchListener {

        private final int viewWidth;

        private final int viewHeight;

        private final Rect bounds;

        private ColorChanger(int viewWidth, int viewHeight) {
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;

            bounds = new Rect(0, 0, viewWidth, viewHeight);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_MOVE:
                    float x = event.getX();
                    float y = event.getY();
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

    private class TouchForwarder implements View.OnTouchListener {

        private final Rect bounds;

        public TouchForwarder(int viewWidth, int viewHeight) {
            bounds = new Rect(0, 0, viewWidth, viewHeight);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            if (bounds.contains((int) x, (int) y)) {
                ccss.generateTouch((int) x, (int) y, event.getActionMasked());
            }

            return true;
        }
    }

    private class LocalTouchDispatcher implements TouchListener {

        private final View view;

        public LocalTouchDispatcher(View view) {
            this.view = view;
        }

        @Override
        public void onTouch(int x, int y, int type) {
            MotionEvent motionEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), type, x, y, 0);

            view.dispatchTouchEvent(motionEvent);

            motionEvent.recycle();
        }
    }
}
