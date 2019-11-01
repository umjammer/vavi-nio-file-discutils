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

package DiscUtils.Core;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import vavi.util.Debug;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Internal.VirtualDiskFactory;
import DiscUtils.Core.Internal.VirtualDiskTransport;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Core.Partitions.GuidPartitionTable;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.FileAccess;


/**
 * Base class representing virtual hard disks.
 */
public abstract class VirtualDisk implements Serializable, Closeable {
    private VirtualDiskTransport _transport;

    /**
     * Finalizes an instance of the VirtualDisk class.
     */
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Gets the set of disk formats supported as an array of file extensions.
     */
    public static Collection<String> getSupportedDiskFormats() {
        return VirtualDiskManager.getSupportedDiskFormats();
    }

    /**
     * Gets the set of disk types supported, as an array of identifiers.
     */
    public static Collection<String> getSupportedDiskTypes() {
        return VirtualDiskManager.getSupportedDiskTypes();
    }

    /**
     * Gets the geometry of the disk.
     */
    public abstract Geometry getGeometry();

    /**
     * Gets the geometry of the disk as it is anticipated a hypervisor BIOS will
     * represent it.
     */
    public Geometry getBiosGeometry() {
        return Geometry.makeBiosSafe(getGeometry(), getCapacity());
    }

    /**
     * Gets the type of disk represented by this object.
     */
    public abstract VirtualDiskClass getDiskClass();

    /**
     * Gets the capacity of the disk (in bytes).
     */
    public abstract long getCapacity();

    /**
     * Gets the size of the disk's logical blocks (aka sector size), in bytes.
     */
    public int getBlockSize() {
        return Sizes.Sector;
    }

    /**
     * Gets the logical sector size of the disk, in bytes. This is an alias for the
     * {@code BlockSize} property.
     */
    public int getSectorSize() {
        return getBlockSize();
    }

    /**
     * Gets the content of the disk as a stream. Note the returned stream is not
     * guaranteed to be at any particular position. The actual position will depend
     * on the last partition table/file system activity, since all access to the
     * disk contents pass through a single stream instance. Set the stream position
     * before accessing the stream.
     */
    public abstract SparseStream getContent();

    /**
     * Gets the layers that make up the disk.
     */
    public abstract Iterable<VirtualDiskLayer> getLayers();

    /**
     * Gets or sets the Windows disk signature of the disk, which uniquely
     * identifies the disk.
     */
    public int getSignature() {
        return EndianUtilities.toInt32LittleEndian(getMasterBootRecord(), 0x01B8);
    }

    public void setSignature(int value) throws IOException {
        byte[] mbr = getMasterBootRecord();
        EndianUtilities.writeBytesLittleEndian(value, mbr, 0x01B8);
        setMasterBootRecord(mbr);
    }

    /**
     * Gets a value indicating whether the disk appears to have a valid partition
     * table. There is no reliable way to determine whether a disk has a valid
     * partition table. The 'guess' consists of checking for basic indicators and
     * looking for obviously invalid data, such as overlapping partitions.
     */
    public boolean isPartitioned() {
        return PartitionTable.isPartitioned(getContent());
    }

    /**
     * Gets the object that interprets the partition structure. It is theoretically
     * possible for a disk to contain two independent partition structures - a
     * BIOS/GPT one and an Apple one, for example. This method will return in order
     * of preference, a GUID partition table, a BIOS partition table, then in
     * undefined preference one of any other partition tables found. See
     * PartitionTable.GetPartitionTables to gain access to all the discovered
     * partition tables on a disk.
     */
    public PartitionTable getPartitions() {
        List<PartitionTable> tables = PartitionTable.getPartitionTables(this);
        if (tables == null || tables.size() == 0) {
            return null;
        }

        if (tables.size() == 1) {
            return tables.get(0);
        }

        PartitionTable best = null;
        int bestScore = -1;
        for (int i = 0; i < tables.size(); ++i) {
            int newScore = 0;
            if (tables.get(i) instanceof GuidPartitionTable) {
                newScore = 2;
            } else if (tables.get(i) instanceof BiosPartitionTable) {
                newScore = 1;
            }

            if (newScore > bestScore) {
                bestScore = newScore;
                best = tables.get(i);
            }

        }
        return best;
    }

