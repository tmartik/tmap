package org.tmar.tmap.map.zip;

import org.tmar.tmap.map.file.ManifestTileReader;

import java.io.IOException;
import java.io.InputStream;

/*
    Tile reader implementation for reading tiles from a zip file structure.

    The "zipFolders" type expects the following folder structure in the same folder where the JSON file is located:
        6.zip
            37
                18.png
                19.png
                ...
            ...
        7.zip
        8.zip
        9/9124.zip
            4757.jpg
            4758.jpg
            4759.jpg
            ...
        ...
    This layout enables saving huge maps on FAT32-formatted memory cards.

 */

public class ZipTileReader extends ManifestTileReader
{
    private static final String TYPE = "zipFolders";
    public ZipTileReader(String path) throws Exception {
        super(path);

        if(!mType.toLowerCase().equals(TYPE.toLowerCase())) {
            throw new IllegalArgumentException("Wrong manifest type.");
        }
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
