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

package discUtils.vmdk;

import java.util.Collections;
import java.util.EnumSet;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;


/**
 * Represents and extent from a sparse disk from 'hosted' software (VMware
 * Workstation, etc). Hosted disks and server disks (ESX, etc) are subtly
 * different formats.
 */
public final class HostedSparseExtentStream extends CommonSparseExtentStream {

    private HostedSparseExtentHeader hostedHeader;

    public HostedSparseExtentStream(Stream file,
            Ownership ownsFile,
            long diskOffset,
            SparseStream parentDiskStream,
            Ownership ownsParentDiskStream) {
        fileStream = file;
        ownsFileStream = ownsFile;
        this.diskOffset = diskOffset;
        this.parentDiskStream = parentDiskStream;
        this.ownsParentDiskStream = ownsParentDiskStream;

        file.setPosition(0);
        byte[] headerSector = StreamUtilities.readExact(file, Sizes.Sector);
        hostedHeader = HostedSparseExtentHeader.read(headerSector, 0);
        if (hostedHeader.gdOffset == -1) {
            // Fall back to secondary copy that (should) be at the end of the
            // stream, just
            // before the end-of-stream sector marker
            file.setPosition(file.getLength() - Sizes.OneKiB);
            headerSector = StreamUtilities.readExact(file, Sizes.Sector);
            hostedHeader = HostedSparseExtentHeader.read(headerSector, 0);

            if (hostedHeader.magicNumber != HostedSparseExtentHeader.VmdkMagicNumber) {
                throw new dotnet4j.io.IOException("Unable to locate valid VMDK header or footer");
            }
        }

        header = hostedHeader;

        if (hostedHeader.compressAlgorithm != 0 && hostedHeader.compressAlgorithm != 1) {
            throw new UnsupportedOperationException("Only uncompressed and DEFLATE compressed disks supported");
        }

        gtCoverage = header.numGTEsPerGT * header.grainSize * Sizes.Sector;

        loadGlobalDirectory();
    }

    public boolean canWrite() {
        // No write support for streamOptimized disks
        return fileStream.canWrite() &&
               Collections.disjoint(hostedHeader.flags,
                                    EnumSet.of(HostedSparseExtentFlags.CompressedGrains, HostedSparseExtentFlags.MarkersInUse));
    }

    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();

        if (!canWrite()) {
            throw new dotnet4j.io.IOException("Cannot write to this stream");
        }

