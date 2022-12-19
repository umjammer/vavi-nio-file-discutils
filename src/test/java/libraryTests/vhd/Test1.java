/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.vhd;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.FileSystemParameters;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.streams.util.Ownership;
import discUtils.vhd.Disk;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-05 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
public class Test1 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vhd")
    String vhd = "src/test/resources/test.vhd";
    @Property(name = "volumeNumber")
    int volumeNumber;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @DisplayName("works avoiding a bug git:3d157bfe")
    void test1() throws Exception {
        try (Stream stream = new FileStream(vhd, FileMode.Open, FileAccess.Read);
             Disk disk = new Disk(stream, Ownership.None)) {
            Stream s = disk.getContent();
Debug.println(s);
        }
    }

    @Test
    @DisplayName("check fixing a bug git:3d157bfe")
    void test2() throws Exception {
        try (Disk disk = new Disk(vhd)) {
            Stream s = disk.getContent();
Debug.println(s);
        }
    }

    @Test
    void test3() throws Exception {
        try (Disk disk = new Disk(vhd)) {
            VolumeManager manager = new VolumeManager();
            manager.addDisk(disk);
            LogicalVolumeInfo lvi = manager.getLogicalVolumes().get(0);
            FileSystemInfo fsi = FileSystemManager.detectFileSystems(lvi).get(0);
            DiscFileSystem fs = fsi.open(lvi, new FileSystemParameters());
Debug.println(fs);
            DiscDirectoryInfo root = fs.getDirectoryInfo("");
Debug.println(root);

            walk(root);

            fs.close();
        }
    }

    /** */
    void walk(DiscDirectoryInfo root) {
        List<DiscFileInfo> files = root.getFiles();
        files.forEach(i -> System.err.println("/" + i.getFullName()));
        List<DiscDirectoryInfo> folders = root.getDirectories();
        folders.forEach(this::walk);
    }
}
