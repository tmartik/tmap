package org.tmar.tmap;

import java.io.File;

public class MapDescriptor {
    private String mName;
    private File mFile;
    private boolean mVisible;
    private boolean mOverlay;
    private boolean mShared;

    MapDescriptor(String name, File file, boolean visible, boolean overlay, boolean shared) {
        mName = name;
        mFile = file;
        mVisible = visible;
        mOverlay = overlay;

        mShared = shared;
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
}
