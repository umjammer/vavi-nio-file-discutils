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

import discUtils.fat.BootSector;
import discUtils.fat.FatType;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.serdes.Serdes;
import vavix.io.fat.PC98BiosParameterBlock;


/**
 * Pc98BootSector.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-08-06 nsano initial version <br>
 */
public class Pc98BootSector implements BootSector {

    private static final Logger logger = System.getLogger(Pc98BootSector.class.getName());

    private final PC98BiosParameterBlock bpb;

    public Pc98BootSector(Stream stream) {
        try {
            byte[] sector = StreamUtilities.readExact(stream, 512);
            ByteArrayInputStream bais = new ByteArrayInputStream(sector);

            this.bpb = new PC98BiosParameterBlock();
            Serdes.Util.deserialize(bais, bpb);
logger.log(Level.TRACE, "■ bootRecord ----\n" + bpb);
            bpb.compute();
logger.log(Level.TRACE, "■ bootRecord ----\n" + bpb);
logger.log(Level.TRACE, "fat: " + getFatVariant());
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override
    public int getFatCount() {
        return bpb.numberOfFAT;
    }

    @Override
    public byte getActiveFat() {
        return 0;
    }

    @Override
    public int getBytesPerSector() {
        return bpb.getBytesPerSector();
    }

    @Override
    public long getFatSize() {
        return bpb.numberOfFATSector;
    }

    @Override
    public FatType getFatVariant() {
        return FatType.valueOf(bpb.getFatType().getFatSize());
    }

    @Override
    public String getOemName() {
        return bpb.oemLabel;
    }

    @Override
    public int getReservedSectorCount() {
        return bpb.reservedSectors;
    }

    @Override
    public int getSectorsPerCluster() {
        return bpb.getSectorsPerCluster();
    }

    @Override
    public long getTotalSectors() {
        return bpb.numberOfSmallSectors != 0 ? bpb.numberOfSmallSectors : bpb.numberOfLargeSectors;
    }

    @Override
    public String getVolumeLabel() {
        return bpb.volumeLabel;
    }

    @Override
    public int getMaxRootDirectoryEntries() {
        return bpb.maxRootDirectoryEntries;
    }

    @Override
    public long getRootDirectoryCluster() {
        return bpb.getStartClusterOfRootDirectory();
    }

    @Override
    public int getFatSize16() {
        return bpb.numberOfFATSector;
    }
}
