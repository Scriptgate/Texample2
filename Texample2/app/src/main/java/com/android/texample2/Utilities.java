package com.android.texample2;

import java.nio.FloatBuffer;

import android.util.Log;

import static android.opengl.GLES20.*;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

public class Utilities {

    public static final int BYTES_PER_FLOAT = 4;
    public static final int BYTES_PER_SHORT = 2;
    private static final String TAG = "Utilities";



    public static FloatBuffer newFloatBuffer(float[] verticesData) {
        FloatBuffer floatBuffer = allocateDirect(verticesData.length * BYTES_PER_FLOAT).order(nativeOrder()).asFloatBuffer();
        floatBuffer.put(verticesData).position(0);
        return floatBuffer;
    }
}
