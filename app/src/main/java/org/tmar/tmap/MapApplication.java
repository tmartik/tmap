package org.tmar.tmap;

import android.app.Application;
import android.support.annotation.RawRes;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class MapApplication extends Application {
    // Called when the application is starting, before any other application objects have been created.

    private final static String TAG = "TMAP";

    @Override
    public void onCreate() {
        super.onCreate();

        prepareResources();
    }

    /*
        Unzip resource files to application private folder.
     */
    private void prepareResources() {
        // Unzip raw resource files to the private folder.
        File privateFolder = getFilesDir();
        String[] files = privateFolder.list();

        if(files.length == 0) {
            // Nothing unzipped yet
            try {
                List<Integer> copyResourceIds = getRawResourceIds();

                for (Integer id: copyResourceIds) {
                    File targetFile = new File(privateFolder.getAbsolutePath() + File.separator + id);
                    copyResourceFile(id, targetFile);
                    unzipFile(targetFile);
                    deleteFile(targetFile);
                }
            } catch (IllegalAccessException | IOException e) {
                e.printStackTrace();

                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
        List all raw resources ids.
     */
    private List<Integer> getRawResourceIds() throws IllegalAccessException {
        List<Integer> results = new ArrayList<>();

        Field[] fields = R.raw.class.getFields();

        for (Field field : fields) {
            @RawRes int rawId = (Integer)field.get(null);
            results.add(rawId);
        }

        return results;
    }

    /*
        Copy the resource indicated by the resource id to the given folder.
     */
    private void copyResourceFile(int resId, File privateFolder) throws IOException {
        InputStream inputStream = getResources().openRawResource(resId);
        FileOutputStream outputStream = new FileOutputStream(privateFolder);
        copy(inputStream, outputStream);
    }

    /*
        Copy bytes from source to target stream.
     */
    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while((length = source.read(buf)) != -1) {
            target.write(buf, 0, length);
        }
    }

    /*
        Unzip the given zip file to the same folder where the zip file is located.
     */
    private void unzipFile(File zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File path = new File(zipFile.getParentFile().getAbsolutePath() + File.separator + entry.getName());
                if(entry.isDirectory()) {
                    boolean res = path.mkdirs();
                    Log.i(TAG, "Created folder: " + path.getAbsolutePath() + " - "+ res);
                } else {
                    InputStream zis = zip.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(path);

                    Log.i(TAG, "Unzipping file: " + zipFile.getAbsolutePath() + " to path: " + path.getAbsolutePath());
                    copy(zis, fos);
                }
            }
        }
    }

    /*
        Delete the specified file.
     */
    private void deleteFile(File file) {
        file.delete();
    }
}