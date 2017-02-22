package com.android.texample2.domain;


public class FontProgram {

    private int programHandle;
    private int colorHandle;
    private int textureUniformHandle;
    private int mvpMatricesHandle;

    public FontProgram(int programHandle, int colorHandle, int textureUniformHandle, int mvpMatricesHandle) {
        this.programHandle = programHandle;
        this.colorHandle = colorHandle;
        this.textureUniformHandle = textureUniformHandle;
        this.mvpMatricesHandle = mvpMatricesHandle;
    }

    public int getProgramHandle() {
        return programHandle;
    }

    public int getColorHandle() {
        return colorHandle;
    }

    public int getTextureUniformHandle() {
        return textureUniformHandle;
    }

    public int getMvpMatricesHandle() {
        return mvpMatricesHandle;
    }
}
