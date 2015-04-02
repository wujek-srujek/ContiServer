package com.jambit.conti.server;


import android.graphics.Bitmap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


// I don't care in this dirty hack, but real-world code should probably make it a singleton
// also, byte imageBuffer access should most likely be exclusive - when one party is writing/reading,
// nobody else may interfere or else data corruption will occur
public class CCSS {

    private final ByteBuffer imageBuffer;

    private final ByteBuffer touchEventBuffer;

    private final IntBuffer touchEventIntBuffer;

    private TouchListener touchListener;

    private ServerSocketChannel serverSocketChannel;

    private SocketChannel socketChannel;

    public CCSS(int capacity, final int port) throws IOException {
        imageBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        touchEventBuffer = ByteBuffer.allocateDirect(12 /* 3 ints */).order(ByteOrder.nativeOrder());
        touchEventIntBuffer = touchEventBuffer.asIntBuffer();

        // start a thread which waits for a single connection
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.socket().bind(new InetSocketAddress(port));

                    socketChannel = serverSocketChannel.accept();

                    while (true) {
                        // read the touch event
                        while (touchEventBuffer.hasRemaining()) {
                            socketChannel.read(touchEventBuffer);
                        }

                        int x = touchEventIntBuffer.get();
                        int y = touchEventIntBuffer.get();
                        int type = touchEventIntBuffer.get();

                        touchEventBuffer.clear();
                        touchEventIntBuffer.clear();

                        // notify the listener
                        if (touchListener != null) {
                            touchListener.onTouch(x, y, type);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public boolean hasConnection() {
        return socketChannel != null;
    }

    public void setTouchListener(TouchListener touchListener) {
        this.touchListener = touchListener;
    }

    public void sendUpdate(Bitmap bitmap) {
        bitmap.copyPixelsToBuffer(imageBuffer);

        // make ready for reading
        imageBuffer.flip();

        // the imageBuffer has been written to, now it's time to send it
        try {
            while (imageBuffer.hasRemaining()) {
                socketChannel.write(imageBuffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // clear for subsequent usage
        imageBuffer.clear();
    }
}
