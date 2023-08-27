package org.tmar.tmap.map.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipCache {

    private static ZipCache[] mInstance = new ZipCache[18];

    private int mZoom = 0;
    private ZipEntry[][] mEntries = new ZipEntry[10][10];
    private ZipFile mZipFile;
    private boolean cached;

    private ZipCache(int z) {
        mZoom = z;
    }

    public static ZipCache instance(int z) {
        if(mInstance[z] == null) {
            mInstance[z] = new ZipCache(z);
        }
        return mInstance[z];
    }

    public static void clear() {
        for (ZipCache zc : mInstance) {
            if(zc != null) {
                zc.mEntries = new ZipEntry[10][10];
                zc.mZipFile = null;
                zc.cached = false;
            }
        }
    }

    public InputStream getTile(String path, int x, int y) throws IOException {
        final String filename = path + File.separator + mZoom + ".zip";

        if(!cached) {
            mEntries = new ZipEntry[10][10];
            File f = new File(filename);
            if (f.exists()) {
                mZipFile = new ZipFile(filename);
                cache();
                cached = true;
            } else {
                // Stripe files (one zip would be too large for FAT32)
                final String subFilename = path + File.separator + mZoom + File.separator + x + ".zip";
                f = new File(subFilename);
                if(f.exists()) {
                    ZipStripeCache stripe = ZipStripeCache.instance(subFilename);
                    return stripe.getTile(y);
                } else {
                    return null;
                }
            }
        }

        InputStream stream = null;

        // Take entry from cache
        if(x < mEntries.length) {
            ZipEntry[] line = mEntries[x];
            if(y < line.length) {
                ZipEntry entry = line[y];
                if(entry != null) {
                    stream = mZipFile.getInputStream(entry);
                }
            }
        }

        return stream;
    }

    private void cache() throws IOException {
        Enumeration<? extends ZipEntry> entries = mZipFile.entries();

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            if(!entry.isDirectory()) {
                String path = entry.getName();
                String[] folders = path.split("\\/");
                if(folders.length > 0) {
                    String fullName = folders[folders.length - 1];
                    String[] nameParts = fullName.split("\\.");
                    if(nameParts.length > 0) {
                        int x = Integer.parseInt(folders[0]);
                        int y = Integer.parseInt(nameParts[0]);

                        InputStream stream = mZipFile.getInputStream(entry);

                        // Save to cache
                        if(x >= mEntries.length) {
                            // Must re-allocate
                            int newWidth = x + 10;
                            mEntries = expandArrayWidth(mEntries, newWidth);
                        }

                        ZipEntry[] line = mEntries[x];

                        if(y >= line.length) {
                            // Must re-allocate
                            int newHeight = y + 1;
                            mEntries[x] = expandArray(line, newHeight);
                        }

                        // Save to cache
                        mEntries[x][y] = entry;
                    }
                }
            }
        }
    }

    private static ZipEntry[][] expandArrayWidth(ZipEntry[][] array, int width) {
        ZipEntry[][] newArray = new ZipEntry[width][];

        int height = array[0].length;

        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = i < array.length ? array[i] : new ZipEntry[height];
        }

        return newArray;
    }

    private static ZipEntry[] expandArray(ZipEntry[] array, int newSize) {
        ZipEntry[] newArray = new ZipEntry[newSize];

        System.arraycopy(array, 0, newArray, 0, array.length);

        return newArray;
    }
}
