/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.du;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeManager;
import dotnet4j.io.FileAccess;


/**
 * DuFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/11/17 umjammer initial version <br>
 */
public final class DuFileSystemRepository extends FileSystemRepositoryBase {

    /** */
    public DuFileSystemRepository() {
        super("discutils", new DuFileSystemFactoryProvider());
    }

    /**
     * TODO root from uri
     *
     * @throws NoSuchElementException required values are not in env
     */
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        try {
            String[] rawSchemeSpecificParts = uri.getRawSchemeSpecificPart().split("!");
            URI file = new URI(rawSchemeSpecificParts[0]);
            if (!"file".equals(file.getScheme())) {
                // currently only support "file"
                throw new IllegalArgumentException(file.toString());
            }
            // TODO virtual relative directory from rawSchemeSpecificParts[1]

//Debug.println("file: " + Paths.get(file).toAbsolutePath());

            VirtualDisk disk = VirtualDisk.openDisk(Paths.get(file).toAbsolutePath().toString(), null, FileAccess.Read, null, null);
            VolumeManager manager = new VolumeManager();
            manager.addDisk(disk);
            LogicalVolumeInfo lvi = manager.getLogicalVolumes().get(0);
            FileSystemInfo fsi = FileSystemManager.detectFileSystems(lvi).get(0);
            DiscFileSystem fs = fsi.open(lvi, new FileSystemParameters());
            final DuFileStore fileStore = new DuFileStore(fs, factoryProvider.getAttributesFactory());
            return new DuFileSystemDriver(fileStore, factoryProvider, fs, env);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* ad-hoc hack for ignoring checking opacity */
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
