package com.android.texample2.domain;


import com.android.texample2.AttributeVariable;
import com.android.texample2.UniformVariable;

import static android.opengl.GLES20.*;
import static com.android.texample2.UniformVariable.*;

public class FontProgram {

    private int programHandle;

    public FontProgram(int programHandle) {
        this.programHandle = programHandle;
    }

    public int getProgramHandle() {
        return programHandle;
    }

    public int getColorHandle() {
        return getHandle(COLOR);
    }

    public int getTextureUniformHandle() {
        return getHandle(TEXTURE);
    }

    public int getMvpMatricesHandle() {
        return getHandle(MVP_MATRIX);
    }

    public int getHandle(UniformVariable uniformVariable) {
        return glGetUniformLocation(programHandle, uniformVariable.getName());
    }

    public int getHandle(AttributeVariable attributeVariable) {
        return glGetAttribLocation(programHandle, attributeVariable.getName());
    }
}
