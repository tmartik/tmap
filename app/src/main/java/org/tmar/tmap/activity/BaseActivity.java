package org.tmar.tmap.activity;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

import android.util.DisplayMetrics;
import android.view.GestureDetector;
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
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.overlay.Overlay;
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
import org.tmar.tmap.BuildConfig;
import org.tmar.tmap.MapApplication;
import org.tmar.tmap.helpers.LocationOverlay;
import org.tmar.tmap.map.LocationProvider;
import org.tmar.tmap.view.PoiInfoWindow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    An activity that implements generic map browsing functions.
 */
public class BaseActivity extends Activity {
    private static final int OPENFILE_RESULT_CODE = 9;
    private static final int SHOWMAPS_RESULT_CODE = 10;
    private static final int SHOWDOCUMENT_RESULT_CODE = 12;
    private static final int PERMISSION_CODE = 11;

    protected MapApplication mApp;

    private IMyLocationProvider mLocationProvider = new LocationProvider(this);

    protected MapView mMapView;
    protected LocationOverlay mMyLocation;
    private MarkerInfoWindow mInfoWindow;

    protected SharedPreferences mPref;
    private String mPrefsFileName;
    private List<Overlay> mPermanentOverlays = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (MapApplication) getApplication();

        try {
            // Get preference file name for current activity from activity metadata (see the manifest)
            PackageManager pm = getPackageManager();
            ActivityInfo ai = pm.getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA);
            mPrefsFileName = ai.metaData.getString("prefsName", "prefs");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        mPref = getApplicationContext().getSharedPreferences(mPrefsFileName, 0);      // 0 - for private mode

