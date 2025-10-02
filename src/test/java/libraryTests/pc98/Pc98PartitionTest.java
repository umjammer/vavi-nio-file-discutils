/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.pc98;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
 * Pc98PartitionTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-15 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class Pc98PartitionTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String nhd;

    @BeforeEach
    void before() throws IOException {
        if (localPropertiesExists()) {
        PropsEntity.Util.bind(this);
    }
    }

    @Test
    @DisplayName("api")
    void test1() throws Exception {
Debug.println("file: " + nhd);

        VirtualDisk disk = VirtualDisk.openDisk(nhd, "EMU", FileAccess.Read, null, null);
Debug.println("disk: " + disk);
        VolumeManager manager = new VolumeManager();
Debug.println("manager: " + manager);
        manager.addDisk(disk);
        LogicalVolumeInfo lvi = manager.getLogicalVolumes().get(0);
Debug.println("lvi: " + lvi + " / " + manager.getLogicalVolumes().size());
        FileSystemInfo fsi = FileSystemManager.detectFileSystems(lvi).get(0);
Debug.println("fsi: " + fsi + " / " + FileSystemManager.detectFileSystems(lvi).size());
        assertTrue(fsi.toString().contains("PC98_FAT"));
        DiscFileSystem fs = fsi.open(lvi, new FileSystemParameters());
Debug.println("fs: " + fs);
        assertInstanceOf(FatFileSystem.class, fs);
    }

    @Test
    @DisplayName("via spi")
    void test2() throws Exception {
        String file = nhd;
        URI uri = DuFileSystemProvider.createURI(file);
        Map<String, Object> env = new HashMap<>();
        env.put("forceType", "EMU"); // works w/o this env value
        FileSystem fs = new DuFileSystemProvider().newFileSystem(uri, env);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        fs.close();
    }
}
