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

package discUtils.vmdk;

import java.io.IOException;

import discUtils.core.DiskImageBuilder;
import discUtils.core.FileLocator;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.VirtualDiskFactory;
import discUtils.core.internal.VirtualDiskFactoryAttribute;
import dotnet4j.io.FileAccess;


@VirtualDiskFactoryAttribute(type = "VMDK", fileExtensions = { ".vmdk" })
public final class DiskFactory implements VirtualDiskFactory {

    @Override
    public String[] getVariants() {
        return new String[] {
            "fixed", "dynamic", "vmfsfixed", "vmfsdynamic"
        };
    }

    @Override
    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo(variantToCreateType(variant));
    }

    @Override
    public DiskImageBuilder getImageBuilder(String variant) {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(variantToCreateType(variant));
        return builder;
    }

    @Override
    public VirtualDisk createDisk(FileLocator locator,
                                  String variant,
                                  String path,
                                  VirtualDiskParameters diskParameters) throws IOException {
        DiskParameters vmdkParams = new DiskParameters(diskParameters);
        vmdkParams.setCreateType(variantToCreateType(variant));
        return Disk.initialize(locator, path, vmdkParams);
    }

    @Override
    public VirtualDisk openDisk(String path, FileAccess access) throws IOException {
        return new Disk(path, access);
    }

    @Override
    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) throws IOException {
        return new Disk(locator, path, access);
    }

    @Override
    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        return new DiskImageFile(locator, path, access);
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo(DiskCreateType createType) {
        return new VirtualDiskTypeInfo();
    }

    private static DiskCreateType variantToCreateType(String variant) {
        return switch (variant) {
            case "fixed" -> DiskCreateType.MonolithicFlat;
            case "dynamic" -> DiskCreateType.MonolithicSparse;
            case "vmfsfixed" -> DiskCreateType.Vmfs;
            case "vmfsdynamic" -> DiskCreateType.VmfsSparse;
            default -> throw new IllegalArgumentException(String.format("Unknown VMDK disk variant '%s'", variant));
        };
    }

    private static String createTypeToVariant(DiskCreateType createType) {
        return switch (createType) {
            case MonolithicFlat, TwoGbMaxExtentFlat -> "fixed";
            case MonolithicSparse, TwoGbMaxExtentSparse -> "dynamic";
            case Vmfs -> "vmfsfixed";
            case VmfsSparse -> "vmfsdynamic";
            default -> "fixed";
        };
    }

}
