package org.tmar.tmap.view;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

/*
    This class defines the layout of a map marker window.
 */
public class PoiInfoWindow extends MarkerInfoWindow {
    private Context mContext;
    public PoiInfoWindow(Context c, int layoutResId, MapView mapView) {
        super(layoutResId, mapView);

        mContext = c;
    }

    @Override public void onOpen(Object item) {
        super.onOpen(item);

        String packageName = mContext.getPackageName();  // get application package name
        int id = mContext.getResources().getIdentifier("id/bubble_description", null, packageName);
        TextView linkTextView = (TextView)mView.findViewById(id);
        linkTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}