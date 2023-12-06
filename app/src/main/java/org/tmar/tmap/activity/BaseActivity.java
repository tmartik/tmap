package org.tmar.tmap.activity;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;
import org.json.JSONException;
import org.tmar.tmap.R;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.tmar.tmap.BuildConfig;
import org.tmar.tmap.document.FileParserResolver;
import org.tmar.tmap.map.LocationProvider;
import org.tmar.tmap.document.IFileParser;
import org.tmar.tmap.view.PoiInfoWindow;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

/*
    An activity that implements generic map browsing functions.
 */
public class BaseActivity extends Activity {
    private static final int OPENFILE_RESULT_CODE = 9;
    private static final int PERMISSION_CODE = 11;

    private IMyLocationProvider mLocationProvider = new LocationProvider(this);

    protected MapView mMapView;
    private MyLocationNewOverlay mMyLocation;
    private MarkerInfoWindow mInfoWindow;

    protected SharedPreferences mPref;
    private String mPrefsFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Get preference file name for current activity from activity metadata (see the manifest)
            PackageManager pm = getPackageManager();
            ActivityInfo ai = pm.getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA);
            mPrefsFileName = ai.metaData.getString("prefsName", "prefs");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        mPref = getApplicationContext().getSharedPreferences(mPrefsFileName, 0);      // 0 - for private mode

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);         // Show application on lock screen for quick access
        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);      // Set user-agent
        mMapView = (MapView) findViewById(R.id.mapView);

        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.setMinZoomLevel(5.0);
        mMapView.setMaxZoomLevel(18.0);

        // Overlays
        Polyline line = new Polyline();
        line.getPaint().setColor(Color.BLUE);
        line.getPaint().setStrokeWidth(4);
        line.getPaint().setStyle(Paint.Style.STROKE);

        // Location indicator
        if(hasPermissions(Manifest.permission.ACCESS_FINE_LOCATION)) {
            setupLocationIndicator();
        }

        // Scalebar
        final DisplayMetrics dm = getResources().getDisplayMetrics();       // TODO: Scale according to DPI!
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setLineWidth(5);
        mScaleBarOverlay.setTextSize(25);
        mMapView.getOverlays().add(mScaleBarOverlay);

        // Read map location from preferences
        IGeoPoint geoPoint = new GeoPoint(mPref.getFloat("Lat", 65), mPref.getFloat("Lon", 25));
        mMapView.getController().setCenter(geoPoint);
        mMapView.getController().setZoom(mPref.getFloat("Zoom", 8));

        mMapView.setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector mGestureDetector = new GestureDetector(BaseActivity.this, new GestureListener());

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mGestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        mInfoWindow = new PoiInfoWindow(this, R.layout.bubble_layout, mMapView);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if(mMyLocation != null) {
            mMyLocation.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        if(mMyLocation != null) {
            mMyLocation.disableMyLocation();
        }
    }

    @Override
    public void onDestroy() {
        // save current location and zoom level to preferences
        super.onDestroy();
        SharedPreferences.Editor editor = mPref.edit();

        // TODO: save open files: editor.putString("", "");

        editor.putFloat("Lat", (float) mMapView.getMapCenter().getLatitude());
        editor.putFloat("Lon", (float) mMapView.getMapCenter().getLongitude());
        editor.putFloat("Zoom", (float) mMapView.getZoomLevelDouble());
        editor.commit();
    }

    @Override
    public void onBackPressed() {
        // Disable the back button when device is locked
        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (!myKM.inKeyguardRestrictedInputMode()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.openMap);
        if(item != null) {
            SubMenu submenu = item.getSubMenu();
            submenu.clear();

            onPrepareLayerMenu(submenu);

            if (submenu.size() == 0) {
                menu.removeItem(R.id.openMap);
            }
        }

        MenuItem followMenuItem = menu.findItem(R.id.follow);
        followMenuItem.setEnabled(mMyLocation != null && !mMyLocation.isFollowLocationEnabled());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onAttachedToWindow() {
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        if(!hasBackKey) {
            openOptionsMenu();
        }
    };

    protected void onPrepareLayerMenu(SubMenu subMenu) {
        // Empty default implementation
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.follow:
                mMyLocation.enableFollowLocation();
                return true;
            case R.id.openMap:
                return true;
            case R.id.openFile:
                // open f.ex. GPX file
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("application/octet-stream");

                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                startActivityForResult(chooseFile, OPENFILE_RESULT_CODE);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPENFILE_RESULT_CODE:
                if (resultCode == -1) {
                    try {
                        // Open file and draw contents on the map
                        GPX gpx = openFile(data.getData());
                        if(gpx != null) {
                            drawGpx(gpx);
                        } else {
                            Toast.makeText(this, getString(R.string.cannotOpenFile), Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    } catch (SAXException e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                    }
                }

                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            String p = permissions[i];
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            if(granted) {
                onPermissionGranted(p);
            }
        }
    }

    protected void onPermissionGranted(String permission) {
        if(permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            setupLocationIndicator();
        }
    }

    protected final boolean hasPermissions(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    protected final void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
    }

    protected void setupLocationIndicator() {
        mLocationProvider = new LocationProvider(this);

        mMyLocation = new MyLocationNewOverlay(mMapView);
        mMyLocation.enableMyLocation(mLocationProvider);
        mMyLocation.enableFollowLocation();
        mMapView.getOverlayManager().add(mMyLocation);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            openOptionsMenu();                      // Open the options menu on a long-press
        }
    }

    /*
        Open file for viewing on the map.
     */
    private GPX openFile(Uri fileUri) throws Exception {
        // Read from file
        FileParserResolver resolver = new FileParserResolver(this);
        IFileParser parser = resolver.resolve(fileUri);
        return parser != null ? parser.parse(fileUri) : null;
    }

    /*
        Show GPX contents on the map.
     */
    private void drawGpx(GPX gpx) {
        // Show routes
        HashSet<Track> tracks = gpx.getTracks();

        if (tracks != null) {
            Iterator<Track> i = tracks.iterator();
            while (i.hasNext()) {
                final Track track = i.next();
                ArrayList<Waypoint> trackPoints = track.getTrackPoints();

                List<GeoPoint> geoPoints = new ArrayList<>();

                for (Waypoint trackPoint : trackPoints) {
                    GeoPoint gp = new GeoPoint(trackPoint.getLatitude(), trackPoint.getLongitude());
                    geoPoints.add(gp);
                }

                Polyline line = new Polyline();
                Paint paint = line.getOutlinePaint();
                paint.setColor(Color.RED);
                paint.setAlpha(90);
                line.setPoints(geoPoints);
                line.setOnClickListener((polyline, mapView, eventPos) -> {
                    Toast.makeText(mapView.getContext(), "Track: " + track.getName(), Toast.LENGTH_LONG).show();
                    return false;
                });
                mMapView.getOverlayManager().add(line);
            }
        }

        // Show POIs
        HashSet<Waypoint> wpts = gpx.getWaypoints();

        if (wpts != null) {
            Iterator<Waypoint> i = wpts.iterator();
            while (i.hasNext()) {
                Waypoint wpt = i.next();

                // Convert URLs to HTML tags
                String comment = wpt.getComment() != null ? replaceUrls(wpt.getComment()) : "";
                comment = replaceLineFeeds(comment);

                GeoPoint pt = new GeoPoint(wpt.getLatitude(), wpt.getLongitude());
                Marker marker = new Marker(mMapView);
                marker.setInfoWindow(mInfoWindow);
                marker.setPosition(pt);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setPanToView(false);
                marker.setTitle(wpt.getName());
                marker.setSnippet(comment);

                mMapView.getOverlays().add(marker);
            }
        }
    }

    protected String replaceUrls(String text) {
        String result = text;

        Pattern p = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
        Matcher m = p.matcher(text);
        while(m.find()) {
            String url = m.group();
            String linkTag = "<A href=\"" + url + "\">" + url + "</A>";
            result = result.replace(url, linkTag);
        }

        return result;
    }

    protected String replaceLineFeeds(String text) {
        String regex = "(\\r\\n|\\r|\\n)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while(m.find()) {
            String linkTag = "<BR>";
            text = m.replaceFirst(linkTag);
            m = p.matcher(text);
        }
        return text;
    }
}
