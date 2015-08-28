package com.admicro.vertx.utils;

import io.vertx.core.Vertx;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class FileUtils {

    public static String readAll(Vertx vertx, String path) throws IOException, NullPointerException {
        FileReader reader = null;
        try {
            URL url = vertx.getClass().getClassLoader().getResource(path);
            if (url == null)
                throw new NullPointerException("Url not found");

            File file = new File(url.getFile());
            reader = new FileReader(file);
            char[] buffer = new char[1024];
            StringBuilder builder = new StringBuilder();

            int bytesRead;
            while ((bytesRead = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, bytesRead);
            }
            return builder.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
