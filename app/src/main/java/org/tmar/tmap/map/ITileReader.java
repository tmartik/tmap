package org.tmar.tmap.map;

import java.io.InputStream;

/*
    Interface for reading map tiles from a storage.
 */
public interface ITileReader {
    public String getName();
    /*
        Returns an input stream for the given map tile. The caller will close the stream.
     */
    public InputStream getTile(int z, int x, int y);
}
