package org.tmar.tmap.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

/*
    Location provider for MapView.

    Make sure to obtain fine location permission before using this class.
 */
public class LocationProvider implements IMyLocationProvider {
    private final Context mContext;
    private LocationManager mLocationManager;
    private IMyLocationConsumer mLocationConsumer;

    public LocationProvider(Context c) {
        mContext = c;
    }

    /*
        Pass location update event to consumer.
     */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocationConsumer.onLocationChanged(location, null);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /*
        Start listening for location updates. Fine location permission must have been obtained before calling.
     */
    @SuppressLint("MissingPermission")
    @Override
    public boolean startLocationProvider(IMyLocationConsumer locationConsumer) {
        mLocationConsumer = locationConsumer;
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000, 15, mLocationListener);
        return true;
    }

    @Override
    public void stopLocationProvider() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location getLastKnownLocation() {
        return mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    }

    @Override
    public void destroy() {
        stopLocationProvider();
        mLocationManager = null;
        mLocationConsumer = null;
    }
}
