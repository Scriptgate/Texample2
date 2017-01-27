package com.android.texample2.programs;

import com.android.texample2.AttributeVariable;

import static com.android.texample2.AttributeVariable.*;
import static com.android.texample2.RawResourceReader.readShaderFileFromResource;


public class BatchTextProgram extends Program {

    @Override
    public void init() {
        super.init(
                readShaderFileFromResource("batch_vertex_shader"),
                readShaderFileFromResource("batch_fragment_shader"),
                new AttributeVariable[]{POSITION, TEXTURE_COORDINATE, MVP_MATRIX}
        );
    }

}
