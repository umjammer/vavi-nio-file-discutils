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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import discUtils.core.DiscFileLocator;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileLocator;
import discUtils.core.GenericDiskAdapterType;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.Tuple;


/**
 * Represents a VMDK-backed disk.
 */
public final class Disk extends VirtualDisk {

    public static final String ExtendedParameterKeyAdapterType = "VMDK.AdapterType";

    public static final String ExtendedParameterKeyCreateType = "VMDK.CreateType";

    /**
     * The stream representing the content of this disk.
     */
    private SparseStream content;

    /**
     * The list of files that make up the disk.
     */
    private final List<Tuple<VirtualDiskLayer, Ownership>> files;

    private String path;

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param path The path to the disk.
     * @param access The access requested to the disk.
     */
    public Disk(String path, FileAccess access) throws IOException {
        this(new LocalFileLocator(Utilities.getDirectoryFromPath(path)), path, access);
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * @param fileSystem The file system containing the disk.
     * @param path The file system relative path to the disk.
     * @param access The access requested to the disk.
     */
    public Disk(DiscFileSystem fileSystem, String path, FileAccess access) throws IOException {
        this.path = path;
        FileLocator fileLocator = new DiscFileLocator(fileSystem, Utilities.getDirectoryFromPath(path));
        files = new ArrayList<>();
        files.add(new Tuple<>(new DiskImageFile(fileLocator,
                Utilities.getFileFromPath(path),
                access),
                Ownership.Dispose));
        resolveFileChain();
    }

    /**
     * Initializes a new instance of the Disk class. Only monolithic sparse
     * streams are supported.
     *
     * @param stream The stream containing the VMDK file.
     * @param ownsStream Indicates if the new instances owns the stream.
     */
    public Disk(Stream stream, Ownership ownsStream) {
        if (stream instanceof FileStream) {
            path = ((FileStream) stream).getName();
        }

        files = new ArrayList<>();
        files.add(new Tuple<>(new DiskImageFile(stream, ownsStream), Ownership.Dispose));
    }

    Disk(DiskImageFile file, Ownership ownsStream) throws IOException {
        files = new ArrayList<>();
        boolean add = files.add(new Tuple<>(file, ownsStream));
        resolveFileChain();
    }

    Disk(FileLocator layerLocator, String path, FileAccess access) throws IOException {
        this.path = path;
        files = new ArrayList<>();
        files.add(new Tuple<>(new DiskImageFile(layerLocator, path, access), Ownership.Dispose));
        resolveFileChain();
    }

    /**
     * Gets the geometry of the disk as it is anticipated a hypervisor BIOS will
     * represent it.
     */
    @Override
    public Geometry getBiosGeometry() {
        VirtualDiskLayer item1 = files.get(files.size() - 1).getItem1();
        if (item1 instanceof DiskImageFile) {
            return ((DiskImageFile) item1).getBiosGeometry();
        } else {
            return Geometry.makeBiosSafe(item1.getGeometry(), getCapacity());
        }
    }

    /**
     * Gets the capacity of this disk (in bytes).
     */
    @Override
    public long getCapacity() {
        return files.get(0).getItem1().getCapacity();
    }

    /**
     * Gets the contents of this disk as a stream. Note the returned stream is
     * not guaranteed to be at any particular position. The actual position will
     * depend on the last partition table/file system activity, since all access
     * to the disk contents pass through a single stream instance. Set the
     * stream position before accessing the stream.
     */
    @Override
    public SparseStream getContent() {
        if (content == null) {
            SparseStream stream = null;
            for (int i = files.size() - 1; i >= 0; --i) {
                stream = files.get(i).getItem1().openContent(stream, Ownership.Dispose);
            }

            content = stream;
        }

        return content;
    }

    /**
     * Gets the type of disk represented by this object.
     */
    @Override
    public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.HardDisk;
    }

