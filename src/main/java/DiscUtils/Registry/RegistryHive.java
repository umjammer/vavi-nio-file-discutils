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

package DiscUtils.Registry;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * A registry hive.
 */
public final class RegistryHive implements Closeable {
    private static final long BinStart = 4 * Sizes.OneKiB;

    private final List<BinHeader> _bins;

    private Stream _fileStream;

    private final HiveHeader _header;

    private final Ownership _ownsStream;

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
        _fileStream = hive;
        _fileStream.setPosition(0);
        _ownsStream = ownership;
        byte[] buffer = StreamUtilities.readExact(_fileStream, HiveHeader.HeaderSize);
        _header = new HiveHeader();
        _header.readFrom(buffer, 0);
        _bins = new ArrayList<>();
        int pos = 0;
        while (pos < _header.Length) {
            _fileStream.setPosition(BinStart + pos);
            byte[] headerBuffer = StreamUtilities.readExact(_fileStream, BinHeader.HeaderSize);
            BinHeader header = new BinHeader();
            header.readFrom(headerBuffer, 0);
            _bins.add(header);
            pos += header.BinSize;
        }
    }

    /**
     * Gets the root key in the registry hive.
     */
    public RegistryKey getRoot() {
        return new RegistryKey(this, getCell(_header.RootCell));
    }

    /**
     * Disposes of this instance, freeing any underlying stream (if any).
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        if (_fileStream != null && _ownsStream == Ownership.Dispose) {
            _fileStream.close();
            _fileStream = null;
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
        binHeader.FileOffset = 0;
        binHeader.BinSize = (int) (4 * Sizes.OneKiB);
        HiveHeader hiveHeader = new HiveHeader();
        hiveHeader.Length = binHeader.BinSize;
        stream.setPosition(0);
        byte[] buffer = new byte[(int) hiveHeader.getSize()];
        hiveHeader.writeTo(buffer, 0);
        stream.write(buffer, 0, buffer.length);
        buffer = new byte[(int) binHeader.getSize()];
        binHeader.writeTo(buffer, 0);
        stream.setPosition(BinStart);
        stream.write(buffer, 0, buffer.length);
        buffer = new byte[4];
        EndianUtilities.writeBytesLittleEndian(binHeader.BinSize - binHeader.getSize(), buffer, 0);
        stream.write(buffer, 0, buffer.length);
        // Make sure the file is initialized out to the end of the firs bin
        stream.setPosition(BinStart + binHeader.BinSize - 1);
        stream.writeByte((byte) 0);
        // Temporary hive to perform construction of higher-level structures
        RegistryHive newHive = new RegistryHive(stream);
        KeyNodeCell rootCell = new KeyNodeCell("root", -1);
        rootCell.Flags = EnumSet.of(RegistryKeyFlags.Normal, RegistryKeyFlags.Root);
        newHive.updateCell(rootCell, true);
        RegistrySecurity sd = new RegistrySecurity();
        sd.setSecurityDescriptorSddlForm("O:BAG:BAD:PAI(A;;KA;;;SY)(A;CI;KA;;;BA)", AccessControlSections.All);
        SecurityCell secCell = new SecurityCell(sd);
        newHive.updateCell(secCell, true);
        secCell.setNextIndex(secCell.getIndex());
        secCell.setPreviousIndex(secCell.getIndex());
        newHive.updateCell(secCell, false);
        rootCell.SecurityIndex = secCell.getIndex();
        newHive.updateCell(rootCell, false);
        // Ref the root cell from the hive header
        hiveHeader.RootCell = rootCell.getIndex();
        buffer = new byte[(int) hiveHeader.getSize()];
        hiveHeader.writeTo(buffer, 0);
        stream.setPosition(0);
        stream.write(buffer, 0, buffer.length);
        return new RegistryHive(stream, ownership);
    }

    // Finally, return the new hive
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
            cell.setIndex(allocateRawCell((int) cell.getSize()));
        }

        Bin bin = getBin(cell.getIndex());
        if (bin != null) {
            if (bin.updateCell(cell)) {
                return cell.getIndex();
            }

            if (canRelocate) {
                int oldCell = cell.getIndex();
                cell.setIndex(allocateRawCell((int) cell.getSize()));
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
        int minSize = MathUtilities.roundUp(capacity + 4, 8);
        for (BinHeader binHeader : _bins) {
            // Allow for size header and ensure multiple of 8
            // Incredibly inefficient algorithm...
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
        int binsIdx = Collections.binarySearch(_bins, null, new BinFinder(index));
        if (binsIdx >= 0) {
            return _bins.get(binsIdx);
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
        _fileStream.setPosition(BinStart + binHeader.FileOffset);
        return new Bin(this, _fileStream);
    }

    private BinHeader allocateBin(int minSize) {
        BinHeader lastBin = _bins.get(_bins.size() - 1);
        BinHeader newBinHeader = new BinHeader();
        newBinHeader.FileOffset = lastBin.FileOffset + lastBin.BinSize;
        newBinHeader.BinSize = MathUtilities.roundUp(minSize + (int) newBinHeader.getSize(), 4 * (int) Sizes.OneKiB);
        byte[] buffer = new byte[(int) newBinHeader.getSize()];
        newBinHeader.writeTo(buffer, 0);
        _fileStream.setPosition(BinStart + newBinHeader.FileOffset);
        _fileStream.write(buffer, 0, buffer.length);
        byte[] cellHeader = new byte[4];
        EndianUtilities.writeBytesLittleEndian(newBinHeader.BinSize - newBinHeader.getSize(), cellHeader, 0);
        _fileStream.write(cellHeader, 0, 4);
        // Update hive with new length
        _header.Length = newBinHeader.FileOffset + newBinHeader.BinSize;
        _header.Timestamp = System.currentTimeMillis();
        _header.Sequence1++;
        _header.Sequence2++;
        _fileStream.setPosition(0);
        byte[] hiveHeader = StreamUtilities.readExact(_fileStream, (int) _header.getSize());
        _header.writeTo(hiveHeader, 0);
        _fileStream.setPosition(0);
        _fileStream.write(hiveHeader, 0, hiveHeader.length);
        // Make sure the file is initialized to desired position
        _fileStream.setPosition(BinStart + _header.Length - 1);
        _fileStream.writeByte((byte) 0);
        _bins.add(newBinHeader);
        return newBinHeader;
    }

    private static class BinFinder implements Comparator<BinHeader> {
        private final int _index;

        public BinFinder(int index) {
            _index = index;
        }

        public int compare(BinHeader x, BinHeader y) {
            if (x.FileOffset + x.BinSize < _index) {
                return -1;
            }

            if (x.FileOffset > _index) {
                return 1;
            }

            return 0;
        }
    }
}
