package com.android.texample2;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

class Triangle {

    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = glCreateShader(type);

        // add the source code to the shader and compile it
        glShaderSource(shader, shaderCode);
        glCompileShader(shader);

        return shader;
    }

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +

                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    "  gl_Position = vPosition * uMVPMatrix;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private final FloatBuffer vertexBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float triangleCoords[] = { // in counterclockwise order:
            0.0f, 50 * 0.622008459f, 0.0f,   // top
            -50 * 0.5f, -50 * 0.311004243f, 0.0f,   // bottom left
            50 * 0.5f, -50 * 0.311004243f, 0.0f    // bottom right
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    private float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    public Triangle() {
        vertexBuffer = allocateDirect(triangleCoords.length * 4).order(nativeOrder()).asFloatBuffer();
        vertexBuffer.put(triangleCoords).position(0);

        // prepare shaders and OpenGL program
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = glCreateProgram();             // create empty OpenGL Program
        glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GL_FLOAT, false, vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = glGetUniformLocation(mProgram, "uMVPMatrix");

        // Apply the projection and view transformation
        glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the triangle
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        glDisableVertexAttribArray(mPositionHandle);
    }
}