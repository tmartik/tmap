package org.tmar.tmap.document.json;

import android.content.Context;
import android.net.Uri;

import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tmar.tmap.document.FileParserBase;
import org.tmar.tmap.helpers.FileHelper;

import java.util.HashSet;

/*
    This class reads in a JSON file and returns its contents in a GPX object.
 */
public class JsonParser extends FileParserBase {
    private Context mContext;

    public JsonParser(Context c) {
        super(new String[] {"json"});
        mContext = c;
    }

    @Override
    public GPX parse(Uri fileUri) throws Exception {
        // Read file contents in as text
        String jsonString = FileHelper.getStringFromFile(mContext, fileUri);

        // Parse as JSON
        JSONObject result = new JSONObject(jsonString);

        GPX gpx = new GPX();

        JSONArray wpts = result.getJSONArray("waypoints");

        HashSet<Waypoint> w = new HashSet<Waypoint>();

        for (int i = 0; i < wpts.length(); i++) {
            JSONObject wpt = wpts.getJSONObject(i);
            Waypoint waypoint = new Waypoint();
            waypoint.setName(wpt.getString("name"));
            waypoint.setLatitude(wpt.getDouble("lat"));
            waypoint.setLongitude(wpt.getDouble("lon"));
            waypoint.setComment(wpt.getString("cmt"));
            w.add(waypoint);
        }

        gpx.setWaypoints(w);

        // Tracks
        HashSet<Track> t = new HashSet<Track>();
        gpx.setTracks(t);

        return gpx;
    }
}
