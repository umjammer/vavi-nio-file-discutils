/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemParameters;
import discUtils.core.VolumeInfo;
import discUtils.core.coreCompat.EncodingHelper;
import discUtils.core.vfs.VfsFileSystemFactory;
import discUtils.core.vfs.VfsFileSystemInfo;
import discUtils.fat.BootSector;
import discUtils.fat.FatFileSystem;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.Debug;
import vavi.util.serdes.Serdes;
import vavix.io.fat.PC98BiosParameterBlock;


/**
 * Pc98FileSystemFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-24 nsano initial version <br>
 */
public class Pc98FileSystemFactory implements VfsFileSystemFactory {

    @Override public FileSystemInfo[] detect(Stream stream, VolumeInfo volume) {
        if (detectFAT(stream)) {
            return new FileSystemInfo[] {
                new VfsFileSystemInfo("PC98_FAT", "NEC FAT", this::open)
            };
        }

        return new FileSystemInfo[0];
    }

    private DiscFileSystem open(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters) {
        BootSector bs = new Pc98BootSector(stream);
        parameters.setFileNameEncoding(EncodingHelper.forCodePage(932)); // TODO system property
        return new FatFileSystem(stream, bs, Ownership.None, parameters);
    }

    /**
     * Detects if a stream contains a FAT file system.
     *
     * @param stream The stream to inspect.
     * @return {@code true} if the stream appears to be a FAT file system, else
     *         {@code false}.
     */
    private static boolean detectFAT(Stream stream) {
        if (stream.getLength() < 512) {
Debug.println(Level.FINE, "stream length < 512");
            return false;
        }

        stream.position(0);
        byte[] sector = StreamUtilities.readExact(stream, 512);
        ByteArrayInputStream bais = new ByteArrayInputStream(sector);

        PC98BiosParameterBlock bpb;
        try {
            bpb = new PC98BiosParameterBlock();
            Serdes.Util.deserialize(bais, bpb);
        } catch (IOException e) {
Debug.println(Level.FINE, e);
            return false;
        }

        return bpb.validate();
    }
}
