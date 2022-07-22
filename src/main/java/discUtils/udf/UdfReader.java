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

package discUtils.udf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import discUtils.core.vfs.VfsFileSystemFacade;
import discUtils.core.vfs.VfsReadOnlyFileSystem;
import discUtils.iso9660.BaseVolumeDescriptor;
import discUtils.iso9660.IsoUtilities;
import discUtils.streams.StreamBuffer;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.Ownership;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


/**
 * Class for accessing OSTA Universal Disk Format file systems.
 */
public final class UdfReader extends VfsFileSystemFacade {
    /**
     * Initializes a new instance of the UdfReader class.
     *
     * @param data The stream containing the UDF file system.
     */
    public UdfReader(Stream data) {
        super(new VfsUdfReader(data));
    }

    /**
     * Initializes a new instance of the UdfReader class.
     *
     * @param data The stream containing the UDF file system.
     * @param sectorSize The sector size of the physical media.
     */
    public UdfReader(Stream data, int sectorSize) {
        super(new VfsUdfReader(data, sectorSize));
    }

    /**
     * Detects if a stream contains a valid UDF file system.
     *
     * @param data The stream to inspect.
     * @return {@code true} if the stream contains a UDF file system, else false.
     */
    public static boolean detect(Stream data) {
        if (data.getLength() < IsoUtilities.SectorSize) {
            return false;
        }

        long vdpos = 0x8000;
        // Skip lead-in
        byte[] buffer = new byte[IsoUtilities.SectorSize];
        boolean validDescriptor = true;
        boolean foundUdfMarker = false;
        BaseVolumeDescriptor bvd;
        while (validDescriptor) {
            data.setPosition(vdpos);
            int numRead = StreamUtilities.readMaximum(data, buffer, 0, IsoUtilities.SectorSize);
            if (numRead != IsoUtilities.SectorSize) {
                break;
            }

            try {
                bvd = new BaseVolumeDescriptor(buffer, 0);
            } catch (NoSuchElementException e) {
                return false;
            }
            if (bvd.standardIdentifier.equals("NSR02") || bvd.standardIdentifier.equals("NSR03")) {
                foundUdfMarker = true;
            } else if (bvd.standardIdentifier.equals("BEA01") || bvd.standardIdentifier.equals("BOOT2") ||
                bvd.standardIdentifier.equals("CD001") || bvd.standardIdentifier.equals("CDW02") ||
                bvd.standardIdentifier.equals("TEA01")) {} else {
                validDescriptor = false;
            }
            vdpos += IsoUtilities.SectorSize;
        }
        return foundUdfMarker;
    }

    /**
     * Gets UDF extended attributes for a file or directory.
     *
     * @param path Path to the file or directory.
     * @return Array of extended attributes, which may be empty or {@code null} if
     *         there are no extended attributes.
     */
    public List<ExtendedAttribute> getExtendedAttributes(String path) {
        VfsUdfReader realFs = VfsUdfReader.class.cast(getRealFileSystem());
        return realFs.getExtendedAttributes(path);
    }

    private final static class VfsUdfReader extends VfsReadOnlyFileSystem<FileIdentifier, File, Directory, UdfContext> {

        private final Stream data;

        private LogicalVolumeDescriptor lvd;

        private PrimaryVolumeDescriptor pvd;

        private final int sectorSize;

        public VfsUdfReader(Stream data) {
            super(null);
            this.data = data;
            if (!detect(data)) {
                throw new IllegalArgumentException("Stream is not a recognized UDF format");
            }

            // Try a number of possible sector sizes, from most common.
            if (probeSectorSize(2048)) {
                sectorSize = 2048;
            } else if (probeSectorSize(512)) {
                sectorSize = 512;
            } else if (probeSectorSize(4096)) {
                sectorSize = 4096;
            } else if (probeSectorSize(1024)) {
                sectorSize = 1024;
            } else {
                throw new IllegalArgumentException("Unable to detect physical media sector size");
            }
            initialize();
        }

        public VfsUdfReader(Stream data, int sectorSize) {
            super(null);
            this.data = data;
            this.sectorSize = sectorSize;
            if (!detect(data)) {
                throw new IllegalArgumentException("Stream is not a recognized UDF format");
            }

            initialize();
        }

        public String getFriendlyName() {
            return "OSTA Universal Disk Format";
        }

        public String getVolumeLabel() {
            return lvd.logicalVolumeIdentifier;
        }

