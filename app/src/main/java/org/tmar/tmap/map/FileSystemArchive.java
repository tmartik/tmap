package org.tmar.tmap.map;

import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.MapTileIndex;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/*
    This class implements a map archive.

    The implementation supports reading map tiles from two types of folder structures in the filesystem.
 */
public class FileSystemArchive implements IArchiveFile {

    protected HashMap<String, ITileReader> mTileReaders = new HashMap<>();

    public FileSystemArchive() {
        super();
    }

    @Override
    public void init(File mapSpec) throws Exception {
        ITileReader tileReader = TileReaderFactory.createFromManifest(mapSpec);
        String name = tileReader.getName();
        mTileReaders.put(name, tileReader);
    }

    /*
        This method is called by the map library to obtain an input stream to a map tile.
        The caller will close the input stream.
     */
    @Override
    public InputStream getInputStream(ITileSource tileSource, long pMapTileIndex) {
        int x = MapTileIndex.getX(pMapTileIndex);
        int y = MapTileIndex.getY(pMapTileIndex);
        int z = MapTileIndex.getZoom(pMapTileIndex);

        Set<String> layersNames = new HashSet<>(mTileReaders.keySet());

        String layerName = tileSource.name();
        layersNames.remove(layerName);

        ITileReader tileReader = mTileReaders.get(layerName);
        InputStream stream = null;
        if(tileReader != null) {
            stream = tileReader.getTile(z, x, y);

            Iterator<String> i = layersNames.iterator();

            while (stream == null && i.hasNext()) {
                // Tile was not found in requested layer; get backup tile from any other layer
                layerName = i.next();
                tileReader = mTileReaders.get(layerName);
                stream = tileReader.getTile(z, x, y);
            }
        }

        return stream;
    }

    @Override
    public void close() {
    }

    @Override
    public Set<String> getTileSources() {
        return mTileReaders.keySet();
    }

    @Override
    public void setIgnoreTileSource(boolean pIgnoreTileSource) {
    }
}