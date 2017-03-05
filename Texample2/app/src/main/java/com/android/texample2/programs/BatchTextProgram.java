package com.android.texample2.programs;

import com.android.texample2.AttributeVariable;

import static com.android.texample2.AttributeVariable.*;
import static com.android.texample2.RawResourceReader.readShaderFileFromResource;


public class BatchTextProgram {

    public static Program createBatchTextProgram() {
        String vertexShaderCode = readShaderFileFromResource("batch_vertex_shader");
        String fragmentShaderCode = readShaderFileFromResource("batch_fragment_shader");
        AttributeVariable[] programVariables = {POSITION, TEXTURE_COORDINATE, MVP_MATRIX};

        return new Program(vertexShaderCode, fragmentShaderCode, programVariables);
    }

}
