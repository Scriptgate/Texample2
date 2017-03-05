// This is a OpenGL ES 1.0 dynamic font rendering system. It loads actual font
// files, generates a font map (texture) from them, and allows rendering of
// text strings.
//
// NOTE: the rendering portions of this class uses a sprite batcher in order
// provide decent speed rendering. Also, rendering assumes a BOTTOM-LEFT
// origin, and the (x,y) positions are relative to that, as well as the
// bottom-left of the string to render.

package com.android.texample2.domain;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.Matrix;

import static android.opengl.GLES20.*;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java8.util.stream.IntStreams.concat;
import static java8.util.stream.IntStreams.of;
import static java8.util.stream.IntStreams.rangeClosed;

public class Font {

    public final static int CHAR_START = 32;           // First Character (ASCII Code)
    public final static int CHAR_END = 126;            // Last Character (ASCII Code)
    public final static int CHAR_CNT = CHAR_END - CHAR_START + 2;  // Character Count (Including Character to use for Unknown)

    public final static int CHAR_NONE = 32;            // Character to Use for Unknown (ASCII Code)
    private final static int CHAR_UNKNOWN = (CHAR_CNT - 1);  // Index of the Unknown Character

    private final static int CHAR_BATCH_SIZE = 24;     // Number of Characters to Render Per Batch must be the same as the size of u_MVPMatrix in BatchTextProgram
    private static final String TAG = "GLTEXT";

    //--Members--//
    private SpriteBatch batch;                                 // Batch Renderer

    private int fontPadX, fontPadY;                            // Font Padding (Pixels; On Each Side, ie. Doubled on Both X+Y Axis)

    private FontMetrics metrics;

    private FontTexture fontTexture;

    private FontCharacters characters;
    private int cellWidth, cellHeight;                         // Character Cell Width/Height

    private float scaleX = 1.0f;                              // Font Scale (X Axis, Default Scale = 1 (Unscaled))
    private float scaleY = 1.0f;                              // Font Scale (Y Axis, Default Scale = 1 (Unscaled))
    private float spaceX = 0.0f;                              // Additional (X,Y Axis) Spacing (Unscaled)

    private FontProgram program;

    Font(FontProgram program) {
        this.program = program;
        batch = new SpriteBatch(CHAR_BATCH_SIZE, program);  // Create Sprite Batch (with Defined Size)
    }

    /**
     * Load font
     * this will load the specified font file, create a texture for the defined character range, and setup all required values used to render with it.
     *
     * @param typeface typeface to use.
     * @param size     Requested pixel size of font (height)
     * @param padX     Extra padding per character on X-Axis to prevent overlapping characters.
     * @param padY     Extra padding per character on Y-Axis to prevent overlapping characters.
     */
    public void load(Typeface typeface, int size, int padX, int padY) {
        fontPadX = padX;
        fontPadY = padY;

        Paint paint = setUpPaint(typeface, size);
        metrics = FontMetrics.loadFromPaint(paint);
        characters = FontCharacters.createFontCharacters(paint);

        cellWidth = (int) characters.charWidthMax + (2 * fontPadX);
        cellHeight = (int) metrics.actualHeightInPixels + (2 * fontPadY);

        fontTexture = new FontTexture(cellWidth, cellHeight);

        float xOffset = fontPadX;
        float yOffset = (cellHeight - 1) - metrics.descentInPixels - fontPadY;

        fontTexture.buildFontMap(paint, cellWidth, cellHeight, xOffset, yOffset);
    }

    private Paint setUpPaint(Typeface typeface, int size) {
        int opaqueWhite = 0xffffffff;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(size);
        paint.setColor(opaqueWhite);
        paint.setTypeface(typeface);
        return paint;
    }

    private static class FontCharacters {

        private final float[] charWidths;
        private final float charWidthMax;

        public FontCharacters(float[] charWidths, float charWidthMax) {
            this.charWidths = charWidths;
            this.charWidthMax = charWidthMax;
        }

        static FontCharacters createFontCharacters(Paint paint) {
            float[] charWidths = new float[CHAR_CNT];
            float charWidthMax = 0.0f;

            char[] characterHolder = new char[1];
            float[] widthHolder = new float[1];

            int[] allCharacters = concat(rangeClosed(CHAR_START, CHAR_END), of(CHAR_NONE)).toArray();

            int cnt = 0;
            for (int character : allCharacters) {
                characterHolder[0] = (char) character;

                paint.getTextWidths(characterHolder, 0, 1, widthHolder);
                charWidths[cnt] = widthHolder[0];

                if (charWidths[cnt] > charWidthMax) {
                    charWidthMax = charWidths[cnt];
                }
                cnt++;
            }
            return new FontCharacters(charWidths, charWidthMax);
        }

        private int getCharacterIndex(char character) {
            int index = (int) character - CHAR_START;
            if (index < 0 || index >= CHAR_CNT) {
                index = CHAR_UNKNOWN;
            }
            return index;
        }

        private float getCharacterWidth(char character) {
            int characterIndex = getCharacterIndex(character);
            return charWidths[characterIndex];
        }

    }

