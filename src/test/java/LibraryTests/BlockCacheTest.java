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

package LibraryTests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Block.BlockCacheSettings;
import DiscUtils.Streams.Block.BlockCacheStream;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.MemoryStream;

public class BlockCacheTest {
    public void dispose() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose), Ownership.Dispose);
        cacheStream.close();
        try {
            cacheStream.setPosition(0);
            cacheStream.readByte();
            assertTrue(false, "Cache stream should have failed - disposed");
        } catch (IOException __dummyCatchVar0) {
        }

        try {
            ms.setPosition(0);
            ms.readByte();
            assertTrue(false, "Cache stream should have failed - disposed");
        } catch (IOException __dummyCatchVar1) {
        }

    }

    public void largeRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[40];
        cacheStream.setPosition(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(1, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void readThrough() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void cachedRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[buffer.length];
        cacheStream.setPosition(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void unalignedRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(3);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 3);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void unalignedCachedRead() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(3);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 3);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[buffer.length];
        cacheStream.setPosition(3);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 3);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void overread() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(90);
        int numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(10, numRead);
        assertSequenced(buffer, 0, 10, 90);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void cachedOverread() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(90);
        int numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(10, numRead);
        assertSequenced(buffer, 0, 10, 90);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[buffer.length];
        cacheStream.setPosition(90);
        numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(10, numRead);
        assertSequenced(buffer, 0, 10, 90);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void cacheBlockRecycle() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[50];
        cacheStream.setPosition(10);
        int numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(50, numRead);
        assertSequenced(buffer, 10);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(0, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(1, cacheStream.getStatistics().getTotalReadsIn());
        buffer = new byte[40];
        cacheStream.setPosition(50);
        numRead = cacheStream.read(buffer, 0, buffer.length);
        assertEquals(40, numRead);
        assertSequenced(buffer, 50);
        assertEquals(0, cacheStream.getStatistics().getLargeReadsIn());
        assertEquals(1, cacheStream.getStatistics().getReadCacheHits());
        assertEquals(2, cacheStream.getStatistics().getTotalReadsIn());
    }

    public void write() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, true);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[20];
        cacheStream.setPosition(10);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 10);
        cacheStream.setPosition(20);
        cacheStream.write(new byte[10], 0, 10);
        assertEquals(30, cacheStream.getPosition());
        cacheStream.setPosition(10);
        buffer = new byte[30];
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0, 10, 10);
        assertSequenced(buffer, 20, 10, 30);
        assertEquals(0, buffer[10]);
        assertEquals(0, buffer[19]);
    }

    public void failWrite() throws Exception {
        MemoryStream ms = createSequencedMemStream(100, false);
        BlockCacheSettings settings = new BlockCacheSettings();
        BlockCacheStream cacheStream = new BlockCacheStream(SparseStream.fromStream(ms, Ownership.Dispose),
                                                            Ownership.Dispose,
                                                            settings);
        byte[] buffer = new byte[25];
        cacheStream.setPosition(0);
        cacheStream.read(buffer, 0, buffer.length);
        assertSequenced(buffer, 0);
        int freeBefore = cacheStream.getStatistics().getFreeReadBlocks();
        cacheStream.setPosition(11);
        try {
            cacheStream.write(new byte[10], 0, 10);
        } catch (UnsupportedOperationException __dummyCatchVar2) {
            assertEquals(freeBefore + 2, cacheStream.getStatistics().getFreeReadBlocks());
        }

    }

    private MemoryStream createSequencedMemStream(int length, boolean writable) throws Exception {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; ++i) {
            buffer[i] = (byte) i;
        }
        return new MemoryStream(buffer, writable);
    }

    private void assertSequenced(byte[] buffer, int seqOffset) throws Exception {
        assertSequenced(buffer, 0, buffer.length, seqOffset);
    }

    private void assertSequenced(byte[] buffer, int offset, int count, int seqOffset) throws Exception {
        for (int i = 0; i < count; ++i) {
            if (buffer[i + offset] != (byte) (i + seqOffset)) {
                assertTrue(false,
                           String.format("Expected {0} at index {1}, was {2}",
                                         (byte) (i + seqOffset),
                                         i + offset,
                                         buffer[i + offset]));
            }
        }
    }
}
