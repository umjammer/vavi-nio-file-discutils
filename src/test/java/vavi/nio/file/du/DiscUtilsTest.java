/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;


/**
 * DiscUtilsTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/18 umjammer initial version <br>
 */
class DiscUtilsTest {

    @Test
    void test() throws Exception {
//        String file = "/Users/nsano/Downloads/Play-20170829.dmg";
        String file = "/Users/nsano/Documents/VirtualBox/HardDisks/nsanov2.vdi";
        URI uri = URI.create("discutils:file:" + file);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
