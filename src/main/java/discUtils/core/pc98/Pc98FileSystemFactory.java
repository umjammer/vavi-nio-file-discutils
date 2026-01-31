/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

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
import vavi.util.StringUtil;
import vavi.util.serdes.Serdes;
import vavix.io.fat.PC98BiosParameterBlock;
import vavix.io.partition.Validator;

import static java.lang.System.getLogger;


/**
 * Pc98FileSystemFactory.
 * <p>
 * system property
 * <li>{@link PC98BiosParameterBlock#VALIDATION_KEY} ... default true</li>
 * <li>{@code "vavix.io.partition.validator.fat"} ... , validator for finding fat literal default is {@code false}</li>
 * <li>{@code "vavix.io.partition.validator.ipl"} ... , validator for finding ipl literal default is {@code true}</li>
 * <li>{@code "vavix.io.partition.validator.nec"} ... , validator for finding nec literal, default is {@code true}</li>
 * </p>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-24 nsano initial version <br>
 */
public class Pc98FileSystemFactory implements VfsFileSystemFactory {

    private static final Logger logger = getLogger(Pc98FileSystemFactory.class.getName());

    /** */
    private static final List<Validator> validators;

    static {
        validators = ServiceLoader.load(Validator.class).stream().map(Provider::get).sorted(Comparator.comparingInt(Validator::weight)).toList();
        logger.log(Level.TRACE, validators.stream().map(v -> v.getClass().getSimpleName()).toList());
    }

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
logger.log(Level.DEBUG, "stream length < 512");
            return false;
        }

        stream.position(0);
        byte[] sector = StreamUtilities.readExact(stream, 512);
logger.log(Level.TRACE, "\n" + StringUtil.getDump(sector));
        boolean matches = validators.stream().filter(Validator::enabled).anyMatch(v -> v.validate(sector));
logger.log(Level.TRACE, "validators any match: " + matches + "\n" + String.join("\n", validators.stream().filter(Validator::enabled).map(v ->  v.getClass().getSimpleName() + ": " + v.validate(sector)).toList()));
        if (!matches) return false;

//logger.log(Level.DEBUG, "\n" + StringUtil.getDump(sector, 64));
        ByteArrayInputStream bais = new ByteArrayInputStream(sector);
        PC98BiosParameterBlock bpb;
        try {
            bpb = new PC98BiosParameterBlock();
            Serdes.Util.deserialize(bais, bpb);
            bpb.compute();
        } catch (IOException e) {
logger.log(Level.DEBUG, e);
            return false;
        }

logger.log(Level.DEBUG, "bpb.fileSystem: " + bpb.fileSystem);
        boolean r = bpb.validate();
if (!r) { logger.log(Level.INFO, "validation failed: " + bpb); }
        return r;
    }
}
