package com.android.texample2;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RawResourceReader {

    public static String readShaderFileFromResource(String resourceName) {
        final InputStream inputStream = RawResourceReader.class.getClassLoader().getResourceAsStream("res/raw/" + resourceName + ".glsl");
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }

        return body.toString();
    }

}
