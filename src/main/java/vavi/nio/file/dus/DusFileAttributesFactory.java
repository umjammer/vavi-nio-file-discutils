/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.dus;

import com.github.fge.filesystem.attributes.FileAttributesFactory;

import DiscUtils.Core.VirtualDisk;


/**
 * DusFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class DusFileAttributesFactory extends FileAttributesFactory {

    public DusFileAttributesFactory() {
        setMetadataClass(VirtualDisk.class);
        addImplementation("basic", DusBasicFileAttributesProvider.class);
    }
}
