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

package DiscUtils.Udf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import DiscUtils.Core.Vfs.VfsFileSystemFacade;
import DiscUtils.Core.Vfs.VfsReadOnlyFileSystem;
import DiscUtils.Iso9660.BaseVolumeDescriptor;
import DiscUtils.Iso9660.IsoUtilities;
import DiscUtils.Streams.StreamBuffer;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.Ownership;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


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

            bvd = new BaseVolumeDescriptor(buffer, 0);
            if (bvd.StandardIdentifier.equals("NSR02") || bvd.StandardIdentifier.equals("NSR03")) {
                foundUdfMarker = true;
            } else if (bvd.StandardIdentifier.equals("BEA01") || bvd.StandardIdentifier.equals("BOOT2") ||
                bvd.StandardIdentifier.equals("CD001") || bvd.StandardIdentifier.equals("CDW02") ||
                bvd.StandardIdentifier.equals("TEA01")) {} else {
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
        private final Stream _data;

        private LogicalVolumeDescriptor _lvd;

        private PrimaryVolumeDescriptor _pvd;

        private final int _sectorSize;

        public VfsUdfReader(Stream data) {
            super(null);
            _data = data;
            if (!detect(data)) {
                throw new IllegalArgumentException("Stream is not a recognized UDF format");
            }

            // Try a number of possible sector sizes, from most common.
            if (probeSectorSize(2048)) {
                _sectorSize = 2048;
            } else if (probeSectorSize(512)) {
                _sectorSize = 512;
            } else if (probeSectorSize(4096)) {
                _sectorSize = 4096;
            } else if (probeSectorSize(1024)) {
                _sectorSize = 1024;
            } else {
                throw new IllegalArgumentException("Unable to detect physical media sector size");
            }
            initialize();
        }

        public VfsUdfReader(Stream data, int sectorSize) {
            super(null);
            _data = data;
            _sectorSize = sectorSize;
            if (!detect(data)) {
                throw new IllegalArgumentException("Stream is not a recognized UDF format");
            }

            initialize();
        }

        public String getFriendlyName() {
            return "OSTA Universal Disk Format";
        }

        public String getVolumeLabel() {
            return _lvd.LogicalVolumeIdentifier;
        }

        public List<ExtendedAttribute> getExtendedAttributes(String path) {
            List<ExtendedAttribute> result = new ArrayList<>();
            File file = getFile(path);
            for (ExtendedAttributeRecord record : file.getExtendedAttributes()) {
                ImplementationUseExtendedAttributeRecord implRecord = record instanceof ImplementationUseExtendedAttributeRecord ? (ImplementationUseExtendedAttributeRecord) record
                                                                                                                                 : (ImplementationUseExtendedAttributeRecord) null;
                if (implRecord != null) {
                    result.add(new ExtendedAttribute(implRecord.ImplementationIdentifier.Identifier,
                                                     implRecord.ImplementationUseData));
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
            return File.fromDescriptor(getContext(), dirEntry.FileLocation);
        }

        private void initialize() {
            UdfContext context = new UdfContext();
            context.PhysicalPartitions = new HashMap<>();
            context.PhysicalSectorSize = _sectorSize;
            context.LogicalPartitions = new ArrayList<>();
            setContext(context);

            IBuffer dataBuffer = new StreamBuffer(_data, Ownership.None);

            AnchorVolumeDescriptorPointer avdp = AnchorVolumeDescriptorPointer
                    .fromStream(_data, 256, _sectorSize, AnchorVolumeDescriptorPointer.class);

            int sector = avdp.MainDescriptorSequence.Location;
            boolean terminatorFound = false;
            while (!terminatorFound) {
                _data.setPosition(sector * (long) _sectorSize);

                DescriptorTag[] dt = new DescriptorTag[1];
                if (!DescriptorTag.tryFromStream(_data, dt)) {
                    break;
                }

                switch (dt[0]._TagIdentifier) {
                case PrimaryVolumeDescriptor:
                    _pvd = PrimaryVolumeDescriptor.fromStream(_data, sector, _sectorSize, PrimaryVolumeDescriptor.class);
                    break;
                case ImplementationUseVolumeDescriptor:
                    // Not used
                    break;
                case PartitionDescriptor:
                    PartitionDescriptor pd = PartitionDescriptor
                            .fromStream(_data, sector, _sectorSize, PartitionDescriptor.class);
                    if (getContext().PhysicalPartitions.containsKey(pd.PartitionNumber)) {
                        throw new IOException("Duplicate partition number reading UDF Partition Descriptor");
                    }

                    getContext().PhysicalPartitions.put(pd.PartitionNumber, new PhysicalPartition(pd, dataBuffer, _sectorSize));
                    break;
                case LogicalVolumeDescriptor:
                    _lvd = LogicalVolumeDescriptor.fromStream(_data, sector, _sectorSize, LogicalVolumeDescriptor.class);
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
            for (int i = 0; i < _lvd.PartitionMaps.length; ++i) {
                // Convert logical partition descriptors into actual partition objects
                getContext().LogicalPartitions.add(LogicalPartition.fromDescriptor(getContext(), _lvd, i));
            }
            byte[] fsdBuffer = UdfUtilities.readExtent(getContext(), _lvd.getFileSetDescriptorLocation());
            if (DescriptorTag.isValid(fsdBuffer, 0)) {
                FileSetDescriptor fsd = EndianUtilities.<FileSetDescriptor> toStruct(FileSetDescriptor.class, fsdBuffer, 0);
                setRootDirectory((Directory) File.fromDescriptor(getContext(), fsd.RootDirectoryIcb));
            }
        }

        private boolean probeSectorSize(int size) {
            if (_data.getLength() < 257 * (long) size) {
                return false;
            }

            _data.setPosition(256 * (long) size);
            DescriptorTag[] dt = new DescriptorTag[1];
            boolean result = !DescriptorTag.tryFromStream(_data, dt);
            if (result) {
                return false;
            }

            return dt[0]._TagIdentifier == TagIdentifier.AnchorVolumeDescriptorPointer && dt[0].TagLocation == 256;
        }
    }
}
