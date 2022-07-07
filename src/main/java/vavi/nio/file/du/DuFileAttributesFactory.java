/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendsdFileAttributesFactory;

import discUtils.core.DiscFileSystemInfo;


/**
 * DuFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileAttributesFactory extends ExtendsdFileAttributesFactory {

    public DuFileAttributesFactory() {
        setMetadataClass(DiscFileSystemInfo.class);
        addImplementation("basic", DuBasicFileAttributesProvider.class);
    }
}
