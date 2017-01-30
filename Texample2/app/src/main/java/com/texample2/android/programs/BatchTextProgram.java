package com.texample2.android.programs;

import com.texample2.android.AttributeVariable;

import static com.texample2.android.AttributeVariable.*;
import static com.texample2.android.RawResourceReader.readShaderFileFromResource;


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
