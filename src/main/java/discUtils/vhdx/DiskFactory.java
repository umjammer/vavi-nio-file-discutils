//
// Copyright (c) 2008-2012, Kenneth Bell
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

package discUtils.vhdx;

import discUtils.core.DiskImageBuilder;
import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.VirtualDiskFactory;
import discUtils.core.internal.VirtualDiskFactoryAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskFactoryAttribute(type = "VHDX", fileExtensions = { ".vhdx", ".avhdx" })
public final class DiskFactory implements VirtualDiskFactory {
    @Override
    public String[] getVariants() {
        return new String[] { "fixed", "dynamic" };
    }

    @Override
    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo(variant);
    }

    @Override
    public DiskImageBuilder getImageBuilder(String variant) {
        DiskBuilder builder = new DiskBuilder();
        switch (variant) {
        case "fixed":
            builder.setDiskType(DiskType.Fixed);
            break;
        case "dynamic":
            builder.setDiskType(DiskType.Dynamic);
            break;
        default:
            throw new IllegalArgumentException(String.format("Unknown VHD disk variant '%s'", variant));
        }
        return builder;
    }

    @Override
    public VirtualDisk createDisk(FileLocator locator,
                                  String variant,
                                  String path,
                                  VirtualDiskParameters diskParameters) {
        switch (variant) {
        case "fixed":
            return Disk.initializeFixed(locator, path, diskParameters.getCapacity(), diskParameters.getGeometry());
        case "dynamic":
            return Disk.initializeDynamic(locator, path, diskParameters.getCapacity(), FileParameters.DefaultDynamicBlockSize);
        default:
            throw new IllegalArgumentException(String.format("Unknown VHD disk variant '%s'", variant));
        }
    }

    @Override
    public VirtualDisk openDisk(String path, FileAccess access) {
        return new Disk(path, access);
    }

    @Override
    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) {
        return new Disk(locator, path, access);
    }

    @Override
    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        return new DiskImageFile(locator, path, access);
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo(String variant) {
        return new VirtualDiskTypeInfo();
    }
}
