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

package DiscUtils.Vmdk;

import java.io.IOException;

import DiscUtils.Core.DiskImageBuilder;
import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskParameters;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Core.Internal.VirtualDiskFactory;
import DiscUtils.Core.Internal.VirtualDiskFactoryAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskFactoryAttribute(type = "VMDK", fileExtensions = { ".vmdk" })
public final class DiskFactory extends VirtualDiskFactory {
    public String[] getVariants() {
        return new String[] {
            "fixed", "dynamic", "vmfsfixed", "vmfsdynamic"
        };
    }

    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo(variantToCreateType(variant));
    }

    public DiskImageBuilder getImageBuilder(String variant) {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(variantToCreateType(variant));
        return builder;
    }

    public VirtualDisk createDisk(FileLocator locator,
                                  String variant,
                                  String path,
                                  VirtualDiskParameters diskParameters) throws IOException {
        DiskParameters vmdkParams = new DiskParameters(diskParameters);
        vmdkParams.setCreateType(variantToCreateType(variant));
        return Disk.initialize(locator, path, vmdkParams);
    }

    public VirtualDisk openDisk(String path, FileAccess access) throws IOException {
        return new Disk(path, access);
    }

    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) throws IOException {
        return new Disk(locator, path, access);
    }

    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        return new DiskImageFile(locator, path, access);
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo(DiskCreateType createType) {
        return new VirtualDiskTypeInfo();
    }

    private static DiskCreateType variantToCreateType(String variant) {
        if (variant.equals("fixed")) {
            return DiskCreateType.MonolithicFlat;
        } else if (variant.equals("dynamic")) {
            return DiskCreateType.MonolithicSparse;
        } else if (variant.equals("vmfsfixed")) {
            return DiskCreateType.Vmfs;
        } else if (variant.equals("vmfsdynamic")) {
            return DiskCreateType.VmfsSparse;
        } else {
            throw new IllegalArgumentException(String.format("Unknown VMDK disk variant '%s'", variant));
        }
    }

    private static String createTypeToVariant(DiskCreateType createType) {
        switch (createType) {
        case MonolithicFlat:
        case TwoGbMaxExtentFlat:
            return "fixed";
        case MonolithicSparse:
        case TwoGbMaxExtentSparse:
            return "dynamic";
        case Vmfs:
            return "vmfsfixed";
        case VmfsSparse:
            return "vmfsdynamic";
        default:
            return "fixed";

        }
    }

}
