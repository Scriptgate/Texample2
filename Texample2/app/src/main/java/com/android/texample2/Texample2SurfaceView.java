package com.android.texample2;

import android.content.Context;
import android.opengl.GLSurfaceView;

class Texample2SurfaceView extends GLSurfaceView {

    public Texample2SurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        setRenderer(new Texample2Renderer(context));
    }
}
