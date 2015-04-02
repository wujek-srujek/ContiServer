package com.jambit.conti.server.ccss;


import java.nio.ByteBuffer;


public interface MirrorScreen {

    void update(ByteBuffer buffer);
}
