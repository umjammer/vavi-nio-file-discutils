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

package discUtils.vhd;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import discUtils.core.FileLocator;
import discUtils.core.Geometry;
import discUtils.core.VirtualDiskExtent;
import discUtils.core.VirtualDiskLayer;
import discUtils.core.internal.LocalFileLocator;
import discUtils.core.internal.Utilities;
import discUtils.streams.MappedStream;
import discUtils.streams.SparseStream;
import discUtils.streams.SubStream;
import discUtils.streams.ZeroStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.Stream;


/**
 * Represents a single .VHD file.
 */
public final class DiskImageFile extends VirtualDiskLayer {

    /**
     * The VHD file's dynamic header (if not static).
     */
    private DynamicHeader dynamicHeader;

    /**
     * The object that can be used to locate relative file paths.
     */
    private FileLocator fileLocator;

    /**
     * The file name of this VHD.
     */
    private String fileName;

    /**
     * The stream containing the VHD file.
     */
    private Stream fileStream;

    /**
     * The VHD file's footer.
     */
    private Footer footer;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private Ownership ownership;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        fileStream = stream;

        readFooter(true);

        readHeaders();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownership Indicates if the new instance should control the
     *            lifetime of the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownership) {
        fileStream = stream;
        this.ownership = ownership;

        readFooter(true);

        readHeaders();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param path The file path to open.
     * @param access Controls how the file can be accessed.
     */
    public DiskImageFile(String path, FileAccess access) {
        this(new LocalFileLocator(Utilities.getDirectoryFromPath(path)), Utilities.getFileFromPath(path), access);
    }

    DiskImageFile(FileLocator locator, String path, Stream stream, Ownership ownsStream) {
        this(stream, ownsStream);

        fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
        fileName = locator.getFileFromPath(path);
    }

    DiskImageFile(FileLocator locator, String path, FileAccess access) {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        fileStream = locator.open(path, FileMode.Open, access, share);
        ownership = Ownership.Dispose;

        try {
            fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
            fileName = locator.getFileFromPath(path);

            readFooter(true);

            readHeaders();
        } catch (Exception e) {
            try {
                fileStream.close();
            } catch (IOException f) {
                f.printStackTrace();
            }
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public long getCapacity() {
        return footer.currentSize;
    }

    /**
     * Gets the timestamp for this file (when it was created).
     */
    public long getCreationTimestamp() {
        return footer.timestamp;
    }

    /**
     * Gets the extent that comprises this file.
     */
    @Override
    public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> result = new ArrayList<>();
        result.add(new DiskExtent(this));
        return result;
    }

    /**
     * Gets the full path to this disk layer, or empty string.
     */
    @Override
    public String getFullPath() {
        if (fileLocator != null && fileName != null) {
            return fileLocator.getFullPath(fileName);
        }

        return "";
    }

    /**
     * Gets the geometry of the virtual disk.
     */
    @Override
    public Geometry getGeometry() {
        return footer.geometry;
    }

    /**
     * Gets detailed information about the VHD file.
     */
    public DiskImageFileInfo getInformation() {
        return new DiskImageFileInfo(footer, dynamicHeader, fileStream);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    @Override
    public boolean isSparse() {
        return footer.diskType != FileType.Fixed;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    @Override
    public boolean needsParent() {
        return footer.diskType == FileType.Differencing;
    }

    /**
     * Gets the unique id of the parent disk.
     */
    public UUID getParentUniqueId() {
        return dynamicHeader == null ? new UUID(0L, 0L) : dynamicHeader.parentUniqueId;
    }

    @Override
    public FileLocator getRelativeFileLocator() {
        return fileLocator;
    }

    long getStoredSize() {
        return fileStream.getLength();
    }

    /**
     * Gets the unique id of this file.
     */
    public UUID getUniqueId() {
        return footer.uniqueId;
    }

    /**
     * Initializes a stream as a fixed-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeFixed(Stream stream, Ownership ownsStream, long capacity) {
        return initializeFixed(stream, ownsStream, capacity, null);
    }

    /**
     * Initializes a stream as a fixed-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeFixed(Stream stream, Ownership ownsStream, long capacity, Geometry geometry) {
        initializeFixedInternal(stream, capacity, geometry);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Initializes a stream as a dynamically-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeDynamic(Stream stream, Ownership ownsStream, long capacity) {
        return initializeDynamic(stream, ownsStream, capacity, null, DynamicHeader.DefaultBlockSize);
    }

    /**
     * Initializes a stream as a dynamically-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeDynamic(Stream stream, Ownership ownsStream, long capacity, Geometry geometry) {
        return initializeDynamic(stream, ownsStream, capacity, geometry, DynamicHeader.DefaultBlockSize);
    }

    /**
     * Initializes a stream as a dynamically-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param blockSize The size of each block (unit of allocation).
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeDynamic(Stream stream, Ownership ownsStream, long capacity, long blockSize) {
        return initializeDynamic(stream, ownsStream, capacity, null, blockSize);
    }

    /**
     * Initializes a stream as a dynamically-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param capacity The desired capacity of the new disk.
     * @param geometry The desired geometry of the new disk, or {@code null} for
     *            default.
     * @param blockSize The size of each block (unit of allocation).
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeDynamic(Stream stream,
                                                  Ownership ownsStream,
                                                  long capacity,
                                                  Geometry geometry,
                                                  long blockSize) {
        initializeDynamicInternal(stream, capacity, geometry, blockSize);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Initializes a stream as a differencing disk VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of
     *            the stream.
     * @param parent The disk this file is a different from.
     * @param parentAbsolutePath The full path to the parent disk.
     * @param parentRelativePath The relative path from the new disk to the
     *            parent disk.
     * @param parentModificationTimeUtc The time the parent disk's file was last
     *            modified (from file system).
     * @return An object that accesses the stream as a VHD file.
     */
    public static DiskImageFile initializeDifferencing(Stream stream,
                                                       Ownership ownsStream,
                                                       DiskImageFile parent,
                                                       String parentAbsolutePath,
                                                       String parentRelativePath,
                                                       long parentModificationTimeUtc) {
        initializeDifferencingInternal(stream, parent, parentAbsolutePath, parentRelativePath, parentModificationTimeUtc);
        return new DiskImageFile(stream, ownsStream);
    }

    /**
     * Opens the content of the disk image file as a stream.
     *
     * @param parent The parent file's content (if any).
     * @param ownsParent Whether the created stream assumes ownership of parent
     *            stream.
     * @return The new content stream.
     */
    @Override
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        return doOpenContent(parent, ownsParent);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return list of candidate file locations.
     */
    @Override
    public List<String> getParentLocations() {
        return getParentLocations(fileLocator);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @param basePath The full path to this file.
     * @return list of candidate file locations.
     * @deprecated Use {@link #getParentLocations()} by preference
     */
    public List<String> getParentLocations(String basePath) {
        return getParentLocations(new LocalFileLocator(basePath));
    }

    static DiskImageFile initializeFixed(FileLocator locator, String path, long capacity, Geometry geometry) {
        DiskImageFile result = null;

        try (Stream stream = locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
            initializeFixedInternal(stream, capacity, geometry);
            result = new DiskImageFile(locator, path, stream, Ownership.Dispose);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        return result;
    }

    static DiskImageFile initializeDynamic(FileLocator locator, String path, long capacity, Geometry geometry, long blockSize) {
        DiskImageFile result = null;

        try (Stream stream = locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
            initializeDynamicInternal(stream, capacity, geometry, blockSize);
            result = new DiskImageFile(locator, path, stream, Ownership.Dispose);
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
        return result;
    }

    DiskImageFile createDifferencing(FileLocator fileLocator, String path) {
        Stream stream = fileLocator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None);
        String fullPath = this.fileLocator.getFullPath(fileName);
        String relativePath = fileLocator.makeRelativePath(this.fileLocator, fileName);
        long lastWriteTime = this.fileLocator.getLastWriteTimeUtc(fileName);
        initializeDifferencingInternal(stream, this, fullPath, relativePath, lastWriteTime);
        return new DiskImageFile(fileLocator, path, stream, Ownership.Dispose);
    }

    MappedStream doOpenContent(SparseStream parent, Ownership ownsParent) {
        if (footer.diskType == FileType.Fixed) {
            if (parent != null && ownsParent == Ownership.Dispose) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }

            return new SubStream(fileStream, 0, fileStream.getLength() - 512);
        }

        if (footer.diskType == FileType.Dynamic) {
            if (parent != null && ownsParent == Ownership.Dispose) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }

            return new DynamicStream(fileStream,
                                     dynamicHeader,
                                     footer.currentSize,
                                     new ZeroStream(footer.currentSize),
                                     Ownership.Dispose);
        }

        if (parent == null) {
            parent = new ZeroStream(footer.currentSize);
            ownsParent = Ownership.Dispose;
        }

        return new DynamicStream(fileStream, dynamicHeader, footer.currentSize, parent, ownsParent);
    }

    /**
     * Disposes of underlying resources.
     */
    @Override
    public void close() throws IOException {
        if (ownership == Ownership.Dispose && fileStream != null) {
            fileStream.close();
        }

        fileStream = null;
    }

    private static void initializeFixedInternal(Stream stream, long capacity, Geometry geometry) {
        if (geometry == null) {
            geometry = Geometry.fromCapacity(capacity);
        }

        Footer footer = new Footer(geometry, capacity, FileType.Fixed);
        footer.updateChecksum();
        byte[] sector = new byte[Sizes.Sector];
        footer.toBytes(sector, 0);
        stream.position(MathUtilities.roundUp(capacity, Sizes.Sector));
        stream.write(sector, 0, sector.length);
        stream.setLength(stream.position());
        stream.position(0);
    }

    private static void initializeDynamicInternal(Stream stream, long capacity, Geometry geometry, long blockSize) {
        if (blockSize > Integer.MAX_VALUE || blockSize < 0) {
            throw new IndexOutOfBoundsException("Must be in the range 0 to Integer.MAX_VALUE");
        }

        if (geometry == null) {
            geometry = Geometry.fromCapacity(capacity);
        }

        Footer footer = new Footer(geometry, capacity, FileType.Dynamic);
        footer.dataOffset = 512; // Offset of Dynamic Header
        footer.updateChecksum();
        byte[] footerBlock = new byte[512];
        footer.toBytes(footerBlock, 0);

        DynamicHeader dynamicHeader = new DynamicHeader(-1, 1024 + 512, (int) blockSize, capacity);
        dynamicHeader.updateChecksum();
        byte[] dynamicHeaderBlock = new byte[1024];
        dynamicHeader.toBytes(dynamicHeaderBlock, 0);

        int batSize = (dynamicHeader.maxTableEntries * 4 + Sizes.Sector - 1) / Sizes.Sector * Sizes.Sector;
        byte[] bat = new byte[batSize];
        Arrays.fill(bat, (byte) 0xFF);

        stream.position(0);
        stream.write(footerBlock, 0, 512);
        stream.write(dynamicHeaderBlock, 0, 1024);
        stream.write(bat, 0, batSize);
        stream.write(footerBlock, 0, 512);
    }

    private static void initializeDifferencingInternal(Stream stream,
                                                       DiskImageFile parent,
                                                       String parentAbsolutePath,
                                                       String parentRelativePath,
                                                       long parentModificationTimeUtc) {
        Footer footer = new Footer(parent.getGeometry(), parent.footer.currentSize, FileType.Differencing);
        footer.dataOffset = 512; // Offset of Dynamic Header
        footer.originalSize = parent.footer.originalSize;
        footer.updateChecksum();
        byte[] footerBlock = new byte[512];
        footer.toBytes(footerBlock, 0);

        long tableOffset = 512 + 1024; // Footer + Header

        int blockSize = parent.dynamicHeader == null ? DynamicHeader.DefaultBlockSize : parent.dynamicHeader.blockSize;

        DynamicHeader dynamicHeader = new DynamicHeader(-1, tableOffset, blockSize, footer.currentSize);
        int batSize = (dynamicHeader.maxTableEntries * 4 + Sizes.Sector - 1) / Sizes.Sector * Sizes.Sector;
        dynamicHeader.parentUniqueId = parent.getUniqueId();
        dynamicHeader.parentTimestamp = parentModificationTimeUtc;
        dynamicHeader.parentUnicodeName = Utilities.getFileFromPath(parentAbsolutePath);
        dynamicHeader.parentLocators[7].platformCode = ParentLocator.PlatformCodeWindowsAbsoluteUnicode;
        dynamicHeader.parentLocators[7].platformDataSpace = 512;
        dynamicHeader.parentLocators[7].platformDataLength = parentAbsolutePath.length() * 2;
        dynamicHeader.parentLocators[7].platformDataOffset = tableOffset + batSize;
        dynamicHeader.parentLocators[6].platformCode = ParentLocator.PlatformCodeWindowsRelativeUnicode;
        dynamicHeader.parentLocators[6].platformDataSpace = 512;
        dynamicHeader.parentLocators[6].platformDataLength = parentRelativePath.length() * 2;
        dynamicHeader.parentLocators[6].platformDataOffset = tableOffset + batSize + 512;
        dynamicHeader.updateChecksum();
        byte[] dynamicHeaderBlock = new byte[1024];
        dynamicHeader.toBytes(dynamicHeaderBlock, 0);

        byte[] platformLocator1 = new byte[512];
        byte[] bytes = parentAbsolutePath.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, platformLocator1, 0, bytes.length);
        byte[] platformLocator2 = new byte[512];
        bytes = parentRelativePath.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, platformLocator2, 0, bytes.length);

        byte[] bat = new byte[batSize];
        Arrays.fill(bat, (byte) 0xFF);

        stream.position(0);
        stream.write(footerBlock, 0, 512);
        stream.write(dynamicHeaderBlock, 0, 1024);
        stream.write(bat, 0, batSize);
        stream.write(platformLocator1, 0, 512);
        stream.write(platformLocator2, 0, 512);
        stream.write(footerBlock, 0, 512);
    }

    /**
     * Gets the locations of the parent file.
     *
     * @param fileLocator The file locator to use.
     * @return list of candidate file locations.
     */
    private List<String> getParentLocations(FileLocator fileLocator) {
        if (!needsParent()) {
            throw new UnsupportedOperationException("Only differencing disks contain parent locations");
        }

        if (fileLocator == null) {
            // Use working directory by default
            fileLocator = new LocalFileLocator("");
        }

        List<String> absPaths = new ArrayList<>(8);
        List<String> relPaths = new ArrayList<>(8);
        for (ParentLocator pl : dynamicHeader.parentLocators) {
            if (ParentLocator.PlatformCodeWindowsAbsoluteUnicode.equals(pl.platformCode) ||
                ParentLocator.PlatformCodeWindowsRelativeUnicode.equals(pl.platformCode)) {
                fileStream.position(pl.platformDataOffset);
                byte[] buffer = StreamUtilities.readExact(fileStream, pl.platformDataLength);
                String locationVal = new String(buffer, StandardCharsets.UTF_16LE);
//Debug.println(locationVal + ", " + pl.platformCode + ", "+ StringUtil.getDump(locationVal));
                if (ParentLocator.PlatformCodeWindowsAbsoluteUnicode.equals(pl.platformCode)) {
                    absPaths.add(locationVal);
                } else {
                    relPaths.add(fileLocator.resolveRelativePath(locationVal));
                }
            }
        }

        // Order the paths to put absolute paths first
        List<String> paths = new ArrayList<>(absPaths.size() + relPaths.size() + 1);
        paths.addAll(absPaths);
        paths.addAll(relPaths);

        // As a back-up, try to infer from the parent name...
        if (paths.size() == 0) {
            paths.add(fileLocator.resolveRelativePath(dynamicHeader.parentUnicodeName));
        }

        return paths;
    }

    private void readFooter(boolean fallbackToFront) {
        fileStream.position(fileStream.getLength() - Sizes.Sector);
        byte[] sector = StreamUtilities.readExact(fileStream, Sizes.Sector);

        footer = Footer.fromBytes(sector, 0);

        if (!footer.isValid()) {
            if (!fallbackToFront) {
                throw new dotnet4j.io.IOException("Corrupt VHD file - invalid footer at end (did not check front of file)");
            }

            fileStream.position(0);
            StreamUtilities.readExact(fileStream, sector, 0, Sizes.Sector);

            footer = Footer.fromBytes(sector, 0);
            if (!footer.isValid()) {
                throw new dotnet4j.io.IOException("Failed to find a valid VHD footer at start or end of file - VHD file is corrupt");
            }
        }
    }

    private void readHeaders() {
        long pos = footer.dataOffset;
        while (pos != -1) {
            fileStream.position(pos);
            Header hdr = Header.fromStream(fileStream);
            if (DynamicHeader.HeaderCookie.equals(hdr.cookie)) {
                fileStream.position(pos);
                dynamicHeader = DynamicHeader.fromStream(fileStream);
                if (!dynamicHeader.isValid()) {
                    throw new dotnet4j.io.IOException("Invalid Dynamic Disc Header");
                }
            }

            pos = hdr.dataOffset;
        }
    }
}
