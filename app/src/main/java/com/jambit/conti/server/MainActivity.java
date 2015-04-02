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
import android.widget.TextView;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity {

    public static final String PORT_EXTRA = "portExtra";

    private View appView;

    private Bitmap bitmap;

    private Canvas canvas;

    private CCSS ccss;

    private Handler mainThreadHandler;

    private Executor networkingExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        appView = findViewById(R.id.app);
        appView.getViewTreeObserver().addOnPreDrawListener(new StartingPreDrawListener());
    }

    private void updateMirror() {
        if (ccss.hasConnection()) {
            appView.draw(canvas);

            networkingExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    ccss.sendUpdate(bitmap);

                    // schedule next update
                    mainThreadHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            updateMirror();
                        }
                    });
                }
            });
        } else {
            // try again in a second
            mainThreadHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    updateMirror();
                }
            }, 1000);
        }
    }

    // the dimensions are known in the observer right before first drawing
    // use this event to start everything
    private class StartingPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

        @Override
        public boolean onPreDraw() {
            appView.getViewTreeObserver().removeOnPreDrawListener(this);

            int port = getIntent().getIntExtra(PORT_EXTRA, -1);
            String ip = "<unknown ip>";
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            ip = inetAddress.getHostAddress();
                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                throw new RuntimeException();
            }

            ((TextView) findViewById(R.id.info)).setText(ip + ":" + port);

            final float x = appView.getX();
            final int width = appView.getWidth();
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

            findViewById(R.id.switcher).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    webView.setVisibility(webView.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                }
            });

            findViewById(R.id.move).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    appView.animate().x(appView.getX() < 0 ? x : -2 * width);
                }
            });

            // prepare the master bitmap and canvas
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            // 4 is ARGB - the generated images will be 32bit bitmaps
            try {
                ccss = new CCSS(width * height * 4, port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ccss.setTouchListener(new LocalTouchDispatcher());

            mainThreadHandler = new Handler(Looper.getMainLooper());
            networkingExecutor = Executors.newSingleThreadExecutor();

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

    private class LocalTouchDispatcher implements TouchListener {

        @Override
        public void onTouch(final int x, final int y, final int type) {
            mainThreadHandler.post(new Runnable() {

                @Override
                public void run() {
                    MotionEvent motionEvent = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), type, x, y, 0);
                    appView.dispatchTouchEvent(motionEvent);

                    motionEvent.recycle();
                }
            });
        }
    }
}
