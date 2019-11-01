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

package DiscUtils.Vhd;

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.DiskImageBuilder;
import DiscUtils.Core.DiskImageFileSpecification;
import DiscUtils.Core.Geometry;
import DiscUtils.Streams.ConcatStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Builder.PassthroughStreamBuilder;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;


/**
 * DiskBuilder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/29 umjammer initial version <br>
 */
public class DiskBuilder extends DiskImageBuilder {
    /**
     * Gets or sets the type of VHD file to build.
     */
    public FileType getDiskType() {
        return _diskType;
    }

    public void setDiskType(FileType fileType) {
        _diskType = fileType;
    }

    private FileType _diskType = FileType.Dynamic;

    /**
     * Initiates the build process.
     *
     * @param baseName The base name for the VHD, for example 'foo' to create
     *            'foo.vhd'.
     * @returns A set of one or more logical files that constitute the VHD. The
     *          first file is the 'primary' file that is normally attached to
     *          VMs.
     */
    @Override
    public List<DiskImageFileSpecification> build(String baseName) {
        if (baseName == null || baseName.isEmpty()) {
            throw new IllegalArgumentException("Invalid base file name");
        }

        if (getContent() == null) {
            throw new IllegalStateException("No content stream specified");
        }

        List<DiskImageFileSpecification> fileSpecs = new ArrayList<>();

        Geometry geometry = Geometry.fromCapacity(getContent().getLength());

        Footer footer = new Footer(geometry, getContent().getLength(), _diskType);

        if (_diskType == FileType.Fixed) {
            footer.updateChecksum();

            byte[] footerSector = new byte[Sizes.Sector];
            footer.toBytes(footerSector, 0);

            SparseStream footerStream = SparseStream.fromStream(new MemoryStream(footerSector, false), Ownership.None);
            Stream imageStream = new ConcatStream(Ownership.None, getContent(), footerStream);
            fileSpecs.add(new DiskImageFileSpecification(baseName + ".vhd", new PassthroughStreamBuilder(imageStream)));
        } else if (_diskType == FileType.Dynamic) {
            fileSpecs.add(new DiskImageFileSpecification(baseName + ".vhd",
                                                         new DynamicDiskBuilder(getContent(), footer, (int) Sizes.OneMiB * 2)));
        } else {
            throw new UnsupportedOperationException("Only Fixed and Dynamic disk types supported");
        }

        return fileSpecs;
    }
}
