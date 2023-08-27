package org.tmar.tmap.map;

import static org.osmdroid.tileprovider.tilesource.FileBasedTileSource.getSource;

import android.content.Context;

import org.osmdroid.tileprovider.IMapTileProviderCallback;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileApproximater;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.tmar.tmap.map.FileSystemArchive;

import java.io.File;

/*
    SEE: https://github.com/osmdroid/osmdroid/wiki/Map-Sources#creating-a-custom-tile-provider-chain
    SEE: https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles
*/
public class OfflineTileProvider extends MapTileProviderArray implements IMapTileProviderCallback {

    private IArchiveFile archive;

    public OfflineTileProvider(Context c, final IRegisterReceiver pRegisterReceiver, File mapSpec) {
        super(getSource("dummy"), pRegisterReceiver);

        try {
            archive = new FileSystemArchive();
            archive.init(mapSpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IArchiveFile[] archives = new IArchiveFile[1];
        archives[0] = archive;

        final MapTileFileArchiveProvider mapTileFileArchiveProvider = new MapTileFileArchiveProvider(pRegisterReceiver, getTileSource(), archives);
        mTileProviderList.add(mapTileFileArchiveProvider);

        final MapTileApproximater approximationProvider = new MapTileApproximater();
        mTileProviderList.add(approximationProvider);
        approximationProvider.addProvider(mapTileFileArchiveProvider);
    }

    public IArchiveFile getArchives() {
        return archive;
    }

    @Override
    protected boolean isDowngradedMode(final long pMapTileIndex) {
        return true;
    }
}
