/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import discUtils.core.VirtualDisk;
import discUtils.core.partitions.PartitionTable;
import discUtils.core.partitions.PartitionTableFactory;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.serdes.Serdes;
import vavix.io.partition.PC98PartitionEntry;


/**
 * Pc98PartitionTableFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/12 umjammer initial version <br>
 */
public final class Pc98PartitionTableFactory implements PartitionTableFactory {

    private static final String[] iplSignatures = {
        "IPL1", "Linux 98", "GRUB/98 "
    };

    @Override
    public boolean detectIsPartitioned(Stream stream) {
        if (stream.getLength() < Sizes.Sector * 2) {
Debug.printf(Level.FINE, "Not enough data for detection: %04x/%04x%n", stream.getLength(), Sizes.Sector);
            return false;
        }

        stream.position(0);

        byte[] bootSector = StreamUtilities.readExact(stream, Sizes.Sector * 2);
//Debug.printf(Level.FINE, "%n" + StringUtil.getDump(bootSector, 512));

        if ((ByteUtil.readLeShort(bootSector, 510) & 0xffff) != 0xaa55) {
Debug.printf(Level.FINE, "No aa55 magic: %04x%n", (ByteUtil.readBeShort(bootSector, 510) & 0xffff));
            return false;
        }

        if (Arrays.stream(iplSignatures).noneMatch(s ->
            new String(bootSector, 4, s.length(), StandardCharsets.US_ASCII).equals(s)
        )) {
Debug.println(Level.FINE, "no matching signature is found: " + new String(bootSector, 4, 4, StandardCharsets.US_ASCII));
            return false;
        }

        if (new String(bootSector, 0x36, 3, StandardCharsets.US_ASCII).equals("FAT")) {
Debug.println(Level.FINE, "strings FAT is found, this partition might be for AT");
            return false;
        }

        int count = 0;
        ByteArrayInputStream baos = new ByteArrayInputStream(bootSector, 512, 512);
        for (int i = 0; i < 16; i++) {
            PC98PartitionEntry pe = new PC98PartitionEntry();
            try {
                Serdes.Util.deserialize(baos, pe);
                if (!pe.isValid()) {
Debug.println(Level.FINE, "pe is invalid: " + pe);
                    continue;
                }
            } catch (IOException e) {
Debug.println(Level.FINE, e);
                continue;
            }
Debug.println(Level.FINE, "[" + count + "]: " + pe);
            count++;
        }

        return count > 0;
    }

    /**
     * adding {@link discUtils.core.partitions.BiosPartitionTable#isValid(Stream disk)}
     * <pre>
     * if (!record.isValid()) {
     *  Debug.println("vavi original check: " + record.isValid());
     *  return false;
     * }
     * </pre>
     * makes test fail.
     */
    @Override
    public void adhoc(List<PartitionTable> tables) {
        boolean exists = false;
        for (PartitionTable table: tables) {
            if (table instanceof discUtils.core.pc98.Pc98PartitionTable) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            return;
        }
        for (PartitionTable table: tables) {
            if (table instanceof discUtils.core.partitions.BiosPartitionTable) {
Debug.println("ad-hoc remove partition, conflict w/ pc98 partition table: " + table);
                tables.remove(table);
            }
        }
    }

    @Override
    public PartitionTable detectPartitionTable(VirtualDisk disk) {
        if (detectIsPartitioned(disk.getContent())) {
            Pc98PartitionTable table = new Pc98PartitionTable(disk);
            return table;
        }

        return null;
    }
}
