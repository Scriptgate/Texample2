package com.texample2.android.programs;

import android.opengl.GLES20;
import android.util.Log;

import com.texample2.android.AttributeVariable;

import static android.opengl.GLES20.*;


public abstract class Program {

    private static final String TAG = "Program";

    private int programHandle;
    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    private boolean initialized;

    public Program() {
        initialized = false;
    }

    public void init() {
        init(null, null, null);
    }

    public void init(String vertexShaderCode, String fragmentShaderCode, AttributeVariable[] programVariables) {
        vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        programHandle = createProgram(vertexShaderHandle, fragmentShaderHandle, programVariables);

        initialized = true;
    }

    public int getHandle() {
        return programHandle;
    }

    public void delete() {
        GLES20.glDeleteShader(vertexShaderHandle);
        GLES20.glDeleteShader(fragmentShaderHandle);
        GLES20.glDeleteProgram(programHandle);
        initialized = false;
    }

    public boolean initialized() {
        return initialized;
    }

    private static int createProgram(int vertexShaderHandle, int fragmentShaderHandle, AttributeVariable[] variables) {
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

    private static int loadShader(int type, String shaderCode) {
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
}