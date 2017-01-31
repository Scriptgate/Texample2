// This is a OpenGL ES 1.0 dynamic font rendering system. It loads actual font
// files, generates a font map (texture) from them, and allows rendering of
// text strings.
//
// NOTE: the rendering portions of this class uses a sprite batcher in order
// provide decent speed rendering. Also, rendering assumes a BOTTOM-LEFT
// origin, and the (x,y) positions are relative to that, as well as the
// bottom-left of the string to render.

package com.android.texample2.domain;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.Matrix;

import com.android.texample2.programs.BatchTextProgram;
import com.android.texample2.programs.Program;

import static android.opengl.GLES20.*;

public class GLText {

    private final static int CHAR_START = 32;           // First Character (ASCII Code)
    private final static int CHAR_END = 126;            // Last Character (ASCII Code)
    private final static int CHAR_CNT = (((CHAR_END - CHAR_START) + 1) + 1);  // Character Count (Including Character to use for Unknown)

    private final static int CHAR_NONE = 32;            // Character to Use for Unknown (ASCII Code)
    private final static int CHAR_UNKNOWN = (CHAR_CNT - 1);  // Index of the Unknown Character

    private final static int FONT_SIZE_MIN = 6;         // Minumum Font Size (Pixels)
    private final static int FONT_SIZE_MAX = 180;       // Maximum Font Size (Pixels)

    private final static int CHAR_BATCH_SIZE = 24;     // Number of Characters to Render Per Batch must be the same as the size of u_MVPMatrix in BatchTextProgram
    private static final String TAG = "GLTEXT";

    //--Members--//
    private AssetManager assets;                               // Asset Manager
    private SpriteBatch batch;                                 // Batch Renderer

    private int fontPadX, fontPadY;                            // Font Padding (Pixels; On Each Side, ie. Doubled on Both X+Y Axis)

    private float fontHeight;                                  // Font Height (Actual; Pixels)
    private float fontAscent;                                  // Font Ascent (Above Baseline; Pixels)
    private float fontDescent;                                 // Font Descent (Below Baseline; Pixels)

    private int textureId;                                     // Font Texture ID [NOTE: Public for Testing Purposes Only!]
    private int textureSize;                                   // Texture Size for Font (Square) [NOTE: Public for Testing Purposes Only!]
    private TextureRegion textureRgn;                          // Full Texture Region

    private float charWidthMax;                                // Character Width (Maximum; Pixels)
    private float charHeight;                                  // Character Height (Maximum; Pixels)
    private final float[] charWidths;                          // Width of Each Character (Actual; Pixels)
    private TextureRegion[] charRgn;                           // Region of Each Character (Texture Coordinates)
    private int cellWidth, cellHeight;                         // Character Cell Width/Height
    private int rowCnt, colCnt;                                // Number of Rows/Columns

    private float scaleX, scaleY;                              // Font Scale (X,Y Axis)
    private float spaceX;                                      // Additional (X,Y Axis) Spacing (Unscaled)

    private Program mProgram;                           // OpenGL Program object
    private int mColorHandle;                           // Shader color handle
    private int mTextureUniformHandle;                 // Shader texture handle

    public static GLTextBuilder createGLText() {
        return new GLTextBuilder();
    }

    /**
     * save program + asset manager, create arrays, and initialize the members
     *
     * @param program
     * @param assets
     */
    public GLText(Program program, AssetManager assets) {
        this.assets = assets;                           // Save the Asset Manager Instance

        batch = new SpriteBatch(CHAR_BATCH_SIZE, program);  // Create Sprite Batch (with Defined Size)

        charWidths = new float[CHAR_CNT];               // Create the Array of Character Widths
        charRgn = new TextureRegion[CHAR_CNT];          // Create the Array of Character Regions

        // initialize remaining members
        fontPadX = 0;
        fontPadY = 0;

        fontHeight = 0.0f;
        fontAscent = 0.0f;
        fontDescent = 0.0f;

        textureId = -1;
        textureSize = 0;

        charWidthMax = 0;
        charHeight = 0;

        cellWidth = 0;
        cellHeight = 0;
        rowCnt = 0;
        colCnt = 0;

        scaleX = 1.0f;                                  // Default Scale = 1 (Unscaled)
        scaleY = 1.0f;                                  // Default Scale = 1 (Unscaled)
        spaceX = 0.0f;

        // Initialize the color and texture handles
        mProgram = program;
        mColorHandle = glGetUniformLocation(mProgram.getHandle(), "u_Color");
        mTextureUniformHandle = glGetUniformLocation(mProgram.getHandle(), "u_Texture");
    }