    /**
     * Gets the parameters of the disk. Most of the parameters are also available
     * individually, such as DiskType and Capacity.
     */
    public VirtualDiskParameters getParameters() {
        return new VirtualDiskParameters();
    }

    /**
     * Gets information about the type of disk. This property provides access to
     * meta-data about the disk format, for example whether the BIOS geometry is
     * preserved in the disk file.
     */
    public abstract VirtualDiskTypeInfo getDiskTypeInfo();

    /**
     * Gets the set of supported variants of a type of virtual disk.
     *
     * @param type A type, as returned by {@link #getSupportedDiskTypes()} .
     * @return A collection of identifiers, or empty if there is no variant concept
     *         for this type of disk.
     */
    public static String[] getSupportedDiskVariants(String type) {
        return VirtualDiskManager.getTypeMap().get(type).getVariants();
    }

    /**
     * Gets information about disk type.
     *
     * @param type The disk type, as returned by {@link #getSupportedDiskTypes()} .
     * @param variant The variant of the disk type.
     * @return Information about the disk type.
     */
    public static VirtualDiskTypeInfo getDiskType(String type, String variant) {
        return VirtualDiskManager.getTypeMap().get(type).getDiskTypeInformation(variant);
    }

    /**
     * Create a new virtual disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param type The type of disk to create (see {@link #getSupportedDiskTypes()}
     *            ).
     * @param variant The variant of the type to create (see
     *            {@link #getSupportedDiskVariants(String)} ).
     * @param path The path (or URI) for the disk to create.
     * @param capacity The capacity of the new disk.
     * @param geometry The geometry of the new disk (or null).
     * @param parameters Untyped parameters controlling the creation process (TBD).
     * @return The newly created disk.
     */
    public static VirtualDisk createDisk(DiscFileSystem fileSystem,
                                         String type,
                                         String variant,
                                         String path,
                                         long capacity,
                                         Geometry geometry,
                                         Map<String, String> parameters)
            throws IOException {
        VirtualDiskFactory factory = VirtualDiskManager.getTypeMap().get(type);

        VirtualDiskParameters diskParams = new VirtualDiskParameters();
        diskParams.setAdapterType(GenericDiskAdapterType.Scsi);
        diskParams.setCapacity(capacity);
        diskParams.geometry = geometry;

        if (parameters != null) {
            for (String key : parameters.keySet()) {
                diskParams.extendedParameters.put(key, parameters.get(key));
            }
        }

        return factory.createDisk(new DiscFileLocator(fileSystem, Utilities.getDirectoryFromPath(path)),
                                  variant.toLowerCase(),
                                  Utilities.getFileFromPath(path),
                                  diskParams);
    }

    /**
     * Create a new virtual disk.
     *
     * @param type The type of disk to create (see {@link #getSupportedDiskTypes()}
     *            ).
     * @param variant The variant of the type to create (see
     *            {@link #getSupportedDiskVariants(String)} ).
     * @param path The path (or URI) for the disk to create.
     * @param capacity The capacity of the new disk.
     * @param geometry The geometry of the new disk (or null).
     * @param parameters Untyped parameters controlling the creation process (TBD).
     * @return The newly created disk.
     */
    public static VirtualDisk createDisk(String type,
                                         String variant,
                                         String path,
                                         long capacity,
                                         Geometry geometry,
                                         Map<String, String> parameters)
            throws IOException {
        return createDisk(type, variant, path, capacity, geometry, null, null, parameters);
    }

    /**
     * Create a new virtual disk.
     *
     * @param type The type of disk to create (see {@link #getSupportedDiskTypes()}
     *            ).
     * @param variant The variant of the type to create (see
     *            {@link #getSupportedDiskVariants(String)} ).
     * @param path The path (or URI) for the disk to create.
     * @param capacity The capacity of the new disk.
     * @param geometry The geometry of the new disk (or null).
     * @param user The user identity to use when accessing the {@code path} (or
     *            null).
     * @param password The password to use when accessing the {@code path} (or
     *            null).
     * @param parameters Untyped parameters controlling the creation process (TBD).
     * @return The newly created disk.
     */
    public static VirtualDisk createDisk(String type,
                                         String variant,
                                         String path,
                                         long capacity,
                                         Geometry geometry,
                                         String user,
                                         String password,
                                         Map<String, String> parameters)
            throws IOException {
        VirtualDiskParameters diskParams = new VirtualDiskParameters();
        diskParams.setAdapterType(GenericDiskAdapterType.Scsi);
        diskParams.setCapacity(capacity);
        diskParams.geometry = geometry;

        if (parameters != null) {
            for (String key : parameters.keySet()) {
                diskParams.extendedParameters.put(key, parameters.get(key));
            }
        }

        return createDisk(type, variant, path, diskParams, user, password);
    }

