/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.logging.Level;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.FileSystemParameters;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeManager;
import discUtils.core.internal.VirtualDiskFactory;
import dotnet4j.io.FileAccess;
import vavi.util.Debug;


/**
 * DuFileSystemRepository.
 * <p>
 * env
 * <ul>
 * <li> forceType ... "RAW" {@link VirtualDiskFactory}
 * <li> volumeNumber ... number {@link LogicalVolumeInfo}
 * </ul>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemRepository extends FileSystemRepositoryBase {

    /** */
    public DuFileSystemRepository() {
        super("discutils", new DuFileSystemFactoryProvider());
    }

    /**
     * @param uri use {@link DuFileSystemProvider#createURI(String)}
     * @throws IllegalArgumentException only "file" scheme is supported, or uri syntax error
     * @throws NoSuchElementException required values are not in env
     * @throws IndexOutOfBoundsException no suitable {@link LogicalVolumeInfo} or {@link FileSystemInfo}
     */
    @Override
    public FileSystemDriver createDriver(URI uri, Map<String, ?> env) throws IOException {
        String[] rawSchemeSpecificParts = uri.getRawSchemeSpecificPart().split("!");
Debug.println(Level.FINE, "part[0]: " + rawSchemeSpecificParts[0]);
        URI filePart = URI.create(rawSchemeSpecificParts[0]);
        if (!"file".equals(filePart.getScheme())) {
            // currently only support "file"
            throw new IllegalArgumentException(filePart.toString());
        }
        String file = rawSchemeSpecificParts[0].substring("file:".length());
        // TODO virtual relative directory from rawSchemeSpecificParts[1]

        String forceType = null;
        if (env.containsKey("forceType")) {
            forceType = (String) env.get("forceType");
        }

        int volumeNumber = 0;
        if (env.containsKey("volumeNumber")) {
            volumeNumber = (int) env.get("volumeNumber");
        }

Debug.println(Level.FINE, "file: " + file);

        VirtualDisk disk = VirtualDisk.openDisk(file, forceType, FileAccess.Read, null, null);
Debug.println(Level.FINE, "disk: " + disk);
        VolumeManager manager = new VolumeManager();
Debug.println(Level.FINE, "manager: " + manager);
        manager.addDisk(disk);
        LogicalVolumeInfo lvi = manager.getLogicalVolumes().get(volumeNumber);
Debug.println(Level.FINE, "lvi: " + lvi + " / " + manager.getLogicalVolumes().size());
        FileSystemInfo fsi = FileSystemManager.detectFileSystems(lvi).get(0);
Debug.println(Level.FINE, "fsi: " + fsi + " / " + FileSystemManager.detectFileSystems(lvi).size());
        DiscFileSystem fs = fsi.open(lvi, new FileSystemParameters());
Debug.println(Level.FINE, "fs: " + fs);
        DuFileStore fileStore = new DuFileStore(fs, factoryProvider.getAttributesFactory());
        return new DuFileSystemDriver(fileStore, factoryProvider, fs, env);
    }

    /* ad-hoc hack for ignoring checking opacity */
    @Override
    protected void checkURI(URI uri) {
        Objects.requireNonNull(uri);
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("uri is not absolute");
        }
        if (!getScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException("bad scheme");
        }
    }
}
