/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98;

import java.util.UUID;
import java.util.logging.Level;

import discUtils.core.partitions.BiosPartitionTable;
import discUtils.core.partitions.PartitionTable;
import vavi.util.Debug;
import vavix.io.partition.PC98PartitionEntry;

import discUtils.core.PhysicalVolumeType;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.SparseStream;


/**
 * Pc98PartitionInfo.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/13 umjammer initial version <br>
 */
public class Pc98PartitionInfo extends PartitionInfo {

    PC98PartitionEntry pe;

    public Pc98PartitionInfo(PC98PartitionEntry pe) {
        this.pe = pe;
    }

    private Pc98PartitionTable table;

    int heads = 0, secs = 0;

    // TODO
    // @see "https://github.com/aaru-dps/Aaru.Helpers/blob/4640bb88d3eb907d0f0617d5ee5159fbc13c5653/CHS.cs"
    public static int toLBA(int cyl, int head, int sector, int maxHead, int maxSector) {
        return maxHead == 0 || maxSector == 0 ? (((cyl * 16)      + head) * 63)        + sector - 1
                                              : (((cyl * maxHead) + head) * maxSector) + sector - 1;
    }

    @Override
    public byte getBiosType() {
        return 0;
    }

    @Override
    public long getFirstSector() {
        if (heads != 0 && secs != 0) {
            return (long) secs * heads * pe.startCylinder;
        } else {
Debug.println(Level.WARNING, "@@@@@@@@@@@@@@@@@@@@@@@@ mgick number is used @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            return 0x100;
        }
    }

    @Override
    public UUID getGuidType() {
        return null;
    }

    @Override
    public long getLastSector() {
        return toLBA(pe.endCylinder, pe.endHeader, pe.endSector, heads, secs);
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
