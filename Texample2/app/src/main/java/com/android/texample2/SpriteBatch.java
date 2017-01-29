package com.android.texample2;

import com.android.texample2.programs.Program;

import android.opengl.Matrix;

import java.util.List;

import static android.opengl.GLES20.*;
import static java.util.Arrays.asList;

public class SpriteBatch {

    private final static int VERTEX_SIZE = 5;                  // Vertex Size (in Components) ie. (X,Y,U,V,M), M is MVP matrix index
    private final static int VERTICES_PER_SPRITE = 4;          // Vertices Per Sprite
    private final static int INDICES_PER_SPRITE = 6;           // Indices Per Sprite
    private static final String TAG = "SpriteBatch";

    private Vertices vertices;                                 // Vertices Instance Used for Rendering
    private float[] vertexBuffer;                              // Vertex Buffer
    private int bufferIndex;                                   // Vertex Buffer Start Index
    private int maxSprites;                                    // Maximum Sprites Allowed in Buffer
    private int numSprites;                                    // Number of Sprites Currently in Buffer
    private float[] viewProjectionMatrix;                            // View and projection matrix specified at begin
    private float[] uMVPMatrices = new float[GLText.CHAR_BATCH_SIZE * 16]; // MVP matrix array to pass to shader
    private int mMVPMatricesHandle;                            // shader handle of the MVP matrix array
    private float[] mMVPMatrix = new float[16];                // used to calculate MVP matrix of each sprite


    /**
     * Prepare the sprite batcher for specified maximum number of sprites
     *
     * @param maxSprites the maximum allowed sprites per batch
     * @param program    program to use when drawing
     */
    public SpriteBatch(int maxSprites, Program program) {
        this.vertexBuffer = new float[maxSprites * VERTICES_PER_SPRITE * VERTEX_SIZE];  // Create Vertex Buffer
        this.vertices = new Vertices(maxSprites * VERTICES_PER_SPRITE, maxSprites * INDICES_PER_SPRITE);  // Create Rendering Vertices
        this.bufferIndex = 0;                           // Reset Buffer Index
        this.maxSprites = maxSprites;                   // Save Maximum Sprites
        this.numSprites = 0;                            // Clear Sprite Counter

        short[] indices = new short[maxSprites * INDICES_PER_SPRITE];  // Create Temp Index Buffer
        int len = indices.length;                       // Get Index Buffer Length
        short j = 0;                                    // Counter
        for (int i = 0; i < len; i += INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE) {  // FOR Each Index Set (Per Sprite)
            indices[i] = j;           // Calculate Index 0
            indices[i + 1] = (short) (j + 1);           // Calculate Index 1
            indices[i + 2] = (short) (j + 2);           // Calculate Index 2
            indices[i + 3] = (short) (j + 2);           // Calculate Index 3
            indices[i + 4] = (short) (j + 3);           // Calculate Index 4
            indices[i + 5] = j;           // Calculate Index 5
        }
        vertices.setIndices(indices, 0, len);         // Set Index Buffer for Rendering
        mMVPMatricesHandle = glGetUniformLocation(program.getHandle(), "u_MVPMatrix");
    }

    public void beginBatch(float[] vpMatrix) {
        numSprites = 0;                                 // Empty Sprite Counter
        bufferIndex = 0;                                // Reset Buffer Index (Empty)
        viewProjectionMatrix = vpMatrix;
    }


    /**
     * Signal the end of a batch. Render the batched sprites
     */
    public void endBatch() {
        if (numSprites > 0) {                        // IF Any Sprites to Render
            // bind MVP matrices array to shader
            glUniformMatrix4fv(mMVPMatricesHandle, numSprites, false, uMVPMatrices, 0);
            glEnableVertexAttribArray(mMVPMatricesHandle);

            vertices.setVertices(vertexBuffer, 0, bufferIndex);
            vertices.bind();
            vertices.draw(GL_TRIANGLES, 0, numSprites * INDICES_PER_SPRITE);  // Render Batched Sprites
            vertices.unbind();
        }
    }

    /**
     * Draw Sprite to Batch
     * batch specified sprite to batch. adds vertices for sprite to vertex buffer
     * NOTE: MUST be called after beginBatch(), and before endBatch()!
     * NOTE: if the batch overflows, this will render the current batch, restart it,
     * and then batch this sprite.
     *
     * @param x           the x-position of the sprite (center)
     * @param y           the y-position of the sprite (center)
     * @param width       the width of the sprite
     * @param height      the height of the sprite
     * @param region      the texture region to use for sprite
     * @param modelMatrix the model matrix to assign to the sprite
     */
    public void drawSprite(float x, float y, float width, float height, TextureRegion region, float[] modelMatrix) {
        if (numSprites == maxSprites) {              // IF Sprite Buffer is Full
            endBatch();                                  // End Batch
            // NOTE: leave current texture bound!!
            numSprites = 0;                              // Empty Sprite Counter
            bufferIndex = 0;                             // Reset Buffer Index (Empty)
        }

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        float leftX = x - halfWidth;
        float bottomY = y - halfHeight;
        float rightX = x + halfWidth;
        float topY = y + halfHeight;

        List<Vertex> vertices = asList(
                new Vertex(leftX, bottomY, region.u1, region.v2, numSprites),
                new Vertex(rightX, bottomY, region.u2, region.v2, numSprites),
                new Vertex(rightX, topY, region.u2, region.v1, numSprites),
                new Vertex(leftX, topY, region.u1, region.v1, numSprites)
        );

        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).addTo(vertexBuffer, bufferIndex);
            bufferIndex += VERTEX_SIZE;
        }

        // add the sprite mvp matrix to uMVPMatrices array
        Matrix.multiplyMM(mMVPMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);

        //TODO: make sure numSprites < 24
        for (int i = 0; i < 16; ++i) {
            uMVPMatrices[numSprites * 16 + i] = mMVPMatrix[i];
        }

        numSprites++;
    }

    private class Vertex {
        float x;
        float y;
        float u;
        float v;
        int numberOfSprites;

        public Vertex(float x, float y, float u, float v, int numberOfSprites) {
            this.x = x;
            this.y = y;
            this.u = u;
            this.v = v;
            this.numberOfSprites = numberOfSprites;
        }

        public void addTo(float[] vertexBuffer, int offset) {
            vertexBuffer[offset] = x;
            vertexBuffer[offset + 1] = y;
            vertexBuffer[offset + 2] = u;
            vertexBuffer[offset + 3] = v;
            vertexBuffer[offset + 4] = numSprites;
        }
    }
}