        if (position + count > getLength()) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of stream");
        }

        int totalWritten = 0;
        while (totalWritten < count) {
            int grainTable = (int) (position / gtCoverage);
            int grainTableOffset = (int) (position - grainTable * gtCoverage);
            loadGrainTable(grainTable);
            int grainSize = (int) (header.grainSize * Sizes.Sector);
            int grain = grainTableOffset / grainSize;
            int grainOffset = grainTableOffset - grain * grainSize;
            if (getGrainTableEntry(grain) == 0) {
                allocateGrain(grainTable, grain);
            }

            int numToWrite = Math.min(count - totalWritten, grainSize - grainOffset);
            fileStream.setPosition((long) getGrainTableEntry(grain) * Sizes.Sector + grainOffset);
            fileStream.write(buffer, offset + totalWritten, numToWrite);
            position += numToWrite;
            totalWritten += numToWrite;
        }
        atEof = position == getLength();
    }

    protected int readGrain(byte[] buffer, int bufferOffset, long grainStart, int grainOffset, int numToRead) {
        if (hostedHeader.flags.contains(HostedSparseExtentFlags.CompressedGrains)) {
            fileStream.setPosition(grainStart);

            byte[] readBuffer = StreamUtilities.readExact(fileStream, CompressedGrainHeader.Size);
            CompressedGrainHeader hdr = new CompressedGrainHeader();
            hdr.read(readBuffer, 0);

            readBuffer = StreamUtilities.readExact(fileStream, hdr.dataSize);

            // This is really a zlib stream, so has header and footer. We ignore
            // this right
            // now, but we sanity
            // check against expected header values...
            short header = EndianUtilities.toUInt16BigEndian(readBuffer, 0);

            if (header % 31 != 0) {
                throw new dotnet4j.io.IOException("Invalid ZLib header found");
            }

            if ((header & 0x0F00) != 8 << 8) {
                throw new UnsupportedOperationException("ZLib compression not using DEFLATE algorithm");
            }

            if ((header & 0x0020) != 0) {
                throw new UnsupportedOperationException("ZLib compression using preset dictionary");
            }

            Stream readStream = new MemoryStream(readBuffer, 2, hdr.dataSize - 2, false);
            DeflateStream deflateStream = new DeflateStream(readStream, CompressionMode.Decompress);

            // Need to skip some bytes, but DefaultStream doesn't support
            // seeking...
            StreamUtilities.readExact(deflateStream, grainOffset);

            return deflateStream.read(buffer, bufferOffset, numToRead);
        }
        return super.readGrain(buffer, bufferOffset, grainStart, grainOffset, numToRead);
    }

    protected StreamExtent mapGrain(long grainStart, int grainOffset, int numToRead) {
        if (hostedHeader.flags.contains(HostedSparseExtentFlags.CompressedGrains)) {
            fileStream.setPosition(grainStart);

            byte[] readBuffer = StreamUtilities.readExact(fileStream, CompressedGrainHeader.Size);
            CompressedGrainHeader hdr = new CompressedGrainHeader();
            hdr.read(readBuffer, 0);

            return new StreamExtent(grainStart + grainOffset, CompressedGrainHeader.Size + hdr.dataSize);
        }
        return super.mapGrain(grainStart, grainOffset, numToRead);
    }

    protected void loadGlobalDirectory() {
        super.loadGlobalDirectory();

        if (hostedHeader.flags.contains(HostedSparseExtentFlags.RedundantGrainTable)) {
            int numGTs = (int) MathUtilities.ceil(header.capacity * Sizes.Sector, gtCoverage);
            redundantGlobalDirectory = new int[numGTs];
            fileStream.setPosition(hostedHeader.rgdOffset * Sizes.Sector);
            byte[] gdAsBytes = StreamUtilities.readExact(fileStream, numGTs * 4);
            for (int i = 0; i < globalDirectory.length; ++i) {
                redundantGlobalDirectory[i] = EndianUtilities.toUInt32LittleEndian(gdAsBytes, i * 4);
            }
        }
    }

    private void allocateGrain(int grainTable, int grain) {
        // Calculate start pos for new grain
        long grainStartPos = MathUtilities.roundUp(fileStream.getLength(), header.grainSize * Sizes.Sector);
        // Copy-on-write semantics, read the bytes from parent and write them
        // out to
        // this extent.
        parentDiskStream
                .setPosition(diskOffset + (grain + (long) header.numGTEsPerGT * grainTable) * header.grainSize * Sizes.Sector);
        byte[] content = StreamUtilities.readExact(parentDiskStream, (int) header.grainSize * Sizes.Sector);
        fileStream.setPosition(grainStartPos);
        fileStream.write(content, 0, content.length);
        loadGrainTable(grainTable);
        setGrainTableEntry(grain, (int) (grainStartPos / Sizes.Sector));
        writeGrainTable();
    }

    private void writeGrainTable() {
        if (grainTable == null) {
            throw new dotnet4j.io.IOException("No grain table loaded");
        }

        fileStream.setPosition(globalDirectory[currentGrainTable] * (long) Sizes.Sector);
        fileStream.write(grainTable, 0, grainTable.length);
        if (redundantGlobalDirectory != null) {
            fileStream.setPosition(redundantGlobalDirectory[currentGrainTable] * (long) Sizes.Sector);
            fileStream.write(grainTable, 0, grainTable.length);
        }

    }

}