    /**
     * Create a new virtual disk.
     *
     * @param type The type of disk to create (see {@link #getSupportedDiskTypes()}
     *            ).
     * @param variant The variant of the type to create (see
     *            {@link #getSupportedDiskVariants(String)} ).
     * @param path The path (or URI) for the disk to create.
     * @param diskParameters Parameters controlling the capacity, geometry, etc of
     *            the new disk.
     * @param user The user identity to use when accessing the {@code path} (or
     *            null).
     * @param password The password to use when accessing the {@code path} (or
     *            null).
     * @return The newly created disk.
     */
    public static VirtualDisk createDisk(String type,
                                         String variant,
                                         String path,
                                         VirtualDiskParameters diskParameters,
                                         String user,
                                         String password)
            throws IOException {
        URI uri = pathToUri(path);
        VirtualDisk result = null;
        if (!VirtualDiskManager.getDiskTransports().containsKey(uri.getScheme().toUpperCase())) {
            throw new dotnet4j.io.FileNotFoundException(String.format("Unable to parse path '%s'", path));
        }

        VirtualDiskTransport transport = VirtualDiskManager.getDiskTransports().get(uri.getScheme().toUpperCase());
        transport.connect(uri, user, password);
        if (transport.isRawDisk()) {
            result = transport.openDisk(FileAccess.ReadWrite);
        } else {
            VirtualDiskFactory factory = VirtualDiskManager.getTypeMap().get(type);
            result = factory.createDisk(transport.getFileLocator(),
                                        variant.toLowerCase(),
                                        Utilities.getFileFromPath(path),
                                        diskParameters);
        }
        if (result != null) {
            result._transport = transport;
        }

        return result;
    }

    /**
     * Opens an existing virtual disk.
     *
     * @param path The path of the virtual disk to open, can be a URI.
     * @param access The desired access to the disk.
     * @return The Virtual Disk, or {@code null} if an unknown disk format.
     */
    public static VirtualDisk openDisk(String path, FileAccess access) throws IOException {
        return openDisk(path, null, access, null, null);
    }

    /**
     * Opens an existing virtual disk.
     *
     * @param path The path of the virtual disk to open, can be a URI.
     * @param access The desired access to the disk.
     * @param user The user name to use for authentication (if necessary).
     * @param password The password to use for authentication (if necessary).
     * @return The Virtual Disk, or {@code null} if an unknown disk format.
     */
    public static VirtualDisk openDisk(String path, FileAccess access, String user, String password) throws IOException {
        return openDisk(path, null, access, user, password);
    }

    /**
     * Opens an existing virtual disk.
     *
     * @param path The path of the virtual disk to open, can be a URI.
     * @param forceType Force the detected disk type ( {@code null} to detect).
     * @param access The desired access to the disk.
     * @param user The user name to use for authentication (if necessary).
     * @param password The password to use for authentication (if necessary).
     * @return The Virtual Disk, or {@code null} if an unknown disk format. The
     *         detected disk type can be forced by specifying a known disk type:
     *         RAW, VHD, VMDK, etc.
     */
    public static VirtualDisk openDisk(String path, String forceType, FileAccess access, String user, String password)
            throws IOException {
        URI uri = pathToUri(path);
        VirtualDisk result = null;
        if (!VirtualDiskManager.getDiskTransports().containsKey(uri.getScheme().toUpperCase())) {
            throw new dotnet4j.io.FileNotFoundException(String.format("Unable to parse path '%s'", path));
        }

        VirtualDiskTransport transport = VirtualDiskManager.getDiskTransports().get(uri.getScheme().toUpperCase());
        transport.connect(uri, user, password);
        if (transport.isRawDisk()) {
            result = transport.openDisk(access);
        } else {
            boolean foundFactory;
            VirtualDiskFactory factory;
            if (forceType != null && !forceType.isEmpty()) {
                foundFactory = VirtualDiskManager.getTypeMap().containsKey(forceType);
                factory = VirtualDiskManager.getTypeMap().get(forceType);
            } else {
                String pathString = uri.getPath();
                String extension = pathString.substring(pathString.lastIndexOf(".") + 1).toUpperCase();
                if (extension.startsWith(".")) {
                    extension = extension.substring(1);
                }

                foundFactory = VirtualDiskManager.getExtensionMap().containsKey(extension);
                factory = VirtualDiskManager.getExtensionMap().get(extension);
            }
            if (foundFactory) {
                result = factory.openDisk(transport.getFileLocator(), transport.getFileName(), access);
            }

        }
        if (result != null) {
            result._transport = transport;
        }

        return result;
    }

