package org.tmar.tmap.document.gpx;

import android.content.Context;
import android.net.Uri;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.GPX;
import org.tmar.tmap.document.FileParserBase;

import java.io.InputStream;

/*
    This class reads in a GPX file and returns its contents in a GPX object.
 */
public class GpxParser extends FileParserBase {
    private Context mContext;

    public GpxParser(Context c) {
        super(new String[] {"gpx"});
        mContext = c;
    }

    @Override
    public GPX parse(Uri fileUri) throws Exception {
        InputStream inputStream = mContext.getContentResolver().openInputStream(fileUri);
        GPXParser p = new GPXParser();
        return p.parseGPX(inputStream);
    }
}