        public List<ExtendedAttribute> getExtendedAttributes(String path) {
            List<ExtendedAttribute> result = new ArrayList<>();
            File file = getFile(path);
            for (ExtendedAttributeRecord record : file.getExtendedAttributes()) {
                ImplementationUseExtendedAttributeRecord implRecord = record instanceof ImplementationUseExtendedAttributeRecord ? (ImplementationUseExtendedAttributeRecord) record
                                                                                                                                 : null;
                if (implRecord != null) {
                    result.add(new ExtendedAttribute(implRecord.implementationIdentifier.identifier,
                                                     implRecord.implementationUseData));
                }

            }
            return result;
        }

        /**
         * Size of the Filesystem in bytes
         */
        public long getSize() {
            throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
        }

        /**
         * Used space of the Filesystem in bytes
         */
        public long getUsedSpace() {
            throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
        }

        /**
         * Available space of the Filesystem in bytes
         */
        public long getAvailableSpace() {
            throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
        }

        protected File convertDirEntryToFile(FileIdentifier dirEntry) {
            return File.fromDescriptor(getContext(), dirEntry.fileLocation);
        }

        private void initialize() {
            UdfContext context = new UdfContext();
            context.physicalPartitions = new HashMap<>();
            context.physicalSectorSize = sectorSize;
            context.logicalPartitions = new ArrayList<>();
            setContext(context);

            IBuffer dataBuffer = new StreamBuffer(data, Ownership.None);

            AnchorVolumeDescriptorPointer avdp = AnchorVolumeDescriptorPointer
                    .fromStream(data, 256, sectorSize, AnchorVolumeDescriptorPointer.class);

            int sector = avdp.mainDescriptorSequence.location;
            boolean terminatorFound = false;
            while (!terminatorFound) {
                data.setPosition(sector * (long) sectorSize);

                DescriptorTag[] dt = new DescriptorTag[1];
                if (!DescriptorTag.tryFromStream(data, dt)) {
                    break;
                }

                switch (dt[0].tagIdentifier) {
                case PrimaryVolumeDescriptor:
                    pvd = PrimaryVolumeDescriptor.fromStream(data, sector, sectorSize, PrimaryVolumeDescriptor.class);
                    break;
                case ImplementationUseVolumeDescriptor:
                    // Not used
                    break;
                case PartitionDescriptor:
                    PartitionDescriptor pd = PartitionDescriptor
                            .fromStream(data, sector, sectorSize, PartitionDescriptor.class);
                    if (getContext().physicalPartitions.containsKey(pd.partitionNumber)) {
                        throw new IOException("Duplicate partition number reading UDF Partition Descriptor");
                    }

                    getContext().physicalPartitions.put(pd.partitionNumber, new PhysicalPartition(pd, dataBuffer, sectorSize));
                    break;
                case LogicalVolumeDescriptor:
                    lvd = LogicalVolumeDescriptor.fromStream(data, sector, sectorSize, LogicalVolumeDescriptor.class);
                    break;
                case UnallocatedSpaceDescriptor:
                    // Not used for reading
                    break;
                case TerminatingDescriptor:
                    terminatorFound = true;
                    break;
                default:
                    break;
                }

                sector++;
            }
            for (int i = 0; i < lvd.partitionMaps.length; ++i) {
                // Convert logical partition descriptors into actual partition objects
                getContext().logicalPartitions.add(LogicalPartition.fromDescriptor(getContext(), lvd, i));
            }
            byte[] fsdBuffer = UdfUtilities.readExtent(getContext(), lvd.getFileSetDescriptorLocation());
            if (DescriptorTag.isValid(fsdBuffer, 0)) {
                FileSetDescriptor fsd = EndianUtilities.toStruct(FileSetDescriptor.class, fsdBuffer, 0);
                setRootDirectory((Directory) File.fromDescriptor(getContext(), fsd.rootDirectoryIcb));
            }
        }

        private boolean probeSectorSize(int size) {
            if (data.getLength() < 257 * (long) size) {
                return false;
            }

            data.setPosition(256 * (long) size);
            DescriptorTag[] dt = new DescriptorTag[1];
            boolean result = !DescriptorTag.tryFromStream(data, dt);
            if (result) {
                return false;
            }

            return dt[0].tagIdentifier == TagIdentifier.AnchorVolumeDescriptorPointer && dt[0].tagLocation == 256;
        }
    }
}
