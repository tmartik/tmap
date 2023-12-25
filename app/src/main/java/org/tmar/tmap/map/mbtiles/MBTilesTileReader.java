package org.tmar.tmap.map.mbtiles;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.tmar.tmap.map.ITileReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

public class MBTilesTileReader implements ITileReader {
    private static final String EXTENSION = ".mbtiles";
    private static final String[] FORMATS = new String[] { "jpg", "png", "webp" };
    SQLiteDatabase mDatabase;

    private String mName;
    public MBTilesTileReader(String path)  {
        File file = new File(path);
        String name = file.getName();
        mName = name.substring(0, name.indexOf("."));

        if(!name.toLowerCase().endsWith(EXTENSION)) {
            throw new IllegalArgumentException("Wrong file type.");
        }

        mDatabase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);

        String format = getMetadata("format");
        boolean formatSupported = Arrays.stream(FORMATS).filter(f -> f.equals(format)).toArray().length > 0;

        if(!formatSupported) {
            throw new IllegalArgumentException("Unsupported tile format: " + format);
        }

        String center = getMetadata("center");      // TODO:
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public InputStream getTile(int z, int x, int y) {
        Cursor metadata = mDatabase.query("tiles", new String[] { "zoom_level", "tile_column", "tile_row", "tile_data" }, "zoom_level = ? and tile_column = ? and tile_row = ?", new String[] { String.valueOf(z), String.valueOf(x), String.valueOf((int) Math.pow(2, z) - 1 - y) }, null, null, null);
        if(metadata.moveToFirst()) {
            byte[] bytes = metadata.getBlob(3);
            InputStream stream = new ByteArrayInputStream(bytes);
            return stream;
        }

        return null;
    }

    private String getMetadata(String name) {
        Cursor metadata = mDatabase.query("metadata", new String[] { "name", "value" }, "name = ?", new String[] { name }, null, null, null);
        if(metadata.moveToFirst()) {
            String value = metadata.getString(1);
            return value;
        }

        return null;
    }
}
