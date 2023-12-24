package org.tmar.tmap.map.mbtiles;

import android.nfc.FormatException;

import org.tmar.tmap.map.ITileReader;

import java.io.File;
import java.io.InputStream;

public class MBTilesTileReader implements ITileReader {
    private static final String EXTENSION = ".mbtiles";

    private String mName;
    public MBTilesTileReader(String path) throws FormatException {
        File file = new File(path);
        mName = file.getName();

        if(!mName.toLowerCase().endsWith(EXTENSION)) {
            throw new FormatException("Wrong file type.");
        }
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public InputStream getTile(int z, int x, int y) {
        return null;
    }
}
