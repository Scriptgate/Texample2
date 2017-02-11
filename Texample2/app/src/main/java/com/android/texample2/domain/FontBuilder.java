package com.android.texample2.domain;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import com.android.texample2.programs.BatchTextProgram;
import com.android.texample2.programs.Program;

public class FontBuilder {

    private Program program;
    private AssetManager assets;
    private String fontFile;
    private int size;
    private int paddingX = 0;
    private int paddingY = 0;

    FontBuilder() {
        program = new BatchTextProgram();
        program.init();
    }

    public Font build() {
        Font font = new Font(program);
        // Load the font from file (set size + padding), creates the texture
        // NOTE: after a successful call to this the font is ready for rendering!
        Typeface typeface = Typeface.createFromAsset(assets, fontFile);  // Create the Typeface from Font File
        font.load(typeface, size, paddingX, paddingY);
        return font;
    }

    public FontBuilder assets(AssetManager assets) {
        this.assets = assets;
        return this;
    }

    public FontBuilder font(String fontFile) {
        this.fontFile = fontFile;
        return this;
    }

    public FontBuilder size(int size) {
        this.size = size;
        return this;
    }

    public FontBuilder padding(int paddingX, int paddingY) {
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        return this;
    }
}
