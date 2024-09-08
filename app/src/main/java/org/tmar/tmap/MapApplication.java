package org.tmar.tmap;

import android.annotation.SuppressLint;
import android.app.Application;
import android.database.Cursor;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RawRes;
import androidx.core.content.ContextCompat;

import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;
import org.tmar.tmap.document.FileParserResolver;
import org.tmar.tmap.document.IFileParser;
import org.tmar.tmap.map.ITileReader;
import org.tmar.tmap.map.TileReaderFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MapApplication extends Application {
    // Called when the application is starting, before any other application objects have been created.

    private final static String TAG = "TMAP";
    private final static String MapManifestFileName = "manifest.json";       // Map archive specification filename
    private final static String MapSpecFileName = "mapspec.json";       // Map archive specification filename
    private final static String MBTilesFileName = ".mbtiles";           // Map archive specification filename
    private final static String[] mManifestFilenames = new String[]{ MapManifestFileName, MapSpecFileName, MBTilesFileName };
    private final static String[] SearchPaths = new String[] { Environment.DIRECTORY_DOCUMENTS, Environment.DIRECTORY_DOWNLOADS };

    private List<MapDescriptor> mMapDescriptors;                           // Available map archives

    private final List<GPX> mDocuments = new ArrayList<>();
    private boolean mFollowLocation = true;

    @Override
    public void onCreate() {
        super.onCreate();

        prepareResources();
    }

    /*
        Open file for viewing on the map.
     */
    public GPX openFile(Uri fileUri) throws Exception {
        // Read from file
        FileParserResolver resolver = new FileParserResolver(this);
        String filename = getFilenameFromUri(fileUri);
        GPX document = findDocumentByName(filename);
        if(document == null) {
            // This document is not yet open
            IFileParser parser = resolver.resolve(fileUri);
            document = parser.parse(fileUri);
            document.setCreator(filename);    // This will pass filename to UI
            mDocuments.add(document);
        }

        return document;
    }

    public List<GPX> getOpenFiles() {
        return mDocuments;
    }

    public void closeFile(int index) {
        mDocuments.remove(index);
    }


    public List<MapDescriptor> getMaps() {
        List<MapDescriptor> baseMaps = new ArrayList<>();

        for (MapDescriptor md : mMapDescriptors) {
            if(!md.isOverlay()) {
                baseMaps.add(md);
            }
        }

        return baseMaps;
    }

    public List<MapDescriptor> getMapOverlays() {
        List<MapDescriptor> overlays = new ArrayList<>();

        for (MapDescriptor md : mMapDescriptors) {
            if(md.isOverlay()) {
                overlays.add(md);
            }
        }

        return overlays;
    }

    public String getFilenameFromUri(Uri uri) throws IOException {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                return filename;
            } else {
                throw new IOException("Could not open cursor to Uri!");
            }
        } finally {
            cursor.close();
        }
    }

    public GPX findDocumentByName(String filename) {
        List<GPX> matching = mDocuments.stream().filter(gpx -> gpx.getCreator().equals(filename)).collect(Collectors.toList());
        return matching.size() > 0 ? matching.get(0) : null;
    }

    // Get the path to the topmost 'searchFolder'
    private File getRootPath(File path, String searchFolder) {
        File candidate = new File(path.getAbsolutePath());
        File parent = path;

        do {
            path = new File(parent.getAbsolutePath());
            File searchFile = new File(parent.getAbsolutePath() + File.separator + searchFolder);
            if(searchFile.exists() && candidate.getAbsolutePath().split("/").length > parent.getAbsolutePath().split("/").length) {
                candidate = new File(parent.getAbsolutePath());
            }
            parent = parent.getParentFile();
        } while(parent != null && path.getAbsolutePath().split("/").length > 1);

        return new File(candidate.getAbsolutePath() + File.separator + searchFolder);
    }

    /*
        Find map archives from storage.
    */
    public void findMaps() {
        List<MapDescriptor> maps = new ArrayList<>();

        List<File> searchDirs = new ArrayList<>();
        File[] dirs = ContextCompat.getExternalFilesDirs(this, Environment.DIRECTORY_DOCUMENTS);

        for (File f : dirs) {
            if(f != null) {
                for(String p : SearchPaths) {
                    File path = getRootPath(f, p);
                    searchDirs.add(path);
                }
            }
        }

        for(File f : searchDirs) {
            for (String m : mManifestFilenames) {
                try {
                    final Collection<String> simpleStringCollection = new ArrayList<>();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        Stream<Path> list = Files.find(Paths.get(f.getAbsolutePath()), Integer.MAX_VALUE, (path, basicFileAttributes) -> path.toString().endsWith(m));
                        list.forEach(p -> simpleStringCollection.add(p.toString()));
                    } else {
                        File[] files = f.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return pathname.getName().endsWith(m);
                            }
                        });
                        for (File mf : files) {
                            simpleStringCollection.add(mf.getAbsolutePath());
                        }
                    }

                    Log.e(TAG, simpleStringCollection.toString());
                    for (Iterator<String> it = simpleStringCollection.iterator(); it.hasNext(); ) {
                        File mapSpecFile = new File(it.next());
                        ITileReader tileReader = TileReaderFactory.createFromManifest(mapSpecFile);
                        maps.add(new MapDescriptor(tileReader.getName(), mapSpecFile, true, tileReader.isOverlay(), false));
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        mMapDescriptors = maps;
    }

    /*
        Unzip resource files to application private folder.
     */
    private void prepareResources() {
        // Unzip raw resource files to the private folder.
        File privateFolder = getFilesDir();
        String[] files = privateFolder.list();

        if(files.length == 0) {
            // Nothing unzipped yet
            try {
                List<Integer> copyResourceIds = getRawResourceIds();

                for (Integer id: copyResourceIds) {
                    File targetFile = new File(privateFolder.getAbsolutePath() + File.separator + id);
                    copyResourceFile(id, targetFile);
                    unzipFile(targetFile);
                    deleteFile(targetFile);
                }
            } catch (IllegalAccessException | IOException e) {
                e.printStackTrace();

                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
        List all raw resources ids.
     */
    private List<Integer> getRawResourceIds() throws IllegalAccessException {
        List<Integer> results = new ArrayList<>();

        Field[] fields = R.raw.class.getFields();

        for (Field field : fields) {
            @RawRes int rawId = (Integer)field.get(null);
            results.add(rawId);
        }

        return results;
    }

    /*
        Copy the resource indicated by the resource id to the given folder.
     */
    private void copyResourceFile(int resId, File privateFolder) throws IOException {
        InputStream inputStream = getResources().openRawResource(resId);
        FileOutputStream outputStream = new FileOutputStream(privateFolder);
        copy(inputStream, outputStream);
    }

    /*
        Copy bytes from source to target stream.
     */
    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while((length = source.read(buf)) != -1) {
            target.write(buf, 0, length);
        }
    }

    /*
        Unzip the given zip file to the same folder where the zip file is located.
     */
    private void unzipFile(File zipFile) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File path = new File(zipFile.getParentFile().getAbsolutePath() + File.separator + entry.getName());
                if(entry.isDirectory()) {
                    boolean res = path.mkdirs();
                    Log.i(TAG, "Created folder: " + path.getAbsolutePath() + " - "+ res);
                } else {
                    InputStream zis = zip.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(path);

                    Log.i(TAG, "Unzipping file: " + zipFile.getAbsolutePath() + " to path: " + path.getAbsolutePath());
                    copy(zis, fos);
                }
            }
        }
    }

    /*
        Delete the specified file.
     */
    private void deleteFile(File file) {
        file.delete();
    }

    public boolean followEnabled() {
        return mFollowLocation;
    }

    public void setFollowEnabled(boolean enabled) {
        mFollowLocation = enabled;
    }

    public static RectF getDocumentBoundingBox(GPX gpx) {
        RectF boundingBox = null;

        // Tracks
        HashSet<Track> tracks = gpx.getTracks();

        if (tracks != null) {
            for (Track track: tracks) {
                ArrayList<Waypoint> trackPoints = track.getTrackPoints();

                for (Waypoint trackPoint : trackPoints) {
                    PointF gp = new PointF(Double.valueOf(trackPoint.getLongitude()).floatValue(), Double.valueOf(trackPoint.getLatitude()).floatValue());
                    boundingBox = extendBoundingBox(boundingBox, gp);
                }
            }
        }

        // Waypoints
        HashSet<Waypoint> wpts = gpx.getWaypoints();

        if (wpts != null) {
            for (Waypoint wpt: wpts) {
                PointF gp = new PointF(Double.valueOf(wpt.getLongitude()).floatValue(), Double.valueOf(wpt.getLatitude()).floatValue());
                boundingBox = extendBoundingBox(boundingBox, gp);
            }
        }

        return boundingBox;
    }

    private static RectF extendBoundingBox(RectF boundingBox, PointF coordinate) {
        if(boundingBox == null) {
            // Initialize empty bounding box with the first coordinate
            boundingBox = new RectF(coordinate.x, coordinate.y, coordinate.x, coordinate.y);
        }

        if(coordinate.x < boundingBox.left) {
            boundingBox.set(coordinate.x, boundingBox.top, boundingBox.right, boundingBox.bottom);
        }
        if(coordinate.x > boundingBox.right) {
            boundingBox.set(boundingBox.left, boundingBox.top, coordinate.x, boundingBox.bottom);
        }
        if(coordinate.y > boundingBox.top) {
            boundingBox.set(boundingBox.left, coordinate.y, boundingBox.right, boundingBox.bottom);
        }
        if(coordinate.y < boundingBox.bottom) {
            boundingBox.set(boundingBox.left, boundingBox.top, boundingBox.right, coordinate.y);
        }

        return boundingBox;
    }
}