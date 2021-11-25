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

package LibraryTests.Ntfs;

import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sun.jna.ptr.IntByReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Core.Compression.BlockCompressor;
import DiscUtils.Core.Compression.CompressionResult;


@Disabled
public class LZNT1Test {
    private byte[] _uncompressedData;

    public LZNT1Test() {
        Random rng = new Random(3425);
        _uncompressedData = new byte[64 * 1024];
        for (int i = 0; i < 16 * 4096; ++i) {
            // Some test data that is reproducible, and fairly compressible
            byte b = (byte) (rng.nextInt(26) + 'A');
            int start = rng.nextInt(_uncompressedData.length);
            int len = rng.nextInt(20);
            for (int j = start; j < _uncompressedData.length && j < start + len; j++) {
                _uncompressedData[j] = b;
            }
        }
        for (int i = 5 * 4096; i < 6 * 4096; ++i) {
            // Make one block uncompressible
            _uncompressedData[i] = (byte) rng.nextInt(256);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createInstance(Class<T> clazz, String name) throws Exception {
        return (T) Class.forName(name).newInstance();
    }

    @Test
    public void compress() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        int[] compressedLength = new int[] {
            16 * 4096
        };
        byte[] compressedData = new byte[compressedLength[0]];
        // Double-check, make sure native code round-trips
        byte[] nativeCompressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 4096);
        assertArrayEquals(_uncompressedData, nativeDecompress(nativeCompressed, 0, nativeCompressed.length));
        compressor.setBlockSize(4096);
        CompressionResult r = compressor
                .compress(_uncompressedData, 0, _uncompressedData.length, compressedData, 0, compressedLength);
        assertEquals(CompressionResult.Compressed, r);
        assertArrayEquals(_uncompressedData, nativeDecompress(compressedData, 0, compressedLength[0]));
        assertTrue(compressedLength[0] < _uncompressedData.length * 0.66);
    }

