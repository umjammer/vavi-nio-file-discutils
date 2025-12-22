/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.emu;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemParameters;
import discUtils.core.VolumeInfo;
import discUtils.core.vfs.VfsFileSystemFactory;
import discUtils.core.vfs.VfsFileSystemInfo;
import discUtils.fat.ATBootSector;
import discUtils.fat.BootSector;
import discUtils.fat.FatFileSystem;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;
import vavi.emu.disk.LogicalDisk;


/**
 * FileSystemFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-12-16 nsano initial version <br>
 */
public class FileSystemFactory implements VfsFileSystemFactory {

    /** TODO using thread local */
    public static ThreadLocal<LogicalDisk> logicalDisk = new InheritableThreadLocal<>();

    @Override
    public FileSystemInfo[] detect(Stream stream, VolumeInfo volume) {
        if (logicalDisk.get() != null) {
            return new FileSystemInfo[] {
                    new VfsFileSystemInfo("EMU", logicalDisk.get().getClass().getSimpleName(), this::open)
            };
        }

        return new FileSystemInfo[0];
    }

    private DiscFileSystem open(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters) {
        return new EmuFileSystem(stream, logicalDisk.get(), Ownership.None, parameters);
    }
}
