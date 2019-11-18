/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;


/**
 * DuFileSystemFactoryProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemFactoryProvider extends FileSystemFactoryProvider {

    public DuFileSystemFactoryProvider() {
        setAttributesFactory(new DuFileAttributesFactory());
        setOptionsFactory(new DuFileSystemOptionsFactory());
    }
}
