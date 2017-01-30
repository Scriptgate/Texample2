package com.android.texample2.domain;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

import static android.opengl.GLES20.*;

class TextureHelper {

    public static int loadTexture(final Context context, final int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; // No pre-scaling
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        return loadTexture(bitmap);
    }

    public static int loadTexture(Bitmap bitmap) {
        final int[] textureHandle = new int[1];

        glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            glBindTexture(GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);  // Set U Wrapping
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);  // Set V Wrapping

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }
}
