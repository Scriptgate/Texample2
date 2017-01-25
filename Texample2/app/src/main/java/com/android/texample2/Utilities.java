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

    public static int createProgram(int vertexShaderHandle, int fragmentShaderHandle, AttributeVariable[] variables) {
        int mProgram = glCreateProgram();

        if (mProgram != 0) {
            glAttachShader(mProgram, vertexShaderHandle);
            glAttachShader(mProgram, fragmentShaderHandle);

            for (AttributeVariable var : variables) {
                glBindAttribLocation(mProgram, var.getHandle(), var.getName());
            }

            glLinkProgram(mProgram);

            final int[] linkStatus = new int[1];
            glGetProgramiv(mProgram, GL_LINK_STATUS, linkStatus, 0);

            if (linkStatus[0] == 0) {
                Log.v(TAG, glGetProgramInfoLog(mProgram));
                glDeleteProgram(mProgram);
                mProgram = 0;
            }
        }

        if (mProgram == 0) {
            throw new RuntimeException("Error creating program.");
        }
        return mProgram;
    }

    public static int loadShader(int type, String shaderCode) {
        int shaderHandle = glCreateShader(type);

        if (shaderHandle != 0) {
            glShaderSource(shaderHandle, shaderCode);
            glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.v(TAG, "Shader fail info: " + glGetShaderInfoLog(shaderHandle));
                glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }


        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader " + type);
        }
        return shaderHandle;
    }

    public static FloatBuffer newFloatBuffer(float[] verticesData) {
        FloatBuffer floatBuffer = allocateDirect(verticesData.length * BYTES_PER_FLOAT).order(nativeOrder()).asFloatBuffer();
        floatBuffer.put(verticesData).position(0);
        return floatBuffer;
    }
}
