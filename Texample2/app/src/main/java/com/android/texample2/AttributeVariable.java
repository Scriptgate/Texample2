package com.android.texample2;


public enum AttributeVariable {
    POSITION(1, "a_Position"),
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
