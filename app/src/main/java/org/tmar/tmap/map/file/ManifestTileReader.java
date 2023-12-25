package org.tmar.tmap.map.file;

import android.location.Location;

import org.json.JSONObject;
import org.tmar.tmap.helpers.FileHelper;
import org.tmar.tmap.map.ITileReader;

import java.io.File;

public abstract class ManifestTileReader implements ITileReader {
    protected final String mPath;
    protected final String mType;

    /*
        Initialize with a configuration file. The file specifies the details of the archive.

        The mapSpec is a JSON-formatted file, which has the following properties:
            name - the name of the map
            type - either "zipFolders" or "files"

     */
    public ManifestTileReader(String path) throws Exception {
        File mapSpec = new File(path);
        mPath = mapSpec.getParent();

        if(!mapSpec.getName().toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Wrong file type.");
        }

        String json = FileHelper.getStringFromFile(mapSpec.getAbsolutePath());
        JSONObject jObject = new JSONObject(json);
        mType = jObject.getString("type");
    }

    @Override
    public String getName() {
        File path = new File(mPath);
        return path.getName();
    }

    @Override
    public Location getDefaultLocation() {
        return null;
    }

    @Override
    public int getDefaultZoom() {
        return 0;
    }
}
