/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.dus;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * DusFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class DusFileSystemProvider extends FileSystemProviderBase {

    public static final String PARAM_ID = "id";

    public DusFileSystemProvider() {
        super(new DusFileSystemRepository());
    }
}
