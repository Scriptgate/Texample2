package com.android.texample2.domain;

import com.android.texample2.AttributeVariable;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.*;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.ByteOrder.nativeOrder;

class Vertices {

    //--Constants--//
    private final static int POSITION_CNT_2D = 2;              // Number of Components in Vertex Position for 2D
    final static int POSITION_CNT_3D = 3;              // Number of Components in Vertex Position for 3D
    final static int COLOR_CNT = 4;                    // Number of Components in Vertex Color
    final static int TEXCOORD_CNT = 2;                 // Number of Components in Vertex Texture Coords
    final static int NORMAL_CNT = 3;                   // Number of Components in Vertex Normal
    private static final int MVP_MATRIX_INDEX_CNT = 1; // Number of Components in MVP matrix index

    private final static int INDEX_SIZE = Short.SIZE / 8;      // Index Byte Size (Short.SIZE = bits)

    private static final String TAG = "Vertices";

    //--Members--//
    // NOTE: all members are constant, and initialized in constructor!
    public final int positionCnt;                      // Number of Position Components (2=2D, 3=3D)
    public final int vertexStride;                     // Vertex Stride (Element Size of a Single Vertex)
    public final int vertexSize;                       // Bytesize of a Single Vertex
    private final IntBuffer vertices;                          // Vertex Buffer
    private final ShortBuffer indices;                         // Index Buffer
    public int numVertices;                            // Number of Vertices in Buffer
    public int numIndices;                             // Number of Indices in Buffer
    final int[] tmpBuffer;                             // Temp Buffer for Vertex Conversion
    private int mTextureCoordinateHandle;
    private int mPositionHandle;
    private int mMVPIndexHandle;

    /**
     * create the vertices/indices as specified (for 2d/3d)
     *
     * @param maxVertices maximum vertices allowed in buffer
     * @param maxIndices  maximum indices allowed in buffer
     */
    public Vertices(int maxVertices, int maxIndices, FontProgram program) {
        this.positionCnt = POSITION_CNT_2D;  // Set Position Component Count
        this.vertexStride = this.positionCnt + TEXCOORD_CNT + MVP_MATRIX_INDEX_CNT;  // Calculate Vertex Stride
        this.vertexSize = this.vertexStride * 4;        // Calculate Vertex Byte Size

        this.vertices = allocateDirect(maxVertices * vertexSize).order(nativeOrder()).asIntBuffer();

        if (maxIndices > 0) {                        // IF Indices Required
            this.indices = allocateDirect(maxIndices * INDEX_SIZE).order(nativeOrder()).asShortBuffer();
        } else {
            this.indices = null;                              // No Index Buffer
        }

        numVertices = 0;                                // Zero Vertices in Buffer
        numIndices = 0;                                 // Zero Indices in Buffer

        this.tmpBuffer = new int[maxVertices * vertexSize / 4];  // Create Temp Buffer

        // initialize the shader attribute handles
        mTextureCoordinateHandle = program.getHandle(AttributeVariable.TEXTURE_COORDINATE);
        mMVPIndexHandle = program.getHandle(AttributeVariable.MVP_MATRIX);
        mPositionHandle = program.getHandle(AttributeVariable.POSITION);
    }

    /**
     * set the specified vertices in the vertex buffer <br/>
     * NOTE: optimized to use integer buffer!
     *
     * @param vertices array of vertices (floats) to set
     * @param offset   offset to first vertex in array
     * @param length   number of floats in the vertex array (total) <br/>for easy setting use: vtx_cnt * (this.vertexSize / 4)
     */
    public void setVertices(float[] vertices, int offset, int length) {
        this.vertices.clear();                          // Remove Existing Vertices
        int last = offset + length;                     // Calculate Last Element
        for (int i = offset, j = 0; i < last; i++, j++) { // FOR Each Specified Vertex
            tmpBuffer[j] = Float.floatToRawIntBits(vertices[i]);  // Set Vertex as Raw Integer Bits in Buffer
        }
        this.vertices.put(tmpBuffer, 0, length);      // Set New Vertices
        this.vertices.flip();                           // Flip Vertex Buffer
        this.numVertices = length / this.vertexStride;  // Save Number of Vertices
    }

    /**
     * set the specified indices in the index buffer
     *
     * @param indices array of indices (shorts) to set
     * @param offset  offset to first index in array
     * @param length  number of indices in array (from offset)
     */
    public void setIndices(short[] indices, int offset, int length) {
        this.indices.clear();
        this.indices.put(indices, offset, length).flip();
        this.numIndices = length;
    }

    /**
     * perform all required binding/state changes before rendering batches.<br/>
     * USAGE: call once before calling draw() multiple times for this buffer.
     */
    public void bind() {
        vertices.position(0);                         // Set Vertex Buffer to Position
        glVertexAttribPointer(mPositionHandle, positionCnt, GL_FLOAT, false, vertexSize, vertices);
        glEnableVertexAttribArray(mPositionHandle);

        vertices.position(positionCnt);  // Set Vertex Buffer to Texture Coords (NOTE: position based on whether color is also specified)
        glVertexAttribPointer(mTextureCoordinateHandle, TEXCOORD_CNT, GL_FLOAT, false, vertexSize, vertices);
        glEnableVertexAttribArray(mTextureCoordinateHandle);

        vertices.position(positionCnt + TEXCOORD_CNT);
        glVertexAttribPointer(mMVPIndexHandle, MVP_MATRIX_INDEX_CNT, GL_FLOAT, false, vertexSize, vertices);
        glEnableVertexAttribArray(mMVPIndexHandle);
    }

    /**
     * draw the currently bound vertices in the vertex/index buffers<br/>
     * USAGE: can only be called after calling bind() for this buffer.
     *
     * @param primitiveType the type of primitive to draw
     * @param offset        the offset in the vertex/index buffer to start at
     * @param numVertices   the number of vertices (indices) to draw
     */
    public void draw(int primitiveType, int offset, int numVertices) {
        if (indices != null) {                       // IF Indices Exist
            indices.position(offset);                  // Set Index Buffer to Specified Offset
            //draw indexed
            glDrawElements(primitiveType, numVertices, GL_UNSIGNED_SHORT, indices);
        } else {                                         // ELSE No Indices Exist
            //draw direct
            glDrawArrays(primitiveType, offset, numVertices);
        }
    }

    /**
     * clear binding states when done rendering batches.<br/>
     * USAGE: call once before calling draw() multiple times for this buffer.
     */
    public void unbind() {
        glDisableVertexAttribArray(mTextureCoordinateHandle);
    }
}
