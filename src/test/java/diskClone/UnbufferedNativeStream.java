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

package diskClone;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import dotnet4j.io.SeekOrigin;
import com.sun.jna.Pointer;


/**
 * A stream implementation that honours the alignment rules for unbuffered
 * streams.
 *
 * To support the stream interface, which permits unaligned access, all accesses
 * are routed through an appropriately aligned buffer.
 */
public class UnbufferedNativeStream extends SparseStream {
    private static final int BufferSize = 64 * 1024;

    private static final int Alignment = 512;

    private long position;

    private Pointer handle;

    private int bufferAllocHandle;

    private int[] buffer;

    public UnbufferedNativeStream(Pointer handle) {
        this.handle = handle;
        bufferAllocHandle = Marshal.AllocHGlobal(BufferSize + Alignment);
        buffer = new IntPtr(((bufferAllocHandle.ToInt64() + Alignment - 1) / Alignment) * Alignment);
        position = 0;
    }

    public void close() throws IOException {
        if (bufferAllocHandle != IntPtr.Zero) {
            Marshal.FreeHGlobal(bufferAllocHandle);
            bufferAllocHandle = IntPtr.Zero;
            bufferAllocHandle = IntPtr.Zero;
        }

        if (!handle.IsClosed) {
            handle.close();
        }
    }

    public boolean canRead() {
        return true;
    }

    public boolean canSeek() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    public void flush() {
    }

    public long getLength() {
        long result;
        long[] refVar___0 = new long[1];
        boolean boolVar___0 = NativeMethods.INSTANCE.getFileSizeEx(handle, refVar___0);
        result = refVar___0[0];
        if (boolVar___0) {
            return result;
        } else {
            throw Win32Wrapper.getIOExceptionForLastError();
        }
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long value) {
        position = value;
    }

    public int read(byte[] buffer, int offset, int count) {
        int totalBytesRead = 0;
        long length = getLength();
        while (totalBytesRead < count) {
            long alignedStart = (position / Alignment) * Alignment;
            int alignmentOffset = (int) (position - alignedStart);
            long[] newPos = new long[1];
            if (!NativeMethods.INSTANCE.setFilePointerEx(handle, alignedStart, newPos, 0)) {
                throw Win32Wrapper.getIOExceptionForLastError();
            }

            int toRead = (int) Math.min(length - alignedStart, BufferSize);
            int[] numRead = new int[1];
            if (!NativeMethods.INSTANCE.readFile(handle, this.buffer, toRead, numRead, null)) {
                throw Win32Wrapper.getIOExceptionForLastError();
            }

            int usefulData = numRead[0] - alignmentOffset;
            if (usefulData <= 0) {
                return totalBytesRead;
            }

            int toCopy = Math.min(count - totalBytesRead, usefulData);
            System.arraycopy(this.buffer, alignmentOffset, buffer, offset + totalBytesRead, toCopy);
            totalBytesRead += toCopy;
            position += toCopy;
        }
        return totalBytesRead;
    }

    public long seek(long offset, SeekOrigin origin) {
        long effectiveOffset = offset;
        if (origin == SeekOrigin.Current) {
            effectiveOffset += position;
        } else if (origin == SeekOrigin.End) {
            effectiveOffset += getLength();
        }

        if (effectiveOffset < 0) {
            throw new dotnet4j.io.IOException("Attempt to move before beginning of disk");
        } else {
            position = effectiveOffset;
            return position;
        }
    }

    public void setLength(long value) {
        throw new UnsupportedOperationException();
    }

    public void write(byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException();
    }

    public List<StreamExtent> getExtents() {
        return Collections.singletonList(new StreamExtent(0, getLength()));
    }
}
