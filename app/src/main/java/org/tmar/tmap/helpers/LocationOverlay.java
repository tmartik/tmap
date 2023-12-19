package org.tmar.tmap.helpers;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class LocationOverlay extends MyLocationNewOverlay {

    public interface IListener {
        public void followLocationChanged(boolean enabled);
    }

    private IListener mListener;

    public LocationOverlay(MapView mapView) {
        super(mapView);
    }

    @Override
    public void enableFollowLocation() {
        super.enableFollowLocation();

        if(mListener != null) {
            mListener.followLocationChanged(mIsFollowing);
        }
    }

    @Override
    public void disableFollowLocation() {
        super.disableFollowLocation();

        if(mListener != null) {
            mListener.followLocationChanged(mIsFollowing);
        }
    }

    public void setListener(IListener listener) {
        mListener = listener;
    }
}
