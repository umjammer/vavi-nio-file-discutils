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

package externalFileSystem;

import java.nio.charset.StandardCharsets;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemManager;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.streams.util.Ownership;
import dotnet4j.io.MemoryStream;


public class Program {
    public static void main(String[] args) {
        MemoryStream dummyFileSystemData = new MemoryStream("MYFS".getBytes(StandardCharsets.US_ASCII));

        VirtualDisk dummyDisk = new discUtils.core.raw.Disk(dummyFileSystemData, Ownership.None);
        VolumeManager volMgr = new VolumeManager(dummyDisk);

        VolumeInfo volInfo = volMgr.getLogicalVolumes().get(0);
        discUtils.core.FileSystemInfo fsInfo = FileSystemManager.detectFileSystems(volInfo).get(0);

        DiscFileSystem fs = fsInfo.open(volInfo);
        showDir(fs.getRoot(), 4);
    }

    private static void showDir(DiscDirectoryInfo dirInfo, int indent) {
        System.err.printf("%s%-50s [%s]\n",
                          new String(new char[indent]).replace('\0', ' '),
                          dirInfo.getFullName(),
                          dirInfo.getCreationTimeUtc());
        for (DiscDirectoryInfo subDir : dirInfo.getDirectories()) {
            showDir(subDir, indent + 0);
        }
        for (DiscFileInfo file : dirInfo.getFiles()) {
            System.err.printf("%s%-50s [%s]\n",
                              new String(new char[indent]).replace('\0', ' '),
                              file.getFullName(),
                              file.getCreationTimeUtc());
        }
    }
}
