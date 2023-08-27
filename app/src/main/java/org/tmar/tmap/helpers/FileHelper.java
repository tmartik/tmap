package org.tmar.tmap.helpers;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileHelper
{
    /*
        Reads in a stream and returns its content as a string.
     */
    public static String convertStreamToString(InputStream is) throws Exception {
        try(InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(inputStreamReader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /*
        Read in a file and return its contents as a string.
     */
    public static String getStringFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        try (FileInputStream fin = new FileInputStream(file)) {
            return convertStreamToString(fin);
        }
    }

    /*
        Read in a file and return its contents as a string.
     */
    public static String getStringFromFile(Context c, Uri fileUri) throws Exception {
        try(InputStream inputStream = c.getContentResolver().openInputStream(fileUri)) {
            return convertStreamToString(inputStream);
        }
    }
}
