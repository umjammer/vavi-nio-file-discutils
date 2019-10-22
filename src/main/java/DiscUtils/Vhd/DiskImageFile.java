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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import DiscUtils.Core.FileLocator;
import DiscUtils.Core.Geometry;
import DiscUtils.Core.VirtualDiskExtent;
import DiscUtils.Core.VirtualDiskLayer;
import DiscUtils.Core.Internal.LocalFileLocator;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Streams.MappedStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.ZeroStream;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileAccess;
import moe.yo3explorer.dotnetio4j.FileMode;
import moe.yo3explorer.dotnetio4j.FileShare;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a single .VHD file.
 */
public final class DiskImageFile extends VirtualDiskLayer {
    /**
     * The VHD file's dynamic header (if not static).
     */
    private DynamicHeader _dynamicHeader;

    /**
     * The object that can be used to locate relative file paths.
     */
    private FileLocator _fileLocator;

    /**
     * The file name of this VHD.
     */
    private String _fileName;

    /**
     * The stream containing the VHD file.
     */
    private Stream _fileStream;

    /**
     * The VHD file's footer.
     */
    private Footer _footer;

    /**
     * Indicates if this object controls the lifetime of the stream.
     */
    private Ownership _ownership;

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     */
    public DiskImageFile(Stream stream) {
        _fileStream = stream;
        readFooter(true);
        readHeaders();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param stream The stream to interpret.
     * @param ownership Indicates if the new instance should control the lifetime of
     *            the stream.
     */
    public DiskImageFile(Stream stream, Ownership ownership) {
        _fileStream = stream;
        _ownership = ownership;
        readFooter(true);
        readHeaders();
    }

    /**
     * Initializes a new instance of the DiskImageFile class.
     *
     * @param path The file path to open.
     * @param access Controls how the file can be accessed.
     */
    public DiskImageFile(String path, FileAccess access) throws IOException {
        this(new LocalFileLocator(Paths.get(path).getParent() == null ? "" : Paths.get(path).getParent().toString()),
             Paths.get(path).getFileName().toString(),
             access);
    }

    public DiskImageFile(FileLocator locator, String path, Stream stream, Ownership ownsStream) {
        this(stream, ownsStream);
        _fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
        _fileName = locator.getFileFromPath(path);
    }

    public DiskImageFile(FileLocator locator, String path, FileAccess access) throws IOException {
        FileShare share = access == FileAccess.Read ? FileShare.Read : FileShare.None;
        _fileStream = locator.open(path, FileMode.Open, access, share);
        _ownership = Ownership.Dispose;
        try {
            _fileLocator = locator.getRelativeLocator(locator.getDirectoryFromPath(path));
            _fileName = locator.getFileFromPath(path);
            readFooter(true);
            readHeaders();
        } catch (Exception __dummyCatchVar0) {
            try {
                _fileStream.close();
            } catch (IOException e) {
                throw new moe.yo3explorer.dotnetio4j.IOException(e);
            }
            throw __dummyCatchVar0;
        }

    }

    public long getCapacity() {
        return _footer.CurrentSize;
    }

    /**
     * Gets the timestamp for this file (when it was created).
     */
    public long getCreationTimestamp() {
        return _footer.Timestamp;
    }

    /**
     * Gets the extent that comprises this file.
     */
    public List<VirtualDiskExtent> getExtents() {
        List<VirtualDiskExtent> result = new ArrayList<>();
        result.add(new DiskExtent(this));
        return result;
    }

    /**
     * Gets the full path to this disk layer, or empty string.
     */
    public String getFullPath() {
        if (_fileLocator != null && _fileName != null) {
            return _fileLocator.getFullPath(_fileName);
        }

        return "";
    }

    /**
     * Gets the geometry of the virtual disk.
     */
    public Geometry getGeometry() {
        return _footer.Geometry;
    }

    /**
     * Gets detailed information about the VHD file.
     */
    public DiskImageFileInfo getInformation() {
        return new DiskImageFileInfo(_footer, _dynamicHeader, _fileStream);
    }

    /**
     * Gets a value indicating if the layer only stores meaningful sectors.
     */
    public boolean isSparse() {
        return _footer.DiskType != FileType.Fixed;
    }

    /**
     * Gets a value indicating whether the file is a differencing disk.
     */
    public boolean needsParent() {
        return _footer.DiskType == FileType.Differencing;
    }

    /**
     * Gets the unique id of the parent disk.
     */
    public UUID getParentUniqueId() {
        return _dynamicHeader == null ? new UUID(0L, 0L) : _dynamicHeader.ParentUniqueId;
    }

    public FileLocator getRelativeFileLocator() {
        return _fileLocator;
    }

    public long getStoredSize() {
        return _fileStream.getLength();
    }

    /**
     * Gets the unique id of this file.
     */
    public UUID getUniqueId() {
        return _footer.UniqueId;
    }

    /**
     * Initializes a stream as a fixed-sized VHD file.
     *
     * @param stream The stream to initialize.
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
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
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
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
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
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
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
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
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
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
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
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
     * @param ownsStream Indicates if the new instance controls the lifetime of the
     *            stream.
     * @param parent The disk this file is a different from.
     * @param parentAbsolutePath The full path to the parent disk.
     * @param parentRelativePath The relative path from the new disk to the parent
     *            disk.
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
    public SparseStream openContent(SparseStream parent, Ownership ownsParent) {
        return doOpenContent(parent, ownsParent);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @return Array of candidate file locations.
     */
    public List<String> getParentLocations() {
        return getParentLocations(_fileLocator);
    }

    /**
     * Gets the location of the parent file, given a base path.
     *
     * @param basePath The full path to this file.
     * @return Array of candidate file locations.
     */
    public List<String> getParentLocations(String basePath) {
        return getParentLocations(new LocalFileLocator(basePath));
    }

    public static DiskImageFile initializeFixed(FileLocator locator, String path, long capacity, Geometry geometry)
            throws IOException {
        DiskImageFile result = null;

        try (Stream stream = locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
            initializeFixedInternal(stream, capacity, geometry);
            result = new DiskImageFile(locator, path, stream, Ownership.Dispose);
        }
        return result;
    }

    public static DiskImageFile initializeDynamic(FileLocator locator,
                                                  String path,
                                                  long capacity,
                                                  Geometry geometry,
                                                  long blockSize)
            throws IOException {
        DiskImageFile result = null;

        try (Stream stream = locator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None)) {
            initializeDynamicInternal(stream, capacity, geometry, blockSize);
            result = new DiskImageFile(locator, path, stream, Ownership.Dispose);
        }
        return result;
    }