    /**
     * Load font
     * this will load the specified font file, create a texture for the defined character range, and setup all required values used to render with it.
     *
     * @param file Filename of the font (.ttf, .otf) to use. In 'Assets' folder.
     * @param size Requested pixel size of font (height)
     * @param padX Extra padding per character on X-Axis to prevent overlapping characters.
     * @param padY Extra padding per character on Y-Axis to prevent overlapping characters.
     * @return true if successful, false otherwise
     */
    public boolean load(String file, int size, int padX, int padY) {
        fontPadX = padX;
        fontPadY = padY;

        // load the font and setup paint instance for drawing
        Paint paint = setUpPaint(file, size);

        // get font metrics
        loadFontMetrics(paint);

        //determine the width of each character (including unknown character) also determine the maximum character width
        determineCharacterWidths(paint);

        // set character height to font height
        charHeight = fontHeight;                        // Set Character Height

        // find the maximum size, validate, and setup cell sizes
        cellWidth = (int) charWidthMax + (2 * fontPadX);  // Set Cell Width
        cellHeight = (int) charHeight + (2 * fontPadY);  // Set Cell Height
        int maxSize = cellWidth > cellHeight ? cellWidth : cellHeight;  // Save Max Size (Width/Height)
        if (maxSize < FONT_SIZE_MIN || maxSize > FONT_SIZE_MAX) {  // IF Maximum Size Outside Valid Bounds
            return false;                                // Return Error
        }

//      get texture size based on max font size (width or height)
        textureSize = calculateTextureSize(maxSize);


        // calculate rows/columns
        // NOTE: while not required for anything, these may be useful to have :)
        colCnt = textureSize / cellWidth;               // Calculate Number of Columns
        rowCnt = (int) Math.ceil((float) CHAR_CNT / (float) colCnt);  // Calculate Number of Rows


        buildFontMap(paint);
        // setup the array of character texture regions
        createTextureRegions();
        // create full texture region
        textureRgn = new TextureRegion(textureSize, textureSize, 0, 0, textureSize, textureSize);  // Create Full Texture Region

        return true;
    }

    private void loadFontMetrics(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();  // Get Font Metrics
        fontHeight = (float) Math.ceil(Math.abs(fm.bottom) + Math.abs(fm.top));  // Calculate Font Height
        fontAscent = (float) Math.ceil(Math.abs(fm.ascent));  // Save Font Ascent
        fontDescent = (float) Math.ceil(Math.abs(fm.descent));  // Save Font Descent
    }

    private Paint setUpPaint(String file, int size) {
        Typeface tf = Typeface.createFromAsset(assets, file);  // Create the Typeface from Font File
        Paint paint = new Paint();                      // Create Android Paint Instance
        paint.setAntiAlias(true);                     // Enable Anti Alias
        paint.setTextSize(size);                      // Set Text Size
        paint.setColor(0xffffffff);                   // Set ARGB (White, Opaque)
        paint.setTypeface(tf);                        // Set Typeface
        return paint;
    }

    private void createTextureRegions() {
        float x = 0;
        float y = 0;
        for (int c = 0; c < CHAR_CNT; c++) {         // FOR Each Character (On Texture)
            charRgn[c] = new TextureRegion(textureSize, textureSize, x, y, cellWidth - 1, cellHeight - 1);  // Create Region for Character
            x += cellWidth;                              // Move to Next Char (Cell)
            if (x + cellWidth > textureSize) {
                x = 0;                                    // Reset X Position to Start
                y += cellHeight;                          // Move to Next Row (Cell)
            }
        }
    }

    private void determineCharacterWidths(Paint paint) {
        char[] characterArray = new char[1];                         // Create Character Array
        charWidthMax = charHeight = 0;                  // Reset Character Width/Height Maximums
        float[] workingWidth = new float[2];                       // Working Width Value
        int cnt = 0;                                    // Array Counter
        for (char c = CHAR_START; c <= CHAR_END; c++) {  // FOR Each Character
            characterArray[0] = c;                                    // Set Character
            paint.getTextWidths(characterArray, 0, 1, workingWidth);           // Get Character Bounds
            charWidths[cnt] = workingWidth[0];                      // Get Width
            if (charWidths[cnt] > charWidthMax)        // IF Width Larger Than Max Width
                charWidthMax = charWidths[cnt];           // Save New Max Width
            cnt++;                                       // Advance Array Counter
        }
        characterArray[0] = CHAR_NONE;                               // Set Unknown Character
        paint.getTextWidths(characterArray, 0, 1, workingWidth);              // Get Character Bounds
        charWidths[cnt] = workingWidth[0];                         // Get Width
        if (charWidths[cnt] > charWidthMax) {           // IF Width Larger Than Max Width
            charWidthMax = charWidths[cnt];              // Save New Max Width
        }
    }

