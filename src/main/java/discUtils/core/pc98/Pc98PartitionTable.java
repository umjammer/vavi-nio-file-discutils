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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import discUtils.core.VirtualDisk;
import discUtils.core.partitions.PartitionInfo;
import discUtils.core.partitions.PartitionTable;
import discUtils.core.partitions.WellKnownPartitionType;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.Stream;
import vavi.util.serdes.Serdes;
import vavix.io.partition.PC98PartitionEntry;

import static java.lang.System.getLogger;


/**
 * Pc98PartitionTable.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/06 umjammer initial version <br>
 */
public class Pc98PartitionTable extends PartitionTable {

    private static final Logger logger = getLogger(Pc98PartitionTable.class.getName());

    private final Stream diskData;

    private final int bytesPerSector;

    /**
     * The partition entries
     */
    private Map<PC98PartitionEntry, Pc98PartitionInfo> partitions = new HashMap<>();

    /**
     * Create a new instance
     */
    public Pc98PartitionTable(VirtualDisk disk) {
        byte[] bootSector = new byte[1024];
        diskData = disk.getContent();
        diskData.position(0);
        diskData.read(bootSector, 0, 1024);
        ByteArrayInputStream baos = new ByteArrayInputStream(bootSector, 512, 512);
        for (int i = 0; i < 16; i++) {
            try {
                PC98PartitionEntry pe = new PC98PartitionEntry();
                Serdes.Util.deserialize(baos, pe);
                if (!pe.isValid()) {
                    continue;
                }
//logger.log(Level.DEBUG, "[" + i + "]: " + pe);
                partitions.put(pe, new Pc98PartitionInfo(pe, this, disk.getGeometry()));
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
        bytesPerSector = disk.getBlockSize();
    }

    @Override
    public UUID getDiskGuid() {
        return new UUID(0L, 0L);
    }

    @Override
    public List<PartitionInfo> getPartitions() {
        return new ArrayList<>(partitions.values());
    }

    @Override
    public int create(WellKnownPartitionType type, boolean active) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public int create(long size, WellKnownPartitionType type, boolean active) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public int createAligned(WellKnownPartitionType type, boolean active, int alignment) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public int createAligned(long size, WellKnownPartitionType type, boolean active, int alignment) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void delete(int index) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    SparseStream open(PC98PartitionEntry pe) {
        long s = partitions.get(pe).getFirstSector() * bytesPerSector;
        long e = partitions.get(pe).getLastSector() * bytesPerSector;
logger.log(Level.DEBUG, String.format("%s: %08x, %08x", pe, s, e));
        return new SubStream(diskData, Ownership.None, s, e - s);
    }
}
