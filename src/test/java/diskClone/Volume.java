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

import java.io.Closeable;
import java.io.IOException;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import diskClone.NativeMethods.EIOControlCode;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StringUtilities;


public final class Volume implements Closeable {

    private String path;

    private final Pointer handle;

    private Stream stream;

    private final long length;

    public Volume(String path, long length) {
        this.path = path.replaceFirst(StringUtilities.escapeForRegex("\\*$"), "");
        this.length = length;
        if (!this.path.startsWith("\\\\")) {
            this.path = "\\\\.\\" + this.path;
        }

        handle = NativeMethods.INSTANCE.createFileW(this.path, FileAccess.Read, FileShare.ReadWrite, null, FileMode.Open, 0, null);
        if (handle.IsInvalid) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        // Enable reading the full contents of the volume (not just the region
        // bounded by the file system)
        int[] bytesRet = new int[1];
        bytesRet[0] = NativeMethods.INSTANCE
                .deviceIoControl(handle, EIOControlCode.FsctlAllowExtendedDasdIo, null, 0, null, 0, bytesRet, null);
        if (bytesRet[0] == 0) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }

        if (!handle.IsClosed) {
            handle.close();
        }
    }

    public Stream getContent() {
        if (stream == null) {
            stream = new VolumeStream(handle);
        }

        return stream;
    }

    public diskClone.NativeMethods.DiskExtent[] getDiskExtents() {
        int numExtents = 1;
        int bufferSize = 8 + Structure.sizeOf(diskClone.NativeMethods.DiskExtent.class) * numExtents;
        byte[] buffer = new byte[bufferSize];
        int[] bytesRet = new int[] {NativeMethods.INSTANCE
                .deviceIoControl(handle, EIOControlCode.VolumeGetDiskExtents, null, 0, buffer, bufferSize, bytesRet, null)};
        if (bytesRet[0] == 0) {
            if (Marshal.GetLastWin32Error() != NativeMethods.ERROR_MORE_DATA) {
                throw Win32Wrapper.getIOExceptionForLastError();
            }

            numExtents = Marshal.readInt32(buffer, 0);
            bufferSize = 8 + Marshal.SizeOf(diskClone.NativeMethods.DiskExtent.class) * numExtents;
            buffer = new byte[bufferSize];
            bytesRet[0] = NativeMethods.INSTANCE.deviceIoControl(handle,
                    EIOControlCode.VolumeGetDiskExtents,
                    null,
                    0,
                    buffer,
                    bufferSize,
                    bytesRet,
                    null);
            if (bytesRet[0] != 0) {
                throw Win32Wrapper.getIOExceptionForLastError();
            }
        }

        return Win32Wrapper.byteArrayToStructureArray(
                diskClone.NativeMethods.DiskExtent.class, buffer, 8, 1);
    }
}
