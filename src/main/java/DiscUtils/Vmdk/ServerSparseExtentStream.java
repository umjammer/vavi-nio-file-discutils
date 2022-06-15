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

package DiscUtils.Vmdk;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.Stream;


public final class ServerSparseExtentStream extends CommonSparseExtentStream {
    private final ServerSparseExtentHeader _serverHeader;

    public ServerSparseExtentStream(Stream file,
            Ownership ownsFile,
            long diskOffset,
            SparseStream parentDiskStream,
            Ownership ownsParentDiskStream) {
        _fileStream = file;
        _ownsFileStream = ownsFile;
        _diskOffset = diskOffset;
        _parentDiskStream = parentDiskStream;
        _ownsParentDiskStream = ownsParentDiskStream;
        file.setPosition(0);
        byte[] firstSectors = StreamUtilities.readExact(file, Sizes.Sector * 4);
        _serverHeader = ServerSparseExtentHeader.read(firstSectors, 0);
        _header = _serverHeader;
        _gtCoverage = _header.NumGTEsPerGT * _header.GrainSize * Sizes.Sector;
        loadGlobalDirectory();
    }

    public void write(byte[] buffer, int offset, int count) {
        checkDisposed();
        if (_position + count > getLength()) {
            throw new dotnet4j.io.IOException("Attempt to write beyond end of stream");
        }

        int totalWritten = 0;
        while (totalWritten < count) {
            int grainTable = (int) (_position / _gtCoverage);
            int grainTableOffset = (int) (_position - grainTable * _gtCoverage);
            if (!loadGrainTable(grainTable)) {
                allocateGrainTable(grainTable);
            }

            int grainSize = (int) (_header.GrainSize * Sizes.Sector);
            int startGrain = grainTableOffset / grainSize;
            int startGrainOffset = grainTableOffset - startGrain * grainSize;
            int numGrains = 0;
            while (startGrain + numGrains < _header.NumGTEsPerGT &&
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
            _fileStream.setPosition((long) getGrainTableEntry(startGrain) * Sizes.Sector + startGrainOffset);
            _fileStream.write(buffer, offset + totalWritten, numToWrite);
            _position += numToWrite;
            totalWritten += numToWrite;
        }
        _atEof = _position == getLength();
    }

    private void allocateGrains(int grainTable, int grain, int count) {
        // Calculate start pos for new grain
        long grainStartPos = (long) _serverHeader.FreeSector * Sizes.Sector;
        // Copy-on-write semantics, read the bytes from parent and write them out to this extent.
        _parentDiskStream.setPosition(_diskOffset + (grain + (long) _header.NumGTEsPerGT * grainTable) * _header.GrainSize * Sizes.Sector);
        byte[] content = StreamUtilities.readExact(_parentDiskStream, (int) (_header.GrainSize * Sizes.Sector * count));
        _fileStream.setPosition(grainStartPos);
        _fileStream.write(content, 0, content.length);
        // Update next-free-sector in disk header
        _serverHeader.FreeSector += MathUtilities.ceil(content.length, Sizes.Sector);
        byte[] headerBytes = _serverHeader.getBytes();
        _fileStream.setPosition(0);
        _fileStream.write(headerBytes, 0, headerBytes.length);
        loadGrainTable(grainTable);
        for (int i = 0; i < count; ++i) {
            setGrainTableEntry(grain + i, (int) (grainStartPos / Sizes.Sector + _header.GrainSize * i));
        }
        writeGrainTable();
    }

    private void allocateGrainTable(int grainTable) {
        // Write out new blank grain table.
        int startSector = _serverHeader.FreeSector;
        byte[] emptyGrainTable = new byte[_header.NumGTEsPerGT * 4];
        _fileStream.setPosition(startSector * (long) Sizes.Sector);
        _fileStream.write(emptyGrainTable, 0, emptyGrainTable.length);
        // Update header
        _serverHeader.FreeSector += MathUtilities.ceil(emptyGrainTable.length, Sizes.Sector);
        byte[] headerBytes = _serverHeader.getBytes();
        _fileStream.setPosition(0);
        _fileStream.write(headerBytes, 0, headerBytes.length);
        // Update the global directory
        _globalDirectory[grainTable] = startSector;
        writeGlobalDirectory();
        _grainTable = new byte[_header.NumGTEsPerGT * 4];
        _currentGrainTable = grainTable;
    }

    private void writeGlobalDirectory() {
        byte[] buffer = new byte[_globalDirectory.length * 4];
        for (int i = 0; i < _globalDirectory.length; ++i) {
            EndianUtilities.writeBytesLittleEndian(_globalDirectory[i], buffer, i * 4);
        }
        _fileStream.setPosition(_serverHeader.GdOffset * Sizes.Sector);
        _fileStream.write(buffer, 0, buffer.length);
    }

    private void writeGrainTable() {
        if (_grainTable == null) {
            throw new IllegalStateException("No grain table loaded");
        }

        _fileStream.setPosition(_globalDirectory[_currentGrainTable] * (long) Sizes.Sector);
        _fileStream.write(_grainTable, 0, _grainTable.length);
    }

}
