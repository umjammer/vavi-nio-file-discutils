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

import java.util.ArrayList;
import java.util.List;

import discUtils.streams.SparseMemoryBuffer;
import discUtils.streams.SparseMemoryStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.ThreadSafeStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.IOException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        tss.position(100);
        assertEquals(0, altView.position());
        assertEquals(100, tss.position());
        // Check I/O is synchronous
        byte[] buffer = new byte[200];
        tss.writeByte((byte) 99);
        altView.read(buffer, 0, 200);
        assertEquals(99, buffer[100]);
        // Check positions are updated correctly
        assertEquals(200, altView.position());
        assertEquals(101, tss.position());
    }

    @Test
    public void changeLengthFails() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream();
        memStream.setLength(2);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        assertEquals(2, tss.getLength());
        try {
            tss.setLength(10);
            fail("SetLength should fail");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void extents() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        SparseStream altView = tss.openView();
        tss.position(100);
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
            fail("Disposed stream didn't stop view");
        } catch (IOException ignored) {
        }
    }

    @Test
    public void seek() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        memStream.setLength(1024);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        tss.seek(10, SeekOrigin.Begin);
        assertEquals(10, tss.position());
        tss.seek(10, SeekOrigin.Current);
        assertEquals(20, tss.position());
        tss.seek(-10, SeekOrigin.End);
        assertEquals(1014, tss.position());
    }

    @Test
    public void canWrite() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        assertTrue(tss.canWrite());
        memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.Read);
        tss = new ThreadSafeStream(memStream);
        assertFalse(tss.canWrite());
    }

    @Test
    public void canRead() throws Exception {
        SparseMemoryStream memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.ReadWrite);
        ThreadSafeStream tss = new ThreadSafeStream(memStream);
        assertTrue(tss.canRead());
        memStream = new SparseMemoryStream(new SparseMemoryBuffer(1), FileAccess.Write);
        tss = new ThreadSafeStream(memStream);
        assertFalse(tss.canRead());
    }
}
