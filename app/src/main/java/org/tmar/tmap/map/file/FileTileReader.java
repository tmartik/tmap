package org.tmar.tmap.map.file;

import org.tmar.tmap.map.ITileReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/*
    Tile reader implementation for reading ZXY tiles from the filesystem.
 */
public class FileTileReader implements ITileReader
{
    private final String mPath;

    public FileTileReader(String path) {
        mPath = path;
    }

    @Override
    public InputStream getTile(int z, int x, int y) {

        final String fileName =  mPath + File.separator + z + File.separator + x + File.separator + y;

        File tile = new File(fileName);
        if(!tile.exists()) {
            tile = new File(fileName + ".png");
        }
        if(!tile.exists()) {
            tile = new File(fileName + ".jpg");
        }
        if(!tile.exists()) {
            return null;
        }

        try {
            FileInputStream inputStream = new FileInputStream(tile);
            return inputStream;
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }
}
