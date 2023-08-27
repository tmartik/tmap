package org.tmar.tmap.map.zip;

import org.tmar.tmap.map.ITileReader;

import java.io.IOException;
import java.io.InputStream;

/*
    Tile reader implementation for reading tiles from a zip file structure.
 */
public class ZipTileReader implements ITileReader
{
    private String mPath;

    public ZipTileReader(String path) {
        mPath = path;
    }

    @Override
    public InputStream getTile(int z, int x, int y) {
        try {
            ZipCache cache = ZipCache.instance(z);
            return cache.getTile(mPath, x, y);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
