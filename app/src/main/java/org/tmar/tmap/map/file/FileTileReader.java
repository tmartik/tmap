package org.tmar.tmap.map.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/*
    Tile reader implementation for reading ZXY tiles from the filesystem.

    The "files" type expects the standard ZXY folder structure.
    Both JPG and PNG filetypes are supported.
 */
public class FileTileReader extends ManifestTileReader
{
    private static final String TYPE = "files";

    public FileTileReader(String path) throws Exception {
        super(path);

        if(!mType.toLowerCase().equals(TYPE.toLowerCase())) {
            throw new IllegalArgumentException("Wrong manifest type.");
        }
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

    @Override
    public boolean isOverlay() {
        return false;
    }
}
