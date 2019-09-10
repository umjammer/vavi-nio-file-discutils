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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import DiscUtils.Core.DiscFileLocator;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileLocator;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskExtent;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.ConcatStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a single VMDK file.
 */
public final class DiskImageFile extends VirtualDiskLayer {
    private static final Random _rng = new Random();

    private final FileAccess _access;

    private SparseStream _contentStream;

    private DescriptorFile _descriptor;

    private FileLocator _fileLocator;

    /**
     * The stream containing the VMDK disk, if this is a monolithic disk.
     */
    private Stream _monolithicStream;

    /**
     * Indicates if this instance controls lifetime of _monolithicStream.
     */
    private Ownership _ownsMonolithicStream;

    /**
     * Initializes a new instance of the DiskImageFile class.
     * 
     * @param path The path to the disk.
     * @param access The desired access to the disk.
     */
    public DiskImageFile(String path, FileAccess access) throws IOException {
        _access = access;
        FileAccess fileAccess = FileAccess.Read;
        FileShare fileShare = FileShare.Read;
        if (_access != FileAccess.Read) {
            fileAccess = FileAccess.ReadWrite;
            fileShare = FileShare.None;
        }

        Stream fileStream = null;
        _fileLocator = new LocalFileLocator(Paths.get(path).getParent().toString());
        try {
            fileStream = _fileLocator.open(Paths.get(path).getFileName().toString(), FileMode.Open, fileAccess, fileShare);
            loadDescriptor(fileStream);
            // For monolithic disks, keep hold of the stream - we won't try to use the file name
            // from the embedded descriptor because the file may have been renamed, making the
            // descriptor out of date.
            if (_descriptor.getCreateType() == DiskCreateType.StreamOptimized ||
                _descriptor.getCreateType() == DiskCreateType.MonolithicSparse) {
                _monolithicStream = fileStream;
                _ownsMonolithicStream = Ownership.Dispose;
                fileStream = null;
            }

        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
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
        _access = stream.canWrite() ? FileAccess.ReadWrite : FileAccess.Read;
        loadDescriptor(stream);
        boolean createTypeIsSparse = _descriptor.getCreateType() == DiskCreateType.MonolithicSparse ||
                                     _descriptor.getCreateType() == DiskCreateType.StreamOptimized;
        if (!createTypeIsSparse || _descriptor.getExtents().size() != 1 ||
            _descriptor.getExtents().get(0).getType() != ExtentType.Sparse ||
            _descriptor.getParentContentId() != Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Only Monolithic Sparse and Streaming Optimized disks can be accessed via a stream");
        }

        _monolithicStream = stream;
        _ownsMonolithicStream = ownsStream;
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     * 
     * @param fileLocator An object to open the file and any extents.
     * @param file The file name.
     * @param access The type of access desired.
     */
    public DiskImageFile(FileLocator fileLocator, String file, FileAccess access) {
        _access = access;
        FileAccess fileAccess = FileAccess.Read;
        FileShare fileShare = FileShare.Read;
        if (_access != FileAccess.Read) {
            fileAccess = FileAccess.ReadWrite;
            fileShare = FileShare.None;
        }

        try (Stream fileStream = fileLocator.open(file, FileMode.Open, fileAccess, fileShare)) {
            loadDescriptor(fileStream);
            // For monolithic disks, keep hold of the stream - we won't try to use the file name
            // from the embedded descriptor because the file may have been renamed, making the
            // descriptor out of date.
            if (_descriptor.getCreateType() == DiskCreateType.StreamOptimized ||
                _descriptor.getCreateType() == DiskCreateType.MonolithicSparse) {
                _monolithicStream = fileStream;
                _ownsMonolithicStream = Ownership.Dispose;
            }
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
        _fileLocator = fileLocator.getRelativeLocator(fileLocator.getDirectoryFromPath(file));
    }

    /**
     * Gets the IDE/SCSI adapter type of the disk.
     */
    public DiskAdapterType getAdapterType() {
        return _descriptor.getAdapterType();
    }

    /**
     * Gets the BIOS geometry of this disk.
     */
    public Geometry getBiosGeometry() {
        return _descriptor.getBiosGeometry();
    }

    /**
     * Gets the capacity of this disk (in bytes).
     */
    public long getCapacity() {
        long result = 0;
        for (Object __dummyForeachVar0 : _descriptor.getExtents()) {
            ExtentDescriptor extent = (ExtentDescriptor) __dummyForeachVar0;
            result += extent.getSizeInSectors() * Sizes.Sector;
        }
        return result;
    }

    public int getContentId() {
        return _descriptor.getContentId();
    }

    /**
     * Gets the 'CreateType' of this disk.
     */
    public DiskCreateType getCreateType() {
        return _descriptor.getCreateType();
    }

    /**
     * Gets the relative paths to all of the disk's extents.
     */
    public List<String> getExtentPaths() {
        return _descriptor.getExtents().stream().map(path -> path.getFileName()).collect(Collectors.toList());
    }

    /**
     * Gets the extents that comprise this file.
     */
    public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> extents = new ArrayList<>(_descriptor.getExtents().size());
        if (_monolithicStream != null) {
            extents.add(new DiskExtent(_descriptor.getExtents().get(0), 0, _monolithicStream));
        } else {
            long pos = 0;
            for (Object __dummyForeachVar2 : _descriptor.getExtents()) {
                ExtentDescriptor record = (ExtentDescriptor) __dummyForeachVar2;
                extents.add(new DiskExtent(record, pos, _fileLocator, _access));
                pos += record.getSizeInSectors() * Sizes.Sector;
            }
        }
        return extents;
    }

    /**
     * Gets the Geometry of this disk.
     */
    public Geometry getGeometry() {
        return _descriptor.getDiskGeometry();
    }

    /**
     * Gets an indication as to whether the disk file is sparse.
     */
    public boolean getIsSparse() {
        return _descriptor.getCreateType() == DiskCreateType.MonolithicSparse ||
               _descriptor.getCreateType() == DiskCreateType.TwoGbMaxExtentSparse ||
               _descriptor.getCreateType() == DiskCreateType.VmfsSparse;
    }

    /**
     * Gets a value indicating whether this disk is a linked differencing disk.
     */
    public boolean getNeedsParent() {
        return _descriptor.getParentContentId() != Integer.MAX_VALUE;
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
        return _fileLocator;
    }

    /**
     * Creates a new virtual disk at the specified path.
     * 
     * @param path The name of the VMDK to create.
     * @param parameters The desired parameters for the new disk.
     * @return The newly created disk image.
     */
    public static DiskImageFile initialize(String path, DiskParameters parameters) {
        FileLocator locator = new LocalFileLocator(Paths.get(path).getParent().toString());
        return initialize(locator, Paths.get(path).getFileName().toString(), parameters);
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
            FileLocator locator = new LocalFileLocator(Paths.get(path).getParent().toString());
            return doInitialize(locator,
                                Paths.get(path).getFileName().toString(),
                                parentFile.getCapacity(),
                                type,
                                baseDescriptor);
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
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
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
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
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) throws IOException {
        if (_descriptor.getParentContentId() == Integer.MAX_VALUE) {
            if (parent != null && ownsParent == Ownership.Dispose) {
                parent.close();
            }

            parent = null;
        }

        if (parent == null) {
            parent = new ZeroStream(getCapacity());
            ownsParent = Ownership.Dispose;
        }

        if (_descriptor.getExtents().size() == 1) {
            if (_monolithicStream != null) {
                return new HostedSparseExtentStream(_monolithicStream, Ownership.None, 0, parent, ownsParent);
            }

            return openExtent(_descriptor.getExtents().get(0), 0, parent, ownsParent);
        }

        long extentStart = 0;
        List<SparseStream> streams = new ArrayList<>(_descriptor.getExtents().size());
        for (int i = 0; i < streams.size(); ++i) {
            streams.add(i, openExtent(_descriptor.getExtents().get(i),
                                    extentStart,
                                    parent,
                                    i == streams.size() - 1 ? ownsParent : Ownership.None));
            extentStart += _descriptor.getExtents().get(i).getSizeInSectors() * Sizes.Sector;
        }
        return new ConcatStream(Ownership.Dispose, streams);
    }

    /**
     * Gets the location of the parent.
     * 
     * @return The parent locations as an array.
     */
    public List<String> getParentLocations() {
        return Arrays.asList(_descriptor.getParentFileNameHint());
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
    public static DiskImageFile initialize(FileLocator fileLocator, String path, DiskParameters parameters) {
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

    public static Geometry defaultGeometry(long diskSize) {
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

    public static DescriptorFile createSimpleDiskDescriptor(Geometry geometry,
                                                            Geometry biosGeometery,
                                                            DiskCreateType createType,
                                                            DiskAdapterType adapterType) {
        DescriptorFile baseDescriptor = new DescriptorFile();
        baseDescriptor.setDiskGeometry(geometry);
        baseDescriptor.setBiosGeometry(biosGeometery);
        baseDescriptor.setContentId(_rng.nextInt());
        baseDescriptor.setCreateType(createType);
        baseDescriptor.setUniqueId(UUID.randomUUID());
        baseDescriptor.setHardwareVersion("4");
        baseDescriptor.setAdapterType(adapterType);
        return baseDescriptor;
    }

    public static ServerSparseExtentHeader createServerSparseExtentHeader(long size) {
        int numSectors = (int) MathUtilities.ceil(size, Sizes.Sector);
        int numGDEntries = (int) MathUtilities.ceil(numSectors * (long) Sizes.Sector, 2 * Sizes.OneMiB);
        ServerSparseExtentHeader header = new ServerSparseExtentHeader();
        header.Capacity = numSectors;
        header.GrainSize = 1;
        header.GdOffset = 4;
        header.NumGdEntries = numGDEntries;
        header.FreeSector = (int) (header.GdOffset + MathUtilities.ceil(numGDEntries * 4, Sizes.Sector));
        return header;
    }

    /**
     * Disposes of this instance.
     */
    public void close() throws IOException {
        try {
            if (_contentStream != null) {
                _contentStream.close();
                _contentStream = null;
            }

            if (_ownsMonolithicStream == Ownership.Dispose && _monolithicStream != null) {
                _monolithicStream.close();
                _monolithicStream = null;
            }

        } finally {
            super.close();
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
                fs.setPosition(descriptorStart[0] * Sizes.Sector);
                baseDescriptor.getExtents().add(extent);
                baseDescriptor.write(fs);
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
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
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
            } else if (type == DiskCreateType.TwoGbMaxExtentFlat || type == DiskCreateType.TwoGbMaxExtentSparse) {
                int i = 1;
                while (totalSize < capacity) {
                    String adornment;
                    if (type == DiskCreateType.TwoGbMaxExtentSparse) {
                        adornment = String.format("s{0:x3}", i);
                    } else {
                        adornment = String.format("{0:x6}", i);
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
                        throw new moe.yo3explorer.dotnetio4j.IOException(e);
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
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
        }
        return new DiskImageFile(fileLocator, file, FileAccess.ReadWrite);
    }

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
        long redundantGrainDirLength = numGrainTables * 4;
        long redundantGrainTablesStart = redundantGrainDirStart + MathUtilities.ceil(redundantGrainDirLength, Sizes.Sector);
        long redundantGrainTablesLength = numGrainTables * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector);
        long grainDirStart = redundantGrainTablesStart + MathUtilities.ceil(redundantGrainTablesLength, Sizes.Sector);
        long grainDirLength = numGrainTables * 4;
        long grainTablesStart = grainDirStart + MathUtilities.ceil(grainDirLength, Sizes.Sector);
        long grainTablesLength = numGrainTables * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector);
        long dataStart = MathUtilities.roundUp(grainTablesStart + MathUtilities.ceil(grainTablesLength, Sizes.Sector),
                                               grainSize);
        // Generate the header, and write it
        HostedSparseExtentHeader header = new HostedSparseExtentHeader();
        header.Flags = EnumSet.of(HostedSparseExtentFlags.ValidLineDetectionTest, HostedSparseExtentFlags.RedundantGrainTable);
        header.Capacity = MathUtilities.roundUp(size, grainSize * Sizes.Sector) / Sizes.Sector;
        header.GrainSize = grainSize;
        header.DescriptorOffset = descriptorStart[0];
        header.DescriptorSize = descriptorLength / Sizes.Sector;
        header.NumGTEsPerGT = GtesPerGt;
        header.RgdOffset = redundantGrainDirStart;
        header.GdOffset = grainDirStart;
        header.Overhead = dataStart;
        extentStream.setPosition(0);
        extentStream.write(header.getBytes(), 0, Sizes.Sector);
        // Zero-out the descriptor space
        if (descriptorLength > 0) {
            byte[] descriptor = new byte[(int) descriptorLength];
            extentStream.setPosition(descriptorStart[0] * Sizes.Sector);
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
        extentStream.setPosition(redundantGrainDirStart * Sizes.Sector);
        extentStream.write(grainDir, 0, grainDir.length);
        // Write out the blank grain tables
        byte[] grainTable = new byte[GtesPerGt * 4];
        for (int i = 0; i < numGrainTables; ++i) {
            extentStream
                    .setPosition(redundantGrainTablesStart * Sizes.Sector + i * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector));
            extentStream.write(grainTable, 0, grainTable.length);
        }
        for (int i = 0; i < numGrainTables; ++i) {
            // Generate the main grain dir, and write it
            EndianUtilities
                    .writeBytesLittleEndian((int) (grainTablesStart + i * MathUtilities.ceil(GtesPerGt * 4, Sizes.Sector)),
                                            grainDir,
                                            i * 4);
        }
        extentStream.setPosition(grainDirStart * Sizes.Sector);
        extentStream.write(grainDir, 0, grainDir.length);
        for (int i = 0; i < numGrainTables; ++i) {
            // Write out the blank grain tables
            extentStream.setPosition(grainTablesStart * Sizes.Sector + i * MathUtilities.roundUp(GtesPerGt * 4, Sizes.Sector));
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
            extentStream.setPosition(0);
            extentStream.write(header.getBytes(), 0, 4 * Sizes.Sector);
            byte[] blankGlobalDirectory = new byte[header.NumGdEntries * 4];
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
        baseDescriptor.setContentId(_rng.nextInt());
        baseDescriptor.setParentContentId(parent.getContentId());
        baseDescriptor.setParentFileNameHint(parentPath);
        baseDescriptor.setCreateType(type);
        return baseDescriptor;
    }

    private SparseStream openExtent(ExtentDescriptor extent,
                                    long extentStart,
                                    SparseStream parent,
                                    Ownership ownsParent) throws IOException {
        FileAccess access = FileAccess.Read;
        FileShare share = FileShare.Read;
        if (extent.getAccess() == ExtentAccess.ReadWrite && _access != FileAccess.Read) {
            access = FileAccess.ReadWrite;
            share = FileShare.None;
        }

        if (extent.getType() != ExtentType.Sparse && extent.getType() != ExtentType.VmfsSparse) {
            if (ownsParent == Ownership.Dispose && parent != null) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
            }

        }

        switch (extent.getType()) {
        case Flat:
        case Vmfs:
            return SparseStream.fromStream(_fileLocator.open(extent.getFileName(), FileMode.Open, access, share), Ownership.Dispose);
        case Zero:
            return new ZeroStream(extent.getSizeInSectors() * Sizes.Sector);
        case Sparse:
            return new HostedSparseExtentStream(_fileLocator
                    .open(extent.getFileName(), FileMode.Open, access, share), Ownership.Dispose, extentStart, parent, ownsParent);
        case VmfsSparse:
            return new ServerSparseExtentStream(_fileLocator
                    .open(extent.getFileName(), FileMode.Open, access, share), Ownership.Dispose, extentStart, parent, ownsParent);
        default:
            throw new UnsupportedOperationException();

        }
    }

    private void loadDescriptor(Stream s) {
        s.setPosition(0);
        byte[] header = StreamUtilities.readExact(s, (int) Math.min(Sizes.Sector, s.getLength()));
        if (header.length < Sizes.Sector ||
            EndianUtilities.toUInt32LittleEndian(header, 0) != HostedSparseExtentHeader.VmdkMagicNumber) {
            s.setPosition(0);
            _descriptor = new DescriptorFile(s);
            if (_access != FileAccess.Read) {
                _descriptor.setContentId(_rng.nextInt());
                s.setPosition(0);
                _descriptor.write(s);
                s.setLength(s.getPosition());
            }

        } else {
            // This is a sparse disk extent, hopefully with embedded descriptor...
            HostedSparseExtentHeader hdr = HostedSparseExtentHeader.read(header, 0);
            if (hdr.DescriptorOffset != 0) {
                Stream descriptorStream = new SubStream(s,
                                                                  hdr.DescriptorOffset * Sizes.Sector,
                                                                  hdr.DescriptorSize * Sizes.Sector);
                _descriptor = new DescriptorFile(descriptorStream);
                if (_access != FileAccess.Read) {
                    _descriptor.setContentId(_rng.nextInt());
                    descriptorStream.setPosition(0);
                    _descriptor.write(descriptorStream);
                    byte[] blank = new byte[(int) (descriptorStream.getLength() - descriptorStream.getPosition())];
                    descriptorStream.write(blank, 0, blank.length);
                }

            }

        }
    }

}
