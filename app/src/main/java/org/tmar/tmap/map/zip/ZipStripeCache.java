package org.tmar.tmap.map.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipStripeCache
{
    private static HashMap<String, ZipStripeCache> mInstances = new HashMap<>();

    public static ZipStripeCache instance(String path) throws IOException {
        ZipStripeCache inst = mInstances.get(path);

        if(inst == null) {
            inst = new ZipStripeCache(path);
            mInstances.put(path, inst);
        }

        return inst;
    }

    private ZipFile mZipFile;
    private HashMap<Integer, ZipEntry> mMap = new HashMap<>();

    private ZipStripeCache(String filename) throws IOException {
        mZipFile = new ZipFile(filename);
        Enumeration<? extends ZipEntry> entries = mZipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String name = entry.getName();
                String[] nameParts = name.split("\\.");
                if (nameParts.length > 0) {
                    int y = Integer.parseInt(nameParts[0]);
                    mMap.put(new Integer(y), entry);
                }
            }
        }
    }

    public InputStream getTile(int y) throws IOException {
        ZipEntry entry = mMap.get(new Integer(y));
        return mZipFile.getInputStream(entry);
    }
}
