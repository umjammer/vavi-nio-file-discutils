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

import java.io.IOException;
import com.sun.jna.Pointer;

import DiskClone.NativeMethods.EIOControlCode;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;


public class Win32Wrapper {
    public static Pointer openFileHandle(String path) {
        Pointer handle = NativeMethods.INSTANCE
                .createFileW(path, FileAccess.Read, FileShare.Read, null, FileMode.Open, 0, null);
        if (handle.IsInvalid) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return handle;
    }

    public static DiskClone.NativeMethods.DiskGeometry getDiskGeometry(Pointer handle) {
        DiskClone.NativeMethods.DiskGeometry diskGeometry = new DiskClone.NativeMethods.DiskGeometry();
        int bytesRet = Marshal.SizeOf(diskGeometry);
        int[] refVar___0 = new int[] {bytesRet};
        boolean boolVar___0 = !NativeMethods.INSTANCE.deviceIoControl(handle,
                                                             EIOControlCode.DiskGetDriveGeometry,
                                                             null,
                                                             0,
                                                             diskGeometry,
                                                             bytesRet,
                                                             refVar___0,
                                                             null);
        bytesRet = refVar___0[0];
        if (boolVar___0) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return diskGeometry;
    }

    public static DiskClone.NativeMethods.NtfsVolumeData getNtfsVolumeData(Pointer volumeHandle) {
        DiskClone.NativeMethods.NtfsVolumeData volumeData = new DiskClone.NativeMethods.NtfsVolumeData();
        int bytesRet = Marshal.SizeOf(volumeData);
        Integer[] refVar___1 = new Integer[] {bytesRet};
        boolean boolVar___1 = !NativeMethods.DeviceIoControl(volumeHandle,
                                                             EIOControlCode.FsctlGetNtfsVolumeData,
                                                             null,
                                                             0,
                                                             volumeData,
                                                             bytesRet,
                                                             refVar___1,
                                                             null);
        bytesRet = refVar___1[0];
        if (boolVar___1) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return volumeData;
    }

    public static long getDiskCapacity(Pointer diskHandle) {
        byte[] sizeBytes = new byte[8];
        int bytesRet = sizeBytes.length;
        Integer[] refVar___2 = new Integer[] {bytesRet};
        boolean boolVar___2 = !NativeMethods.DeviceIoControl(diskHandle,
                                                             EIOControlCode.DiskGetLengthInfo,
                                                             null,
                                                             0,
                                                             sizeBytes,
                                                             bytesRet,
                                                             refVar___2,
                                                             null);
        bytesRet = refVar___2[0];
        if (boolVar___2) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return BitConverter.ToInt64(sizeBytes, 0);
    }

    public static String getMessageForError(int code) {
        int[] buffer = new int[1];
        try {
            int[][] refVar___3 = new int[][] { buffer } ;
            NativeMethods.INSTANCE.formatMessageW(NativeMethods.FORMAT_MESSAGE_ALLOCATE_BUFFER |
                                         NativeMethods.FORMAT_MESSAGE_FROM_SYSTEM | NativeMethods.FORMAT_MESSAGE_IGNORE_INSERTS,
                                         null,
                                         code,
                                         0,
                                         refVar___3,
                                         0,
                                         null);
            buffer = refVar___3[0];
            return Marshal.PtrToStringUni(buffer);
        } finally {
            if (buffer != null) {
                NativeMethods.INSTANCE.localFree(buffer);
            }
        }
    }

    public static Exception getIOExceptionForLastError() {
        int lastError = Marshal.GetLastWin32Error();
        int lastErrorHr = Marshal.GetHRForLastWin32Error();
        return new IOException(getMessageForError(lastError), lastErrorHr);
    }

    public static <T> T[] byteArrayToStructureArray(Class<T> clazz, byte[] data, int offset, int count) {
        GCHandle handle = GCHandle.Alloc(data, GCHandleType.Pinned);
        try {
            int elemSize = Marshal.SizeOf(clazz);
            if (count * elemSize + offset > data.length) {
                throw new IllegalArgumentException("Attempting to read too many elements from byte array");
            }

            int[] basePtr = handle.AddrOfPinnedObject();
            T[] result = new T[count];
            for (int i = 0; i < count; ++i) {
                int[] elemPtr = new int[] { (basePtr.ToInt64() + (elemSize * i) + offset) };
                result[i] = (T) Marshal.PtrToStructure(elemPtr, clazz);
            }
            return result;
        } finally {
            handle.Free();
        }
    }
}
