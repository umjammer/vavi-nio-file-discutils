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

package discUtils.dmg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import discUtils.core.compression.BZip2DecoderStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.buffer.Buffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;


public class UdifBuffer extends Buffer {

    private CompressedRun activeRun;

    private long activeRunOffset;

    private byte[] decompBuffer;

    private final ResourceFork resources;

    private final long sectorCount;

    private final Stream stream;

    public UdifBuffer(Stream stream, ResourceFork resources, long sectorCount) {
        this.stream = stream;
        this.resources = resources;
        this.sectorCount = sectorCount;
        blocks = new ArrayList<>();
        for (Resource resource : this.resources.getAllResources("blkx")) {
            blocks.add(((BlkxResource) resource).getBlock());
        }
    }

    private List<CompressedBlock> blocks;

    public List<CompressedBlock> getBlocks() {
        return blocks;
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canWrite() {
        return false;
    }

    @Override public long getCapacity() {
        return sectorCount * Sizes.Sector;
    }

    @Override public int read(long pos, byte[] buffer, int offset, int count) {
        int totalCopied = 0;
        long currentPos = pos;
        while (totalCopied < count && currentPos < getCapacity()) {
            loadRun(currentPos);
            int bufferOffset = (int) (currentPos - (activeRunOffset + activeRun.sectorStart * Sizes.Sector));
            int toCopy = (int) Math.min(activeRun.sectorCount * Sizes.Sector - bufferOffset, count - totalCopied);
            switch (activeRun.type) {
            case Zeros:
                Arrays.fill(buffer, offset + totalCopied, offset + totalCopied + toCopy, (byte) 0);
                break;
            case Raw:
                stream.position(activeRun.compOffset + bufferOffset);
                StreamUtilities.readExact(stream, buffer, offset + totalCopied, toCopy);
                break;
            case AdcCompressed:
            case ZlibCompressed:
            case BZlibCompressed:
                System.arraycopy(decompBuffer, bufferOffset, buffer, offset + totalCopied, toCopy);
                break;
            default:
                throw new UnsupportedOperationException("Reading from run of type " + activeRun.type);

            }
            currentPos += toCopy;
            totalCopied += toCopy;
        }
        return totalCopied;
    }

    @Override public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    @Override public void setCapacity(long value) {
        throw new UnsupportedOperationException();
    }

    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
        List<StreamExtent> result = new ArrayList<>();
        StreamExtent lastRun = null;
        for (CompressedBlock block : getBlocks()) {
            if ((block.firstSector + block.sectorCount) * Sizes.Sector < start) {
                continue;
            }

            // Skip blocks before start of range
            if (block.firstSector * Sizes.Sector > start + count) {
                continue;
            }

            for (CompressedRun run : block.runs) {
                // Skip blocks after end of range
                if (run.sectorCount > 0 && run.type != RunType.Zeros) {
                    long thisRunStart = (block.firstSector + run.sectorStart) * Sizes.Sector;
                    long thisRunEnd = thisRunStart + run.sectorCount * Sizes.Sector;
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
        if (activeRun != null && pos >= activeRunOffset + activeRun.sectorStart * Sizes.Sector &&
            pos < activeRunOffset + (activeRun.sectorStart + activeRun.sectorCount) * Sizes.Sector) {
            return;
        }

        long findSector = pos / 512;
        for (CompressedBlock block : getBlocks()) {
            if (block.firstSector <= findSector && block.firstSector + block.sectorCount > findSector) {
                // Make sure the decompression buffer is big enough
                if (decompBuffer == null || decompBuffer.length < block.decompressBufferRequested * Sizes.Sector) {
                    decompBuffer = new byte[block.decompressBufferRequested * Sizes.Sector];
                }

                for (CompressedRun run : block.runs) {
                    if (block.firstSector + run.sectorStart <= findSector &&
                        block.firstSector + run.sectorStart + run.sectorCount > findSector) {
                        loadRun(run);
                        activeRunOffset = block.firstSector * Sizes.Sector;
                        return;
                    }

                }
                throw new dotnet4j.io.IOException("No run for sector " + findSector + " in block starting at " +
                    block.firstSector);
            }

        }
        throw new dotnet4j.io.IOException("No block for sector " + findSector);
    }

    private void loadRun(CompressedRun run) {
        int toCopy = (int) (run.sectorCount * Sizes.Sector);

        switch (run.type) {
        case ZlibCompressed: {
            /*
             * *** WARNING ***
             * DeflateStream decompression needs zip header (0x78, 0x9c)
             * so spec. is different from original C# DeflateStream
             */
            stream.position(run.compOffset);

            try (DeflateStream ds = new DeflateStream(stream, CompressionMode.Decompress, true)) {
                StreamUtilities.readExact(ds, decompBuffer, 0, toCopy);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
            break;

        case AdcCompressed: {
            stream.position(run.compOffset);
            byte[] compressed = StreamUtilities.readExact(stream, (int) run.compLength);
            if (aDCDecompress(compressed, 0, compressed.length, decompBuffer, 0) != toCopy) {
                throw new IllegalArgumentException("Run too short when decompressed");
            }
        }
            break;

        case BZlibCompressed: {
            try (BZip2DecoderStream ds = new BZip2DecoderStream(new SubStream(stream, run.compOffset, run.compLength),
                                                                Ownership.None)) {
                StreamUtilities.readExact(ds, decompBuffer, 0, toCopy);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
            break;

        case Zeros:
        case Raw:
            break;

        default:
            throw new UnsupportedOperationException("Unrecognized run type " + run.type);
        }

        activeRun = run;
    }
}
