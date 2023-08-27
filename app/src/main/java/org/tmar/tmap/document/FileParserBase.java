package org.tmar.tmap.document;

import org.tmar.tmap.document.IFileParser;

public abstract class FileParserBase implements IFileParser {

    private static String[] supportedExtensions;

    public FileParserBase(String[] extensions) {
        supportedExtensions = extensions;
    }

    @Override
    public String[] supportedExtensions() {
        return supportedExtensions;
    }
}
