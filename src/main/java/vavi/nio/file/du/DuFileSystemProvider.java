/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * DuFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemProvider extends FileSystemProviderBase {

    public static final String PARAM_ID = "id";

    public DuFileSystemProvider() {
        super(new DuFileSystemRepository());
    }
}
