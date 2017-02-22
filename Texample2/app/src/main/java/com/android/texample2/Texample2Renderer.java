package com.android.texample2;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.android.texample2.domain.Font;

import static android.opengl.GLES20.*;
import static com.android.texample2.domain.Font.createGLText;

public class Texample2Renderer implements GLSurfaceView.Renderer {

    private static final String TAG = "TexampleRenderer";
    private Font font;
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

        // Create the Font

        font = createGLText()
                .assets(activityContext.getAssets())
                .font("Roboto-Regular.ttf")
                .size(60)
                .build();

        // enable texture + alpha blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void onDrawFrame(GL10 unused) {
        glClear(GL_COLOR_BUFFER_BIT);

        Matrix.multiplyMM(mVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        font.drawTexture(width, height, mVPMatrix);

        font.begin(1.0f, 1.0f, 1.0f, 1.0f, mVPMatrix);
        {
            font.startDrawing("Test String 3D!").at(0f, 0f, 0f).centerXY().rotateY((float) -30).draw();
            font.startDrawing("Diagonal 1").at(40.0f, 40.0f).rotateZ(40.0f).draw();
            font.startDrawing("Column 1").at(100.0f, 100.0f).rotateZ(90.0f).draw();
        }
        font.end();

        font.begin(0.0f, 0.0f, 1.0f, 1.0f, mVPMatrix);
        {
            font.draw("Lines...", 150, 0);
            font.draw("More Lines...", 150, -font.getScaledCharHeight());
            font.startDrawing("The End.").at(50.0f, 200.0f).rotateZ(180.0f).draw();
        }
        font.end();
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
