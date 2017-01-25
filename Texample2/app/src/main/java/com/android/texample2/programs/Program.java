package com.android.texample2.programs;

import android.opengl.GLES20;

import com.android.texample2.AttributeVariable;
import com.android.texample2.Utilities;

import static com.android.texample2.Utilities.loadShader;


public abstract class Program {

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

        programHandle = Utilities.createProgram(vertexShaderHandle, fragmentShaderHandle, programVariables);

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
}