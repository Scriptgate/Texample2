package com.android.texample2;


public enum AttributeVariable {
    POSITION("a_Position"),
    TEXTURE_COORDINATE("a_TexCoordinate"),
    MVP_MATRIX("a_MVPMatrixIndex");

    private String name;

    AttributeVariable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
