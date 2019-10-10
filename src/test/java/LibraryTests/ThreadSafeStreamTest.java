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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import DiscUtils.Streams.SparseMemoryBuffer;
import DiscUtils.Streams.SparseMemoryStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.ThreadSafeStream;
import DiscUtils.Streams.Util.Ownership;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.SeekOrigin;
import moe.yo3explorer.dotnetio4j.Stream;

public class ThreadSafeStreamTest {
    @Test
    public void openView() throws Exception {
        ThreadSafeStream tss = new ThreadSafeStream(SparseStream.fromStream(Stream.Null, Ownership.None));
        SparseStream view = tss.openView();
    }

    @Test
    public void viewIO() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream();
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        SparseStream altView = tss.openView();
        // Check positions are independant
        tss.setPosition(100);
        assertEquals(0, altView.getPosition());
        assertEquals(100, tss.getPosition());
        // Check I/O is synchronous
        byte[] buffer = new byte[200];
        tss.writeByte((byte) 99);
        altView.read(buffer, 0, 200);
        assertEquals(99, buffer[100]);
        // Check positions are updated correctly
        assertEquals(200, altView.getPosition());
        assertEquals(101, tss.getPosition());
    }

    @Test
    public void changeLengthFails() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream();
        memStream.setLength(2);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        assertEquals(2, tss.getLength());
        try {
            tss.setLength(10);
            assertTrue(false, "SetLength should fail");
        } catch (UnsupportedOperationException __dummyCatchVar0) {
        }
    }

    @Test
    public void extents() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        SparseStream altView = tss.openView();
        tss.setPosition(100);
        tss.writeByte((byte) 99);
        List<StreamExtent> extents = new ArrayList<>(altView.getExtents());
        assertEquals(1, extents.size());
        assertEquals(100, extents.get(0).getStart());
        assertEquals(1, extents.get(0).getLength());
        extents = new ArrayList<>(altView.getExtentsInRange(10, 300));
        assertEquals(1, extents.size());
        assertEquals(100, extents.get(0).getStart());
        assertEquals(1, extents.get(0).getLength());
    }

    @Test
    public void disposeView() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        SparseStream altView = tss.openView();
        altView.close();
        tss.readByte();
        SparseStream altView2 = tss.openView();
        altView2.readByte();
    }

    @Test
    public void dispose_StopsView() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        SparseStream altView = tss.openView();
        tss.close();
        try {
            altView.readByte();
            assertTrue(false, "Disposed stream didn't stop view");
        } catch (UnsupportedOperationException __dummyCatchVar1) {
        }
    }

    @Test
    public void seek() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        tss.seek(10, SeekOrigin.Begin);
        assertEquals(10, tss.getPosition());
        tss.seek(10, SeekOrigin.Current);
        assertEquals(20, tss.getPosition());
        tss.seek(-10, SeekOrigin.End);
        assertEquals(1014, tss.getPosition());
    }

    @Test
    public void canWrite() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        assertEquals(true, tss.canWrite());
        memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.Read);
        tss = new ThreadSafeStream(memStream);
        assertEquals(false, tss.canWrite());
    }

    @Test
    public void canRead() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        assertEquals(true, tss.canRead());
        memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.Write);
        tss = new ThreadSafeStream(memStream);
        assertEquals(false, tss.canRead());
    }
}
