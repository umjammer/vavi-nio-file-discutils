//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.registry;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import discUtils.core.internal.LocalFileLocator;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;
import dotnet4j.security.accessControl.AccessControlSections;
import dotnet4j.security.accessControl.RegistrySecurity;


/**
 * A registry hive.
 */
public final class RegistryHive implements Closeable {

    private static final long BinStart = 4 * Sizes.OneKiB;

    private final List<BinHeader> bins;

    private Stream fileStream;

    private final HiveHeader header;

    private final Ownership ownsStream;

    /**
     * Initializes a new instance of the RegistryHive class.
     *
     * @param hive The stream containing the registry hive.
     *            The created object does not assume ownership of the stream.
     */
    public RegistryHive(Stream hive) {
        this(hive, Ownership.None);
    }

    /**
     * Initializes a new instance of the RegistryHive class.
     *
     * @param hive The stream containing the registry hive.
     * @param ownership Whether the new object assumes object of the stream.
     */
    public RegistryHive(Stream hive, Ownership ownership) {
        fileStream = hive;
        fileStream.position(0);
        ownsStream = ownership;

        byte[] buffer = StreamUtilities.readExact(fileStream, HiveHeader.HeaderSize);

        header = new HiveHeader();
        header.readFrom(buffer, 0);

        bins = new ArrayList<>();
        int pos = 0;
        while (pos < header.length) {
            fileStream.position(BinStart + pos);
            byte[] headerBuffer = StreamUtilities.readExact(fileStream, BinHeader.HeaderSize);
            BinHeader header = new BinHeader();
            header.readFrom(headerBuffer, 0);
            bins.add(header);

            pos += header.binSize;
        }
    }

    /**
     * Gets the root key in the registry hive.
     */
    public RegistryKey getRoot() {
        return new RegistryKey(this, getCell(header.rootCell));
    }

    /**
     * Disposes of this instance, freeing any underlying stream (if any).
     * 
     * @throws IOException when an io error occurs
     */
    @Override public void close() throws IOException {
        if (fileStream != null && ownsStream == Ownership.Dispose) {
            fileStream.close();
            fileStream = null;
        }
    }

    /**
     * Creates a new (empty) registry hive.
     *
     * @param stream The stream to contain the new hive.
     * @return The new hive.
     *         The returned object does not assume ownership of the stream.
     */
    public static RegistryHive create(Stream stream) {
        return create(stream, Ownership.None);
    }

    /**
     * Creates a new (empty) registry hive.
     *
     * @param stream The stream to contain the new hive.
     * @param ownership Whether the returned object owns the stream.
     * @return The new hive.
     */
    public static RegistryHive create(Stream stream, Ownership ownership) {
        if (stream == null) {
            throw new NullPointerException("Attempt to create registry hive in null stream");
        }

        // Construct a file with minimal structure - hive header, plus one (empty) bin
        BinHeader binHeader = new BinHeader();
        binHeader.fileOffset = 0;
        binHeader.binSize = (int) (4 * Sizes.OneKiB);

        HiveHeader hiveHeader = new HiveHeader();
        hiveHeader.length = binHeader.binSize;

        stream.position(0);

        byte[] buffer = new byte[hiveHeader.size()];
        hiveHeader.writeTo(buffer, 0);
        stream.write(buffer, 0, buffer.length);

        buffer = new byte[binHeader.size()];
        binHeader.writeTo(buffer, 0);
        stream.position(BinStart);
        stream.write(buffer, 0, buffer.length);

        buffer = new byte[4];
        EndianUtilities.writeBytesLittleEndian(binHeader.binSize - binHeader.size(), buffer, 0);
        stream.write(buffer, 0, buffer.length);

        // Make sure the file is initialized out to the end of the firs bin
        stream.position(BinStart + binHeader.binSize - 1);
        stream.writeByte((byte) 0);

        // Temporary hive to perform construction of higher-level structures
        RegistryHive newHive = new RegistryHive(stream);
        KeyNodeCell rootCell = new KeyNodeCell("root", -1);
        rootCell.flags = EnumSet.of(RegistryKeyFlags.Normal, RegistryKeyFlags.Root);
        newHive.updateCell(rootCell, true);

        RegistrySecurity sd = new RegistrySecurity();
        sd.setSecurityDescriptorSddlForm("O:BAG:BAD:PAI(A;;KA;;;SY)(A;CI;KA;;;BA)", AccessControlSections.All);
        SecurityCell secCell = new SecurityCell(sd);
        newHive.updateCell(secCell, true);
        secCell.setNextIndex(secCell.getIndex());
        secCell.setPreviousIndex(secCell.getIndex());
        newHive.updateCell(secCell, false);

        rootCell.securityIndex = secCell.getIndex();
        newHive.updateCell(rootCell, false);

        // Ref the root cell from the hive header
        hiveHeader.rootCell = rootCell.getIndex();
        buffer = new byte[hiveHeader.size()];
        hiveHeader.writeTo(buffer, 0);
        stream.position(0);
        stream.write(buffer, 0, buffer.length);

        // Finally, return the new hive
        return new RegistryHive(stream, ownership);
    }