    private void buildFontMap(Paint paint) {
        char[] characterArray = new char[1];
        Bitmap bitmap = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);
        bitmap.eraseColor(0x00000000);

        float x = fontPadX;                             // Set Start Position (X)
        float y = (cellHeight - 1) - fontDescent - fontPadY;  // Set Start Position (Y)
        for (char c = CHAR_START; c <= CHAR_END; c++) {  // FOR Each Character
            characterArray[0] = c;                                    // Set Character to Draw
            canvas.drawText(characterArray, 0, 1, x, y, paint);     // Draw Character
            x += cellWidth;                              // Move to Next Character
            if ((x + cellWidth - fontPadX) > textureSize) {  // IF End of Line Reached
                x = fontPadX;                             // Set X for New Row
                y += cellHeight;                          // Move Down a Row
            }
        }
        characterArray[0] = CHAR_NONE;                               // Set Character to Use for NONE
        canvas.drawText(characterArray, 0, 1, x, y, paint);        // Draw Character

        // save the bitmap in a texture
        textureId = TextureHelper.loadTexture(bitmap);
    }

    private int calculateTextureSize(int maxSize) {
        // NOTE: these values are fixed, based on the defined characters. when
        // changing start/end characters (CHAR_START/CHAR_END) this will need adjustment too!
        if (maxSize <= 24) {
             return 256;
        } else if (maxSize <= 40) {
            return  512;
        } else if (maxSize <= 80) {
            return  1024;
        } else {
            return 2048;
        }
    }

    /**
     * set the scaling to use for the font
     * @param scale uniform scale for both x and y axis scaling
     */
    public void setScale(float scale) {
        scaleX = scaleY = scale;
    }

    /**
     * set the scaling to use for the font
     * @param sx x axis scale
     * @param sy y axis scale
     */
    public void setScale(float sx, float sy) {
        scaleX = sx;
        scaleY = sy;
    }

    //--Get Scale--//
    // D: get the current scaling used for the font
    // A: [none]
    // R: the x/y scale currently used for scale
    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    /**
     * set the spacing (unscaled; ie. pixel size) to use for the font
     * @param space for x axis spacing
     */
    public void setSpace(float space) {
        spaceX = space;
    }

    /**
     * get the current spacing used for the font
     * @return the x space currently used for scale
     */
    public float getSpace() {
        return spaceX;
    }

    /**
     * return the length of the specified string if rendered using current settings
     * @param text the string to get length for
     * @return the length of the specified string (pixels)
     */
    public float getLength(String text) {
        float result = 0.0f;
        int numberOfCharacters = text.length();
        for (int i = 0; i < numberOfCharacters; i++) {           // For Each Character in String (Except Last
            int characterIndex = (int) text.charAt(i) - CHAR_START;  // Calculate Character Index (Offset by First Char in Font)
            result += (charWidths[characterIndex] * scaleX);           // Add Scaled Character Width to Total Length
        }
        result += (numberOfCharacters > 1 ? ((numberOfCharacters - 1) * spaceX) * scaleX : 0);  // Add Space Length
        return result;                                     // Return Total Length
    }

    //--Get Width/Height of Character--//
    // D: return the scaled width/height of a character, or max character width
    //    NOTE: since all characters are the same height, no character index is required!
    //    NOTE: excludes spacing!!
    // A: chr - the character to get width for
    // R: the requested character size (scaled)
    public float getCharWidth(char chr) {
        int characterIndex = chr - CHAR_START;                       // Calculate Character Index (Offset by First Char in Font)
        return charWidths[characterIndex] * scaleX;              // Return Scaled Character Width
    }

    public float getScaledMaxCharWidth() {
        return charWidthMax * scaleX;
    }

    public float getScaledCharHeight() {
        return charHeight * scaleY;
    }

    //--Get Font Metrics--//
    // D: return the specified (scaled) font metric
    // A: [none]
    // R: the requested font metric (scaled)
    public float getAscent() {
        return fontAscent * scaleY;
    }

    public float getDescent() {
        return fontDescent * scaleY;
    }

    public float getActualHeight() {
        return (fontHeight * scaleY);
    }

    //--Begin/End Text Drawing--//
    // D: call these methods before/after (respectively all draw() calls using a text instance
    //    NOTE: color is set on a per-batch basis, and fonts should be 8-bit alpha only!!!
    // A: red, green, blue - RGB values for font (default = 1.0)
    //    alpha - optional alpha value for font (default = 1.0)
    // 	  vpMatrix - View and projection matrix to use
    // R: [none]
    public void begin(float[] vpMatrix) {
        begin(1.0f, 1.0f, 1.0f, 1.0f, vpMatrix);                // Begin with White Opaque
    }

    public void begin(float alpha, float[] vpMatrix) {
        begin(1.0f, 1.0f, 1.0f, alpha, vpMatrix);               // Begin with White (Explicit Alpha)
    }

    public void begin(float red, float green, float blue, float alpha, float[] vpMatrix) {
        initDraw(red, green, blue, alpha);
        batch.beginBatch(vpMatrix);
    }

    private void initDraw(float red, float green, float blue, float alpha) {
        glUseProgram(mProgram.getHandle()); // specify the program to use

        // set color TODO: only alpha component works, text is always black #BUG
        float[] color = {red, green, blue, alpha};
        glUniform4fv(mColorHandle, 1, color, 0);
        glEnableVertexAttribArray(mColorHandle);

        glActiveTexture(GL_TEXTURE0);  // Set the active texture unit to texture unit 0

        glBindTexture(GL_TEXTURE_2D, textureId); // Bind the texture to this unit

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0
        glUniform1i(mTextureUniformHandle, 0);
    }

    public void end() {
        batch.endBatch();
        glDisableVertexAttribArray(mColorHandle);
    }

    /**
     * draw the entire font texture (NOTE: for testing purposes only)
     * @param width the width of the area to draw to. this is used to draw the texture to the top-left corner.
     * @param height the height of the area to draw to. this is used to draw the texture to the top-left corner.
     * @param vpMatrix View and projection matrix to use
     */
    public void drawTexture(int width, int height, float[] vpMatrix) {
        initDraw(1.0f, 1.0f, 1.0f, 1.0f);

        batch.beginBatch(vpMatrix);
        {
            float[] idMatrix = new float[16];
            Matrix.setIdentityM(idMatrix, 0);
            batch.drawSprite((width - textureSize) / 2, (height - textureSize) / 2, textureSize, textureSize, textureRgn, idMatrix);
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
        float scaledCharacterHeight = cellHeight * scaleY;
        float scaledCharacterWidth = cellWidth * scaleX;
        int numberOfCharacters = text.length();
        x += (scaledCharacterWidth / 2.0f) - (fontPadX * scaleX);  // Adjust Start X
        y += (scaledCharacterHeight / 2.0f) - (fontPadY * scaleY);  // Adjust Start Y

        // create a model matrix based on x, y and angleDeg
        float[] modelMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z);
        Matrix.rotateM(modelMatrix, 0, angleDegZ, 0, 0, 1);
        Matrix.rotateM(modelMatrix, 0, angleDegX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, angleDegY, 0, 1, 0);

        float letterX, letterY;
        letterX = letterY = 0;

        for (int i = 0; i < numberOfCharacters; i++) {              // FOR Each Character in String
            int c = (int) text.charAt(i) - CHAR_START;  // Calculate Character Index (Offset by First Char in Font)
            if (c < 0 || c >= CHAR_CNT)                // IF Character Not In Font
                c = CHAR_UNKNOWN;                         // Set to Unknown Character Index
            //TODO: optimize - applying the same model matrix to all the characters in the string
            batch.drawSprite(letterX, letterY, scaledCharacterWidth, scaledCharacterHeight, charRgn[c], modelMatrix);  // Draw the Character
            letterX += (charWidths[c] + spaceX) * scaleX;    // Advance X Position by Scaled Character Width
        }
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
     * @param text the string to draw
     * @param x the x position to draw text at (bottom left of text)
     * @param y the y position to draw text at (bottom left of text)
     * @param z the z position to draw text at (bottom left of text)
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

    public static class GLTextBuilder {

        private Program program;
        private AssetManager assets;
        private String fontFile;
        private int size;
        private int paddingX;
        private int paddingY;

        private GLTextBuilder() {
            program = new BatchTextProgram();
            program.init();
        }

        public GLText build() {
            GLText font = new GLText(program, assets);
            // Load the font from file (set size + padding), creates the texture
            // NOTE: after a successful call to this the font is ready for rendering!
            font.load(fontFile, size, paddingX, paddingY);
            return font;
        }

        public GLTextBuilder assets(AssetManager assets) {
            this.assets = assets;
            return this;
        }

        public GLTextBuilder font(String fontFile) {
            this.fontFile = fontFile;
            return this;
        }

        public GLTextBuilder size(int size) {
            this.size = size;
            return this;
        }

        public GLTextBuilder padding(int paddingX, int paddingY) {
            this.paddingX = paddingX;
            this.paddingY = paddingY;
            return this;
        }
    }
}
