package com.jambit.conti.server.ccss;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;


// I don't care in this dirty hack, but real-world code should probably make it a singleton
// also, byte buffer access should most likely be exclusive - when one party is writing/reading,
// nobody else may interfere or else data corruption will occur
public class CCSS {

    private final ByteBuffer buffer;

    private TouchListener touchListener;

    public CCSS(int capacity) {
        buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public void setTouchListener(TouchListener touchListener) {
        this.touchListener = touchListener;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void commit() {
        // make ready for reading
        buffer.flip();

        // the buffer has been written to, now it's time to send it
        // TODO network I/O

        // clear for subsequent usage
        buffer.clear();
    }

    public void generateTouch(int x, int y, int type) {
        // someone, like the touch sensor on the mirror, generated a touch
        // the mirror sends them to the smartphone and CCSS gets them somehow
        // and notifies whoever might be interested on the smartphone side that
        // they came
        if (touchListener != null) {
            touchListener.onTouch(x, y, type);
        }
    }
}
