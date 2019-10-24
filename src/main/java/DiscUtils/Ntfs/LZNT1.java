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
//
// Contributions by bsobel:
//   - Compression algorithm distantly derived from Puyo tools (BSD license)*
//   - Decompression adjusted to support variety of block sizes
//
// (*) Puyo tools implements a different LZ-style algorithm
//

package DiscUtils.Ntfs;

import java.util.Arrays;

import DiscUtils.Core.Compression.BlockCompressor;
import DiscUtils.Core.Compression.CompressionResult;
import DiscUtils.Streams.Util.EndianUtilities;


/**
 * Implementation of the LZNT1 algorithm used for compressing NTFS files.
 *
 * Due to apparent bugs in Window's LZNT1 decompressor, it is <b>strongly</b>
 * recommended that
 * only the block size of 4096 is used. Other block sizes corrupt data on
 * decompression.
 */
public final class LZNT1 extends BlockCompressor {
    private static final short SubBlockIsCompressedFlag = (short) 0x8000;

    private static final short SubBlockSizeMask = 0x0fff;

    // LZNT1 appears to ignore the actual block size requested, most likely due to
    // a bug in the decompressor, which assumes 4KB block size.  To be bug-compatible,
    // we assume each block is 4KB on decode also.
    private static final int FixedBlockSize = 0x1000;

    private static final byte[] _compressionBits = calcCompressionBits();

    public LZNT1() {
        setBlockSize(4096);
    }

    public CompressionResult compress(byte[] source,
                                      int sourceOffset,
                                      int sourceLength,
                                      byte[] compressed,
                                      int compressedOffset,
                                      int[] compressedLength) {
        int sourcePointer = 0;
        int sourceCurrentBlock = 0;
        int destPointer = 0;
        // Set up the Lz Compression Map
        LzWindowDictionary lzMap = new LzWindowDictionary();
        boolean nonZeroDataFound = false;
        for (int subBlock = 0; subBlock < sourceLength; subBlock += getBlockSize()) {
            lzMap.setMinMatchAmount(3);
            sourceCurrentBlock = sourcePointer;
            int decompressedSize = Math.min(sourceLength - subBlock, getBlockSize());
            int compressedSize = 0;
            // Start compression
            int headerPosition = destPointer;
            compressed[compressedOffset + destPointer] = compressed[compressedOffset + destPointer + 1] = 0;
            destPointer += 2;
            while (sourcePointer - subBlock < decompressedSize) {
                if (destPointer + 1 >= compressedLength[0]) {
                    return CompressionResult.Incompressible;
                }

                byte bitFlag = 0x0;
                int flagPosition = destPointer;
                compressed[compressedOffset + destPointer] = bitFlag;
                // It will be filled in later
                compressedSize++;
                destPointer++;
                for (int i = 0; i < 8; i++) {
                    int lengthBits = 16 - _compressionBits[sourcePointer - subBlock];
                    short lengthMask = (short) ((1 << _compressionBits[sourcePointer - subBlock]) - 1);
                    lzMap.setMaxMatchAmount(Math.min(1 << lengthBits, getBlockSize() - 1));
                    int[] lzSearchMatch = lzMap
                            .search(source, sourceOffset + subBlock, sourcePointer - subBlock, decompressedSize);
                    if (lzSearchMatch[1] > 0) {
                        // There is a compression match
                        if (destPointer + 2 >= compressedLength[0]) {
                            return CompressionResult.Incompressible;
                        }

                        bitFlag |= (byte) (1 << i);
                        int rawOffset = lzSearchMatch[0];
                        int rawLength = lzSearchMatch[1];
                        int convertedOffset = (rawOffset - 1) << lengthBits;
                        int convertedSize = (rawLength - 3) & ((1 << lengthMask) - 1);
                        short convertedData = (short) (convertedOffset | convertedSize);
                        EndianUtilities.writeBytesLittleEndian(convertedData, compressed, compressedOffset + destPointer);
                        lzMap.addEntryRange(source,
                                            sourceOffset + subBlock,
                                            sourcePointer - subBlock,
                                            lzSearchMatch[1]);
                        sourcePointer += lzSearchMatch[1];
                        destPointer += 2;
                        compressedSize += 2;
                    } else {
                        // There wasn't a match
                        if (destPointer + 1 >= compressedLength[0]) {
                            return CompressionResult.Incompressible;
                        }

                        bitFlag |= (byte) (0 << i);
                        if (source[sourceOffset + sourcePointer] != 0) {
                            nonZeroDataFound = true;
                        }

                        compressed[compressedOffset + destPointer] = source[sourceOffset + sourcePointer];
                        lzMap.addEntry(source, sourceOffset + subBlock, sourcePointer - subBlock);
                        sourcePointer++;
                        destPointer++;
                        compressedSize++;
                    }
                    // Check for out of bounds
                    if (sourcePointer - subBlock >= decompressedSize) {
                        break;
                    }

                }
                // Write the real flag.
                compressed[compressedOffset + flagPosition] = bitFlag;
            }
            // If compressed size >= block size just store block
            if (compressedSize >= getBlockSize()) {
                // Set the header to indicate non-compressed block
                EndianUtilities.writeBytesLittleEndian((short) (0x3000 | (getBlockSize() - 1)),
                                                       compressed,
                                                       compressedOffset + headerPosition);
                System.arraycopy(source,
                                 sourceOffset + sourceCurrentBlock,
                                 compressed,
                                 compressedOffset + headerPosition + 2,
                                 getBlockSize());
                destPointer = headerPosition + 2 + getBlockSize();
                // Make sure decompression stops by setting the next two bytes to null, prevents us from having to
                // clear the rest of the array.
                compressed[destPointer] = 0;
                compressed[destPointer + 1] = 0;
            } else {
                // Set the header to indicate compressed and the right length
                EndianUtilities.writeBytesLittleEndian((short) (0xb000 | (compressedSize - 1)),
                                                       compressed,
                                                       compressedOffset + headerPosition);
            }
            lzMap.reset();
        }
        if (destPointer >= sourceLength) {
            compressedLength[0] = 0;
            return CompressionResult.Incompressible;
        }

        if (nonZeroDataFound) {
            compressedLength[0] = destPointer;
            return CompressionResult.Compressed;
        }

        compressedLength[0] = 0;
        return CompressionResult.AllZeros;
    }

