package com.texample2.android;


public enum AttributeVariable {
    POSITION(1, "a_Posit ion"),
    TEXTURE_COORDINATE(2, "a_TexCoordinate"),
    MVP_MATRIX(3, "a_MVPMatrixIndex");

    private int handle;
    private String name;

    AttributeVariable(int handle, String name) {
        this.handle = handle;
        this.name = name;
    }

    public int getHandle() {
        return handle;
    }

    public String getName() {
        return name;
    }
}