    /**
     * Opens an existing virtual disk, possibly from within an existing disk.
     *
     * @param fs The file system to open the disk on.
     * @param path The path of the virtual disk to open.
     * @param access The desired access to the disk.
     * @return The Virtual Disk, or {@code null} if an unknown disk format.
     */
    public static VirtualDisk openDisk(DiscFileSystem fs, String path, FileAccess access) throws IOException {
        if (fs == null) {
            return openDisk(path, access);
        }

        String extension = path.substring(path.lastIndexOf(".") + 1).toUpperCase();
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }

        if (VirtualDiskManager.getExtensionMap().containsKey(extension)) {
            return VirtualDiskManager.getExtensionMap().get(extension).openDisk(fs, path, access);
        }

        return null;
    }

    /**
     * Reads the first sector of the disk, known as the Master Boot Record.
     *
     * @return The MBR as a byte array.
     */
    public byte[] getMasterBootRecord() {
        byte[] sector = new byte[Sizes.Sector];
        long oldPos = getContent().getPosition();
        getContent().setPosition(0);
        StreamUtilities.readExact(getContent(), sector, 0, Sizes.Sector);
        getContent().setPosition(oldPos);
        return sector;
    }

    /**
     * Overwrites the first sector of the disk, known as the Master Boot Record.
     *
     * @param data The master boot record, must be 512 bytes in length.
     */
    public void setMasterBootRecord(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data");
        }

        if (data.length != Sizes.Sector) {
            throw new IllegalArgumentException("The Master Boot Record must be exactly 512 bytes in length " +
                Arrays.toString(data));
        }

        long oldPos = getContent().getPosition();
        getContent().setPosition(0);
        getContent().write(data, 0, Sizes.Sector);
        getContent().setPosition(oldPos);
    }

    /**
     * Create a new differencing disk, possibly within an existing disk.
     *
     * @param fileSystem The file system to create the disk on.
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public abstract VirtualDisk createDifferencingDisk(DiscFileSystem fileSystem, String path) throws IOException;

    /**
     * Create a new differencing disk.
     *
     * @param path The path (or URI) for the disk to create.
     * @return The newly created disk.
     */
    public abstract VirtualDisk createDifferencingDisk(String path) throws IOException;

    public static VirtualDiskLayer openDiskLayer(FileLocator locator, String path, FileAccess access) throws IOException {
        String extension = path.substring(path.lastIndexOf(".") + 1).toUpperCase();
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }

        if (VirtualDiskManager.getExtensionMap().containsKey(extension)) {
            return VirtualDiskManager.getExtensionMap().get(extension).openDiskLayer(locator, path, access);
        }
Debug.println(extension + " / " + VirtualDiskManager.getExtensionMap());
        return null;
    }

    /**
     * Disposes of underlying resources.
     */
    public void close() throws IOException {
        if (_transport != null) {
            _transport.close();
        }

        _transport = null;
    }

    private static URI pathToUri(String path) {
        if (Objects.isNull(path) || path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty " + path);
        }

        if (path.indexOf("://") > -1) {
            return URI.create(path);
        }

        path = Paths.get(path).toAbsolutePath().toString();

        // Built-in Uri class does cope well with query params on file Uris, so do some
        // parsing ourselves...
        if (path.length() >= 1 && path.charAt(0) == '\\') {
            URI builder = URI.create("file:" + path.replace('\\', '/'));
            return builder;
        }

        if (path.startsWith("//")) {
            URI builder = URI.create("file:" + path);
            return builder;
        }

        if (path.length() >= 2 && path.charAt(1) == ':') {
            URI builder = URI.create("file:///" + path.replace('\\', '/'));
            return builder;
        }

        return URI.create("file://" + path); // TODO "file:" added
    }
}
