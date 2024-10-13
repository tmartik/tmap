package org.tmar.tmap.activity;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.tmar.tmap.MapApplication;
import org.tmar.tmap.MapDescriptor;
import org.tmar.tmap.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
    This activity implements a file-based map source for offline browsing.
 */
public class FileMapActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            MapApplication app = (MapApplication) getApplication();
            app.findMaps();
            openDefaultMap();
        } else {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    @Override
    protected void onPermissionGranted(String permission) {
        super.onPermissionGranted(permission);

        if(permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            MapApplication app = (MapApplication) getApplication();
            app.findMaps();
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
        List<MapDescriptor> visibleMaps = app.getMaps();

        int itemId = 100;
        for(MapDescriptor map : visibleMaps) {
            subMenu.add(Menu.NONE, itemId++, Menu.NONE, map.getName());
        }
    }

    @Override
    protected void reloadMap() {
        openDefaultMap();
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
                        saveSelectedMapIndex(selectedIndex);

                        // Set default location and zoom
                        MapApplication app = (MapApplication) getApplication();
                        MapDescriptor map = app.getSelectedMapDescriptor(selectedIndex);
                        IGeoPoint location = map.getCenter();
                        if(location != null) {
                            mMapView.getController().setCenter(location);
                            if (map.getDefaultZoom() > 0) {
                                mMapView.getController().setZoom((float) map.getDefaultZoom());
                            }

                            mMyLocation.disableFollowLocation();
                        }

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
        File[] maps = app.getSelectedMap(index);
        File[] overlays = app.getVisibleOverlays();

        // Basemaps
        OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this), maps);
        mMapView.setTileProvider(tileProvider);
        mMapView.setTileSource(tileProvider.getTileSource());

        // Clear tile overlays
        for (Overlay o : new ArrayList<>(mMapView.getOverlayManager())) {
            if(o instanceof TilesOverlay) {
                removePermanentOverlay(o);
            }
        }

        // Overlays
        for (File o : overlays) {
            MapTileProviderBase anotherTileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(this), new File[] { o });
            TilesOverlay secondTilesOverlay = new TilesOverlay(anotherTileProvider, getBaseContext());
            secondTilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
            addPermanentOverlay(secondTilesOverlay);
        }
    }

    private void saveSelectedMapIndex(int index) {
        MapApplication app = (MapApplication) getApplication();
        File[] maps = app.getSelectedMap(index);

        String name = maps[0].getName();
        name = name.substring(0, name.lastIndexOf('.'));
        mPref.edit().putString("archiveName", name).commit();
    }
}
