package com.android.texample2.domain;


class TextureRegion {

    // Top/Left U,V Coordinates
    public float u1, v1;
    // Bottom/Right U,V Coordinates
    public float u2, v2;

    /**
     * Calculate U,V coordinates from specified texture coordinates
     * @param texWidth the width of the texture the region is for
     * @param texHeight the height of the texture the region is for
     * @param x the top/left x-coordinate of the region on the texture (in pixels)
     * @param y the top/left y-coordinate of the region on the texture (in pixels)
     * @param width the width of the region on the texture (in pixels)
     * @param height the height of the region on the texture (in pixels)
     */
    public TextureRegion(float texWidth, float texHeight, float x, float y, float width, float height) {
        this.u1 = x / texWidth;
        this.v1 = y / texHeight;
        this.u2 = this.u1 + (width / texWidth);
        this.v2 = this.v1 + (height / texHeight);
    }
}
