/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.qcow2;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.FileSystemParameters;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeManager;
import discUtils.fat.FatFileSystem;
import dotnet4j.io.FileAccess;
import vavi.nio.file.du.DuFileSystemProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-10-03 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String qcow2;

    @Property(name = "qcow2.vn")
    int volumeNumber;

    @BeforeEach
    void before() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("api")
    void test1() throws Exception {
Debug.println("file: " + qcow2);

        VirtualDisk disk = VirtualDisk.openDisk(qcow2, "QCOW2", FileAccess.Read, null, null);
Debug.println("disk: " + disk);
        VolumeManager manager = new VolumeManager();
Debug.println("manager: " + manager);
        manager.addDisk(disk);
        for (LogicalVolumeInfo lvi : manager.getLogicalVolumes()) {
Debug.println("lvi: " + lvi + " / " + manager.getLogicalVolumes().size());
            for (FileSystemInfo fsi : FileSystemManager.detectFileSystems(lvi)) {
Debug.println("fsi: " + fsi + " / " + FileSystemManager.detectFileSystems(lvi).size());
                DiscFileSystem fs = fsi.open(lvi, new FileSystemParameters());
Debug.println("fs: " + fs);
                fs.close();
            }
        }
    }

    @Test
    @DisplayName("via spi")
    void test2() throws Exception {
        String file = qcow2;
        URI uri = DuFileSystemProvider.createURI(file);
Debug.println(uri);
        Map<String, Object> env = new HashMap<>();
        env.put("volumeNumber",  volumeNumber);
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        fs.close();
    }
}
