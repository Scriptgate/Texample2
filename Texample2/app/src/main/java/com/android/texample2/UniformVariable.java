package com.android.texample2;


public enum UniformVariable {

    COLOR("u_Color"),
    TEXTURE("u_Texture"),
    MVP_MATRIX("u_MVPMatrix");

    private String name;

    UniformVariable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
