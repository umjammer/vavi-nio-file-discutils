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

package libraryTests;

import discUtils.streams.SparseStream;
import discUtils.streams.block.BlockCacheSettings;
import discUtils.streams.block.BlockCacheStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


public class BlockCacheTest {
    @Test
    public void dispose() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose), Ownership.Dispose);
        cacheStream.close();
        try {
            cacheStream.position(0);
            cacheStream.readByte();
            fail("Cache stream should have failed - disposed");
        } catch (IOException ignored) {}

        try {
            ms.position(0);
            ms.readByte();
            fail("Cache stream should have failed - disposed");
        } catch (IOException ignored) {}
    }

    @Test
    public void largeRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[40];
        cacheStream.position(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(1, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void readThrough() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void cachedRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[buffer.length];
        cacheStream.position(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void unalignedRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(3);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 3);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void unalignedCachedRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(3);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 3);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[buffer.length];
        cacheStream.position(3);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 3);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void overread() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(90);
        int numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(10, numRead);
        assertSequenced(buffer, 0, 10, 90);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void cachedOverread() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(90);
        int numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(10, numRead);
        assertSequenced(buffer, 0, 10, 90);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[buffer.length];
        cacheStream.position(90);
        numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(10, numRead);
        assertSequenced(buffer, 0, 10, 90);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void cacheBlockRecycle() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(50);
        settings.setLargeReadSize(100);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[50];
        cacheStream.position(10);
        int numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(50, numRead);
        assertSequenced(buffer, 10);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[40];
        cacheStream.position(50);
        numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(40, numRead);
        assertSequenced(buffer, 50);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    @Test
    public void write() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, true);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.position(10);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 10);
        cacheStream.position(20);
        cacheStream.write(new byte[10], 0, 10);
        assertEquals(30, cacheStream.position());
        cacheStream.position(10);
        buffer = new byte[30];
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0, 10, 10);
        assertSequenced(buffer, 20, 10, 30);
        assertEquals(0, buffer[10]);
        assertEquals(0, buffer[19]);
    }

    @Test
    public void failWrite() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        settings.setBlockSize(10);
        settings.setOptimumReadSize(20);
        settings.setReadCacheSize(100);
        settings.setLargeReadSize(30);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[25];
        cacheStream.position(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        int freeBefore = cacheStream.getStatistics().getFreeReadBlocks();
        cacheStream.position(11);
        try {
            cacheStream.write(new byte[10], 0, 10);
        } catch (IOException e) {
            assertEquals(freeBefore + 2, cacheStream.getStatistics().getFreeReadBlocks());
        }

    }

    private MemoryStream createSequencedMemStream(int length, boolean writable) {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; ++i) {
            buffer[i] = (byte) i;
        }
        return new MemoryStream(buffer, writable);
    }

    private void assertSequenced(byte[] buffer, int seqOffset) {
        assertSequenced(buffer, 0, buffer.length, seqOffset);
    }

    private void assertSequenced(byte[] buffer, int offset, int count, int seqOffset) {
        for (int i = 0; i < count; ++i) {
            if (buffer[i + offset] != (byte) (i + seqOffset)) {
                fail(String.format("Expected %2x at index %d, was %2x",
                        (byte) (i + seqOffset),
                        i + offset,
                        buffer[i + offset]));
            }
        }
    }
}
