//
// Copyright (c) 2008-2013, Kenneth Bell
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

package LibraryTests.Vhdx;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import DiscUtils.Core.DiskImageFileSpecification;
import DiscUtils.Streams.SparseMemoryStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Vhdx.Disk;
import DiscUtils.Vhdx.DiskBuilder;
import DiscUtils.Vhdx.DiskType;


public class DiskBuilderTest {
    private SparseStream diskContent;

    public DiskBuilderTest() throws Exception {
        SparseMemoryStream sourceStream = new SparseMemoryStream();
        sourceStream.setLength(160 * 1024L * 1024);
        for (int i = 0; i < 8; ++i) {
            sourceStream.setPosition(i * 1024L * 1024);
            sourceStream.writeByte((byte) i);
        }
        sourceStream.setPosition(150 * 1024 * 1024);
        sourceStream.writeByte((byte) 0xFF);
        diskContent = sourceStream;
    }

    public void buildFixed() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(DiskType.Fixed);
        builder.setContent(diskContent);
        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(1, fileSpecs.size());
        assertEquals("foo.vhdx", fileSpecs.get(0).getName());
        Disk disk = new Disk(fileSpecs.get(0).openStream(), Ownership.Dispose);
        try {
            {
                for (int i = 0; i < 8; ++i) {
                    disk.getContent().setPosition(i * 1024L * 1024);
                    assertEquals(i, disk.getContent().readByte());
                }
                disk.getContent().setPosition(150 * 1024 * 1024);
                assertEquals(0xFF, disk.getContent().readByte());
            }
        } finally {
            if (disk != null)
                disk.close();

        }
    }

    public void buildEmptyDynamic() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(DiskType.Dynamic);
        builder.setContent(new SparseMemoryStream());
        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(1, fileSpecs.size());
        assertEquals("foo.vhdx", fileSpecs.get(0).getName());
        Disk disk = new Disk(fileSpecs.get(0).openStream(), Ownership.Dispose);
        try {
            {
                assertEquals(0, disk.getContent().getLength());
            }
        } finally {
            if (disk != null)
                disk.close();

        }
    }

    public void buildDynamic() throws Exception {
        DiskBuilder builder = new DiskBuilder();
        builder.setDiskType(DiskType.Dynamic);
        builder.setContent(diskContent);
        List<DiskImageFileSpecification> fileSpecs = builder.build("foo");
        assertEquals(1, fileSpecs.size());
        assertEquals("foo.vhdx", fileSpecs.get(0).getName());
        Disk disk = new Disk(fileSpecs.get(0).openStream(), Ownership.Dispose);
        try {
            {
                for (int i = 0; i < 8; ++i) {
                    disk.getContent().setPosition(i * 1024L * 1024);
                    assertEquals(i, disk.getContent().readByte());
                }
                disk.getContent().setPosition(150 * 1024 * 1024);
                assertEquals(0xFF, disk.getContent().readByte());
            }
        } finally {
            if (disk != null)
                disk.close();

        }
    }

}