    /**
     * Creates a new (empty) registry hive.
     *
     * @param path The file to create the new hive in.
     * @return The new hive.
     */
    public static RegistryHive create(String path) {
        LocalFileLocator locator = new LocalFileLocator("");
        return create(locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None), Ownership.Dispose);
    }

    @SuppressWarnings("unchecked")
    public <K extends Cell> K getCell(int index) {
        Bin bin = getBin(index);

        if (bin != null) {
            return (K) bin.tryGetCell(index);
        }
        return null;
    }

    public void freeCell(int index) {
        Bin bin = getBin(index);

        if (bin != null) {
            bin.freeCell(index);
        }
    }

    public int updateCell(Cell cell, boolean canRelocate) {
        if (cell.getIndex() == -1 && canRelocate) {
            cell.setIndex(allocateRawCell(cell.size()));
        }

        Bin bin = getBin(cell.getIndex());

        if (bin != null) {
            if (bin.updateCell(cell)) {
                return cell.getIndex();
            }
            if (canRelocate) {
                int oldCell = cell.getIndex();
                cell.setIndex(allocateRawCell(cell.size()));
                bin = getBin(cell.getIndex());
                if (!bin.updateCell(cell)) {
                    cell.setIndex(oldCell);
                    throw new IllegalArgumentException("Failed to migrate cell to new location");
                }

                freeCell(oldCell);
                return cell.getIndex();
            }
            throw new IllegalArgumentException("Can't update cell, needs relocation but relocation disabled");
        }
        throw new IllegalArgumentException("No bin found containing index: " + cell.getIndex());
    }

    public byte[] rawCellData(int index, int maxBytes) {
        Bin bin = getBin(index);

        if (bin != null) {
            return bin.readRawCellData(index, maxBytes);
        }
        return null;
    }

    public boolean writeRawCellData(int index, byte[] data, int offset, int count) {
        Bin bin = getBin(index);

        if (bin != null) {
            return bin.writeRawCellData(index, data, offset, count);
        }
        throw new IllegalArgumentException("No bin found containing index: " + index);
    }

    public int allocateRawCell(int capacity) {
        // Allow for size header and ensure multiple of 8
        int minSize = MathUtilities.roundUp(capacity + 4, 8);

        // Incredibly inefficient algorithm...
        for (BinHeader binHeader : bins) {
            Bin bin = loadBin(binHeader);
            int cellIndex = bin.allocateCell(minSize);

            if (cellIndex >= 0) {
                return cellIndex;
            }
        }

        BinHeader newBinHeader = allocateBin(minSize);
        Bin newBin = loadBin(newBinHeader);
        return newBin.allocateCell(minSize);
    }

    private BinHeader findBin(int index) {
        int binsIdx = Collections.binarySearch(bins, null, new BinFinder(index));
        if (binsIdx >= 0) {
            return bins.get(binsIdx);
        }

        return null;
    }

    private Bin getBin(int cellIndex) {
        BinHeader binHeader = findBin(cellIndex);
        if (binHeader != null) {
            return loadBin(binHeader);
        }

        return null;
    }

    private Bin loadBin(BinHeader binHeader) {
        fileStream.position(BinStart + binHeader.fileOffset);
        return new Bin(this, fileStream);
    }

    private BinHeader allocateBin(int minSize) {
        BinHeader lastBin = bins.get(bins.size() - 1);

        BinHeader newBinHeader = new BinHeader();
        newBinHeader.fileOffset = lastBin.fileOffset + lastBin.binSize;
        newBinHeader.binSize = MathUtilities.roundUp(minSize + newBinHeader.size(), 4 * (int) Sizes.OneKiB);

        byte[] buffer = new byte[newBinHeader.size()];
        newBinHeader.writeTo(buffer, 0);
        fileStream.position(BinStart + newBinHeader.fileOffset);
        fileStream.write(buffer, 0, buffer.length);

        byte[] cellHeader = new byte[4];
        EndianUtilities.writeBytesLittleEndian(newBinHeader.binSize - newBinHeader.size(), cellHeader, 0);
        fileStream.write(cellHeader, 0, 4);

        // Update hive with new length
        header.length = newBinHeader.fileOffset + newBinHeader.binSize;
        header.timestamp = System.currentTimeMillis();
        header.sequence1++;
        header.sequence2++;
        fileStream.position(0);
        byte[] hiveHeader = StreamUtilities.readExact(fileStream, header.size());
        header.writeTo(hiveHeader, 0);
        fileStream.position(0);
        fileStream.write(hiveHeader, 0, hiveHeader.length);

        // Make sure the file is initialized to desired position
        fileStream.position(BinStart + header.length - 1);
        fileStream.writeByte((byte) 0);

        bins.add(newBinHeader);
        return newBinHeader;
    }

    private static class BinFinder implements Comparator<BinHeader> {

        private final int index;

        public BinFinder(int index) {
            this.index = index;
        }

        @Override public int compare(BinHeader x, BinHeader y) {
            if (x.fileOffset + x.binSize < index) {
                return -1;
            }
            if (x.fileOffset > index) {
                return 1;
            }
            return 0;
        }
    }
}
