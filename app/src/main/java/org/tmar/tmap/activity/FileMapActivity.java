package org.tmar.tmap.activity;

import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import org.tmar.tmap.R;
import org.tmar.tmap.map.OfflineTileProvider;
import org.tmar.tmap.map.zip.ZipCache;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/*
    This activity implements a file-based map source for offline browsing.
 */
public class FileMapActivity extends BaseActivity {

    final static String mapSpecJson = "mapspec.json";       // Map archive specification filename
    private List<File> mapDirs;                             // Available map archives

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
        if (mapDirs != null && mapDirs.size() > 0) {
            int itemId = 100;

            for (File m : mapDirs) {
                String layerName = m.getName();
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
        mapDirs = getMapDirs(this);

        if(mapDirs.size() > 0) {
            final String archiveName = mPref.getString("archiveName","");
            for (int i = 0; i < mapDirs.size(); i++) {
                File dir = mapDirs.get(i);
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
        File mapFile = new File(mapDirs.get(index) + File.separator + mapSpecJson);
        OfflineTileProvider tileProvider = new OfflineTileProvider(this, new SimpleRegisterReceiver(this), mapFile);
        mMapView.setTileProvider(tileProvider);

        IArchiveFile archives = tileProvider.getArchives();
        Set<String> tileSources = archives.getTileSources();
        String source = tileSources.iterator().next();
        mMapView.setTileSource(FileBasedTileSource.getSource(source));
        ZipCache.clear();

        // Save to settings
        File dir = mapDirs.get(index);
        String[] parts = dir.getAbsolutePath().split("/");
        mPref.edit().putString("archiveName", parts.length > 0 ? parts[parts.length - 1] : "").commit();
    }

    /*
        Find map archives from storage.
     */
    private List<File> getMapDirs(Context c) {
        List<File> searchDirs = new ArrayList<>();
        searchDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        searchDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));

        File[] mediaDirs = c.getExternalMediaDirs();

        for (File f : mediaDirs) {
            File x = new File(f.getAbsolutePath());
            while(x.getParentFile() != null) {
                searchDirs.add(x);
                x = x.getParentFile();
            }
        }

        // Add proprietary data dirs
        List<File> copy = new ArrayList<>(searchDirs);
        for (File f : copy) {
            searchDirs.add(new File(f.getAbsolutePath() + File.separator + "offline-maps"));
        }

        List<File> mapDirs = new ArrayList<>();

        // Search dirs for maps
        for (File d : searchDirs) {
            File[] fileList = d.listFiles();
            if(fileList != null) {
                for (File f : fileList) {
                    if(f.isDirectory()) {
                        File mapSpecFile = new File(f.getAbsolutePath() + File.separator + mapSpecJson);
                        if(mapSpecFile.exists()) {
                            mapDirs.add(f);
                        }
                    }
                }
            }
        }

        return mapDirs;
    }
}
