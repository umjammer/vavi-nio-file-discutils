/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.chd;

import java.nio.file.LinkOption;

import com.github.fge.filesystem.options.FileSystemOptionsFactory;


/**
 * ChdFileSystemOptionsFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2025/09/30 umjammer initial version <br>
 */
public class ChdFileSystemOptionsFactory extends FileSystemOptionsFactory {

    public ChdFileSystemOptionsFactory() {
        addLinkOption(LinkOption.NOFOLLOW_LINKS);
    }
}
