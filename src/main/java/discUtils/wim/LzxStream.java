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

package discUtils.wim;

import java.io.IOException;

import discUtils.core.compression.HuffmanTree;
import discUtils.streams.util.EndianUtilities;
import dotnet4j.io.BufferedStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;


/**
 * Class to read data compressed using LZX algorithm.
 * This is not a general purpose LZX decompressor - it makes
 * simplifying assumptions, such as being able to load the entire stream
 * contents into memory..
 */
public class LzxStream extends Stream {

    private static final int[] positionSlots;

    private static final int[] extraBits;

    private HuffmanTree alignedOffsetTree;

    private final LzxBitStream bitStream;

    private byte[] buffer;

    private int bufferCount;

    private final int fileSize;

    private HuffmanTree lengthTree;

    // block state
    private HuffmanTree mainTree;

    private final int numPositionSlots;

    private long position;

    private final int[] repeatedOffsets = {
        1, 1, 1
    };

    private final int windowBits;
    static {
        try {
            positionSlots = new int[50];
            extraBits = new int[50];
            int numBits = 0;
            positionSlots[1] = 1;
            for (int i = 2; i < 50; i += 2) {
                extraBits[i] = numBits;
                extraBits[i + 1] = numBits;
                positionSlots[i] = positionSlots[i - 1] + (1 << extraBits[i - 1]);
                positionSlots[i + 1] = positionSlots[i] + (1 << numBits);
                if (numBits < 17) {
                    numBits++;
                }

            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public LzxStream(Stream stream, int windowBits, int fileSize) {
        bitStream = new LzxBitStream(new BufferedStream(stream, 8192));
        this.windowBits = windowBits;
        this.fileSize = fileSize;
        numPositionSlots = this.windowBits * 2;
        buffer = new byte[1 << windowBits];
        readBlocks();
    }

    public boolean canRead() {
        return true;
    }

    public boolean canSeek() {
        return false;
    }

    public boolean canWrite() {
        return false;
    }

    public long getLength() {
        return bufferCount;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        position = value;
    }

    public void flush() {
    }

    public int read(byte[] buffer, int offset, int count) {
        if (position > getLength()) {
            return 0;
        }

        int numToRead = (int) Math.min(count, bufferCount - position);
        System.arraycopy(this.buffer, (int) position, buffer, offset, numToRead);
        position += numToRead;
        return numToRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        throw new UnsupportedOperationException();
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    private void readBlocks() {
        BlockType blockType = BlockType.values()[bitStream.read(3)];
        buffer = new byte[32768];
        bufferCount = 0;
        while (blockType != BlockType.None) {
            int blockSize = bitStream.read(1) == 1 ? 1 << 15 : bitStream.read(16);
            if (blockType == BlockType.Uncompressed) {
                decodeUncompressedBlock(blockSize);
            } else {
                decodeCompressedBlock(blockType, blockSize);
                bufferCount += blockSize;
            }
            // Read start of next block (if any)
            blockType = BlockType.values()[bitStream.read(3)];
        }
        fixupBlockBuffer();
    }

    /**
     * Fix up CALL instruction optimization.
     * A slightly odd feature of LZX for optimizing executable compression is
     * that
     * relative CALL instructions (opcode E8) are converted to absolute values
     * before compression.
     * This feature seems to always be turned-on in WIM files, so we have to
     * apply the reverse
     * conversion.
     */
    private void fixupBlockBuffer() {
        byte[] temp = new byte[4];
        int i = 0;
        while (i < bufferCount - 10) {
            if ((buffer[i] & 0xff) == 0xE8) {
                System.arraycopy(buffer, i + 1, temp, 0, 4);
                int absoluteValue = EndianUtilities.toInt32LittleEndian(buffer, i + 1);
                if (absoluteValue >= -i && absoluteValue < fileSize) {
                    int offsetValue;
                    if (absoluteValue >= 0) {
                        offsetValue = absoluteValue - i;
                    } else {
                        offsetValue = absoluteValue + fileSize;
                    }
                    EndianUtilities.writeBytesLittleEndian(offsetValue, buffer, i + 1);
                }

                i += 4;
            }

            ++i;
        }
    }

    private void decodeUncompressedBlock(int blockSize) {
        bitStream.align(16);
        repeatedOffsets[0] = EndianUtilities.toUInt32LittleEndian(bitStream.readBytes(4), 0);
        repeatedOffsets[1] = EndianUtilities.toUInt32LittleEndian(bitStream.readBytes(4), 0);
        repeatedOffsets[2] = EndianUtilities.toUInt32LittleEndian(bitStream.readBytes(4), 0);
        int numRead = bitStream.readBytes(buffer, bufferCount, blockSize);
        bufferCount += numRead;
        if ((numRead & 1) != 0) {
            bitStream.readBytes(1);
        }
    }

    private void decodeCompressedBlock(BlockType blockType, int blockSize) {
        if (blockType == BlockType.AlignedOffset) {
            alignedOffsetTree = readFixedHuffmanTree(8, 3);
        }

        readMainTree();
        readLengthTree();
        int numRead = 0;
        while (numRead < blockSize) {
            int symbol = mainTree.nextSymbol(bitStream);
            if (symbol < 256) {
                buffer[bufferCount + numRead++] = (byte) symbol;
            } else {
                int lengthHeader = (symbol - 256) & 7;
                int matchLength = lengthHeader + 2 + (lengthHeader == 7 ? lengthTree.nextSymbol(bitStream) : 0);
                int positionSlot = (symbol - 256) >>> 3;
                int matchOffset;
                if (positionSlot == 0) {
                    matchOffset = repeatedOffsets[0];
                } else if (positionSlot == 1) {
                    matchOffset = repeatedOffsets[1];
                    repeatedOffsets[1] = repeatedOffsets[0];
                    repeatedOffsets[0] = matchOffset;
                } else if (positionSlot == 2) {
                    matchOffset = repeatedOffsets[2];
                    repeatedOffsets[2] = repeatedOffsets[0];
                    repeatedOffsets[0] = matchOffset;
                } else {
                    int extra = extraBits[positionSlot];
                    int formattedOffset;
                    if (blockType == BlockType.AlignedOffset) {
                        int verbatimBits = 0;
                        int alignedBits = 0;
                        if (extra >= 3) {
                            verbatimBits = bitStream.read(extra - 3) << 3;
                            alignedBits = alignedOffsetTree.nextSymbol(bitStream);
                        } else if (extra > 0) {
                            verbatimBits = bitStream.read(extra);
                        }

                        formattedOffset = positionSlots[positionSlot] + verbatimBits + alignedBits;
                    } else {
                        int verbatimBits = extra > 0 ? bitStream.read(extra) : 0;
                        formattedOffset = positionSlots[positionSlot] + verbatimBits;
                    }
                    matchOffset = formattedOffset - 2;
                    repeatedOffsets[2] = repeatedOffsets[1];
                    repeatedOffsets[1] = repeatedOffsets[0];
                    repeatedOffsets[0] = matchOffset;
                }
                int destOffset = bufferCount + numRead;
                int srcOffset = destOffset - matchOffset;
                if (matchLength >= 0) System.arraycopy(buffer, srcOffset + 0, buffer, destOffset + 0, matchLength);
                numRead += matchLength;
            }
        }
    }

    private void readMainTree() {
        int[] lengths;
        if (mainTree == null) {
            lengths = new int[256 + 8 * numPositionSlots];
        } else {
            lengths = mainTree.getLengths();
        }
        HuffmanTree preTree = readFixedHuffmanTree(20, 4);
        readLengths(preTree, lengths, 0, 256);
        preTree = readFixedHuffmanTree(20, 4);
        readLengths(preTree, lengths, 256, 8 * numPositionSlots);
        mainTree = new HuffmanTree(lengths);
    }

    private void readLengthTree() {
        HuffmanTree preTree = readFixedHuffmanTree(20, 4);
        lengthTree = readDynamicHuffmanTree(249, preTree, lengthTree);
    }

    private HuffmanTree readFixedHuffmanTree(int count, int bits) {
        int[] treeLengths = new int[count];
        for (int i = 0; i < treeLengths.length; ++i) {
            treeLengths[i] = bitStream.read(bits);
        }
        return new HuffmanTree(treeLengths);
    }

    private HuffmanTree readDynamicHuffmanTree(int count, HuffmanTree preTree, HuffmanTree oldTree) {
        int[] lengths;
        if (oldTree == null) {
            lengths = new int[256 + 8 * numPositionSlots];
        } else {
            lengths = oldTree.getLengths();
        }
        readLengths(preTree, lengths, 0, count);
        return new HuffmanTree(lengths);
    }

    private void readLengths(HuffmanTree preTree, int[] lengths, int offset, int count) {
        int i = 0;
        while (i < count) {
            int value = preTree.nextSymbol(bitStream);
            if (value == 17) {
                int numZeros = 4 + bitStream.read(4);
                for (int j = 0; j < numZeros; ++j) {
                    lengths[offset + i] = 0;
                    ++i;
                }
            } else if (value == 18) {
                int numZeros = 20 + bitStream.read(5);
                for (int j = 0; j < numZeros; ++j) {
                    lengths[offset + i] = 0;
                    ++i;
                }
            } else if (value == 19) {
                int same = bitStream.read(1);
                value = preTree.nextSymbol(bitStream);
                if (value > 16) {
                    throw new IllegalArgumentException("Invalid table encoding");
                }

                int symbol = (17 + lengths[offset + i] - value) % 17;
                for (int j = 0; j < 4 + same; ++j) {
                    lengths[offset + i] = symbol;
                    ++i;
                }
            } else {
                lengths[offset + i] = (17 + lengths[offset + i] - value) % 17;
                ++i;
            }
        }
    }

    private enum BlockType {
        None,
        Verbatim,
        AlignedOffset,
        Uncompressed
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    }
}
