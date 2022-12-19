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

import discUtils.streams.SparseStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public final class ServerSparseExtentStream extends CommonSparseExtentStream {

    private final ServerSparseExtentHeader serverHeader;

    public ServerSparseExtentStream(Stream file,
            Ownership ownsFile,
            long diskOffset,
            SparseStream parentDiskStream,
            Ownership ownsParentDiskStream) {
        fileStream = file;
        ownsFileStream = ownsFile;
        this.diskOffset = diskOffset;
        this.parentDiskStream = parentDiskStream;
        this.ownsParentDiskStream = ownsParentDiskStream;
        file.position(0);
        byte[] firstSectors = StreamUtilities.readExact(file, Sizes.Sector * 4);
        serverHeader = ServerSparseExtentHeader.read(firstSectors, 0);
        header = serverHeader;
        gtCoverage = header.numGTEsPerGT * header.grainSize * Sizes.Sector;
        loadGlobalDirectory();
    }

    @Override public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (position + count > getLength()) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of stream");
        }

        int totalWritten = 0;
        while (totalWritten < count) {
            int grainTable = (int) (position / gtCoverage);
            int grainTableOffset = (int) (position - grainTable * gtCoverage);
            if (!loadGrainTable(grainTable)) {
                allocateGrainTable(grainTable);
            }

            int grainSize = (int) (header.grainSize * Sizes.Sector);
            int startGrain = grainTableOffset / grainSize;
            int startGrainOffset = grainTableOffset - startGrain * grainSize;
            int numGrains = 0;
            while (startGrain + numGrains < header.numGTEsPerGT &&
                   numGrains * grainSize - startGrainOffset < count - totalWritten &&
                   getGrainTableEntry(startGrain + numGrains) == 0) {
                ++numGrains;
            }
            if (numGrains != 0) {
                allocateGrains(grainTable, startGrain, numGrains);
            } else {
                numGrains = 1;
            }
            int numToWrite = Math.min(count - totalWritten, grainSize * numGrains - startGrainOffset);
            fileStream.position((long) getGrainTableEntry(startGrain) * Sizes.Sector + startGrainOffset);
            fileStream.write(buffer, offset + totalWritten, numToWrite);
            position += numToWrite;
            totalWritten += numToWrite;
        }
        atEof = position == getLength();
    }

    private void allocateGrains(int grainTable, int grain, int count) {
        // Calculate start pos for new grain
        long grainStartPos = (long) serverHeader.freeSector * Sizes.Sector;
        // Copy-on-write semantics, read the bytes from parent and write them out to this extent.
        parentDiskStream.position(diskOffset + (grain + (long) header.numGTEsPerGT * grainTable) * header.grainSize * Sizes.Sector);
        byte[] content = StreamUtilities.readExact(parentDiskStream, (int) (header.grainSize * Sizes.Sector * count));
        fileStream.position(grainStartPos);
        fileStream.write(content, 0, content.length);
        // Update next-free-sector in disk header
        serverHeader.freeSector += MathUtilities.ceil(content.length, Sizes.Sector);
        byte[] headerBytes = serverHeader.getBytes();
        fileStream.position(0);
        fileStream.write(headerBytes, 0, headerBytes.length);
        loadGrainTable(grainTable);
        for (int i = 0; i < count; ++i) {
            setGrainTableEntry(grain + i, (int) (grainStartPos / Sizes.Sector + header.grainSize * i));
        }
        writeGrainTable();
    }

    private void allocateGrainTable(int grainTable) {
        // Write out new blank grain table.
        int startSector = serverHeader.freeSector;
        byte[] emptyGrainTable = new byte[header.numGTEsPerGT * 4];
        fileStream.position(startSector * (long) Sizes.Sector);
        fileStream.write(emptyGrainTable, 0, emptyGrainTable.length);
        // Update header
        serverHeader.freeSector += MathUtilities.ceil(emptyGrainTable.length, Sizes.Sector);
        byte[] headerBytes = serverHeader.getBytes();
        fileStream.position(0);
        fileStream.write(headerBytes, 0, headerBytes.length);
        // Update the global directory
        globalDirectory[grainTable] = startSector;
        writeGlobalDirectory();
        this.grainTable = new byte[header.numGTEsPerGT * 4];
        currentGrainTable = grainTable;
    }

    private void writeGlobalDirectory() {
        byte[] buffer = new byte[globalDirectory.length * 4];
        for (int i = 0; i < globalDirectory.length; ++i) {
            ByteUtil.writeLeInt(globalDirectory[i], buffer, i * 4);
        }
        fileStream.position(serverHeader.gdOffset * Sizes.Sector);
        fileStream.write(buffer, 0, buffer.length);
    }

    private void writeGrainTable() {
        if (grainTable == null) {
            throw new IllegalStateException("No grain table loaded");
        }

        fileStream.position(globalDirectory[currentGrainTable] * (long) Sizes.Sector);
        fileStream.write(grainTable, 0, grainTable.length);
    }
}
