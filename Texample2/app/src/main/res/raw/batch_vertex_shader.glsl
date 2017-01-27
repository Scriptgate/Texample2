uniform mat4 u_MVPMatrix[24];      // An array representing the combined model/view/projection matrices for each sprite

attribute float a_MVPMatrixIndex;  // The index of the MVPMatrix of the particular sprite
attribute vec4 a_Position;         // Per-vertex position information we will pass in.
attribute vec2 a_TexCoordinate;    // Per-vertex texture coordinate information we will pass in
varying vec2 v_TexCoordinate;      // This will be passed into the fragment shader.
void main()
{
   int mvpMatrixIndex = int(a_MVPMatrixIndex);
   v_TexCoordinate = a_TexCoordinate;
   // gl_Position is a special variable used to store the final position.
   // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
   gl_Position = u_MVPMatrix[mvpMatrixIndex] * a_Position;
}