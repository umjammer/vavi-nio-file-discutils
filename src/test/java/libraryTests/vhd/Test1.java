/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.vhd;

import java.nio.file.Files;
import java.nio.file.Paths;

import discUtils.streams.util.Ownership;
import discUtils.vhd.Disk;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-05 nsano initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
public class Test1 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vhd")
    String vhd = "src/test/resources/test.vhd";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    void test1() throws Exception {
        try (Stream stream = new FileStream(vhd, FileMode.Open, FileAccess.Read);
             Disk disk = new Disk(stream, Ownership.None)) {
            Stream s = disk.getContent();
Debug.println(s);
        }
    }

    @Test
    void test2() throws Exception {
        try (Disk disk = new Disk(vhd)) {
            Stream s = disk.getContent();
Debug.println(s);
        }
    }
}
