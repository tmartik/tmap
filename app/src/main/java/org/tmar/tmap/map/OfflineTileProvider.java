package org.tmar.tmap.map;

import static org.osmdroid.tileprovider.tilesource.FileBasedTileSource.getSource;

import org.osmdroid.tileprovider.IMapTileProviderCallback;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileApproximater;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;

import java.io.File;
import java.util.List;

/*
    SEE: https://github.com/osmdroid/osmdroid/wiki/Map-Sources#creating-a-custom-tile-provider-chain
    SEE: https://github.com/osmdroid/osmdroid/wiki/Offline-Map-Tiles
*/
public class OfflineTileProvider extends MapTileProviderArray implements IMapTileProviderCallback {

    private FileSystemArchive archive;

    public OfflineTileProvider(final IRegisterReceiver pRegisterReceiver, List<File> files) {
        super(getSource("dummy"), pRegisterReceiver);

        try {
            archive = new FileSystemArchive();
            for (File f : files) {
                archive.add(f);
            }
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
