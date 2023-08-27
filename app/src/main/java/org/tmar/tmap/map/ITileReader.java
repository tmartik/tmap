package org.tmar.tmap.map;

import java.io.InputStream;

/*
    Interface for reading map tiles from a storage.
 */
public interface ITileReader {
    /*
        Returns an input stream for the giben map tile. The caller will close the stream.
     */
    public InputStream getTile(int z, int x, int y);
}
