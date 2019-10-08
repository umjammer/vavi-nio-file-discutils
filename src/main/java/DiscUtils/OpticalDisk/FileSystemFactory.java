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

package DiscUtils.OpticalDisk;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemParameters;
import DiscUtils.Core.VolumeInfo;
import DiscUtils.Core.Vfs.VfsFileSystemFactory;
import DiscUtils.Core.Vfs.VfsFileSystemInfo;
import DiscUtils.Iso9660.CDReader;
import DiscUtils.Udf.UdfReader;
import moe.yo3explorer.dotnetio4j.Stream;


public class FileSystemFactory extends VfsFileSystemFactory {
    public FileSystemInfo[] detect(Stream stream, VolumeInfo volume) {
        List<FileSystemInfo> detected = new ArrayList<>();
        if (UdfReader.detect(stream)) {
            detected.add(new VfsFileSystemInfo("UDF", "OSTA Universal Disk Format (UDF)", this::openUdf));
        }

        if (CDReader.detect(stream)) {
            detected.add(new VfsFileSystemInfo("ISO9660", "ISO 9660 (CD-ROM)", this::openIso));
        }

        return detected.toArray(new FileSystemInfo[0]);
    }

    private DiscFileSystem openUdf(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters) {
        if (volumeInfo != null) {
            return new UdfReader(stream, volumeInfo.getPhysicalGeometry().getBytesPerSector());
        }

        return new UdfReader(stream);
    }

    private DiscFileSystem openIso(Stream stream, VolumeInfo volumeInfo, FileSystemParameters parameters) {
        return new CDReader(stream, true, true);
    }
}
