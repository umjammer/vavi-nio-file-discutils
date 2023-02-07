/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.github.fge.filesystem.driver.CachedFileSystemDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
public class Main4 {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property
    String discImage;
    @Property
    int volumeNumber;
    @Property
    String mountPoint;

    FileSystem fs;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {
        PropsEntity.Util.bind(this);

        URI uri = DuFileSystemProvider.createURI(discImage);

        Map<String, Object> env = new HashMap<>();
        env.put(CachedFileSystemDriver.ENV_IGNORE_APPLE_DOUBLE, true); // mandatory
        env.put("volumeNumber", volumeNumber);

        fs = FileSystems.newFileSystem(uri, env);
//Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);

        options = new HashMap<>();
        options.put("fsname", "discutils_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
//        options.put("noapplexattr", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
    }

    @Disabled("no reliable writeable devices")
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
    @DisabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testX() throws Exception {
        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");

        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(fs, mountPoint, options);

            // using cdl cause junit stops awt thread suddenly
            CountDownLatch cdl = new CountDownLatch(1);
            cdl.await(); // terminate by yourself
        }
    }

    /** */
    public static void main(String[] args) throws Exception {
//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.javafs.JavaFSFuseProvider");
        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");
//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.fusejna.FuseJnaFuseProvider");

        Main4 app = new Main4();
        app.before();

        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(app.fs, app.mountPoint, app.options);
        }
    }
}

/* */