    public float getLength(String text) {
        float result = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            result += characters.getCharacterWidth(text.charAt(i));
        }
        result += (text.length() - 1) * spaceX;
        return result * scaleX;
    }

    public float getScaledCharHeight() {
        return metrics.actualHeightInPixels * scaleY;
    }

    //--Begin/End Text Drawing--//
    // D: call these methods before/after (respectively all draw() calls using a text instance
    //    NOTE: color is set on a per-batch basis, and fonts should be 8-bit alpha only!!!
    // A: red, green, blue - RGB values for font (default = 1.0)
    //    alpha - optional alpha value for font (default = 1.0)
    // 	  vpMatrix - View and projection matrix to use
    // R: [none]
    public void begin(float[] vpMatrix) {
        // Begin with White Opaque
        begin(1.0f, 1.0f, 1.0f, 1.0f, vpMatrix);
    }

    public void begin(float red, float green, float blue, float alpha, float[] vpMatrix) {
        initDraw(red, green, blue, alpha);
        batch.beginBatch(vpMatrix);
    }

    private void initDraw(float red, float green, float blue, float alpha) {
        glUseProgram(program.getProgramHandle()); // specify the program to use

        // set color TODO: only alpha component works, text is always black #BUG
        float[] color = {red, green, blue, alpha};
        glUniform4fv(program.getColorHandle(), 1, color, 0);
        glEnableVertexAttribArray(program.getColorHandle());

        glActiveTexture(GL_TEXTURE0);  // Set the active texture unit to texture unit 0

        fontTexture.bindTexture();

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0
        glUniform1i(program.getTextureUniformHandle(), 0);
    }

    public void end() {
        batch.endBatch();
        glDisableVertexAttribArray(program.getColorHandle());
    }

    /**
     * draw the entire font texture (NOTE: for testing purposes only)
     *
     * @param width    the width of the area to draw to. this is used to draw the texture to the top-left corner.
     * @param height   the height of the area to draw to. this is used to draw the texture to the top-left corner.
     * @param vpMatrix View and projection matrix to use
     */
    public void drawTexture(int width, int height, float[] vpMatrix) {
        initDraw(1.0f, 1.0f, 1.0f, 1.0f);

        batch.beginBatch(vpMatrix);
        {
            fontTexture.draw(batch, width, height);
        }
        batch.endBatch();
    }

    /**
     * draw text at the specified x,y position
     *
     * @param text      the string to draw
     * @param x         the x-position to draw text at (bottom left of text; including descent)
     * @param y         the y-position to draw text at (bottom left of text; including descent)
     * @param z         the z-position to draw text at (bottom left of text; including descent)
     * @param angleDegX the x-position of the angle to rotate the text
     * @param angleDegY the y-position of the angle to rotate the text
     * @param angleDegZ the z-position of the angle to rotate the text
     */
    private void draw(String text, float x, float y, float z, float angleDegX, float angleDegY, float angleDegZ) {
        x += ((cellWidth / 2.0f) - fontPadX) * scaleX;
        y += ((cellHeight / 2.0f) - fontPadY) * scaleY;

        // create a model matrix based on x, y and angleDeg
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, angleDegZ, 0, 0, 1);
        Matrix.rotateM(modelMatrix, 0, angleDegX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, angleDegY, 0, 1, 0);

        float xOffset = 0;

        for (int i = 0; i < text.length(); i++) {              // FOR Each Character in String
            //TODO: optimize - applying the same model matrix to all the characters in the string
            batch.drawSprite(xOffset, 0.0f, cellWidth * scaleX, cellHeight * scaleY, fontTexture.getTextureCoordinates(characters.getCharacterIndex(text.charAt(i))), modelMatrix);
            xOffset += (characters.getCharacterWidth(text.charAt(i)) + spaceX) * scaleX;
        }
    }

    public TextBuilder startDrawing(String text) {
        return new TextBuilder(this, text);
    }

    public static class TextBuilder {

        private final float length;
        private Font font;
        private String text;
        private float x = 0.0f;
        private float y = 0.0f;
        private float z;
        private float angleDegX = 0.0f;
        private float angleDegY = 0.0f;
        private float angleDegZ = 0.0f;

        public TextBuilder(Font font, String text) {
            this.font = font;
            this.text = text;
            this.length = font.getLength(text);
        }

        public TextBuilder at(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public TextBuilder rotateZ(float angleDegZ) {
            this.angleDegZ = angleDegZ;
            return this;
        }

        public float draw() {
            font.draw(text, x, y, z, angleDegX, angleDegY, angleDegZ);
            return length;
        }

        public TextBuilder rotate(float angleDegX, float angleDegY, float angleDegZ) {
            this.angleDegX = angleDegX;
            this.angleDegY = angleDegY;
            this.angleDegZ = angleDegZ;
            return this;
        }

        public TextBuilder at(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public TextBuilder centerXY() {
            x -= (length / 2.0f);
            y -= (font.getScaledCharHeight() / 2.0f);
            return this;
        }

        public TextBuilder rotateY(float angleDegY) {
            this.angleDegY = angleDegY;
            return this;
        }
    }

    public void draw(String text, float x, float y) {

        draw(text, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static class FontMetrics {

        private final float actualHeightInPixels;
        private final float ascentInPixels;
        private final float descentInPixels;

        public FontMetrics(float actualHeightInPixels, float ascentInPixels, float descentInPixels) {
            this.actualHeightInPixels = actualHeightInPixels;
            this.ascentInPixels = ascentInPixels;
            this.descentInPixels = descentInPixels;
        }

        public static FontMetrics loadFromPaint(Paint paint) {
            Paint.FontMetrics fm = paint.getFontMetrics();
            float height = (float) ceil(abs(fm.bottom) + abs(fm.top));
            float ascent = (float) ceil(abs(fm.ascent));
            float descent = (float) ceil(abs(fm.descent));
            return new FontMetrics(height, ascent, descent);
        }
    }
}
