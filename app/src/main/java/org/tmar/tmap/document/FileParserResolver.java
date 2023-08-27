package org.tmar.tmap.document;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.tmar.tmap.document.IFileParser;
import org.tmar.tmap.document.gpx.GpxParser;
import org.tmar.tmap.document.json.JsonParser;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/*
    This class determines the format of the given file and creates
    an appropriate parser to open it.
 */
public class FileParserResolver {

    private final Context mContext;

    // List of parsers
    private Class[] parseClasses = new Class[] {
            GpxParser.class,
            JsonParser.class
    };

    public FileParserResolver(Context c) {
        mContext = c;
    }

    /*
        Returns a suitable parser for the given file.
     */
    public IFileParser resolve(Uri fileUri) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        try (Cursor cursor = mContext.getContentResolver().query(fileUri, null, null, null, null)) {
            if(cursor.getCount() <= 0) {
                throw new IllegalArgumentException("Cursor is empty!");
            }

            cursor.moveToFirst();
            String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

            // Extract file extension
            String[] parts = fileName.split("\\.");
            String extension = parts.length > 0 ? parts[parts.length - 1] : "";

            // Find compatible parser for this filetype
            for (Class c : parseClasses) {
                IFileParser parser = (IFileParser) c.getConstructor(Context.class).newInstance(mContext);

                if(Arrays.asList(parser.supportedExtensions()).contains(extension)) {
                    return parser;
                }
            }

            return null;
        }
    }
}
