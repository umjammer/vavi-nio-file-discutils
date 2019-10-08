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

import java.util.ArrayList;
import java.util.List;

import DiscUtils.Core.DiskImageBuilder;
import DiscUtils.Core.DiskImageFileSpecification;
import DiscUtils.Core.GenericDiskAdapterType;
import DiscUtils.Core.Geometry;
import DiscUtils.Streams.Builder.PassthroughStreamBuilder;
import moe.yo3explorer.dotnetio4j.MemoryStream;


/**
 * Creates new VMDK disks by wrapping existing streams.
 * Using this method for creating virtual disks avoids consuming
 * large amounts of memory, or going via the local file system when the aim
 * is simply to present a VMDK version of an existing disk.
 */
public final class DiskBuilder extends DiskImageBuilder {
    /**
     * Initializes a new instance of the DiskBuilder class.
     */
    public DiskBuilder() {
        setDiskType(DiskCreateType.Vmfs);
        setAdapterType(DiskAdapterType.LsiLogicScsi);
    }

    /**
     * Gets or sets the specific VMware disk adapter type to embed in the VMDK.
     */
    private DiskAdapterType __AdapterType = DiskAdapterType.None;

    public DiskAdapterType getAdapterType() {
        return __AdapterType;
    }

    public void setAdapterType(DiskAdapterType value) {
        __AdapterType = value;
    }

    /**
     * Gets or sets the type of VMDK disk file required.
     */
    private DiskCreateType __DiskType = DiskCreateType.None;

    public DiskCreateType getDiskType() {
        return __DiskType;
    }

    public void setDiskType(DiskCreateType value) {
        __DiskType = value;
    }

    /**
     * Gets or sets the adaptor type for created virtual disk, setting to SCSI
     * implies LSI logic adapter.
     */
    public GenericDiskAdapterType getGenericAdapterType() {
        return getAdapterType() == DiskAdapterType.Ide ? GenericDiskAdapterType.Ide : GenericDiskAdapterType.Scsi;
    }

    public void setGenericAdapterType(GenericDiskAdapterType value) {
        if (value == GenericDiskAdapterType.Ide) {
            setAdapterType(DiskAdapterType.Ide);
        } else if (getAdapterType() == DiskAdapterType.Ide) {
            setAdapterType(DiskAdapterType.LsiLogicScsi);
        }

    }

    /**
     * Gets whether this file format preserves BIOS geometry information.
     */
    public boolean getPreservesBiosGeometry() {
        return true;
    }

    /**
     * Initiates the build process.
     *
     * @param baseName The base name for the VMDK, for example 'foo' to create
     *            'foo.vmdk'.
     * @return A set of one or more logical files that constitute the VMDK. The
     *         first file is
     *         the 'primary' file that is normally attached to VMs.
     */
    public List<DiskImageFileSpecification> build(String baseName) {
        if (baseName == null || baseName.isEmpty()) {
            throw new IllegalArgumentException("Invalid base file name");
        }

        if (getContent() == null) {
            throw new UnsupportedOperationException("No content stream specified");
        }

        if (getDiskType() != DiskCreateType.Vmfs && getDiskType() != DiskCreateType.VmfsSparse &&
            getDiskType() != DiskCreateType.MonolithicSparse) {
            throw new UnsupportedOperationException("Only MonolithicSparse, Vmfs and VmfsSparse disks implemented");
        }

        List<DiskImageFileSpecification> fileSpecs = new ArrayList<>();
        Geometry geometry = getGeometry() != null ? getGeometry() : DiskImageFile.defaultGeometry(getContent().getLength());
        Geometry biosGeometry = getBiosGeometry() != null ? getBiosGeometry()
                                                          : Geometry.lbaAssistedBiosGeometry(getContent().getLength());
        DescriptorFile baseDescriptor = DiskImageFile
                .createSimpleDiskDescriptor(geometry, biosGeometry, getDiskType(), getAdapterType());
        if (getDiskType() == DiskCreateType.Vmfs) {
            ExtentDescriptor extent = new ExtentDescriptor(ExtentAccess.ReadWrite,
                                                           getContent().getLength() / 512,
                                                           ExtentType.Vmfs,
                                                           baseName + "-flat.vmdk",
                                                           0);
            baseDescriptor.getExtents().add(extent);
            MemoryStream ms = new MemoryStream();
            baseDescriptor.write(ms);
            fileSpecs.add(new DiskImageFileSpecification(baseName + ".vmdk", new PassthroughStreamBuilder(ms)));
            fileSpecs.add(new DiskImageFileSpecification(baseName + "-flat.vmdk", new PassthroughStreamBuilder(getContent())));
        } else if (getDiskType() == DiskCreateType.VmfsSparse) {
            ExtentDescriptor extent = new ExtentDescriptor(ExtentAccess.ReadWrite,
                                                           getContent().getLength() / 512,
                                                           ExtentType.VmfsSparse,
                                                           baseName + "-sparse.vmdk",
                                                           0);
            baseDescriptor.getExtents().add(extent);
            MemoryStream ms = new MemoryStream();
            baseDescriptor.write(ms);
            fileSpecs.add(new DiskImageFileSpecification(baseName + ".vmdk", new PassthroughStreamBuilder(ms)));
            fileSpecs.add(new DiskImageFileSpecification(baseName + "-sparse.vmdk", new VmfsSparseExtentBuilder(getContent())));
        } else if (getDiskType() == DiskCreateType.MonolithicSparse) {
            ExtentDescriptor extent = new ExtentDescriptor(ExtentAccess.ReadWrite,
                                                           getContent().getLength() / 512,
                                                           ExtentType.Sparse,
                                                           baseName + ".vmdk",
                                                           0);
            baseDescriptor.getExtents().add(extent);
            fileSpecs.add(new DiskImageFileSpecification(baseName + ".vmdk",
                                                         new MonolithicSparseExtentBuilder(getContent(), baseDescriptor)));
        }

        return fileSpecs;
    }

}
