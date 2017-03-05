package com.android.texample2.programs;

import com.android.texample2.domain.FontProgram;

public class FontProgramAdapter {

    public static FontProgram createFontProgram(Program program) {
        int programHandle = program.getHandle();
        return new FontProgram(programHandle);
    }

}