        applyPreferences();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);         // Show application on lock screen for quick access
        setContentView(R.layout.activity_main);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);      // Set user-agent
        mMapView = (MapView) findViewById(R.id.mapView);

        mMapView.setBuiltInZoomControls(false);
        mMapView.setMultiTouchControls(true);
        mMapView.setMinZoomLevel(5.0);
        mMapView.setMaxZoomLevel(18.0);
        mMapView.setUseDataConnection(false);

        // Add permanent overlays

        // Scalebar
        final DisplayMetrics dm = getResources().getDisplayMetrics();       // TODO: Scale according to DPI!
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setLineWidth(5);
        mScaleBarOverlay.setTextSize(25);
        addPermanentOverlay(mScaleBarOverlay);

        // Read map location from preferences
        if(getIntent().getData() == null) {
            // Restore location only if started without params
            IGeoPoint geoPoint = new GeoPoint(mPref.getFloat("Lat", 60), mPref.getFloat("Lon", 5));
            mMapView.getController().setCenter(geoPoint);
            mMapView.getController().setZoom(mPref.getFloat("Zoom", 5));
        }

        mMapView.setOnTouchListener(new View.OnTouchListener() {
            private final GestureDetector mGestureDetector = new GestureDetector(BaseActivity.this, new GestureListener());

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mGestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        mInfoWindow = new PoiInfoWindow(this, R.layout.bubble_layout, mMapView);

        mMyLocation = new LocationOverlay(mMapView);
        Bitmap locationBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.location);
        mMyLocation.setDirectionArrow(locationBitmap, locationBitmap);
        mMyLocation.setPersonHotspot(locationBitmap.getWidth() / 2.0F - 0.5F, locationBitmap.getHeight() / 2.0F - 0.5F);    // Calculation based on: https://github.com/osmdroid/osmdroid/pull/1460/files
        mMyLocation.setListener(enabled -> {
            mApp.setFollowEnabled(enabled);
        } );

        // Location indicator
        setupLocationIndicator();

        View menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOptionsMenu();
            }
        });

        // Open file from intent
        mMapView.addOnFirstLayoutListener(new MapView.OnFirstLayoutListener() {
            @Override
            public void onFirstLayout(View v, int left, int top, int right, int bottom) {
                Uri uri = getIntent().getData();
                if(uri != null) {
                    openGpx(uri);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if(mMyLocation != null) {
            mMyLocation.enableMyLocation();

            if(mApp.followEnabled()) {
                mMyLocation.enableFollowLocation();
            }
        }

        // Show open documents
        updateDocuments();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        if(mMyLocation != null) {
            mMyLocation.disableMyLocation();
        }

        // save current location and zoom level to preferences
        SharedPreferences.Editor editor = mPref.edit();

        // TODO: save open files: editor.putString("", "");

        editor.putFloat("Lat", (float) mMapView.getMapCenter().getLatitude());
        editor.putFloat("Lon", (float) mMapView.getMapCenter().getLongitude());
        editor.putFloat("Zoom", (float) mMapView.getZoomLevelDouble());
        editor.commit();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
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

            item.setVisible(submenu.size() > 0);
        }

        MenuItem followMenuItem = menu.findItem(R.id.follow);
        followMenuItem.setEnabled(mMyLocation != null && !mMyLocation.isFollowLocationEnabled());

        invalidateOptionsMenu();        // SEE: https://issuetracker.google.com/issues/315761686?pli=1

        return super.onPrepareOptionsMenu(menu);
    }

    protected void onPrepareLayerMenu(SubMenu subMenu) {
        // Empty default implementation
    }

    protected void reloadMap() {
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
                chooseFile.setType("application/*");

                chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                chooseFile.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                startActivityForResult(chooseFile, OPENFILE_RESULT_CODE);
                return true;
            case R.id.showOpenFiles:
                Intent documentsIntent = new Intent(this, DocumentActivity.class);
                startActivityForResult(documentsIntent, SHOWDOCUMENT_RESULT_CODE);
                return true;
            case R.id.showMaps:
                Intent intent = new Intent(this, MapsActivity.class);
                startActivityForResult(intent, SHOWMAPS_RESULT_CODE);
                return true;
            case R.id.openSettings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OPENFILE_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    openGpx(data.getData());
                }

                break;
            case SHOWMAPS_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    reloadMap();
                }
                break;
            case SHOWDOCUMENT_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    openGpx(data.getData());
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
        setupLocationIndicator();
    }

    protected final boolean hasPermissions(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    protected final void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
    }

    protected void setupLocationIndicator() {
        if(hasPermissions(Manifest.permission.ACCESS_FINE_LOCATION)) {
            mLocationProvider = new LocationProvider(this);
            addPermanentOverlay(mMyLocation);      // TODO: make sure this stays on top!
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            openOptionsMenu();                      // Open the options menu on a long-press
        }
    }

    protected void addPermanentOverlay(Overlay overlay) {
        if(!mMapView.getOverlayManager().contains(overlay)) {
            mMapView.getOverlayManager().add(overlay);
        }

        if(!mPermanentOverlays.contains(overlay)) {
            mPermanentOverlays.add(overlay);
        }
    }

    protected void removePermanentOverlay(Overlay overlay) {
        mMapView.getOverlayManager().remove(overlay);
        mPermanentOverlays.remove(overlay);
    }

    protected void makeLocationIndicatorTopmost() {
        mMapView.getOverlayManager().remove(mMyLocation);
        mMapView.getOverlayManager().add(mMyLocation);
    }

    // Remove all non-permanent overlays (i.e. documents)
    protected void clearDocuments() {
        List<Overlay> overlays = new ArrayList<>(mMapView.getOverlayManager());

        for (Overlay l : overlays) {
            if(!mPermanentOverlays.contains(l)) {
                mMapView.getOverlayManager().remove(l);
            }
        }
    }

    protected void updateDocuments() {
        clearDocuments();

        List<GPX> documents = mApp.getOpenFiles();
        for (GPX gpx : documents) {
            drawGpx(gpx);
        }

        makeLocationIndicatorTopmost();
    }
    /*
        Show GPX contents on the map.
     */
    private void drawGpx(GPX gpx) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

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
                String color = preferences.getString("trackColor", "#0000FF");
                int c = Color.parseColor(color);
                paint.setColor(c);
                paint.setAlpha(255 * preferences.getInt("trackAlpha", 80) / 100);
                paint.setStrokeWidth(preferences.getInt("trackWidth", 20));
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

                mMapView.getOverlayManager().add(marker);
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

    private void applyPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Iterator<String> iter = preferences.getAll().keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            sharedPreferenceChangeListener.onSharedPreferenceChanged(preferences, key);
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        switch (key) {
            case "screenOn":
                keepScreenOn(sharedPreferences.getBoolean(key, false));
                break;
            case "orientation":
                String orientation = sharedPreferences.getString(key, "-1");
                setOrientation(Integer.parseInt(orientation));
                break;
            case "fullscreen":
                setFullscreen(sharedPreferences.getBoolean(key, false));
                break;
            case "systemBrightness":
            case "brightness":
                setScreenBrightness(sharedPreferences.getBoolean("systemBrightness", true) ? -1 : sharedPreferences.getInt("brightness", -1));
                break;
        }
    };

    private void keepScreenOn(boolean enabled) {
        if (enabled) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setOrientation(int orientation) {
        setRequestedOrientation(orientation);
    }

    private void setFullscreen(boolean enabled) {
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        if(enabled) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
        }
    }

    private void setScreenBrightness(int percentage) {
        float brightness = percentage > 0 ? percentage / 100F : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness;
        getWindow().setAttributes(layoutParams);
    }

    private void openGpx(Uri uri) {
        try {
            // Open file and draw contents on the map
            GPX gpx = mApp.findDocumentByUri(uri);

            if(gpx == null) {
                // This file is not yet open
                gpx = mApp.openFile(uri);
                drawGpx(gpx);
                
                makeLocationIndicatorTopmost();
            }

            // Zoom to the document
            mMyLocation.disableFollowLocation();
            RectF bbox = mApp.getDocumentBoundingBox(gpx);
            if(bbox != null) {
                BoundingBox box = new BoundingBox(bbox.top, bbox.right, bbox.bottom, bbox.left);
                mMapView.zoomToBoundingBox(box, true);
            }
        } catch (Exception e) {
            mApp.handleException(e);
        }
    }
}
