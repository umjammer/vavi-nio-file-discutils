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

package DiscUtils.Dmg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.Compression.BZip2DecoderStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Buffer.Buffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.CompressionMode;
import moe.yo3explorer.dotnetio4j.DeflateStream;
import moe.yo3explorer.dotnetio4j.Stream;


public class UdifBuffer extends Buffer {
    private CompressedRun _activeRun;

    private long _activeRunOffset;

    private byte[] _decompBuffer;

    private final ResourceFork _resources;

    private final long _sectorCount;

    private final Stream _stream;

    public UdifBuffer(Stream stream, ResourceFork resources, long sectorCount) {
        _stream = stream;
        _resources = resources;
        _sectorCount = sectorCount;
        __Blocks = new ArrayList<>();
        for (Resource resource : _resources.getAllResources("blkx")) {
            getBlocks().add(((BlkxResource) resource).getBlock());
        }
    }

    private List<CompressedBlock> __Blocks = new ArrayList<>();

    public List<CompressedBlock> getBlocks() {
        return __Blocks;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public long getCapacity() {
        return _sectorCount * Sizes.Sector;
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        int totalCopied = 0;
        long currentPos = pos;
        while (totalCopied < count && currentPos < getCapacity()) {
            loadRun(currentPos);
            int bufferOffset = (int) (currentPos - (_activeRunOffset + _activeRun.SectorStart * Sizes.Sector));
            int toCopy = (int) Math.min(_activeRun.SectorCount * Sizes.Sector - bufferOffset, count - totalCopied);
            switch (_activeRun.Type) {
            case Zeros:
                Arrays.fill(buffer, offset + totalCopied, toCopy, (byte) 0);
                break;
            case Raw:
                _stream.setPosition(_activeRun.CompOffset + bufferOffset);
                StreamUtilities.readExact(_stream, buffer, offset + totalCopied, toCopy);
                break;
            case AdcCompressed:
            case ZlibCompressed:
            case BZlibCompressed:
                System.arraycopy(_decompBuffer, bufferOffset, buffer, offset + totalCopied, toCopy);
                break;
            default:
                throw new UnsupportedOperationException("Reading from run of type " + _activeRun.Type);

            }
            currentPos += toCopy;
            totalCopied += toCopy;
        }
        return totalCopied;
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        StreamExtent lastRun = null;
        for (CompressedBlock block : getBlocks()) {
            if ((block.FirstSector + block.SectorCount) * Sizes.Sector < start) {
                continue;
            }

            // Skip blocks before start of range
            if (block.FirstSector * Sizes.Sector > start + count) {
                continue;
            }

            for (CompressedRun run : block.Runs) {
                // Skip blocks after end of range
                if (run.SectorCount > 0 && run.Type != RunType.Zeros) {
                    long thisRunStart = (block.FirstSector + run.SectorStart) * Sizes.Sector;
                    long thisRunEnd = thisRunStart + run.SectorCount * Sizes.Sector;
                    thisRunStart = Math.max(thisRunStart, start);
                    thisRunEnd = Math.min(thisRunEnd, start + count);
                    long thisRunLength = thisRunEnd - thisRunStart;
                    if (thisRunLength > 0) {
                        if (lastRun != null && lastRun.getStart() + lastRun.getLength() == thisRunStart) {
                            lastRun = new StreamExtent(lastRun.getStart(), lastRun.getLength() + thisRunLength);
                        } else {
                            if (lastRun != null) {
                                result.add(lastRun);
                            }

                            lastRun = new StreamExtent(thisRunStart, thisRunLength);
                        }
                    }
                }
            }
        }
        if (lastRun != null) {
            result.add(lastRun);
        }
        return result;
    }

    private static int aDCDecompress(byte[] inputBuffer,
                                     int inputOffset,
                                     int inputCount,
                                     byte[] outputBuffer,
                                     int outputOffset) {
        int consumed = 0;
        int written = 0;
        while (consumed < inputCount) {
            byte focusByte = inputBuffer[inputOffset + consumed];
            if ((focusByte & 0x80) != 0) {
                // Data Run
                int chunkSize = (focusByte & 0x7F) + 1;
                System.arraycopy(inputBuffer, inputOffset + consumed + 1, outputBuffer, outputOffset + written, chunkSize);
                consumed += chunkSize + 1;
                written += chunkSize;
            } else if ((focusByte & 0x40) != 0) {
                // 3 byte code
                int chunkSize = (focusByte & 0x3F) + 4;
                int offset = EndianUtilities.toUInt16BigEndian(inputBuffer, inputOffset + consumed + 1);
                for (int i = 0; i < chunkSize; ++i) {
                    outputBuffer[outputOffset + written + i] = outputBuffer[outputOffset + written + i - offset - 1];
                }
                consumed += 3;
                written += chunkSize;
            } else {
                // 2 byte code
                int chunkSize = ((focusByte & 0x3F) >>> 2) + 3;
                int offset = ((focusByte & 0x3) << 8) + (inputBuffer[inputOffset + consumed + 1] & 0xFF);
                for (int i = 0; i < chunkSize; ++i) {
                    outputBuffer[outputOffset + written + i] = outputBuffer[outputOffset + written + i - offset - 1];
                }
                consumed += 2;
                written += chunkSize;
            }
        }
        return written;
    }

    private void loadRun(long pos) {
        if (_activeRun != null && pos >= _activeRunOffset + _activeRun.SectorStart * Sizes.Sector &&
            pos < _activeRunOffset + (_activeRun.SectorStart + _activeRun.SectorCount) * Sizes.Sector) {
            return;
        }

        long findSector = pos / 512;
        for (CompressedBlock block : getBlocks()) {
            if (block.FirstSector <= findSector && block.FirstSector + block.SectorCount > findSector) {
                // Make sure the decompression buffer is big enough
                if (_decompBuffer == null || _decompBuffer.length < block.DecompressBufferRequested * Sizes.Sector) {
                    _decompBuffer = new byte[block.DecompressBufferRequested * Sizes.Sector];
                }

                for (CompressedRun run : block.Runs) {
                    if (block.FirstSector + run.SectorStart <= findSector &&
                        block.FirstSector + run.SectorStart + run.SectorCount > findSector) {
                        loadRun(run);
                        _activeRunOffset = block.FirstSector * Sizes.Sector;
                        return;
                    }

                }
                throw new moe.yo3explorer.dotnetio4j.IOException("No run for sector " + findSector + " in block starting at " +
                                                                 block.FirstSector);
            }

        }
        throw new moe.yo3explorer.dotnetio4j.IOException("No block for sector " + findSector);
    }

    private void loadRun(CompressedRun run) {
        int toCopy = (int) (run.SectorCount * Sizes.Sector);
        switch (run.Type) {
        case ZlibCompressed: {
            _stream.setPosition(run.CompOffset + 2);
            // 2 byte zlib header
            DeflateStream ds = new DeflateStream(_stream, CompressionMode.Decompress, true);
            try {
                {
                    StreamUtilities.readExact(ds, _decompBuffer, 0, toCopy);
                }
            } finally {
                if (ds != null)
                    try {
                        ds.close();
                    } catch (IOException e) {
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
                    }
            }
        }
            break;
        case AdcCompressed: {
            _stream.setPosition(run.CompOffset);
            byte[] compressed = StreamUtilities.readExact(_stream, (int) run.CompLength);
            if (aDCDecompress(compressed, 0, compressed.length, _decompBuffer, 0) != toCopy) {
                throw new IllegalArgumentException("Run too short when decompressed");
            }
        }
            break;
        case BZlibCompressed: {
            BZip2DecoderStream ds = new BZip2DecoderStream(new SubStream(_stream, run.CompOffset, run.CompLength),
                                                           Ownership.None);
            try {
                StreamUtilities.readExact(ds, _decompBuffer, 0, toCopy);
            } finally {
                try {
                    ds.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
            }
        }
            break;
        case Zeros:
        case Raw:
            break;
        default:
            throw new UnsupportedOperationException("Unrecognized run type " + run.Type);

        }
        _activeRun = run;
    }
}
