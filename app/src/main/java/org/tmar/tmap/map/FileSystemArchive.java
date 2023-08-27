package org.tmar.tmap.map;

import org.json.JSONObject;
import org.tmar.tmap.helpers.FileHelper;
import org.tmar.tmap.map.file.FileTileReader;
import org.tmar.tmap.map.zip.ZipTileReader;
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

    protected File mFile;
    protected HashMap<String, ITileReader> mTileReaders = new HashMap<>();

    public FileSystemArchive() {
        super();
    }

    /*
        Initialize with a configuration file. The file specifies the details of the archive.

        The mapSpec is a JSON-formatted file, which has the following properties:
            name - the name of the map
            type - either "zipFolders" or "files"

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

        The "files" type expects the standard ZXY folder structure.
        Both JPG and PNG filetypes are supported.
     */
    @Override
    public void init(File mapSpec) throws Exception {
        mFile = mapSpec;
        String json = FileHelper.getStringFromFile(mapSpec.getAbsolutePath());
        JSONObject jObject = new JSONObject(json);
        String name = mapSpec.getParentFile().getName();
        String type = jObject.getString("type");
        String path = mapSpec.getParent();

        switch (type) {
            case "zipFolders":
                mTileReaders.put(name, new ZipTileReader(path));
                break;
            case "files":
                mTileReaders.put(name, new FileTileReader(path));
                break;
            default:
                throw new IllegalArgumentException("Unknown layer type: " + type);
        }
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