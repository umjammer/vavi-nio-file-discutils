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

package DiskClone;

import com.sun.jna.Pointer;

import java.io.Closeable;
import java.io.IOException;

import DiscUtils.Core.Internal.Utilities;
import DiskClone.NativeMethods.EIOControlCode;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


public final class Volume implements Closeable {
    private String _path;

    private Pointer _handle;

    private Stream _stream;

    private long _length;

    public Volume(String path, long length) {
        _path = path.replaceFirst(Utilities.escapeForRegex("\\*$"), "");
        _length = length;
        if (!_path.startsWith("\\\\")) {
            _path = "\\\\.\\" + _path;
        }

        _handle = NativeMethods.INSTANCE.createFileW(_path, FileAccess.Read, FileShare.ReadWrite, null, FileMode.Open, 0, null);
        if (_handle.IsInvalid) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        // Enable reading the full contents of the volume (not just the region
        // bounded by the file system)
        int bytesRet = 0;
        int[] refVar___0 = new int[] {
            bytesRet
        };
        boolean boolVar___0 = !NativeMethods.INSTANCE
                .deviceIoControl(_handle, EIOControlCode.FsctlAllowExtendedDasdIo, null, 0, null, 0, refVar___0, null);
        bytesRet = refVar___0[0];
        if (boolVar___0) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }
    }

    public void close() throws IOException {
        if (_stream != null) {
            _stream.close();
            _stream = null;
        }

        if (!_handle.IsClosed) {
            _handle.close();
        }
    }

    public Stream getContent() {
        if (_stream == null) {
            _stream = new VolumeStream(_handle);
        }

        return _stream;
    }

    public DiskClone.NativeMethods.DiskExtent[] getDiskExtents() {
        int numExtents = 1;
        int bufferSize = 8 + Marshal.SizeOf(DiskClone.NativeMethods.DiskExtent.class) * numExtents;
        byte[] buffer = new byte[bufferSize];
        int bytesRet = 0;
        int[] refVar___1 = new int[] {
            bytesRet
        };
        boolean boolVar___1 = !NativeMethods.INSTANCE
                .deviceIoControl(_handle, EIOControlCode.VolumeGetDiskExtents, null, 0, buffer, bufferSize, refVar___1, null);
        bytesRet = refVar___1[0];
        if (boolVar___1) {
            if (Marshal.GetLastWin32Error() != NativeMethods.ERROR_MORE_DATA) {
                throw Win32Wrapper.getIOExceptionForLastError();
            }

            numExtents = Marshal.readInt32(buffer, 0);
            bufferSize = 8 + Marshal.SizeOf(DiskClone.NativeMethods.DiskExtent.class) * numExtents;
            buffer = new byte[bufferSize];
            int[] refVar___2 = new int[] {
                bytesRet
            };
            boolean boolVar___2 = !NativeMethods.INSTANCE.deviceIoControl(_handle,
                                                                          EIOControlCode.VolumeGetDiskExtents,
                                                                          null,
                                                                          0,
                                                                          buffer,
                                                                          bufferSize,
                                                                          refVar___2,
                                                                          null);
            bytesRet = refVar___2[0];
            if (boolVar___2) {
                throw Win32Wrapper.getIOExceptionForLastError();
            }

        }

        return Win32Wrapper.<DiskClone.NativeMethods.DiskExtent> byteArrayToStructureArray(buffer, 8, 1);
    }
}
