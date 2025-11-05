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

import com.sun.jna.Pointer;
import diskClone.NativeMethods.EIOControlCode;
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

    public static diskClone.NativeMethods.DiskGeometry getDiskGeometry(Pointer handle) {
        diskClone.NativeMethods.DiskGeometry diskGeometry = new diskClone.NativeMethods.DiskGeometry();
        int[] bytesRet = new int[] {Marshal.SizeOf(diskGeometry)};
        bytesRet[0] = NativeMethods.INSTANCE.deviceIoControl(handle,
                                                             EIOControlCode.DiskGetDriveGeometry,
                                                             null,
                                                             0,
                                                             diskGeometry,
                                                             bytesRet[0],
                                                              bytesRet,
                                                             null);
        if (bytesRet[0] == 0) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return diskGeometry;
    }

    public static diskClone.NativeMethods.NtfsVolumeData getNtfsVolumeData(Pointer volumeHandle) {
        diskClone.NativeMethods.NtfsVolumeData volumeData = new diskClone.NativeMethods.NtfsVolumeData();
        int[] bytesRet = new int[] {Marshal.SizeOf(volumeData)};
        bytesRet[0] = NativeMethods.DeviceIoControl(volumeHandle,
                                                             EIOControlCode.FsctlGetNtfsVolumeData,
                                                             null,
                                                             0,
                                                             volumeData,
                                                             bytesRet,
                                                             bytesRet,
                                                             null);
        if (bytesRet[0] == 0) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return volumeData;
    }

    public static long getDiskCapacity(Pointer diskHandle) {
        int[] sizeBytes = new int[] {bytesRet};
        int bytesRet = sizeBytes.length;
        sizeBytes[0] = NativeMethods.DeviceIoControl(diskHandle,
                                                             EIOControlCode.DiskGetLengthInfo,
                                                             null,
                                                             0,
                                                             sizeBytes,
                                                             bytesRet,
                                                             sizeBytes,
                                                             null);
        if (sizeBytes[0] != 0) {
            throw Win32Wrapper.getIOExceptionForLastError();
        }

        return BitConverter.ToInt64(sizeBytes, 0);
    }

    public static String getMessageForError(int code) {
        int[][] buffer = new int[1][];
        try {
            NativeMethods.INSTANCE.formatMessageW(NativeMethods.FORMAT_MESSAGE_ALLOCATE_BUFFER |
                                         NativeMethods.FORMAT_MESSAGE_FROM_SYSTEM | NativeMethods.FORMAT_MESSAGE_IGNORE_INSERTS,
                                         null,
                                         code,
                                         0,
                                          buffer,
                                         0,
                                         null);
            return Marshal.PtrToStringUni(buffer[0]);
        } finally {
            if (buffer[0] != null) {
                NativeMethods.INSTANCE.localFree(buffer);
            }
        }
    }

    public static RuntimeException getIOExceptionForLastError() {
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