    public DiskImageFile createDifferencing(FileLocator fileLocator, String path) throws IOException {
        Stream stream = fileLocator.open(path, FileMode.Create, FileAccess.ReadWrite, FileShare.None);
        String fullPath = _fileLocator.getFullPath(_fileName);
        String relativePath = fileLocator.makeRelativePath(_fileLocator, _fileName);
        long lastWriteTime = _fileLocator.getLastWriteTimeUtc(_fileName);
        initializeDifferencingInternal(stream, this, fullPath, relativePath, lastWriteTime);
        return new DiskImageFile(fileLocator, path, stream, Ownership.Dispose);
    }

    public MappedStream doOpenContent(SparseStream parent, Ownership ownsParent) {
        if (_footer.DiskType == FileType.Fixed) {
            if (parent != null && ownsParent == Ownership.Dispose) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
            }

            return new SubStream(_fileStream, 0, _fileStream.getLength() - 512);
        }

        if (_footer.DiskType == FileType.Dynamic) {
            if (parent != null && ownsParent == Ownership.Dispose) {
                try {
                    parent.close();
                } catch (IOException e) {
                    throw new moe.yo3explorer.dotnetio4j.IOException(e);
                }
            }

            return new DynamicStream(_fileStream,
                                     _dynamicHeader,
                                     _footer.CurrentSize,
                                     new ZeroStream(_footer.CurrentSize),
                                     Ownership.Dispose);
        }

        if (parent == null) {
            parent = new ZeroStream(_footer.CurrentSize);
            ownsParent = Ownership.Dispose;
        }

