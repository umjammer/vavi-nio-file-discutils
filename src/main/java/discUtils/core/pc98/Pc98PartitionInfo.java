/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.UUID;

import discUtils.core.Geometry;
import discUtils.core.PhysicalVolumeType;
import discUtils.core.partitions.PartitionInfo;
import discUtils.streams.SparseStream;
import vavix.io.partition.PC98PartitionEntry;

import static vavix.io.fat.PC98BiosParameterBlock.toLBA;


/**
 * Pc98PartitionInfo.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/13 umjammer initial version <br>
 */
public class Pc98PartitionInfo extends PartitionInfo {

    private static final Logger logger = System.getLogger(Pc98PartitionInfo.class.getName());

    private final PC98PartitionEntry pe;

    public Pc98PartitionInfo(PC98PartitionEntry pe, Pc98PartitionTable table, Geometry geometry) {
        this.pe = pe;
        this.table = table;
        this.heads = geometry.getHeadsPerCylinder();
        this.secs = geometry.getSectorsPerTrack();
    }

    private final Pc98PartitionTable table;

    private final int heads, secs;

    @Override
    public byte getBiosType() {
        return 0;
    }

    @Override
    public long getFirstSector() {
//logger.log(Level.DEBUG, "%08x, %08x".formatted(secs * heads * pe.startCylinder, toLBA(pe.startCylinder, pe.startHeader, pe.startSector + 1, heads, secs)));
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
logger.log(Level.TRACE, "pc98 partition open: " + pe);
        return table.open(pe);
    }
}
