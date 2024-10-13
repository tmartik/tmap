package org.tmar.tmap;

import org.osmdroid.api.IGeoPoint;

import java.io.File;

public class MapDescriptor {
    private String mName;
    private File mFile;
    private boolean mVisible;
    private boolean mOverlay;
    private boolean mShared;
    private IGeoPoint mCenter;
    private int mDefaultZoom;

    MapDescriptor(String name, File file, boolean visible, boolean overlay, boolean shared, IGeoPoint center, int defaultZoom) {
        mName = name;
        mFile = file;
        mVisible = visible;
        mOverlay = overlay;

        mShared = shared;
        mCenter = center;
        mDefaultZoom = defaultZoom;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void setVisible(boolean mVisible) {
        this.mVisible = mVisible;
    }

    public boolean isShared() {
        return mShared;
    }

    public void setShared(boolean mShared) {
        this.mShared = mShared;
    }

    public String getName() {
        return mName;
    }

    public boolean isOverlay() {
        return mOverlay;
    }

    public IGeoPoint getCenter() {
        return mCenter;
    }

    public int getDefaultZoom() {
        return mDefaultZoom;
    }
}
