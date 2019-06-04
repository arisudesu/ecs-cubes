package dev.arisu.demoecs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class File {
    public static String readToString(String name) throws IOException {

        InputStream inputStream = File.class.getClassLoader().getResourceAsStream(name);
        InputStream inputStream1 = Objects.requireNonNull(inputStream);

        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream1))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
