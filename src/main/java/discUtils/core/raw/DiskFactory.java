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

package discUtils.core.raw;

import java.io.IOException;

import discUtils.core.DiskImageBuilder;
import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.VirtualDiskFactory;
import discUtils.core.internal.VirtualDiskFactoryAttribute;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;


@VirtualDiskFactoryAttribute(type = "RAW", fileExtensions = { ".img", ".ima", ".vfd", ".flp", ".bif" })
public final class DiskFactory implements VirtualDiskFactory {
    @Override
    public String[] getVariants() {
        return new String[] {};
    }

    @Override
    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo();
    }

    @Override
    public DiskImageBuilder getImageBuilder(String variant) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualDisk createDisk(FileLocator locator,
                                  String variant,
                                  String path,
                                  VirtualDiskParameters diskParameters) {
        return Disk.initialize(locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None),
                               Ownership.Dispose,
                               diskParameters.capacity,
                               diskParameters.geometry);
    }

    @Override
    public VirtualDisk openDisk(String path, FileAccess access) throws IOException {
        return new Disk(path, access);
    }

    @Override
    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        return new Disk(locator.open(path, FileMode.Open, access, share), Ownership.Dispose);
    }

    @Override
    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        return null;
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo() {
        return new VirtualDiskTypeInfo();
    }
}
