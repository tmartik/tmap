package org.tmar.tmap.map;

import org.tmar.tmap.map.file.FileTileReader;
import org.tmar.tmap.map.mbtiles.MBTilesTileReader;
import org.tmar.tmap.map.zip.ZipTileReader;

import java.io.File;
import java.lang.reflect.Constructor;

public class TileReaderFactory {
    private static final Class<ITileReader>[] tileReaderClasses = new Class[] { MBTilesTileReader.class, ZipTileReader.class, FileTileReader.class };

    public static ITileReader createFromManifest(File manifest) {
        for (Class<ITileReader> c : tileReaderClasses) {
            ITileReader tileReader = instantiate(c, manifest);
            if(tileReader != null) {
                return tileReader;
            }
        }

        return null;
    }

    private static ITileReader instantiate(Class<ITileReader> clazz, File manifest) {
        try {
            Constructor<?> cons = clazz.getConstructor(String.class);
            ITileReader tileReader = (ITileReader) cons.newInstance(manifest.getAbsolutePath());
            return tileReader;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
