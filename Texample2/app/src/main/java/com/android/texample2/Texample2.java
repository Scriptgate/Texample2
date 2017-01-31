// This is based on the OpenGL ES 1.0 sample application from the Android Developer website:
// http://developer.android.com/resources/tutorials/opengl/opengl-es10.html

package com.android.texample2;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class Texample2 extends Activity {
    private GLSurfaceView glView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        glView = new Texample2SurfaceView(this);
        setContentView(glView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        deallocateMemory();
        glView.onPause();
    }

    private void deallocateMemory() {
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
    }

    @Override
    protected void onResume() {
        super.onResume();
        allocateMemory();

        glView.onResume();
    }

    private void allocateMemory() {
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
    }
}

