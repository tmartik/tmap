package org.tmar.tmap.activity;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.tmar.tmap.MapApplication;
import org.tmar.tmap.MapDescriptor;
import org.tmar.tmap.R;
import org.tmar.tmap.map.ITileReader;
import org.tmar.tmap.map.OfflineTileProvider;
import org.tmar.tmap.map.TileReaderFactory;
import org.tmar.tmap.map.zip.ZipCache;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/*
    This activity implements a file-based map source for offline browsing.
 */
public class FileMapActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openDefaultMap();
        } else {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    @Override
    protected void onPermissionGranted(String permission) {
        super.onPermissionGranted(permission);

        if(permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            openDefaultMap();
        }
    }

    /*
        Prepare map archive selection menu.
     */
    @Override
    protected void onPrepareLayerMenu(SubMenu subMenu) {
        // Build layer selection menu
        MapApplication app = (MapApplication) getApplication();
        List<MapDescriptor> maps = app.getMaps();
        List<MapDescriptor> visibleMaps = maps.stream().filter(m -> m.isVisible()).collect(Collectors.toList());

        int itemId = 100;
        for(MapDescriptor map : visibleMaps) {
            subMenu.add(Menu.NONE, itemId++, Menu.NONE, map.getName());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(super.onOptionsItemSelected(item) == false) {
            final int itemId = item.getItemId();

            switch (itemId) {
                default:
                    // Open selected map archive for viewing.
                    if(itemId >= 100) {
                        int selectedIndex = itemId - 100;
                        selectArchive(selectedIndex);
                        return true;
                    } else {
                        return false;
                    }
            }
        }

        return true;
    }

    /*
        Determine the map archive to open for viewing.
     */
    private void openDefaultMap() {
        MapApplication app = (MapApplication) getApplication();
        app.findMaps();
        List<MapDescriptor> maps = app.getMaps();

        if(maps.size() > 0) {
            // Select the map stored in preferences or the first map if no preference set
            final String archiveName = mPref.getString("archiveName","");
            for (int i = 0; i < maps.size(); i++) {
                MapDescriptor map = maps.get(i);
                if(map.getName().equals(archiveName) || archiveName.length() == 0) {
                    selectArchive(i);
                    break;
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.noArchivesFound), Toast.LENGTH_LONG).show();
        }
    }

    /*
        Set map archive for viewing.
     */
    private void selectArchive(int index) {
        MapApplication app = (MapApplication) getApplication();
        MapDescriptor map = app.getMaps().get(index);

        OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this), map.getFile());
        mMapView.setTileProvider(tileProvider);

        IArchiveFile archives = tileProvider.getArchives();
        Set<String> tileSources = archives.getTileSources();
        String source = tileSources.iterator().next();
        mMapView.setTileSource(FileBasedTileSource.getSource(source));
        ZipCache.clear();

        // Set default location and zoom
        ITileReader tileReader = TileReaderFactory.createFromManifest(map.getFile());
        Location location = tileReader.getDefaultLocation();
        if(location != null) {
            IGeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mMapView.getController().setCenter(geoPoint);
            int zoom = tileReader.getDefaultZoom();
            if(zoom > 0) {
                mMapView.getController().setZoom((float) zoom);
            }

            mMyLocation.disableFollowLocation();
        }

        // Save to settings
        String name = map.getName();
        mPref.edit().putString("archiveName", name).commit();
    }
}
