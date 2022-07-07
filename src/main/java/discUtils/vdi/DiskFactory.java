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

package discUtils.vdi;

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


@VirtualDiskFactoryAttribute(type = "VDI", fileExtensions = { ".vdi" })
public final class DiskFactory extends VirtualDiskFactory {
    public String[] getVariants() {
        return new String[] { "fixed", "dynamic" };
    }

    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo(variant);
    }

    public DiskImageBuilder getImageBuilder(String variant) {
        throw new UnsupportedOperationException();
    }

    public VirtualDisk createDisk(FileLocator locator,
                                  String variant,
                                  String path,
                                  VirtualDiskParameters diskParameters) {
        if (variant.equals("fixed")) {
            return Disk.initializeFixed(locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None), Ownership.Dispose, diskParameters.capacity);
        } else if (variant.equals("dynamic")) {
            return Disk.initializeDynamic(locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None), Ownership.Dispose, diskParameters.capacity);
        } else {
            throw new IllegalArgumentException(String.format("Unknown VDI disk variant '%s'", variant));
        }
    }

    public VirtualDisk openDisk(String path, FileAccess access) {
        return new Disk(path, access);
    }

    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        return new Disk(locator.open(path, FileMode.Open, access, share), Ownership.Dispose);
    }

    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        FileMode mode = access == FileAccess.Read ? FileMode.Open : FileMode.OpenOrCreate;
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        return new DiskImageFile(locator.open(path, mode, access, share), Ownership.Dispose);
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo(String variant) {
        return new VirtualDiskTypeInfo();
    }
}
