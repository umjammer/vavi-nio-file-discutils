//
// Copyright (c) 2008-2012, Kenneth Bell
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

package discUtils.vhdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import discUtils.core.DiscFileLocator;
import discUtils.core.DiscFileSystem;
import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskClass;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Ownership;
import dotnet4j.util.compat.Tuple;
import dotnet4j.io.FileAccess;
import dotnet4j.io.Stream;


/**
 * Represents a VHDX-backed disk.
 */
public final class Disk extends VirtualDisk {

    /**
     * The stream representing the disk's contents.
     */
    private SparseStream content;

    /**
     * The list of files that make up the disk.
     */
    private List<Tuple<DiskImageFile, Ownership>> files;

    /**
     * Initializes a new instance of the Disk class. Differencing disks are not
     * supported.
     *
     * @param stream The stream to read.
     * @param ownsStream Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public Disk(Stream stream, Ownership ownsStream) {
        files = new ArrayList<>();
        files.add(new Tuple<>(new DiskImageFile(stream, ownsStream), Ownership.Dispose));
        if (files.get(0).getItem1().needsParent()) {
            throw new UnsupportedOperationException("Differencing disks cannot be opened from a stream");
        }
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param path The path to the disk image.
     */
    public Disk(String path) {
        DiskImageFile file = new DiskImageFile(path, FileAccess.ReadWrite);
        files = new ArrayList<>();
        files.add(new Tuple<>(file, Ownership.Dispose));
        resolveFileChain();
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param path The path to the disk image.
     * @param access The access requested to the disk.
     */
    public Disk(String path, FileAccess access) {
        DiskImageFile file = new DiskImageFile(path, access);
        files = new ArrayList<>();
        files.add(new Tuple<>(file, Ownership.Dispose));
        resolveFileChain();
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param fileSystem The file system containing the disk.
     * @param path The file system relative path to the disk.
     * @param access The access requested to the disk.
     */
    public Disk(DiscFileSystem fileSystem, String path, FileAccess access) {
        FileLocator fileLocator = new DiscFileLocator(fileSystem, Utilities.getDirectoryFromPath(path));
        DiskImageFile file = new DiskImageFile(fileLocator, Utilities.getFileFromPath(path), access);
        files = new ArrayList<>();
        files.add(new Tuple<>(file, Ownership.Dispose));
        resolveFileChain();
    }

    /**
     * Initializes a new instance of the Disk class.
     *
     * The disks should be ordered with the first file referencing the second,
     * etc. The final file must not require any parent.
     *
     * @param files The set of image files.
     * @param ownsFiles Indicates if the new instance controls the lifetime of
     *            the image files.
     */
    public Disk(List<DiskImageFile> files, Ownership ownsFiles) {
        if (files == null || files.size() == 0) {
            throw new IllegalArgumentException("At least one file must be provided");
        }

        if (files.get(files.size() - 1).needsParent()) {
            throw new IllegalArgumentException("Final image file needs a parent");
        }

        List<Tuple<DiskImageFile, Ownership>> tempList = new ArrayList<>(files.size());
        for (int i = 0; i < files.size() - 1; ++i) {
            if (!files.get(i).needsParent()) {
                throw new IllegalArgumentException(String.format("File at index %d does not have a parent disk", i));
            }

            if (!files.get(i).getParentUniqueId().equals(files.get(i + 1).getUniqueId())) {
                throw new IllegalArgumentException(String
                        .format("File at index %d is not the parent of file at index %d - Unique Ids don't match", i + 1, i));
            }

            tempList.add(new Tuple<>(files.get(i), ownsFiles));
        }
        tempList.add(new Tuple<>(files.get(files.size() - 1), ownsFiles));
        this.files = tempList;
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param locator The locator to access relative files.
     * @param path The path to the disk image.
     * @param access The access requested to the disk.
     */
    Disk(FileLocator locator, String path, FileAccess access) {
        DiskImageFile file = new DiskImageFile(locator, path, access);
        files = new ArrayList<>();
        files.add(new Tuple<>(file, Ownership.Dispose));
        resolveFileChain();
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are not
     * supported.
     *
     * @param file The file containing the disk.
     * @param ownsFile Indicates if the new instance should control the lifetime
     *            of the file.
     */
    private Disk(DiskImageFile file, Ownership ownsFile) {
        files = new ArrayList<>();
        files.add(new Tuple<>(file, ownsFile));
        resolveFileChain();
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param file The file containing the disk.
     * @param ownsFile Indicates if the new instance should control the lifetime
     *            of the file.
     * @param parentLocator Object used to locate the parent disk.
     * @param parentPath Path to the parent disk (if required).
     */
    private Disk(DiskImageFile file, Ownership ownsFile, FileLocator parentLocator, String parentPath) {
        files = new ArrayList<>();
        files.add(new Tuple<>(file, ownsFile));
        if (file.needsParent()) {
            files.add(new Tuple<>(new DiskImageFile(parentLocator, parentPath, FileAccess.Read), Ownership.Dispose));
            resolveFileChain();
        }
    }

    /**
     * Initializes a new instance of the Disk class. Differencing disks are
     * supported.
     *
     * @param file The file containing the disk.
     * @param ownsFile Indicates if the new instance should control the lifetime
     *            of the file.
     * @param parentFile The file containing the disk's parent.
     * @param ownsParent Indicates if the new instance should control the
     *            lifetime of the parentFile.
     */
    private Disk(DiskImageFile file, Ownership ownsFile, DiskImageFile parentFile, Ownership ownsParent) throws IOException {
        files = new ArrayList<>();
        files.add(new Tuple<>(file, ownsFile));
        if (file.needsParent()) {
            files.add(new Tuple<>(parentFile, ownsParent));
            resolveFileChain();
        } else {
            if (parentFile != null && ownsParent == Ownership.Dispose) {
                parentFile.close();
            }
        }
    }

    /**
     * Gets the size of the disk's logical blocks (aka sector size), in bytes.
     */
    public int getBlockSize() {
        return (int) files.get(0).getItem1().getLogicalSectorSize();
    }

    /**
     * Gets the capacity of the disk (in bytes).
     */
    public long getCapacity() {
        return files.get(0).getItem1().getCapacity();
    }

    /**
     * Gets the content of the disk as a stream. Note the returned stream is not
     * guaranteed to be at any particular position. The actual position will
     * depend on the last partition table/file system activity, since all access
     * to the disk contents pass through a single stream instance. Set the
     * stream position before accessing the stream.
     */
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
    public VirtualDiskClass getDiskClass() {
        return VirtualDiskClass.HardDisk;
    }

    /**
     * Gets information about the type of disk. This property provides access to
     * meta-data about the disk format, for example whether the BIOS geometry is
     * preserved in the disk file.
     */
    public VirtualDiskTypeInfo getDiskTypeInfo() {
        return DiskFactory.makeDiskTypeInfo(files.get(files.size() - 1).getItem1().isSparse() ? "dynamic" : "fixed");
    }

    /**
     * Gets the geometry of the disk.
     */
    public Geometry getGeometry() {
        return files.get(0).getItem1().getGeometry();
    }

    /**
     * Gets the layers that make up the disk.
     */
    public List<VirtualDiskLayer> getLayers() {
        return files.stream().map(file -> file.getItem1()).collect(Collectors.toList());
    }

    /**
     * Initializes a stream as a fixed-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VHDX file.
     */
    public static Disk initializeFixed(Stream stream, Ownership ownsStream, long capacity) {
        return initializeFixed(stream, ownsStream, capacity, null);
    }

    /**
     * Initializes a stream as a fixed-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @return An object that accesses the stream as a VHDX file.
     */
    public static Disk initializeFixed(Stream stream,
                                       Ownership ownsStream,
                                       long capacity,
                                       Geometry geometry) {
        return new Disk(DiskImageFile.initializeFixed(stream, ownsStream, capacity, geometry), Ownership.Dispose);
    }

    /**
     * Initializes a stream as a dynamically-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VHDX file.
     */
    public static Disk initializeDynamic(Stream stream, Ownership ownsStream, long capacity) {
        return new Disk(DiskImageFile.initializeDynamic(stream, ownsStream, capacity), Ownership.Dispose);
    }

    /**
     * Initializes a stream as a dynamically-sized VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param blockSize The size of each block (unit of allocation).
     * @return An object that accesses the stream as a VHDX file.
     */
    public static Disk initializeDynamic(Stream stream,
                                         Ownership ownsStream,
                                         long capacity,
                                         long blockSize) {
        return new Disk(DiskImageFile.initializeDynamic(stream, ownsStream, capacity, blockSize), Ownership.Dispose);
    }

    /**
     * Creates a new VHDX differencing disk file.
     *
     * @param path The path to the new disk file.
     * @param parentPath The path to the parent disk file.
     * @return An object that accesses the new file as a Disk.
     */
    public static Disk initializeDifferencing(String path, String parentPath) {
        LocalFileLocator parentLocator = new LocalFileLocator(Utilities.getDirectoryFromPath(parentPath));
        String parentFileName = Utilities.getFileFromPath(parentPath);
        DiskImageFile newFile;

        try (DiskImageFile parent = new DiskImageFile(parentLocator, parentFileName, FileAccess.Read)) {
            LocalFileLocator locator = new LocalFileLocator(Utilities.getDirectoryFromPath(path));
            newFile = parent.createDifferencing(locator, Utilities.getFileFromPath(path));
            return new Disk(newFile, Ownership.Dispose, parentLocator, parentFileName);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Initializes a stream as a differencing disk VHDX file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the {@code stream} .
     * @param parent The disk this file is a different from.
     * @param ownsParent Indicates if the new instance controls the lifetime of
     *            the {@code parent} file.
     * @param parentAbsolutePath The full path to the parent disk.
     * @param parentRelativePath The relative path from the new disk to the
     *            parent disk.
     * @param parentModificationTime The time the parent disk's file was last
     *            modified (from file system).
     * @return An object that accesses the stream as a VHDX file.
     */
    public static Disk initializeDifferencing(Stream stream,
                                              Ownership ownsStream,
                                              DiskImageFile parent,
                                              Ownership ownsParent,
                                              String parentAbsolutePath,
                                              String parentRelativePath,
                                              long parentModificationTime) throws IOException {
        DiskImageFile file = DiskImageFile.initializeDifferencing(stream,
                                                                  ownsStream,
                                                                  parent,
                                                                  parentAbsolutePath,
                                                                  parentRelativePath,
                                                                  parentModificationTime);
        return new Disk(file, Ownership.Dispose, parent, ownsParent);
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) {
        FileLocator locator = new DiscFileLocator(fileSystem, Utilities.getDirectoryFromPath(path));
        DiskImageFile file = files.get(0).getItem1().createDifferencing(locator, Utilities.getFileFromPath(path));
        return new Disk(file, Ownership.Dispose);
    }

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public VirtualDisk createDifferencingDisk(String path) {
        FileLocator locator = new LocalFileLocator(Utilities.getDirectoryFromPath(path));
        DiskImageFile file = files.get(0).getItem1().createDifferencing(locator, Utilities.getFileFromPath(path));
        return new Disk(file, Ownership.Dispose);
    }

    static Disk initializeFixed(FileLocator fileLocator,
                                       String path,
                                       long capacity,
                                       Geometry geometry) {
        return new Disk(DiskImageFile.initializeFixed(fileLocator, path, capacity, geometry), Ownership.Dispose);
    }

    static Disk initializeDynamic(FileLocator fileLocator,
                                         String path,
                                         long capacity,
                                         long blockSize) {
        return new Disk(DiskImageFile.initializeDynamic(fileLocator, path, capacity, blockSize), Ownership.Dispose);
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        try {
            if (content != null) {
                content.close();
                content = null;
            }

            if (files != null) {
                for (Tuple<DiskImageFile, Ownership> record : files) {
                    if (record.getItem2() == Ownership.Dispose) {
                        record.getItem1().close();
                    }
                }
                files = null;
            }
        } finally {
            super.close();
        }
    }

    private void resolveFileChain() {
        DiskImageFile file = files.get(files.size() - 1).getItem1();
        while (file.needsParent()) {
            FileLocator fileLocator = file.getRelativeFileLocator();
            boolean found = false;
            for (String testPath : file.getParentLocations()) {
                if (fileLocator.exists(testPath)) {
                    DiskImageFile newFile = new DiskImageFile(fileLocator, testPath, FileAccess.Read);
                    if (!newFile.getUniqueId().equals(file.getParentUniqueId())) {
                        throw new dotnet4j.io.IOException(String
                                .format("Invalid disk chain found looking for parent with id %s, found %s with id %s",
                                        file.getParentUniqueId(),
                                        newFile.getFullPath(),
                                        newFile.getUniqueId()));
                    }

                    file = newFile;
                    files.add(new Tuple<>(file, Ownership.Dispose));
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new dotnet4j.io.IOException(String.format("Failed to find parent for disk '%s'", file.getFullPath()));
            }
        }
    }
}
