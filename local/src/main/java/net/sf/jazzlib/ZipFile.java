package net.sf.jazzlib;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Stub for net.sf.jazzlib.ZipFile, originally bundled with epublib.
 * Mimics java.util.zip.ZipFile API.
 */
public class ZipFile implements Closeable {

    public ZipFile(File file) throws IOException {
        throw new UnsupportedOperationException("stub");
    }

    public ZipFile(String name) throws IOException {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("stub");
    }
}