        return new DynamicStream(_fileStream, _dynamicHeader, _footer.CurrentSize, parent, ownsParent);
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        try {
            if (_ownership == Ownership.Dispose && _fileStream != null) {
                _fileStream.close();
            }

            _fileStream = null;

        } finally {
            super.close();
        }
    }

    private static void initializeFixedInternal(Stream stream, long capacity, Geometry geometry) {
        if (geometry == null) {
            geometry = Geometry.fromCapacity(capacity);
        }

        Footer footer = new Footer(geometry, capacity, FileType.Fixed);
        footer.updateChecksum();
        byte[] sector = new byte[Sizes.Sector];
        footer.toBytes(sector, 0);
        stream.setPosition(MathUtilities.roundUp(capacity, Sizes.Sector));
        stream.write(sector, 0, sector.length);
        stream.setLength(stream.getPosition());
        stream.setPosition(0);
    }

    private static void initializeDynamicInternal(Stream stream, long capacity, Geometry geometry, long blockSize) {
        if (blockSize > Integer.MAX_VALUE || blockSize < 0) {
            throw new IndexOutOfBoundsException("Must be in the range 0 to Integer.MAX_VALUE");
        }

        if (geometry == null) {
            geometry = Geometry.fromCapacity(capacity);
        }

        Footer footer = new Footer(geometry, capacity, FileType.Dynamic);
        footer.DataOffset = 512; // Offset of Dynamic Header
        footer.updateChecksum();
        byte[] footerBlock = new byte[512];
        footer.toBytes(footerBlock, 0);

        DynamicHeader dynamicHeader = new DynamicHeader(-1, 1024 + 512, (int) blockSize, capacity);
        dynamicHeader.updateChecksum();
        byte[] dynamicHeaderBlock = new byte[1024];
        dynamicHeader.toBytes(dynamicHeaderBlock, 0);

        int batSize = (dynamicHeader.MaxTableEntries * 4 + Sizes.Sector - 1) / Sizes.Sector * Sizes.Sector;
        byte[] bat = new byte[batSize];
        for (int i = 0; i < bat.length; ++i) {
            bat[i] = (byte) 0xFF;
        }

        stream.setPosition(0);
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
        Footer footer = new Footer(parent.getGeometry(), parent._footer.CurrentSize, FileType.Differencing);
        footer.DataOffset = 512; // Offset of Dynamic Header
        footer.OriginalSize = parent._footer.OriginalSize;
        footer.updateChecksum();
        byte[] footerBlock = new byte[512];
        footer.toBytes(footerBlock, 0);

        long tableOffset = 512 + 1024; // Footer + Header

        int blockSize = parent._dynamicHeader == null ? DynamicHeader.DefaultBlockSize : parent._dynamicHeader.BlockSize;

        DynamicHeader dynamicHeader = new DynamicHeader(-1, tableOffset, blockSize, footer.CurrentSize);
        int batSize = (dynamicHeader.MaxTableEntries * 4 + Sizes.Sector - 1) / Sizes.Sector * Sizes.Sector;
        dynamicHeader.ParentUniqueId = parent.getUniqueId();
        dynamicHeader.ParentTimestamp = parentModificationTimeUtc;
        dynamicHeader.ParentUnicodeName = Utilities.getFileFromPath(parentAbsolutePath);
        dynamicHeader.ParentLocators[7].PlatformCode = ParentLocator.PlatformCodeWindowsAbsoluteUnicode;
        dynamicHeader.ParentLocators[7].PlatformDataSpace = 512;
        dynamicHeader.ParentLocators[7].PlatformDataLength = parentAbsolutePath.length() * 2;
        dynamicHeader.ParentLocators[7].PlatformDataOffset = tableOffset + batSize;
        dynamicHeader.ParentLocators[6].PlatformCode = ParentLocator.PlatformCodeWindowsRelativeUnicode;
        dynamicHeader.ParentLocators[6].PlatformDataSpace = 512;
        dynamicHeader.ParentLocators[6].PlatformDataLength = parentRelativePath.length() * 2;
        dynamicHeader.ParentLocators[6].PlatformDataOffset = tableOffset + batSize + 512;
        dynamicHeader.updateChecksum();
        byte[] dynamicHeaderBlock = new byte[1024];
        dynamicHeader.toBytes(dynamicHeaderBlock, 0);

        byte[] platformLocator1 = new byte[512];
        System.arraycopy(parentAbsolutePath
                .getBytes(Charset.forName("UTF-16LE")), 0, platformLocator1, 0, parentAbsolutePath.length());
        byte[] platformLocator2 = new byte[512];
        System.arraycopy(parentRelativePath
                .getBytes(Charset.forName("UTF-16LE")), 0, platformLocator2, 0, parentRelativePath.length());

        byte[] bat = new byte[batSize];
        for (int i = 0; i < bat.length; ++i) {
            bat[i] = (byte) 0xFF;
        }

        stream.setPosition(0);
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
     * @return Array of candidate file locations.
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
        for (ParentLocator pl : _dynamicHeader.ParentLocators) {
            if (ParentLocator.PlatformCodeWindowsAbsoluteUnicode.equals(pl.PlatformCode) ||
                ParentLocator.PlatformCodeWindowsRelativeUnicode.equals(pl.PlatformCode)) {
                _fileStream.setPosition(pl.PlatformDataOffset);
                byte[] buffer = StreamUtilities.readExact(_fileStream, pl.PlatformDataLength);
                String locationVal = new String(buffer, Charset.forName("UTF-16LE"));
                if (ParentLocator.PlatformCodeWindowsAbsoluteUnicode.equals(pl.PlatformCode)) {
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
            paths.add(fileLocator.resolveRelativePath(_dynamicHeader.ParentUnicodeName));
        }

        return paths;
    }

    private void readFooter(boolean fallbackToFront) {
        _fileStream.setPosition(_fileStream.getLength() - Sizes.Sector);
        byte[] sector = StreamUtilities.readExact(_fileStream, Sizes.Sector);
        _footer = Footer.fromBytes(sector, 0);
        if (!_footer.isValid()) {
            if (!fallbackToFront) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Corrupt VHD file - invalid footer at end (did not check front of file)");
            }

            _fileStream.setPosition(0);
            StreamUtilities.readExact(_fileStream, sector, 0, Sizes.Sector);
            _footer = Footer.fromBytes(sector, 0);
            if (!_footer.isValid()) {
                throw new moe.yo3explorer.dotnetio4j.IOException("Failed to find a valid VHD footer at start or end of file - VHD file is corrupt");
            }
        }
    }

    private void readHeaders() {
        long pos = _footer.DataOffset;
        while (pos != -1) {
            _fileStream.setPosition(pos);
            Header hdr = Header.fromStream(_fileStream);
            if (DynamicHeader.HeaderCookie.equals(hdr.Cookie)) {
                _fileStream.setPosition(pos);
                _dynamicHeader = DynamicHeader.fromStream(_fileStream);
                if (!_dynamicHeader.isValid()) {
                    throw new moe.yo3explorer.dotnetio4j.IOException("Invalid Dynamic Disc Header");
                }
            }

            pos = hdr.DataOffset;
        }
    }
}
