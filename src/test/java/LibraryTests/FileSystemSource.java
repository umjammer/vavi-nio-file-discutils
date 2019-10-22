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

package LibraryTests;

import java.util.Arrays;
import java.util.List;

import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FloppyDiskType;
import DiscUtils.Core.Geometry;
import DiscUtils.Diagnostics.ValidatingFileSystem;
import DiscUtils.Ntfs.NtfsFileSystem;
import DiscUtils.Ntfs.NtfsFileSystemChecker;
import DiscUtils.Streams.SparseMemoryBuffer;
import DiscUtils.Streams.SparseMemoryStream;


public class FileSystemSource {
    public static List<NewFileSystemDelegate[]> getReadWriteFileSystems() {
        return Arrays.asList(
            new NewFileSystemDelegate[] {
                FileSystemSource::fatFileSystem
            },
            // TODO: When format code complete, format a vanilla partition rather than relying on file on disk
            new NewFileSystemDelegate[] {
                FileSystemSource::diagnosticNtfsFileSystem
            }
        );
    }

    public static List<NewFileSystemDelegate[]> getQuickReadWriteFileSystems() {
        return Arrays.asList(
            new NewFileSystemDelegate[] {
                FileSystemSource::fatFileSystem
            },
            new NewFileSystemDelegate[] {
                FileSystemSource::ntfsFileSystem
            }
        );
    }

    private static DiscFileSystem fatFileSystem() {
        @SuppressWarnings("unused")
        SparseMemoryBuffer buffer = new SparseMemoryBuffer(4096);
        SparseMemoryStream ms = new SparseMemoryStream();
        @SuppressWarnings("unused")
        Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
        return DiscUtils.Fat.FatFileSystem.formatFloppy(ms, FloppyDiskType.Extended, null);
    }

    public static DiscFileSystem diagnosticNtfsFileSystem() {
        @SuppressWarnings("unused")
        SparseMemoryBuffer buffer = new SparseMemoryBuffer(4096);
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
        DiscUtils.Ntfs.NtfsFileSystem.format(ms, "", diskGeometry, 0, diskGeometry.getTotalSectorsLong());
        ValidatingFileSystem<?, ?> discFs = new ValidatingFileSystem<>(NtfsFileSystem.class, NtfsFileSystemChecker.class, ms);
        discFs.setCheckpointInterval(1);
        discFs.setGlobalIOTraceCapturesStackTraces(false);
        return discFs;
    }

    public static DiscUtils.Ntfs.NtfsFileSystem ntfsFileSystem() {
        @SuppressWarnings("unused")
        SparseMemoryBuffer buffer = new SparseMemoryBuffer(4096);
        SparseMemoryStream ms = new SparseMemoryStream();
        Geometry diskGeometry = Geometry.fromCapacity(30 * 1024 * 1024);
        return DiscUtils.Ntfs.NtfsFileSystem.format(ms, "", diskGeometry, 0, diskGeometry.getTotalSectorsLong());
    }
}
