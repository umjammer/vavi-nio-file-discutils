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

package DiscUtils.Core.Internal;

import java.io.IOException;
import java.util.Map;

import DiscUtils.Core.DiscFileLocator;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.DiskImageBuilder;
import DiscUtils.Core.FileLocator;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.VirtualDiskParameters;
import DiscUtils.Core.VirtualDiskTypeInfo;
import moe.yo3explorer.dotnetio4j.FileAccess;


public abstract class VirtualDiskFactory {

    public abstract String[] getVariants();

    public abstract VirtualDiskTypeInfo getDiskTypeInformation(String variant);

    public abstract DiskImageBuilder getImageBuilder(String variant);

    public abstract VirtualDisk createDisk(FileLocator locator,
                                           String variant,
                                           String path,
                                           VirtualDiskParameters diskParameters) throws IOException;

    public abstract VirtualDisk openDisk(String path, FileAccess access) throws IOException;

    public abstract VirtualDisk openDisk(FileLocator locator, String path, FileAccess access) throws IOException;

    public VirtualDisk openDisk(FileLocator locator,
                                String path,
                                String extraInfo,
                                Map<String, String> parameters,
                                FileAccess access) throws IOException {
        return openDisk(locator, path, access);
    }

    public VirtualDisk openDisk(DiscFileSystem fileSystem, String path, FileAccess access) throws IOException {
        return openDisk(new DiscFileLocator(fileSystem, "\\"), path, access);
    }

    public abstract VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) throws IOException;
}
