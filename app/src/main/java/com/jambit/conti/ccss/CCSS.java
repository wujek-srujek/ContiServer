package com.jambit.conti.ccss;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;


// I don't care in this dirty hack, but real-world code should probably make it a singleton
// also, byte buffer access should most likely be exclusive - when one party is writing/reading,
// nobody else may interfere or else data corruption will occur
public class CCSS {

    private final ByteBuffer buffer;

    private final MirrorScreen mirrorScreen;

    public CCSS(int capacity, MirrorScreen mirrorScreen) {
        buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.mirrorScreen = mirrorScreen;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void commit() {
        // make ready for reading
        buffer.flip();

        // the buffer has been written to, now it's time to send it somewhere and render somehow
        // image this actually performs network I/O and sends the buffer to the real display
        mirrorScreen.update(buffer);

        // clear for subsequent usage
        buffer.clear();
    }
}
