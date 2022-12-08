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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import discUtils.core.DiscFileLocator;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskExtent;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.ConcatStream;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


/**
 * Represents a single VMDK file.
 */
public final class DiskImageFile extends VirtualDiskLayer {

    private static final Random rng = new Random();

    private final FileAccess access;

    private SparseStream contentStream;

    private DescriptorFile descriptor;

    private FileLocator fileLocator;

    /**
     * The stream containing the VMDK disk, if this is a monolithic disk.
     */
    private Stream monolithicStream;

    /**
     * Indicates if this instance controls lifetime of monolithicStream.
     */
    private Ownership ownsMonolithicStream;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param path The path to the disk.
     * @param access The desired access to the disk.
     */
    public DiskImageFile(String path, FileAccess access) throws IOException {
        this.access = access;
        FileAccess fileAccess = FileAccess.Read;
        FileShare fileShare = FileShare.Read;
        if (this.access != FileAccess.Read) {
            fileAccess = FileAccess.ReadWrite;
            fileShare = FileShare.None;
        }

        Stream fileStream = null;
        Path parent = Paths.get(path).getParent();
        fileLocator = new LocalFileLocator(parent == null ? "" : parent.toString());
        try {
            fileStream = fileLocator.open(Utilities.getFileFromPath(path), FileMode.Open, fileAccess, fileShare);
            loadDescriptor(fileStream);
            // For monolithic disks, keep hold of the stream - we won't try to use the file name
            // from the embedded descriptor because the file may have been renamed, making the
            // descriptor out of date.
            if (descriptor.getCreateType() == DiskCreateType.StreamOptimized ||
                descriptor.getCreateType() == DiskCreateType.MonolithicSparse) {
                monolithicStream = fileStream;
                ownsMonolithicStream = Ownership.Dispose;
                fileStream = null;
            }
        } finally {
            if (fileStream != null) {
                fileStream.close();
            }
        }
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream containing a monolithic disk.
     * @param ownsStream Indicates if the created instance should own the
     *            stream.
     */
    public DiskImageFile(Stream stream, Ownership ownsStream) {
        access = stream.canWrite() ? FileAccess.ReadWrite : FileAccess.Read;
        loadDescriptor(stream);
        boolean createTypeIsSparse = descriptor.getCreateType() == DiskCreateType.MonolithicSparse ||
                                     descriptor.getCreateType() == DiskCreateType.StreamOptimized;
        if (!createTypeIsSparse || descriptor.getExtents().size() != 1 ||
            descriptor.getExtents().get(0).getType() != ExtentType.Sparse ||
            descriptor.getParentContentId() != 0xffffffff) {
            throw new IllegalArgumentException("Only Monolithic Sparse and Streaming Optimized disks can be accessed via a stream");
        }

        monolithicStream = stream;
        ownsMonolithicStream = ownsStream;
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param fileLocator An object to open the file and any extents.
     * @param file The file name.
     * @param access The type of access desired.
     */
    DiskImageFile(FileLocator fileLocator, String file, FileAccess access) {
        this.access = access;
        FileAccess fileAccess = FileAccess.Read;
        FileShare fileShare = FileShare.Read;
        if (this.access != FileAccess.Read) {
            fileAccess = FileAccess.ReadWrite;
            fileShare = FileShare.None;
        }

        try (Stream fileStream = fileLocator.open(file, FileMode.Open, fileAccess, fileShare)) {
            loadDescriptor(fileStream);
            // For monolithic disks, keep hold of the stream - we won't try to use the file name
            // from the embedded descriptor because the file may have been renamed, making the
            // descriptor out of date.
            if (descriptor.getCreateType() == DiskCreateType.StreamOptimized ||
                descriptor.getCreateType() == DiskCreateType.MonolithicSparse) {
                monolithicStream = fileStream;
                ownsMonolithicStream = Ownership.Dispose;
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        this.fileLocator = fileLocator.getRelativeLocator(fileLocator.getDirectoryFromPath(file));
    }

    /**
     * Gets the IDE/SCSI adapter type of the disk.
     */
    DiskAdapterType getAdapterType() {
        return descriptor.getAdapterType();
    }

    /**
     * Gets the BIOS geometry of this disk.
     */
    Geometry getBiosGeometry() {
        return descriptor.getBiosGeometry();
    }

    /**
     * Gets the capacity of this disk (in bytes).
     */
    public long getCapacity() {
        long result = 0;
        for (ExtentDescriptor extent : descriptor.getExtents()) {
            result += extent.getSizeInSectors() * Sizes.Sector;
        }
        return result;
    }

    int getContentId() {
        return descriptor.getContentId();
    }

    /**
     * Gets the 'CreateType' of this disk.
     */
    DiskCreateType getCreateType() {
        return descriptor.getCreateType();
    }

    /**
     * Gets the relative paths to all of the disk's extents.
     */
    public List<String> getExtentPaths() {
        return descriptor.getExtents().stream().map(ExtentDescriptor::getFileName).collect(Collectors.toList());
    }

    /**
     * Gets the extents that comprise this file.
     */
    public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> extents = new ArrayList<>(descriptor.getExtents().size());
        if (monolithicStream != null) {
            extents.add(new DiskExtent(descriptor.getExtents().get(0), 0, monolithicStream));
        } else {
            long pos = 0;
            for (ExtentDescriptor record : descriptor.getExtents()) {
                extents.add(new DiskExtent(record, pos, fileLocator, access));
                pos += record.getSizeInSectors() * Sizes.Sector;
            }
        }
        return extents;
    }

    /**
     * Gets the Geometry of this disk.
     */
    public Geometry getGeometry() {
        return descriptor.getDiskGeometry();
    }

    /**
     * Gets an indication as to whether the disk file is sparse.
     */
    public boolean isSparse() {
        return descriptor.getCreateType() == DiskCreateType.MonolithicSparse ||
               descriptor.getCreateType() == DiskCreateType.TwoGbMaxExtentSparse ||
               descriptor.getCreateType() == DiskCreateType.VmfsSparse;
    }

    /**
     * Gets a value indicating whether this disk is a linked differencing disk.
     */
    public boolean needsParent() {
        return descriptor.getParentContentId() != 0xffffffff;
    }

    /**
     * Gets a
     * {@code FileLocator}
     * that can resolve relative paths, or
     * {@code null}
     * .
     *
     * Typically used to locate parent disks.
     */
    public FileLocator getRelativeFileLocator() {
        return fileLocator;
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param parameters The desired parameters for the new disk.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(String path, DiskParameters parameters) {
        FileLocator locator = new LocalFileLocator(Utilities.getDirectoryFromPath(path));
        return initialize(locator, Utilities.getFileFromPath(path), parameters);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The name of the VMDK to create.
     * @param parameters The desired parameters for the new disk.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(DiscFileSystem fileSystem,
                                           String path,
                                           DiskParameters parameters) {
        FileLocator locator = new DiscFileLocator(fileSystem, Utilities.getDirectoryFromPath(path));
        return initialize(locator, Utilities.getFileFromPath(path), parameters);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param type The type of virtual disk to create.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(String path, long capacity, DiskCreateType type) {
        DiskParameters diskParams = new DiskParameters();
        diskParams.setCapacity(capacity);
        diskParams.setCreateType(type);
        return initialize(path, diskParams);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or
     *            {@code null}
     *            for default.
     * @param createType The type of virtual disk to create.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(String path,
                                           long capacity,
                                           Geometry geometry,
                                           DiskCreateType createType) {
        DiskParameters diskParams = new DiskParameters();
        diskParams.setCapacity(capacity);
        diskParams.setGeometry(geometry);
        diskParams.setCreateType(createType);
        return initialize(path, diskParams);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or
     *            {@code null}
     *            for default.
     * @param createType The type of virtual disk to create.
     * @param adapterType The type of disk adapter used with the disk.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(String path,
                                           long capacity,
                                           Geometry geometry,
                                           DiskCreateType createType,
                                           DiskAdapterType adapterType) {
        DiskParameters diskParams = new DiskParameters();
        diskParams.setCapacity(capacity);
        diskParams.setGeometry(geometry);
        diskParams.setCreateType(createType);
        diskParams.setAdapterType(adapterType);
        return initialize(path, diskParams);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param fileSystem The file system to create the VMDK on.
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param createType The type of virtual disk to create.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(DiscFileSystem fileSystem,
                                           String path,
                                           long capacity,
                                           DiskCreateType createType) {
        DiskParameters diskParams = new DiskParameters();
        diskParams.setCapacity(capacity);
        diskParams.setCreateType(createType);
        return initialize(fileSystem, path, diskParams);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param fileSystem The file system to create the VMDK on.
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param createType The type of virtual disk to create.
     * @param adapterType The type of disk adapter used with the disk.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(DiscFileSystem fileSystem,
                                           String path,
                                           long capacity,
                                           DiskCreateType createType,
                                           DiskAdapterType adapterType) {
        DiskParameters diskParams = new DiskParameters();
        diskParams.setCapacity(capacity);
        diskParams.setCreateType(createType);
        diskParams.setAdapterType(adapterType);
        return initialize(fileSystem, path, diskParams);
    }

    /**
     * Creates a new virtual disk that is a linked clone of an existing disk.
     *
     * @param path The path to the new disk.
     * @param type The type of the new disk.
     * @param parent The disk to clone.
     * @return The new virtual disk.
     */
    public static DiskImageFile initializeDifferencing(String path, DiskCreateType type, String parent) {
        if (type != DiskCreateType.MonolithicSparse && type != DiskCreateType.TwoGbMaxExtentSparse &&
            type != DiskCreateType.VmfsSparse) {
            throw new IllegalArgumentException("Differencing disks must be sparse");
        }

        try (DiskImageFile parentFile = new DiskImageFile(parent, FileAccess.Read)) {
            DescriptorFile baseDescriptor = createDifferencingDiskDescriptor(type, parentFile, parent);
            FileLocator locator = new LocalFileLocator(Utilities.getDirectoryFromPath(path));
            return doInitialize(locator,
                                Utilities.getFileFromPath(path),
                                parentFile.getCapacity(),
                                type,
                                baseDescriptor);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Creates a new virtual disk that is a linked clone of an existing disk.
     *
     * @param fileSystem The file system to create the VMDK on.
     * @param path The path to the new disk.
     * @param type The type of the new disk.
     * @param parent The disk to clone.
     * @return The new virtual disk.
     */
    public static DiskImageFile initializeDifferencing(DiscFileSystem fileSystem,
                                                       String path,
                                                       DiskCreateType type,
                                                       String parent) {
        if (type != DiskCreateType.MonolithicSparse && type != DiskCreateType.TwoGbMaxExtentSparse &&
            type != DiskCreateType.VmfsSparse) {
            throw new IllegalArgumentException("Differencing disks must be sparse");
        }

        String basePath = Utilities.getDirectoryFromPath(path);
        FileLocator locator = new DiscFileLocator(fileSystem, basePath);
        FileLocator parentLocator = locator.getRelativeLocator(Utilities.getDirectoryFromPath(parent));

        try (DiskImageFile parentFile = new DiskImageFile(parentLocator, Utilities.getFileFromPath(parent), FileAccess.Read)) {
            DescriptorFile baseDescriptor = createDifferencingDiskDescriptor(type, parentFile, parent);
            return doInitialize(locator, Utilities.getFileFromPath(path), parentFile.getCapacity(), type, baseDescriptor);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Gets the contents of this disk as a stream.
     *
     * @param parent The content of the parent disk (needed if this is a
     *            differencing disk).
     * @param ownsParent A value indicating whether ownership of the parent
     *            stream is transfered.
     * @return The stream containing the disk contents.
     */
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        if (descriptor.getParentContentId() == 0xffffffff) {
            if (parent != null && ownsParent == Ownership.Dispose) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }

            parent = null;
        }

        if (parent == null) {
            parent = new ZeroStream(getCapacity());
            ownsParent = Ownership.Dispose;
        }

        if (descriptor.getExtents().size() == 1) {
            if (monolithicStream != null) {
                return new HostedSparseExtentStream(monolithicStream, Ownership.None, 0, parent, ownsParent);
            }

            return openExtent(descriptor.getExtents().get(0), 0, parent, ownsParent);
        }

        long extentStart = 0;
        List<SparseStream> streams = new ArrayList<>(descriptor.getExtents().size());
        for (int i = 0; i < descriptor.getExtents().size(); ++i) {
            streams.add(i, openExtent(descriptor.getExtents().get(i),
                                    extentStart,
                                    parent,
                                    i == streams.size() - 1 ? ownsParent : Ownership.None));
            extentStart += descriptor.getExtents().get(i).getSizeInSectors() * Sizes.Sector;
        }
        return new ConcatStream(Ownership.Dispose, streams);
    }

    /**
     * Gets the location of the parent.
     *
     * @return The parent locations as an array.
     */
    public List<String> getParentLocations() {
        return Collections.singletonList(descriptor.getParentFileNameHint());
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param fileLocator The object used to locate / create the component
     *            files.
     * @param path The name of the VMDK to create.
     * @param parameters The desired parameters for the new disk.
     * @return The newly created disk image.
     */
    static DiskImageFile initialize(FileLocator fileLocator, String path, DiskParameters parameters) {
        if (parameters.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }

        Geometry geometry = parameters.getGeometry() != null ? parameters.getGeometry()
                                                             : defaultGeometry(parameters.getCapacity());
        Geometry biosGeometry;
        if (parameters.getBiosGeometry() != null) {
            biosGeometry = parameters.getBiosGeometry();
        } else {
            biosGeometry = Geometry.makeBiosSafe(geometry, parameters.getCapacity());
        }
        DiskAdapterType adapterType = parameters.getAdapterType() == DiskAdapterType.None ? DiskAdapterType.LsiLogicScsi
                                                                                          : parameters.getAdapterType();
        DiskCreateType createType = parameters.getCreateType() == DiskCreateType.None ? DiskCreateType.MonolithicSparse
                                                                                      : parameters.getCreateType();
        DescriptorFile baseDescriptor = createSimpleDiskDescriptor(geometry, biosGeometry, createType, adapterType);
        return doInitialize(fileLocator, path, parameters.getCapacity(), createType, baseDescriptor);
    }

    static Geometry defaultGeometry(long diskSize) {
        int heads;
        int sectors;
        if (diskSize < Sizes.OneGiB) {
            heads = 64;
            sectors = 32;
        } else if (diskSize < 2 * Sizes.OneGiB) {
            heads = 128;
            sectors = 32;
        } else {
            heads = 255;
            sectors = 63;
        }
        int cylinders = (int) (diskSize / (heads * sectors * Sizes.Sector));
        return new Geometry(cylinders, heads, sectors);
    }

    static DescriptorFile createSimpleDiskDescriptor(Geometry geometry,
                                                            Geometry biosGeometery,
                                                            DiskCreateType createType,
                                                            DiskAdapterType adapterType) {
        DescriptorFile baseDescriptor = new DescriptorFile();
        baseDescriptor.setDiskGeometry(geometry);
        baseDescriptor.setBiosGeometry(biosGeometery);
        baseDescriptor.setContentId(rng.nextInt());
        baseDescriptor.setCreateType(createType);
        baseDescriptor.setUniqueId(UUID.randomUUID());
        baseDescriptor.setHardwareVersion("4");
        baseDescriptor.setAdapterType(adapterType);
        return baseDescriptor;
    }

    static ServerSparseExtentHeader createServerSparseExtentHeader(long size) {
        int numSectors = (int) MathUtilities.ceil(size, Sizes.Sector);
        int numGDEntries = (int) MathUtilities.ceil(numSectors * (long) Sizes.Sector, 2 * Sizes.OneMiB);
        ServerSparseExtentHeader header = new ServerSparseExtentHeader();
        header.capacity = numSectors;
        header.grainSize = 1;
        header.gdOffset = 4;
        header.numGdEntries = numGDEntries;
        header.freeSector = (int) (header.gdOffset + MathUtilities.ceil(numGDEntries * 4, Sizes.Sector));
        return header;
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        if (contentStream != null) {
            contentStream.close();
            contentStream = null;
        }

        if (ownsMonolithicStream == Ownership.Dispose && monolithicStream != null) {
            monolithicStream.close();
            monolithicStream = null;
        }
    }

    private static DiskImageFile doInitialize(FileLocator fileLocator,
                                              String file,
                                              long capacity,
                                              DiskCreateType type,
                                              DescriptorFile baseDescriptor) {
        if (type == DiskCreateType.MonolithicSparse) {
            // MonolithicSparse is a special case, the descriptor is embedded in the file itself...
            try (Stream fs = fileLocator.open(file, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
                long[] descriptorStart = new long[1];
                createExtent(fs, capacity, ExtentType.Sparse, 10 * Sizes.OneKiB, descriptorStart);
                ExtentDescriptor extent = new ExtentDescriptor(ExtentAccess.ReadWrite,
                                                               capacity / Sizes.Sector,
                                                               ExtentType.Sparse,
                                                               file,
                                                               0);
                fs.position(descriptorStart[0] * Sizes.Sector);
                baseDescriptor.getExtents().add(extent);
                baseDescriptor.write(fs);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        } else {
            ExtentType extentType = createTypeToExtentType(type);
            long totalSize = 0;
            List<ExtentDescriptor> extents = new ArrayList<>();
            if (type == DiskCreateType.MonolithicFlat || type == DiskCreateType.VmfsSparse || type == DiskCreateType.Vmfs) {
                String adornment = "flat";
                if (type == DiskCreateType.VmfsSparse) {
                    adornment = baseDescriptor.getParentFileNameHint() == null ||
                                baseDescriptor.getParentFileNameHint().isEmpty() ? "sparse" : "delta";
                }

                String fileName = adornFileName(file, adornment);

                try (Stream fs = fileLocator.open(fileName, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
                    createExtent(fs, capacity, extentType);
                    extents.add(new ExtentDescriptor(ExtentAccess.ReadWrite, capacity / Sizes.Sector, extentType, fileName, 0));
                    totalSize = capacity;
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            } else if (type == DiskCreateType.TwoGbMaxExtentFlat || type == DiskCreateType.TwoGbMaxExtentSparse) {
                int i = 1;
                while (totalSize < capacity) {
                    String adornment;
                    if (type == DiskCreateType.TwoGbMaxExtentSparse) {
                        adornment = String.format("s%3x", i);
                    } else {
                        adornment = String.format("%6x", i);
                    }

                    String fileName = adornFileName(file, adornment);

                    try (Stream fs = fileLocator.open(fileName, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
                        long extentSize = Math.min(2 * Sizes.OneGiB - Sizes.OneMiB, capacity - totalSize);
                        createExtent(fs, extentSize, extentType);
                        extents.add(new ExtentDescriptor(ExtentAccess.ReadWrite,
                                                         extentSize / Sizes.Sector,
                                                         extentType,
                                                         fileName,
                                                         0));
                        totalSize += extentSize;
                    } catch (IOException e) {
                        throw new dotnet4j.io.IOException(e);
                    }

                    ++i;
                }
            } else {
                throw new UnsupportedOperationException("Creating disks of this type is not supported");
            }

            try (Stream fs = fileLocator.open(file, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
                baseDescriptor.getExtents().addAll(extents);
                baseDescriptor.write(fs);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        return new DiskImageFile(fileLocator, file, FileAccess.ReadWrite);
    }

    /**
     * @param descriptorStart {@cs out}
     */
    private static void createSparseExtent(Stream extentStream,
                                           long size,
                                           long descriptorLength,
                                           long[] descriptorStart) {
        // Figure out grain size and number of grain tables, and adjust actual extent size to be a multiple
        // of grain size
        final int GtesPerGt = 512;
        long grainSize = 128;
        int numGrainTables = (int) MathUtilities.ceil(size, grainSize * GtesPerGt * Sizes.Sector);
        descriptorLength = MathUtilities.roundUp(descriptorLength, Sizes.Sector);
        descriptorStart[0] = 0;
        if (descriptorLength != 0) {
            descriptorStart[0] = 1;
        }

        long redundantGrainDirStart = Math.max(descriptorStart[0], 1) + MathUtilities.ceil(descriptorLength, Sizes.Sector);
        long redundantGrainDirLength = numGrainTables * 4L;
        long redundantGrainTablesStart = redundantGrainDirStart + MathUtilities.ceil(redundantGrainDirLength, Sizes.Sector);
        long redundantGrainTablesLength = (long) numGrainTables * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector);
        long grainDirStart = redundantGrainTablesStart + MathUtilities.ceil(redundantGrainTablesLength, Sizes.Sector);
        long grainDirLength = numGrainTables * 4L;
        long grainTablesStart = grainDirStart + MathUtilities.ceil(grainDirLength, Sizes.Sector);
        long grainTablesLength = (long) numGrainTables * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector);
        long dataStart = MathUtilities.roundUp(grainTablesStart + MathUtilities.ceil(grainTablesLength, Sizes.Sector),
                                               grainSize);
        // Generate the header, and write it
        HostedSparseExtentHeader header = new HostedSparseExtentHeader();
        header.flags = EnumSet.of(HostedSparseExtentFlags.ValidLineDetectionTest, HostedSparseExtentFlags.RedundantGrainTable);
        header.capacity = MathUtilities.roundUp(size, grainSize * Sizes.Sector) / Sizes.Sector;
        header.grainSize = grainSize;
        header.descriptorOffset = descriptorStart[0];
        header.descriptorSize = descriptorLength / Sizes.Sector;
        header.numGTEsPerGT = GtesPerGt;
        header.rgdOffset = redundantGrainDirStart;
        header.gdOffset = grainDirStart;
        header.overhead = dataStart;
        extentStream.position(0);
        extentStream.write(header.getBytes(), 0, Sizes.Sector);
        // Zero-out the descriptor space
        if (descriptorLength > 0) {
            byte[] descriptor = new byte[(int) descriptorLength];
            extentStream.position(descriptorStart[0] * Sizes.Sector);
            extentStream.write(descriptor, 0, descriptor.length);
        }

        // Generate the redundant grain dir, and write it
        byte[] grainDir = new byte[numGrainTables * 4];
        for (int i = 0; i < numGrainTables; ++i) {
            EndianUtilities.writeBytesLittleEndian(
                                                   (int) (redundantGrainTablesStart +
                                                          i * MathUtilities.ceil(GtesPerGt * 4, Sizes.Sector)),
                                                   grainDir,
                                                   i * 4);
        }
        extentStream.position(redundantGrainDirStart * Sizes.Sector);
        extentStream.write(grainDir, 0, grainDir.length);
        // Write out the blank grain tables
        byte[] grainTable = new byte[GtesPerGt * 4];
        for (int i = 0; i < numGrainTables; ++i) {
            extentStream.position(redundantGrainTablesStart * Sizes.Sector + (long) i * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector));
            extentStream.write(grainTable, 0, grainTable.length);
        }
        for (int i = 0; i < numGrainTables; ++i) {
            // Generate the main grain dir, and write it
            EndianUtilities
                    .writeBytesLittleEndian((int) (grainTablesStart + i * MathUtilities.ceil(GtesPerGt * 4, Sizes.Sector)),
                                            grainDir,
                                            i * 4);
        }
        extentStream.position(grainDirStart * Sizes.Sector);
        extentStream.write(grainDir, 0, grainDir.length);
        for (int i = 0; i < numGrainTables; ++i) {
            // Write out the blank grain tables
            extentStream.position(grainTablesStart * Sizes.Sector + (long) i * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector));
            extentStream.write(grainTable, 0, grainTable.length);
        }
        // Make sure stream is correct length
        if (extentStream.getLength() != dataStart * Sizes.Sector) {
            extentStream.setLength(dataStart * Sizes.Sector);
        }

    }

    private static void createExtent(Stream extentStream, long size, ExtentType type) {
        long[] descriptorStart = new long[1];
        createExtent(extentStream, size, type, 0, descriptorStart);
    }

    /**
     * @param descriptorStart {@cs out}
     */
    private static void createExtent(Stream extentStream,
                                     long size,
                                     ExtentType type,
                                     long descriptorLength,
                                     long[] descriptorStart) {
        if (type == ExtentType.Flat || type == ExtentType.Vmfs) {
            extentStream.setLength(size);
            descriptorStart[0] = 0;
            return;
        }

        if (type == ExtentType.Sparse) {
            createSparseExtent(extentStream, size, descriptorLength, descriptorStart);
        } else if (type == ExtentType.VmfsSparse) {
            ServerSparseExtentHeader header = createServerSparseExtentHeader(size);
            extentStream.position(0);
            extentStream.write(header.getBytes(), 0, 4 * Sizes.Sector);
            byte[] blankGlobalDirectory = new byte[header.numGdEntries * 4];
            extentStream.write(blankGlobalDirectory, 0, blankGlobalDirectory.length);
            descriptorStart[0] = 0;
        } else {
            throw new UnsupportedOperationException("Extent type not implemented");
        }
    }

    private static String adornFileName(String name, String adornment) {
        if (!name.endsWith(".vmdk")) {
            throw new IllegalArgumentException("name must end in .vmdk to be adorned");
        }

        return name.substring(0, name.length() - 5) + "-" + adornment + ".vmdk";
    }

    private static ExtentType createTypeToExtentType(DiskCreateType type) {
        switch (type) {
        case FullDevice:
        case MonolithicFlat:
        case PartitionedDevice:
        case TwoGbMaxExtentFlat:
            return ExtentType.Flat;
        case MonolithicSparse:
        case StreamOptimized:
        case TwoGbMaxExtentSparse:
            return ExtentType.Sparse;
        case Vmfs:
            return ExtentType.Vmfs;
        case VmfsPassthroughRawDeviceMap:
            return ExtentType.VmfsRdm;
        case VmfsRaw:
        case VmfsRawDeviceMap:
            return ExtentType.VmfsRaw;
        case VmfsSparse:
            return ExtentType.VmfsSparse;
        default:
            throw new IllegalArgumentException(String.format("Unable to convert %s", type));
        }
    }

    private static DescriptorFile createDifferencingDiskDescriptor(DiskCreateType type,
                                                                   DiskImageFile parent,
                                                                   String parentPath) {
        DescriptorFile baseDescriptor = new DescriptorFile();
        baseDescriptor.setContentId(rng.nextInt());
        baseDescriptor.setParentContentId(parent.getContentId());
        baseDescriptor.setParentFileNameHint(parentPath);
        baseDescriptor.setCreateType(type);
        return baseDescriptor;
    }

    private SparseStream openExtent(ExtentDescriptor extent,
                                    long extentStart,
                                    SparseStream parent,
                                    Ownership ownsParent) {
        FileAccess access = FileAccess.Read;
        FileShare share = FileShare.Read;
        if (extent.getAccess() == ExtentAccess.ReadWrite && this.access != FileAccess.Read) {
            access = FileAccess.ReadWrite;
            share = FileShare.None;
        }

        if (extent.getType() != ExtentType.Sparse && extent.getType() != ExtentType.VmfsSparse) {
            if (ownsParent == Ownership.Dispose && parent != null) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }
        }

        switch (extent.getType()) {
        case Flat:
        case Vmfs:
            return SparseStream.fromStream(fileLocator.open(extent.getFileName(), FileMode.Open, access, share), Ownership.Dispose);
        case Zero:
            return new ZeroStream(extent.getSizeInSectors() * Sizes.Sector);
        case Sparse:
            return new HostedSparseExtentStream(fileLocator
                    .open(extent.getFileName(), FileMode.Open, access, share), Ownership.Dispose, extentStart, parent, ownsParent);
        case VmfsSparse:
            return new ServerSparseExtentStream(fileLocator
                    .open(extent.getFileName(), FileMode.Open, access, share), Ownership.Dispose, extentStart, parent, ownsParent);
        default:
            throw new UnsupportedOperationException();
        }
    }

    private void loadDescriptor(Stream s) {
        s.position(0);
        byte[] header = StreamUtilities.readExact(s, (int) Math.min(Sizes.Sector, s.getLength()));
        if (header.length < Sizes.Sector ||
            EndianUtilities.toUInt32LittleEndian(header, 0) != HostedSparseExtentHeader.VmdkMagicNumber) {
            s.position(0);
            descriptor = new DescriptorFile(s);
            if (access != FileAccess.Read) {
                descriptor.setContentId(rng.nextInt());
                s.position(0);
                descriptor.write(s);
                s.setLength(s.position());
            }

        } else {
            // This is a sparse disk extent, hopefully with embedded descriptor...
            HostedSparseExtentHeader hdr = HostedSparseExtentHeader.read(header, 0);
            if (hdr.descriptorOffset != 0) {
                Stream descriptorStream = new SubStream(s,
                                                                  hdr.descriptorOffset * Sizes.Sector,
                                                                  hdr.descriptorSize * Sizes.Sector);
                descriptor = new DescriptorFile(descriptorStream);
                if (access != FileAccess.Read) {
                    descriptor.setContentId(rng.nextInt());
                    descriptorStream.position(0);
                    descriptor.write(descriptorStream);
                    byte[] blank = new byte[(int) (descriptorStream.getLength() - descriptorStream.position())];
                    descriptorStream.write(blank, 0, blank.length);
                }
            }
        }
    }
}
