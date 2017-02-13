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

import com.android.texample2.programs.Program;

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

    private float charWidthMax;                                // Character Width (Maximum; Pixels)
    private float[] charWidths;                          // Width of Each Character (Actual; Pixels)
    private int cellWidth, cellHeight;                         // Character Cell Width/Height

    private float scaleX = 1.0f;                              // Font Scale (X Axis, Default Scale = 1 (Unscaled))
    private float scaleY = 1.0f;                              // Font Scale (Y Axis, Default Scale = 1 (Unscaled))
    private float spaceX = 0.0f;                              // Additional (X,Y Axis) Spacing (Unscaled)

    private Program program;                           // OpenGL Program object
    private int colorHandle;                           // Shader color handle
    private int textureUniformHandle;                 // Shader texture handle

    public static FontBuilder createGLText() {
        return new FontBuilder();
    }

    Font(Program program) {

        batch = new SpriteBatch(CHAR_BATCH_SIZE, program);  // Create Sprite Batch (with Defined Size)
        // Initialize the color and texture handles
        this.program = program;
        colorHandle = glGetUniformLocation(this.program.getHandle(), "u_Color");
        textureUniformHandle = glGetUniformLocation(this.program.getHandle(), "u_Texture");
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

        // load the font and setup paint instance for drawing
        Paint paint = setUpPaint(typeface, size);

        // get font metrics
        metrics = FontMetrics.loadFromPaint(paint);

        //determine the width of each character (including unknown character) also determine the maximum character width
        charWidths = new float[CHAR_CNT];
        charWidthMax = calculateCharWidths(paint, charWidths);

        // find the maximum size, validate, and setup cell sizes
        cellWidth = (int) charWidthMax + (2 * fontPadX);
        cellHeight = (int) metrics.height + (2 * fontPadY);

        float xOffset = fontPadX;
        float yOffset = (cellHeight - 1) - metrics.descent - fontPadY;

        fontTexture = new FontTexture(paint, cellWidth, cellHeight, xOffset, yOffset);
    }

    private Paint setUpPaint(Typeface typeface, int size) {
        Paint paint = new Paint();                      // Create Android Paint Instance
        paint.setAntiAlias(true);                     // Enable Anti Alias
        paint.setTextSize(size);                      // Set Text Size
        paint.setColor(0xffffffff);                   // Set ARGB (White, Opaque)
        paint.setTypeface(typeface);                        // Set Typeface
        return paint;
    }

    private float calculateCharWidths(Paint paint, float[] charWidths) {
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
        return charWidthMax;
    }


    /**
     * set the scaling to use for the font
     *
     * @param scale uniform scale for both x and y axis scaling
     */
    public void setScale(float scale) {
        scaleX = scaleY = scale;
    }

    /**
     * set the scaling to use for the font
     *
     * @param sx x axis scale
     * @param sy y axis scale
     */
    public void setScale(float sx, float sy) {
        scaleX = sx;
        scaleY = sy;
    }

    /**
     * get the current x scale used for the font
     *
     * @return the x scale currently used for scale
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * get the current y scale used for the font
     *
     * @return the y scale currently used for scale
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * set the spacing (unscaled; ie. pixel size) to use for the font
     *
     * @param space for x axis spacing
     */
    public void setSpace(float space) {
        spaceX = space;
    }

    /**
     * get the current spacing used for the font
     *
     * @return the x space currently used for scale
     */
    public float getSpace() {
        return spaceX;
    }

    /**
     * return the length of the specified string if rendered using current settings
     *
     * @param text the string to get length for
     * @return the length of the specified string (pixels)
     */
    public float getLength(String text) {
        float result = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            result += getScaledCharacterWidth(text.charAt(i));
        }
        result += (text.length() - 1) * spaceX * scaleX;
        return result;
    }

    /**
     * return the scaled width of a character, or max character width
     * NOTE: excludes spacing!!
     *
     * @param chr the character to get width for
     * @return the requested character width (scaled)
     */
    public float getScaledCharacterWidth(char chr) {
        return getCharacterWidth(chr) * scaleX;
    }

    public float getScaledMaxCharacterWidth() {
        return charWidthMax * scaleX;
    }

    public float getScaledCharHeight() {
        return metrics.height * scaleY;
    }

    public float getAscent() {
        return metrics.ascent * scaleY;
    }

    public float getDescent() {
        return metrics.descent * scaleY;
    }

    public float getActualHeight() {
        return (metrics.height * scaleY);
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
        glUseProgram(program.getHandle()); // specify the program to use

        // set color TODO: only alpha component works, text is always black #BUG
        float[] color = {red, green, blue, alpha};
        glUniform4fv(colorHandle, 1, color, 0);
        glEnableVertexAttribArray(colorHandle);

        glActiveTexture(GL_TEXTURE0);  // Set the active texture unit to texture unit 0

        fontTexture.bindTexture();

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0
        glUniform1i(textureUniformHandle, 0);
    }

    public void end() {
        batch.endBatch();
        glDisableVertexAttribArray(colorHandle);
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
            int characterIndex = getCharacterIndex(text.charAt(i));
            //TODO: optimize - applying the same model matrix to all the characters in the string
            batch.drawSprite(xOffset, 0.0f, cellWidth * scaleX, cellHeight * scaleY, fontTexture.getTextureCoordinates(characterIndex), modelMatrix);
            xOffset += (charWidths[characterIndex] + spaceX) * scaleX;
        }
    }

    private float getCharacterWidth(char character) {
        int characterIndex = getCharacterIndex(character);
        return charWidths[characterIndex];
    }

    private int getCharacterIndex(char character) {
        int index = (int) character - CHAR_START;
        if (index < 0 || index >= CHAR_CNT) {
            index = CHAR_UNKNOWN;
        }
        return index;
    }

    private void draw(String text, float x, float y, float z, float angleDegZ) {
        draw(text, x, y, z, 0, 0, angleDegZ);
    }

    public void draw(String text, float x, float y, float angleDeg) {
        draw(text, x, y, 0, angleDeg);
    }

    public void draw(String text, float x, float y) {
        draw(text, x, y, 0, 0);
    }

    /**
     * draw text CENTERED at the specified x,y position
     *
     * @param text      the string to draw
     * @param x         the x position to draw text at (bottom left of text)
     * @param y         the y position to draw text at (bottom left of text)
     * @param z         the z position to draw text at (bottom left of text)
     * @param angleDegX the x position of the angle to rotate the text
     * @param angleDegY the y position of the angle to rotate the text
     * @param angleDegZ the z position of the angle to rotate the text
     * @return the total width of the text that was drawn
     */
    public float drawCentered(String text, float x, float y, float z, float angleDegX, float angleDegY, float angleDegZ) {
        float textLength = getLength(text);
        float centeredX = x - (textLength / 2.0f);
        float centeredY = y - (getScaledCharHeight() / 2.0f);
        draw(text, centeredX, centeredY, z, angleDegX, angleDegY, angleDegZ);  // Draw Text Centered
        return textLength;
    }

    private float drawCentered(String text, float x, float y, float z, float angleDegZ) {
        return drawCentered(text, x, y, z, 0, 0, angleDegZ);
    }

    private float drawCentered(String text, float x, float y, float angleDeg) {
        return drawCentered(text, x, y, 0, angleDeg);
    }

    private float drawCentered(String text, float x, float y) {
        float textLength = getLength(text);                  // Get Text Length
        float centeredX = x - (textLength / 2.0f);
        float centeredY = y - (getScaledCharHeight() / 2.0f);
        return drawCentered(text, centeredX, centeredY, 0);

    }

    private float drawCenteredOnXAxis(String text, float x, float y) {
        float textLength = getLength(text);
        float centeredX = x - (textLength / 2.0f);
        draw(text, centeredX, y);
        return textLength;
    }

    private void drawCenteredOnYAxis(String text, float x, float y) {
        float centeredY = y - (getScaledCharHeight() / 2.0f);
        draw(text, x, centeredY);
    }

    private static class FontMetrics {

        // Font Height (Actual; Pixels)
        private final float height;
        // Font Ascent (Above Baseline; Pixels)
        private final float ascent;
        // Font Descent (Below Baseline; Pixels)
        private final float descent;

        public FontMetrics(float height, float ascent, float descent) {
            this.height = height;
            this.ascent = ascent;
            this.descent = descent;
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