    /**
     * Gets information about the type of disk. This property provides access to
     * meta-data about the disk format, for example whether the BIOS geometry is
     * preserved in the disk file.
     */
    @Override
    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo(((DiskImageFile) files.get(files.size() - 1).getItem1()).getCreateType());
    }

    /**
     * Gets the Geometry of this disk.
     */
    @Override
    public Geometry getGeometry() {
        return files.get(files.size() - 1).getItem1().getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    @Override
    public List<VirtualDiskLayer> getLayers() {
        return files.stream().map(Tuple::getItem1).collect(Collectors.toList());
    }

    /**
     * Gets the links that make up the disk (type-safe version of Layers).
     */
    public List<DiskImageFile> getLinks() {
        return files.stream().map(file -> (DiskImageFile) file.getItem1()).collect(Collectors.toList());
    }

    /**
     * Gets the parameters of the disk. Most of the parameters are also
     * available individually, such as DiskType and Capacity.
     */
    @Override
    public VirtualDiskParameters getParameters() {
        DiskImageFile file = (DiskImageFile) files.get(files.size() - 1).getItem1();

        VirtualDiskParameters diskParams = new VirtualDiskParameters();
        diskParams.setDiskType(getDiskClass());
        diskParams.setCapacity(getCapacity());
        diskParams.geometry = getGeometry();
        diskParams.setBiosGeometry(getBiosGeometry());
        diskParams.setAdapterType(file.getAdapterType() == DiskAdapterType.Ide ? GenericDiskAdapterType.Ide
                                                                               : GenericDiskAdapterType.Scsi);

        diskParams.getExtendedParameters().put(ExtendedParameterKeyAdapterType, file.getAdapterType().toString());
        diskParams.getExtendedParameters().put(ExtendedParameterKeyCreateType, file.getCreateType().toString());

        return diskParams;
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param parameters The desired parameters for the new disk.
     * @return The newly created disk image.
     */
    public static Disk initialize(String path, DiskParameters parameters) throws IOException {
        return new Disk(DiskImageFile.initialize(path, parameters), Ownership.Dispose);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param type The type of virtual disk to create.
     * @return The newly created disk image.
     */
    public static Disk initialize(String path, long capacity, DiskCreateType type) throws IOException {
        return initialize(path, capacity, null, type);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @param type The type of virtual disk to create.
     * @return The newly created disk image.
     */
    public static Disk initialize(String path, long capacity, Geometry geometry, DiskCreateType type) throws IOException {
        return new Disk(DiskImageFile.initialize(path, capacity, geometry, type), Ownership.Dispose);
    }

    /**
     * Creates a new virtual disk at the specified location on a file system.
     *
     * @param fileSystem The file system to contain the disk.
     * @param path The file system path to the disk.
     * @param capacity The desired capacity of the new disk.
     * @param type The type of virtual disk to create.
     * @return The newly created disk image.
     */
    public static Disk initialize(DiscFileSystem fileSystem,
                                  String path,
                                  long capacity,
                                  DiskCreateType type) throws IOException {
        return new Disk(DiskImageFile.initialize(fileSystem, path, capacity, type), Ownership.Dispose);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param type The type of virtual disk to create.
     * @param adapterType The type of virtual disk adapter.
     * @return The newly created disk image.
     */
    public static Disk initialize(String path,
                                  long capacity,
                                  DiskCreateType type,
                                  DiskAdapterType adapterType) throws IOException {
        return initialize(path, capacity, null, type, adapterType);
    }

    /**
     * Creates a new virtual disk at the specified path.
     *
     * @param path The name of the VMDK to create.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @param type The type of virtual disk to create.
     * @param adapterType The type of virtual disk adapter.
     * @return The newly created disk image.
     */
    public static Disk initialize(String path,
                                  long capacity,
                                  Geometry geometry,
                                  DiskCreateType type,
                                  DiskAdapterType adapterType) throws IOException {
        return new Disk(DiskImageFile.initialize(path, capacity, geometry, type, adapterType), Ownership.Dispose);
    }

    /**
     * Creates a new virtual disk at the specified location on a file system.
     *
     * @param fileSystem The file system to contain the disk.
     * @param path The file system path to the disk.
     * @param capacity The desired capacity of the new disk.
     * @param type The type of virtual disk to create.
     * @param adapterType The type of virtual disk adapter.
     * @return The newly created disk image.
     */
    public static Disk initialize(DiscFileSystem fileSystem,
                                  String path,
                                  long capacity,
                                  DiskCreateType type,
                                  DiskAdapterType adapterType) throws IOException {
        return new Disk(DiskImageFile.initialize(fileSystem, path, capacity, type, adapterType), Ownership.Dispose);
    }

    /**
     * Creates a new virtual disk as a thin clone of an existing disk.
     *
     * @param path The path to the new disk.
     * @param type The type of disk to create.
     * @param parentPath The path to the parent disk.
     * @return The new disk.
     */
    public static Disk initializeDifferencing(String path, DiskCreateType type, String parentPath) throws IOException {
        return new Disk(DiskImageFile.initializeDifferencing(path, type, parentPath), Ownership.Dispose);
    }

    /**
     * Creates a new virtual disk as a thin clone of an existing disk.
     *
     * @param fileSystem The file system to contain the disk.
     * @param path The path to the new disk.
     * @param type The type of disk to create.
     * @param parentPath The path to the parent disk.
     * @return The new disk.
     */
    public static Disk initializeDifferencing(DiscFileSystem fileSystem,
                                              String path,
                                              DiskCreateType type,
                                              String parentPath) throws IOException {
        return new Disk(DiskImageFile.initializeDifferencing(fileSystem, path, type, parentPath), Ownership.Dispose);
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) throws IOException {
        return initializeDifferencing(fileSystem, path, diffDiskCreateType(files.get(0).getItem1()), this.path);
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    @Override
    public VirtualDisk createDifferencingDisk(String path) throws IOException {
        VirtualDiskLayer firstLayer = files.get(0).getItem1();
        return initializeDifferencing(path,
                                      diffDiskCreateType(firstLayer),
                                      firstLayer.getRelativeFileLocator().getFullPath(this.path));
    }

    static Disk initialize(FileLocator fileLocator, String path, DiskParameters parameters) throws IOException {
        return new Disk(DiskImageFile.initialize(fileLocator, path, parameters), Ownership.Dispose);
    }

    /**
     * Disposes of this instance.
     */
    @Override
    public void close() throws IOException {
        try {
            if (content != null) {
                content.close();
                content = null;

                for (Tuple<VirtualDiskLayer, Ownership> file : files) {
                    if (file.getItem2() == Ownership.Dispose) {
                        file.getItem1().close();
                    }
                }
            }
        } finally {
            super.close();
        }
    }

    private static DiskCreateType diffDiskCreateType(VirtualDiskLayer layer) {
        DiskImageFile vmdkLayer = layer instanceof DiskImageFile ? (DiskImageFile) layer : null;
        if (vmdkLayer != null) {
            return switch (vmdkLayer.getCreateType()) {
                case FullDevice, MonolithicFlat, MonolithicSparse, PartitionedDevice, StreamOptimized,
                     TwoGbMaxExtentFlat, TwoGbMaxExtentSparse -> DiskCreateType.MonolithicSparse;
                default -> DiskCreateType.VmfsSparse;
            };
        }

        return DiskCreateType.MonolithicSparse;
    }

    private void resolveFileChain() throws IOException {
        VirtualDiskLayer file = files.get(files.size() - 1).getItem1();

        while (file.needsParent()) {
            boolean foundParent = false;
            FileLocator locator = file.getRelativeFileLocator();

            for (String posParent : file.getParentLocations()) {
                if (locator.exists(posParent)) {
                    file = openDiskLayer(file.getRelativeFileLocator(), posParent, FileAccess.Read);
                    files.add(new Tuple<>(file, Ownership.Dispose));
                    foundParent = true;
                    break;
                }
            }

            if (!foundParent) {
                throw new dotnet4j.io.IOException("Parent disk not found");
            }
        }
    }
}
