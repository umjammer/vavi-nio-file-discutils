/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98;

import java.util.UUID;

import discUtils.core.Geometry;
import discUtils.core.PhysicalVolumeType;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.SparseStream;
import vavix.io.partition.PC98PartitionEntry;


/**
 * Pc98PartitionInfo.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/13 umjammer initial version <br>
 */
public class Pc98PartitionInfo extends PartitionInfo {

    private PC98PartitionEntry pe;

    public Pc98PartitionInfo(PC98PartitionEntry pe, Pc98PartitionTable table, Geometry geometry) {
        this.pe = pe;
        this.table = table;
        this.heads = geometry.getHeadsPerCylinder();
        this.secs = geometry.getSectorsPerTrack();
    }

    private Pc98PartitionTable table;

    private int heads, secs;

    // @see "https://github.com/aaru-dps/Aaru.Helpers/blob/4640bb88d3eb907d0f0617d5ee5159fbc13c5653/CHS.cs"
    public static int toLBA(int cyl, int head, int sector, int maxHead, int maxSector) {
//logger.log(Level.DEBUG, String.format("heads: %d, secs: %d", maxHead, maxSector));
        return maxHead == 0 || maxSector == 0 ? (((cyl * 16)      + head) * 63)        + sector - 1
                                              : (((cyl * maxHead) + head) * maxSector) + sector - 1;
    }

    @Override
    public byte getBiosType() {
        return 0;
    }

    @Override
    public long getFirstSector() {
//logger.log(Level.DEBUG, String.format("%08x, %08x", secs * heads * pe.startCylinder, toLBA(pe.startCylinder, pe.startHeader, pe.startSector + 1, heads, secs)));
        return toLBA(pe.startCylinder, pe.startHeader, pe.startSector + 1, heads, secs);
    }

    @Override
    public UUID getGuidType() {
        return new UUID(0L, 0L);
    }

    @Override
    public long getLastSector() {
        return toLBA(pe.endCylinder, pe.endHeader, pe.endSector + 1, heads, secs);
    }

    @Override
    public String getTypeAsString() {
        return getClass().getName();
    }

    @Override
    public PhysicalVolumeType getVolumeType() {
        return PhysicalVolumeType.BiosPartition;
    }

    @Override
    public SparseStream open() {
        return table.open(pe);
    }
}

/* */
