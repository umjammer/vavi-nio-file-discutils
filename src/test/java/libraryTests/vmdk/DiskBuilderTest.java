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

package libraryTests.vmdk;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.core.DiskImageFileSpecification;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import discUtils.vmdk.Disk;
import discUtils.vmdk.DiskBuilder;
import discUtils.vmdk.DiskCreateType;
import libraryTests.DiskBuilderFileSystem;
import dotnet4j.io.FileAccess;
import dotnet4j.io.MemoryStream;


public class DiskBuilderTest {
    private SparseStream diskContent;

    public DiskBuilderTest() throws Exception {
        MemoryStream fileStream = new MemoryStream();
        discUtils.vhd.Disk baseFile = discUtils.vhd.Disk.initializeDynamic(fileStream, Ownership.Dispose, 16 * 1024L * 1024);
        for (int i = 0; i < 8; i += 1024 * 1024) {
            baseFile.getContent().position(i);
            baseFile.getContent().writeByte((byte) i);
        }
        baseFile.getContent().position(15 * 1024 * 1024);
        baseFile.getContent().writeByte((byte) 0xFF);
        diskContent = baseFile.getContent();
    }

    @Test
    public void buildFixed() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(DiskCreateType.Vmfs);
        builder.setContent(diskContent);
        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(2, fileSpecs.size());
        assertEquals("foo.vmdk", fileSpecs.get(0).getName());
        DiskBuilderFileSystem dbfs = new DiskBuilderFileSystem(fileSpecs);
        try (Disk disk = new Disk(dbfs, "foo.vmdk", FileAccess.Read)) {
            for (int i = 0; i < 8; i += 1024 * 1024) {
                disk.getContent().position(i);
                assertEquals(i, disk.getContent().readByte());
            }
            disk.getContent().position(15 * 1024 * 1024);
            assertEquals(0xFF, disk.getContent().readByte() & 0xff);
        }
    }

    @Test
    public void buildDynamic() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(DiskCreateType.VmfsSparse);
        builder.setContent(diskContent);
        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(2, fileSpecs.size());
        assertEquals("foo.vmdk", fileSpecs.get(0).getName());
        DiskBuilderFileSystem dbfs = new DiskBuilderFileSystem(fileSpecs);
        try (Disk disk = new Disk(dbfs, "foo.vmdk", FileAccess.Read)) {
            for (int i = 0; i < 8; i += 1024 * 1024) {
                disk.getContent().position(i);
                assertEquals(i, disk.getContent().readByte());
            }
            disk.getContent().position(15 * 1024 * 1024);
            assertEquals(0xFF, disk.getContent().readByte() & 0xff);
        }
    }
}
