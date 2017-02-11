package com.android.texample2.domain;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.Matrix;

import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glBindTexture;
import static com.android.texample2.domain.Font.CHAR_CNT;
import static com.android.texample2.domain.Font.CHAR_END;
import static com.android.texample2.domain.Font.CHAR_NONE;
import static com.android.texample2.domain.Font.CHAR_START;
import static java.lang.Math.ceil;

class FontTexture {

    private final static int FONT_SIZE_MIN = 6;         // Minumum Font Size (Pixels)
    private final static int FONT_SIZE_MAX = 180;       // Maximum Font Size (Pixels)

    // Texture Size for Font (Square)
    private int size;
    // Number of Rows/Columns
    private int rowCnt, colCnt;
    // Full Texture Region
    private TextureRegion region;
    // Font Texture ID
    private int textureId;
    // Region of Each Character (Texture Coordinates)
    private TextureRegion[] textureCoordinates;

    public FontTexture(Paint paint, int cellWidth, int cellHeight, float xOffset, float yOffset) {
        //get texture size based on max font size (width or height)
        this.size = calculateTextureSize(cellWidth, cellHeight);
        // calculate rows/columns
        // NOTE: while not required for anything, these may be useful to have :)
        colCnt = this.size / cellWidth;
        rowCnt = (int) ceil((float) CHAR_CNT / (float) colCnt);

        // create full texture region
        region = new TextureRegion(size, size, 0, 0, size, size);

        textureId = buildFontMap(paint, size, cellWidth, cellHeight, xOffset, yOffset);

        // setup the array of character texture regions
        textureCoordinates = initializeTextureCoordinates(cellWidth, cellHeight);
    }

    private int calculateTextureSize(int cellWidth, int cellHeight) {
        int maxSize = cellWidth > cellHeight ? cellWidth : cellHeight;  // Save Max Size (Width/Height)
        if (maxSize < FONT_SIZE_MIN || maxSize > FONT_SIZE_MAX) {  // IF Maximum Size Outside Valid Bounds
            throw new IllegalArgumentException("Invalid cell size: [width: " + cellWidth + ", height: " + cellHeight + "], bounds: [minimum: " + FONT_SIZE_MIN + ", maximum: " + FONT_SIZE_MAX + "]");
        }
        return calculateTextureSize(maxSize);
    }

    private int calculateTextureSize(int maxSize) {
        // NOTE: these values are fixed, based on the defined characters. when
        // changing start/end characters (CHAR_START/CHAR_END) this will need adjustment too!
        if (maxSize <= 24) {
            return 256;
        } else if (maxSize <= 40) {
            return 512;
        } else if (maxSize <= 80) {
            return 1024;
        } else {
            return 2048;
        }
    }

    private int buildFontMap(Paint paint, int textureSize, int cellWidth, int cellHeight, float xOffset, float yOffset) {
        Bitmap bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ALPHA_8);
        bitmap.eraseColor(0x00000000);

        Canvas canvas = new Canvas(bitmap);
        float x = xOffset;
        float y = yOffset;

        char[] characterArray = new char[1];
        for (char c = CHAR_START; c <= CHAR_END; c++) {
            characterArray[0] = c;
            canvas.drawText(characterArray, 0, 1, x, y, paint);
            x += cellWidth;
            if ((x + cellWidth - xOffset) > textureSize) {
                x = xOffset;
                y += cellHeight;
            }
        }
        characterArray[0] = CHAR_NONE;
        canvas.drawText(characterArray, 0, 1, x, y, paint);

        return TextureHelper.loadTexture(bitmap);
    }

    private TextureRegion[] initializeTextureCoordinates(int cellWidth, int cellHeight) {
        TextureRegion[] textureCoordinates = new TextureRegion[CHAR_CNT];
        float x = 0;
        float y = 0;
        for (int characterIndex = 0; characterIndex < CHAR_CNT; characterIndex++) {
            textureCoordinates[characterIndex] = new TextureRegion(size, size, x, y, cellWidth - 1, cellHeight - 1);
            x += cellWidth;
            if (x + cellWidth > size) {
                x = 0;
                y += cellHeight;
            }
        }
        return textureCoordinates;
    }

    public void draw(SpriteBatch batch, int width, int height) {
        float[] idMatrix = new float[16];
        Matrix.setIdentityM(idMatrix, 0);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        batch.drawSprite(x, y, size, size, region, idMatrix);
    }

    public TextureRegion getTextureCoordinates(int characterIndex) {
        return textureCoordinates[characterIndex];
    }

    public void bindTexture() {
        // Bind the texture to this unit
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
}
