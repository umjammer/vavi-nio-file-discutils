/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.logging.Level;

import com.github.fge.filesystem.provider.FileSystemProviderBase;

import vavi.util.Debug;


/**
 * DuFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemProvider extends FileSystemProviderBase {

    public DuFileSystemProvider() {
        super(new DuFileSystemRepository());
    }

    /**
     * utility
     * TODO consider more
     */
    public static URI createURI(String path) throws IOException {
        String url = URLEncoder.encode(Paths.get(path).toAbsolutePath().toString(), "utf-8");
        url = url.replace("%2F", "/");
        url = url.replace("+", "%20");
        URI uri = URI.create("discutils:file:" + url);
Debug.println(Level.FINE, "uri: " + uri);
        return uri;
    }
}
