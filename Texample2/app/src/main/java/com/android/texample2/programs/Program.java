package com.android.texample2.programs;

import android.util.Log;

import com.android.texample2.AttributeVariable;

import static android.opengl.GLES20.*;


public class Program {

    private static final String TAG = "Program";

    private int programHandle;

    public Program(String vertexShaderCode, String fragmentShaderCode, AttributeVariable[] programVariables) {
        int vertexShaderHandle = loadShader(GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShaderHandle = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode);

        programHandle = createProgram(vertexShaderHandle, fragmentShaderHandle, programVariables);
    }

    public int getHandle() {
        return programHandle;
    }

    private static int createProgram(int vertexShaderHandle, int fragmentShaderHandle, AttributeVariable[] variables) {
        int mProgram = glCreateProgram();

        if (mProgram != 0) {
            glAttachShader(mProgram, vertexShaderHandle);
            glAttachShader(mProgram, fragmentShaderHandle);

            for (int i = 0; i < variables.length; i++) {
                glBindAttribLocation(mProgram, i, variables[i].getName());
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