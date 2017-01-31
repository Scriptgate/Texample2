package com.android.texample2;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.android.texample2.domain.GLText;

import static android.opengl.GLES20.*;
import static com.android.texample2.domain.GLText.createGLText;

public class Texample2Renderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TexampleRenderer";
    private GLText glText;
    private Context activityContext;

    private int width = 100;                           // Updated to the Current Width + Height in onSurfaceChanged()
    private int height = 100;
    private float[] mProjMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mVPMatrix = new float[16];

    public Texample2Renderer(Context context) {
        super();
        this.activityContext = context;
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        // Create the GLText

        glText = createGLText()
                .assets(activityContext.getAssets())
                .font("Roboto-Regular.ttf")
                .size(30)
                .padding(2,2)
                .build();

        // enable texture + alpha blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        int clearMask = GL_COLOR_BUFFER_BIT;

        glClear(clearMask);

        Matrix.multiplyMM(mVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        // TEST: render the entire font texture
        glText.drawTexture(width, height, mVPMatrix);            // Draw the Entire Texture

        // TEST: render some strings with the font
        glText.begin(1.0f, 1.0f, 1.0f, 1.0f, mVPMatrix);         // Begin Text Rendering (Set Color WHITE)
        glText.drawCentered("Test String 3D!", 0f, 0f, 0f, 0, -30, 0);
//		glText.drawCentered( "Test String :)", 0, 0, 0 );
        glText.draw("Diagonal 1", 40, 40, 40);
        glText.draw("Column 1", 100, 100, 90);
        glText.end();

        glText.begin(0.0f, 0.0f, 1.0f, 1.0f, mVPMatrix);         // Begin Text Rendering (Set Color BLUE)
        glText.draw("More Lines...", 50, 200);
        glText.draw("The End.", 50, 200 + glText.getScaledCharHeight(), 180);
        glText.end();
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        // Take into account device orientation
        if (width > height) {
            Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1, 10);
        } else {
            Matrix.frustumM(mProjMatrix, 0, -1, 1, -1 / ratio, 1 / ratio, 1, 10);
        }

        this.width = width;
        this.height = height;

        int useForOrtho = Math.min(width, height);

        //TODO: Is this wrong?
        Matrix.orthoM(mVMatrix, 0, -useForOrtho / 2, useForOrtho / 2, -useForOrtho / 2, useForOrtho / 2, 0.1f, 100f);
    }
}
