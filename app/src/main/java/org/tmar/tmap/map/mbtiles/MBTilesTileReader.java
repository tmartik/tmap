package org.tmar.tmap.map.mbtiles;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

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
        Arrays.sort(FORMATS);
        boolean formatSupported = Arrays.binarySearch(FORMATS, format) >= 0;

        if(!formatSupported) {
            throw new IllegalArgumentException("Unsupported tile format: " + format);
        }
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public Location getDefaultLocation() {
        Location location = null;
        String bounds = getMetadata("bounds");
        if(bounds != null) {
            String[] coordinates = bounds.split(",");
            if(coordinates.length == 4) {
                double left = Double.parseDouble(coordinates[0]);
                double bottom = Double.parseDouble(coordinates[1]);
                double right = Double.parseDouble(coordinates[2]);
                double top = Double.parseDouble(coordinates[3]);

                double lon = left + (right - left) / 2;
                double lat = bottom + (top - bottom) / 2;

                location = new Location("");
                location.setLatitude(lat);
                location.setLongitude(lon);
            }
        }

        return location;
    }

    @Override
    public int getDefaultZoom() {
        String zoomMax = getMetadata("maxzoom");
        return zoomMax != null ? Integer.parseInt(zoomMax) : 0;
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

    @Override
    public boolean isOverlay() {
        String type = getMetadata("type");
        return type != null && type.equals("overlay");
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