    @Test
    public void compressMidSourceBuffer() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] inData = new byte[128 * 1024];
        System.arraycopy(_uncompressedData, 0, inData, 32 * 1024, 64 * 1024);
        int[] compressedLength = new int[] {
            16 * 4096
        };
        byte[] compressedData = new byte[compressedLength[0]];
        // Double-check, make sure native code round-trips
        byte[] nativeCompressed = nativeCompress(inData, 32 * 1024, _uncompressedData.length, 4096);
        assertArrayEquals(_uncompressedData, nativeDecompress(nativeCompressed, 0, nativeCompressed.length));
        compressor.setBlockSize(4096);
        CompressionResult r = compressor
                .compress(inData, 32 * 1024, _uncompressedData.length, compressedData, 0, compressedLength);
        assertEquals(CompressionResult.Compressed, r);
        assertArrayEquals(_uncompressedData, nativeDecompress(compressedData, 0, compressedLength[0]));
    }

    @Test
    public void compressMidDestBuffer() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        // Double-check, make sure native code round-trips
        byte[] nativeCompressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 4096);
        assertArrayEquals(_uncompressedData, nativeDecompress(nativeCompressed, 0, nativeCompressed.length));
        int[] compressedLength = new int[] {
            128 * 1024
        };
        byte[] compressedData = new byte[compressedLength[0]];
        compressor.setBlockSize(4096);
        CompressionResult r = compressor
                .compress(_uncompressedData, 0, _uncompressedData.length, compressedData, 32 * 1024, compressedLength);
        assertEquals(CompressionResult.Compressed, r);
        assertTrue(compressedLength[0] < _uncompressedData.length);
        assertArrayEquals(_uncompressedData, nativeDecompress(compressedData, 32 * 1024, compressedLength[0]));
    }

    @Test
    public void compress1KBlockSize() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        int[] compressedLength = new int[] {
            16 * 4096
        };
        byte[] compressedData = new byte[compressedLength[0]];
        // Double-check, make sure native code round-trips
        byte[] nativeCompressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 1024);
        assertArrayEquals(_uncompressedData, nativeDecompress(nativeCompressed, 0, nativeCompressed.length));
        compressor.setBlockSize(1024);
        CompressionResult r = compressor
                .compress(_uncompressedData, 0, _uncompressedData.length, compressedData, 0, compressedLength);
        assertEquals(CompressionResult.Compressed, r);
        byte[] duDecompressed = new byte[_uncompressedData.length];
        int numDuDecompressed = compressor.decompress(compressedData, 0, compressedLength[0], duDecompressed, 0);
        byte[] rightSizedDuDecompressed = new byte[numDuDecompressed];
        System.arraycopy(duDecompressed, 0, rightSizedDuDecompressed, 0, numDuDecompressed);
        // Note: Due to bug in Windows LZNT1, we compare against native decompression,
        // not the original data, since
        // Windows LZNT1 corrupts data on decompression when block size != 4096.
        assertArrayEquals(rightSizedDuDecompressed, nativeDecompress(compressedData, 0, compressedLength[0]));
    }

    @Test
    public void compress1KBlock() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] uncompressed1K = new byte[1024];
        System.arraycopy(_uncompressedData, 0, uncompressed1K, 0, 1024);
        int[] compressedLength = new int[] {
            1024
        };
        byte[] compressedData = new byte[compressedLength[0]];
        // Double-check, make sure native code round-trips
        byte[] nativeCompressed = nativeCompress(uncompressed1K, 0, 1024, 1024);
        assertArrayEquals(uncompressed1K, nativeDecompress(nativeCompressed, 0, nativeCompressed.length));
        compressor.setBlockSize(1024);
        CompressionResult r = compressor.compress(uncompressed1K, 0, 1024, compressedData, 0, compressedLength);
        assertEquals(CompressionResult.Compressed, r);
        assertArrayEquals(uncompressed1K, nativeDecompress(compressedData, 0, compressedLength[0]));
    }

    @Test
    public void compressAllZeros() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] compressed = new byte[64 * 1024];
        int[] numCompressed = new int[] {
            64 * 1024
        };
        assertEquals(CompressionResult.AllZeros,
                     compressor.compress(new byte[64 * 1024], 0, 64 * 1024, compressed, 0, numCompressed));
    }

    @Test
    public void compressIncompressible() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        Random rng = new Random(6324);
        byte[] uncompressed = new byte[64 * 1024];
        rng.nextBytes(uncompressed);
        byte[] compressed = new byte[64 * 1024];
        int[] numCompressed = new int[] {
            64 * 1024
        };
        assertEquals(CompressionResult.Incompressible,
                     compressor.compress(uncompressed, 0, uncompressed.length, compressed, 0, numCompressed));
    }

    @Test
    public void decompress() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] compressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 4096);
        // Double-check, make sure native code round-trips
        assertArrayEquals(_uncompressedData, nativeDecompress(compressed, 0, compressed.length));
        byte[] decompressed = new byte[_uncompressedData.length];
        int numDecompressed = compressor.decompress(compressed, 0, compressed.length, decompressed, 0);
        assertEquals(numDecompressed, _uncompressedData.length);
        assertArrayEquals(_uncompressedData, decompressed);
    }

    @Test
    public void decompressMidSourceBuffer() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] compressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 4096);
        byte[] inData = new byte[128 * 1024];
        System.arraycopy(compressed, 0, inData, 32 * 1024, compressed.length);
        // Double-check, make sure native code round-trips
        assertArrayEquals(_uncompressedData, nativeDecompress(inData, 32 * 1024, compressed.length));
        byte[] decompressed = new byte[_uncompressedData.length];
        int numDecompressed = compressor.decompress(inData, 32 * 1024, compressed.length, decompressed, 0);
        assertEquals(numDecompressed, _uncompressedData.length);
        assertArrayEquals(_uncompressedData, decompressed);
    }

    @Test
    public void decompressMidDestBuffer() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] compressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 4096);
        // Double-check, make sure native code round-trips
        assertArrayEquals(_uncompressedData, nativeDecompress(compressed, 0, compressed.length));
        byte[] outData = new byte[128 * 1024];
        int numDecompressed = compressor.decompress(compressed, 0, compressed.length, outData, 32 * 1024);
        assertEquals(numDecompressed, _uncompressedData.length);
        byte[] decompressed = new byte[_uncompressedData.length];
        System.arraycopy(outData, 32 * 1024, decompressed, 0, _uncompressedData.length);
        assertArrayEquals(_uncompressedData, decompressed);
    }

    @Test
    public void decompress1KBlockSize() throws Exception {
        BlockCompressor compressor = createInstance(BlockCompressor.class, "DiscUtils.Ntfs.LZNT1");
        byte[] compressed = nativeCompress(_uncompressedData, 0, _uncompressedData.length, 1024);
        assertArrayEquals(_uncompressedData, nativeDecompress(compressed, 0, compressed.length));
        byte[] decompressed = new byte[_uncompressedData.length];
        int numDecompressed = compressor.decompress(compressed, 0, compressed.length, decompressed, 0);
        assertEquals(numDecompressed, _uncompressedData.length);
        assertArrayEquals(_uncompressedData, decompressed);
    }

    private static byte[] nativeCompress(byte[] data, int offset, int length, int chunkSize) {
        byte[] uncompressedBuffer = new byte[length];
        System.arraycopy(data, offset, uncompressedBuffer, 0, length);
        int maxSize = MSCompression.INSTANCE.ms_max_compressed_size(MSCompression.MSCOMP_LZNT1, length);
        byte[] compressedBuffer = new byte[maxSize];
        IntByReference compressedSize = new IntByReference();
        int ntStatus = MSCompression.INSTANCE.ms_compress(MSCompression.MSCOMP_LZNT1,
                                                          uncompressedBuffer,
                                                          length,
                                                          compressedBuffer,
                                                          compressedSize);
        assertEquals(MSCompression.MSCOMP_OK, ntStatus);
        return compressedBuffer;
    }

    private static byte[] nativeDecompress(byte[] data, int offset, int length) {
        byte[] compressedBuffer = new byte[length];
        System.arraycopy(data, offset, compressedBuffer, 0, length);
        byte[] uncompressedBuffer = new byte[64 * 1024];
        IntByReference uncompressedSize = new IntByReference();
        int ntStatus = MSCompression.INSTANCE.ms_decompress(MSCompression.MSCOMP_LZNT1,
                                                            compressedBuffer,
                                                            length,
                                                            uncompressedBuffer,
                                                            uncompressedSize);
        assertEquals(MSCompression.MSCOMP_OK, ntStatus);
        return uncompressedBuffer;
    }

    static {
        com.sun.jna.NativeLibrary.addSearchPath("MSCompression", System.getProperty("java.library.path"));
    }

//    private static native int rtlGetCompressionWorkSpaceSize(short formatAndEngine,
//                                                             int[] bufferWorkspaceSize,
//                                                             int[] fragmentWorkspaceSize);
//
//    private static native int rtlCompressBuffer(short formatAndEngine,
//                                                ByteBuffer uncompressedBuffer,
//                                                int uncompressedBufferSize,
//                                                ByteBuffer compressedBuffer,
//                                                int compressedBufferSize,
//                                                int uncompressedChunkSize,
//                                                int[] finalCompressedSize,
//                                                ByteBuffer workspace);
//
//    private static native int rtlDecompressBuffer(short formatAndEngine,
//                                                  ByteBuffer uncompressedBuffer,
//                                                  int uncompressedBufferSize,
//                                                  ByteBuffer compressedBuffer,
//                                                  int compressedBufferSize,
//                                                  int[] finalUncompressedSize);
}
