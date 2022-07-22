/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.fge.filesystem.driver.CachedFileSystemDriver;

import vavi.net.fuse.Base;
import vavi.net.fuse.Fuse;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Main4. (fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/11/20 umjammer initial version <br>
 */
@ExtendWith(ExistsLocalPropertiesCondition.class)
@PropsEntity(url = "file://${user.dir}/local.properties")
public class Main4 {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");
    }

    @Property
    String discImage;
    @Property
    String mountPoint;

    FileSystem fs;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {
        PropsEntity.Util.bind(this);

        URI uri = URI.create("discutils:file:" + discImage);

        Map<String, Object> env = new HashMap<>();
        env.put(CachedFileSystemDriver.ENV_IGNORE_APPLE_DOUBLE, true); // mandatory
        env.put("volumeNumber", 1);

        fs = FileSystems.newFileSystem(uri, env);
//Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);

        options = new HashMap<>();
        options.put("fsname", "discutils_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
//        options.put("noapplexattr", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "vavi.net.fuse.javafs.JavaFSFuseProvider",
        "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider",
        "vavi.net.fuse.fusejna.FuseJnaFuseProvider",
    })
    public void test01(String providerClassName) throws Exception {
        System.setProperty("vavi.net.fuse.FuseProvider.class", providerClassName);

        Base.testFuse(fs, mountPoint, options);

        fs.close();
    }

    //

    @Test
    @Disabled("for fucking intellij")
    void testX() throws Exception {
        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");

        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(fs, mountPoint, options);
while (true) { // for jnrfuse
 Thread.yield();
}
        }
    }

    public static void main(String[] args) throws Exception {
//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.javafs.JavaFSFuseProvider");
        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");
//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.fusejna.FuseJnaFuseProvider");

        Main4 app = new Main4();
        app.before();

        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(app.fs, app.mountPoint, app.options);
while (true) { // for jnrfuse
    Thread.yield();
}
        }
    }
}

/* */
