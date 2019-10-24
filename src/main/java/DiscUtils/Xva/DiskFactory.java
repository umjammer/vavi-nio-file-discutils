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

package DiscUtils.Xva;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import DiscUtils.Core.DiskImageBuilder;
import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskParameters;
import DiscUtils.Core.VirtualDiskTypeInfo;
import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Core.Internal.VirtualDiskFactory;
import DiscUtils.Core.Internal.VirtualDiskFactoryAttribute;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;


@VirtualDiskFactoryAttribute(type = "XVA", fileExtensions = { ".xva" })
public final class DiskFactory extends VirtualDiskFactory {
    public String[] getVariants() {
        return new String[] {
            "dynamic"
        };
    }

    public VirtualDiskTypeInfo getDiskTypeInformation(String variant) {
        return makeDiskTypeInfo();
    }

    public DiskImageBuilder getImageBuilder(String variant) {
        throw new UnsupportedOperationException();
    }

    public VirtualDisk createDisk(FileLocator locator, String variant, String path, VirtualDiskParameters diskParameters) {
        throw new UnsupportedOperationException();
    }

    public VirtualDisk openDisk(String path, FileAccess access) {
        Path parent = Paths.get(path).getParent();
        return openDisk(new LocalFileLocator(parent == null ? "" : parent.toString()),
                        Paths.get(path).getFileName().toString(),
                        access);
    }

    public VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) {
        return openDisk(locator, path, "", new HashMap<String, String>(), access);
    }

    public VirtualDisk openDisk(FileLocator locator,
                                String path,
                                String extraInfo,
                                Map<String, String> parameters,
                                FileAccess access) {
        VirtualMachine machine = new VirtualMachine(locator.open(path, FileMode.Open, FileAccess.Read, FileShare.Read),
                                                    Ownership.Dispose);
        int diskIndex;
        try {
            diskIndex = Integer.parseInt(extraInfo);
        } catch (NumberFormatException e) {
            diskIndex = 0;
        }

        int i = 0;
        for (Disk disk : machine.getDisks()) {
            if (i == diskIndex) {
                return disk;
            }

            ++i;
        }
        return null;
    }

    public VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) {
        return null;
    }

    public static VirtualDiskTypeInfo makeDiskTypeInfo() {
        return new VirtualDiskTypeInfo();
    }
}