    public int decompress(byte[] source, int sourceOffset, int sourceLength, byte[] decompressed, int decompressedOffset) {
        int sourceIdx = 0;
        int destIdx = 0;
        while (sourceIdx < sourceLength) {
            short header = EndianUtilities.toUInt16LittleEndian(source, sourceOffset + sourceIdx);
            sourceIdx += 2;
            // Look for null-terminating sub-block header
            if (header == 0) {
                break;
            }

            if ((header & SubBlockIsCompressedFlag) == 0) {
                int blockSize = (header & SubBlockSizeMask) + 1;
                System.arraycopy(source, sourceOffset + sourceIdx, decompressed, decompressedOffset + destIdx, blockSize);
                sourceIdx += blockSize;
                destIdx += blockSize;
            } else {
                // compressed
                int destSubBlockStart = destIdx;
                int srcSubBlockEnd = sourceIdx + (header & SubBlockSizeMask) + 1;
                while (sourceIdx < srcSubBlockEnd) {
                    byte tag = source[sourceOffset + sourceIdx];
                    ++sourceIdx;
                    for (int token = 0; token < 8; ++token) {
                        // We might have hit the end of the sub block whilst still working though
                        // a tag - abort if we have...
                        if (sourceIdx >= srcSubBlockEnd) {
                            break;
                        }

                        if ((tag & 1) == 0) {
                            if (decompressedOffset + destIdx >= decompressed.length) {
                                return destIdx;
                            }

                            decompressed[decompressedOffset + destIdx] = source[sourceOffset + sourceIdx];
                            ++destIdx;
                            ++sourceIdx;
                        } else {
                            short lengthBits = (short) (16 - _compressionBits[destIdx - destSubBlockStart]);
                            short lengthMask = (short) ((1 << lengthBits) - 1);
                            short phraseToken = EndianUtilities.toUInt16LittleEndian(source, sourceOffset + sourceIdx);
                            sourceIdx += 2;
                            int destBackAddr = destIdx - (phraseToken >>> lengthBits) - 1;
                            int length = (phraseToken & lengthMask) + 3;
                            for (int i = 0; i < length; ++i) {
                                decompressed[decompressedOffset + destIdx++] = decompressed[decompressedOffset +
                                                                                            destBackAddr++];
                            }
                        }
                        tag >>>= 1;
                    }
                }
                // Bug-compatible - if we decompressed less than 4KB, jump to next 4KB boundary.  If
                // that would leave less than a 4KB remaining, abort with data decompressed so far.
                if (decompressedOffset + destIdx + FixedBlockSize > decompressed.length) {
                    return destIdx;
                }

                if (destIdx < destSubBlockStart + FixedBlockSize) {
                    int skip = destSubBlockStart + FixedBlockSize - destIdx;
                    Arrays.fill(decompressed, decompressedOffset + destIdx, decompressedOffset + destIdx + skip, (byte) 0);
                    destIdx += skip;
                }
            }
        }
        return destIdx;
    }

    private static byte[] calcCompressionBits() {
        byte[] result = new byte[4096];
        byte offsetBits = 0;
        int y = 0x10;
        for (int x = 0; x < result.length; x++) {
            result[x] = (byte) (4 + offsetBits);
            if (x == y) {
                y <<= 1;
                offsetBits++;
            }
        }
        return result;
    }
}
