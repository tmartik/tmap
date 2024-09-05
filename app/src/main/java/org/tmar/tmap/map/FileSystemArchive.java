package org.tmar.tmap.map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.MapTileIndex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    This class implements a map archive.

    The implementation supports reading map tiles from two types of folder structures in the filesystem.
 */
public class FileSystemArchive implements IArchiveFile {

    protected List<ITileReader> mTileReaders = new ArrayList<>();

    public FileSystemArchive() {
        super();
    }

    @Override
    public void init(File mapSpec) throws Exception {
        mTileReaders.clear();
        if(mapSpec != null) {
            add(mapSpec);
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

        // Draw final map tile from multiple map sources (the basemap is the first tile reader; the rest are overlays)
        Bitmap tileBitmap = null;

        for(int i = 0; i < mTileReaders.size(); i++) {
            ITileReader r = mTileReaders.get(i);
            InputStream tileStream = r.getTile(z, x, y);
            if(tileStream == null && i == 0) {
                return null;    // Tile not found in basemap; return empty (let's not draw overlays on a black tile)
            } else if(tileStream != null) {
                if(tileBitmap == null) {
                    tileBitmap = Bitmap.createBitmap(tileSource.getTileSizePixels(), tileSource.getTileSizePixels(), Bitmap.Config.RGB_565);
                }

                Bitmap t = toBitmap(tileStream);
                blit(tileBitmap, t);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        tileBitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        byte[] bitmapdata = outputStream.toByteArray();
        InputStream stream = new ByteArrayInputStream(bitmapdata);
        return stream;
    }

    @Override
    public void close() {
    }

    @Override
    public Set<String> getTileSources() {
        Set<String> set = new HashSet<>();
        set.add("dummy");
        return set;
    }

    @Override
    public void setIgnoreTileSource(boolean pIgnoreTileSource) {
    }

    public void add(File file) throws Exception {
        ITileReader tileReader = TileReaderFactory.createFromManifest(file);
        mTileReaders.add(tileReader);
    }

    private Bitmap toBitmap(InputStream stream) {
        return BitmapFactory.decodeStream(stream);
    }

    private void blit(Bitmap targetBitmap, Bitmap sourceBitmap) {
        Canvas canvas = new Canvas(targetBitmap);
        canvas.drawBitmap(sourceBitmap, 0, 0, null);
    }
}