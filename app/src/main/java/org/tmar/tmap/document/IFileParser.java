package org.tmar.tmap.document;

import android.net.Uri;

import org.alternativevision.gpx.beans.GPX;

/*
    File parser interface.
 */
public interface IFileParser {
    /*
        Returns a list of file extension the class is able to read.
     */
    String[] supportedExtensions();

    /*
        Parses the given file and returns its contents as a GPX object.
     */
    GPX parse(Uri fileUri) throws Exception;
}
