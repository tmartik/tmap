package org.tmar.tmap.activity;

import android.Manifest;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.tmar.tmap.MapApplication;
import org.tmar.tmap.R;
import org.tmar.tmap.map.OfflineTileProvider;
import org.tmar.tmap.map.zip.ZipCache;

import java.io.File;
import java.util.List;
import java.util.Set;


/*
    This activity implements a file-based map source for offline browsing.
 */
public class FileMapActivity extends BaseActivity {

    private List<File> mMapDirs;                             // Available map archives

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

            // We have the permission and we have loaded all map archives; we can open options menu now.
            openOptionsMenu();
        }
    }

    @Override
    public void onAttachedToWindow() {
        if(hasPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // We have the permission; we can open options menu now.
            openOptionsMenu();
        }
    }


    /*
        Prepare map archive selection menu.
     */
    @Override
    protected void onPrepareLayerMenu(SubMenu subMenu) {
        // Build layer selection menu
        if (mMapDirs != null && mMapDirs.size() > 0) {
            int itemId = 100;

            for (File m : mMapDirs) {
                File parent = m.getParentFile();
                String layerName = parent.getName();
                subMenu.add(Menu.NONE, itemId++, Menu.NONE, layerName);
            }
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
        mMapDirs = app.getMapDirs();

        if(mMapDirs.size() > 0) {
            final String archiveName = mPref.getString("archiveName","");
            for (int i = 0; i < mMapDirs.size(); i++) {
                File dir = mMapDirs.get(i);
                if(dir.getAbsolutePath().indexOf(archiveName) >= 0 || archiveName.length() == 0) {
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
        File mapFile = mMapDirs.get(index);
        OfflineTileProvider tileProvider = new OfflineTileProvider(this, new SimpleRegisterReceiver(this), mapFile);
        mMapView.setTileProvider(tileProvider);

        IArchiveFile archives = tileProvider.getArchives();
        Set<String> tileSources = archives.getTileSources();
        String source = tileSources.iterator().next();
        mMapView.setTileSource(FileBasedTileSource.getSource(source));
        ZipCache.clear();

        // Save to settings
        File parent = mapFile.getParentFile();
        String name = parent.getName();
        mPref.edit().putString("archiveName", name).commit();
    }
}
