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

package discUtils.iso9660;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.ClusterMap;
import discUtils.core.ClusterRoles;
import discUtils.core.DiscFileSystemOptions;
import discUtils.core.IClusterBasedFileSystem;
import discUtils.core.IUnixFileSystem;
import discUtils.core.InvalidFileSystemException;
import discUtils.core.UnixFileSystemInfo;
import discUtils.core.vfs.VfsReadOnlyFileSystem;
import discUtils.iso9660.rockRidge.RockRidgeExtension;
import discUtils.iso9660.susp.ExtensionSystemUseEntry;
import discUtils.iso9660.susp.GenericSuspExtension;
import discUtils.iso9660.susp.SharingProtocolSystemUseEntry;
import discUtils.iso9660.susp.SuspExtension;
import discUtils.iso9660.susp.SuspRecords;
import discUtils.iso9660.susp.SystemUseEntry;
import discUtils.streams.StreamExtent;
import discUtils.streams.SubStream;
import discUtils.streams.util.MathUtilities;
import discUtils.streams.util.Range;
import discUtils.streams.util.Sizes;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public class VfsCDReader extends VfsReadOnlyFileSystem<ReaderDirEntry, File, ReaderDirectory, IsoContext> implements
                         IClusterBasedFileSystem,
                         IUnixFileSystem {

    private static final Iso9660Variant[] DefaultVariantsNoJoliet = {
        Iso9660Variant.RockRidge, Iso9660Variant.Iso9660
    };

    private static final Iso9660Variant[] DefaultVariantsWithJoliet = {
        Iso9660Variant.Joliet, Iso9660Variant.RockRidge, Iso9660Variant.Iso9660
    };

    private byte[] bootCatalog;

    private BootVolumeDescriptor bootVolDesc;

    private final Stream data;

    private final boolean hideVersions;

    /**
     * Initializes a new instance of the VfsCDReader class.
     *
     * @param data The stream to read the ISO image from.
     * @param joliet Whether to read Joliet extensions.
     * @param hideVersions Hides version numbers (e.g. ";1") from the end of
     *            files.
     */
    public VfsCDReader(Stream data, boolean joliet, boolean hideVersions) {
        this(data, joliet ? DefaultVariantsWithJoliet : DefaultVariantsNoJoliet, hideVersions);
    }

    /**
     * Initializes a new instance of the VfsCDReader class.
     * <p>
     * The implementation considers each of the file system variants in
     * {@code variantProperties} and selects the first which is determined to be
     * present. In this example Joliet, then Rock Ridge, then vanilla iso9660
     * will be considered:
     *
     * <pre>
     * {@code
     *   VfsCDReader(stream, new Iso9660Variant[] {Joliet, rockRidge, iso9660}, true);
     * }
     * </pre>
     *
     * The iso9660 variant should normally be specified as the final entry in
     * the list. Placing it earlier in the list will effectively mask later
     * items and not including it may prevent some ISOs from being read.
     *
     * @param data The stream to read the ISO image from.
     * @param variantPriorities Which possible file system variants to use, and
     *            with which priority.
     * @param hideVersions Hides version numbers (e.g. ";1") from the end of
     *            files.
     */
    @SuppressWarnings("incomplete-switch")
    public VfsCDReader(Stream data, Iso9660Variant[] variantPriorities, boolean hideVersions) {
        super(new DiscFileSystemOptions());

        this.data = data;
        this.hideVersions = hideVersions;

        long vdpos = 0x8000; // Skip lead-in

        byte[] buffer = new byte[IsoUtilities.SectorSize];

        long pvdPos = 0;
        long svdPos = 0;

        BaseVolumeDescriptor bvd;
        do {
            data.position(vdpos);
            int numRead = data.read(buffer, 0, IsoUtilities.SectorSize);
            if (numRead != IsoUtilities.SectorSize) {
                break;
            }

            bvd = new BaseVolumeDescriptor(buffer, 0);

            if (!bvd.standardIdentifier.equals(BaseVolumeDescriptor.Iso9660StandardIdentifier)) {
                throw new InvalidFileSystemException("Volume is not ISO-9660");
            }

            switch (bvd.volumeDescriptorType) {
            case Boot:
                bootVolDesc = new BootVolumeDescriptor(buffer, 0);
                if (!bootVolDesc.getSystemId().equals(BootVolumeDescriptor.ElToritoSystemIdentifier)) {
                    bootVolDesc = null;
                }
                break;
            case Primary: // Primary Vol Descriptor
                pvdPos = vdpos;
                break;
            case Supplementary: // Supplementary Vol Descriptor
                svdPos = vdpos;
                break;
            case Partition: // Volume Partition Descriptor
                break;
            case SetTerminator: // Volume Descriptor Set Terminator
                break;
            }

            vdpos += IsoUtilities.SectorSize;
        } while (bvd.volumeDescriptorType != VolumeDescriptorType.SetTerminator);

        activeVariant = Iso9660Variant.None;
        for (Iso9660Variant variant : variantPriorities) {
            switch (variant) {
            case Joliet:
                if (svdPos != 0) {
                    data.position(svdPos);
                    data.read(buffer, 0, IsoUtilities.SectorSize);
                    SupplementaryVolumeDescriptor volDesc = new SupplementaryVolumeDescriptor(buffer, 0);

                    IsoContext context = new IsoContext();
                    context.setVolumeDescriptor(volDesc);
                    context.setDataStream(this.data);
                    setContext(context);
                    setRootDirectory(new ReaderDirectory(getContext(),
                                                         new ReaderDirEntry(getContext(), volDesc.rootDirectory)));
                    activeVariant = Iso9660Variant.Iso9660;
                }
                break;
            case RockRidge:
            case Iso9660:
                if (pvdPos != 0) {
                    data.position(pvdPos);
                    data.read(buffer, 0, IsoUtilities.SectorSize);
                    PrimaryVolumeDescriptor volDesc = new PrimaryVolumeDescriptor(buffer, 0);

                    IsoContext context = new IsoContext();
                    context.setVolumeDescriptor(volDesc);
                    context.setDataStream(this.data);
                    DirectoryRecord rootSelfRecord = readRootSelfRecord(context);

                    initializeSusp(context, rootSelfRecord);

                    if (variant == Iso9660Variant.Iso9660 ||
                        (variant == Iso9660Variant.RockRidge && context.getRockRidgeIdentifier() != null &&
                         !context.getRockRidgeIdentifier().isEmpty())) {
                        setContext(context);
                        setRootDirectory(new ReaderDirectory(context, new ReaderDirEntry(context, rootSelfRecord)));
                        activeVariant = variant;
                    }
                }
                break;
            }

            if (activeVariant != Iso9660Variant.None) {
                break;
            }
        }

        if (activeVariant == Iso9660Variant.None) {
            throw new IOException("None of the permitted ISO9660 file system variants was detected");
        }
    }

    private Iso9660Variant activeVariant = Iso9660Variant.None;

    public Iso9660Variant getActiveVariant() {
        return activeVariant;
    }

    public BootDeviceEmulation getBootEmulation() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return initialEntry.bootMediaType;
        }

        return BootDeviceEmulation.NoEmulation;
    }

    public long getBootImageStart() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return (long) initialEntry.imageStart * IsoUtilities.SectorSize;
        }

        return 0;
    }

    public int getBootLoadSegment() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return initialEntry.getLoadSegment();
        }

        return 0;
    }

    /**
     * Provides the friendly name for the CD filesystem.
     */
    @Override public String getFriendlyName() {
        return "ISO 9660 (CD-ROM)";
    }

    public boolean getHasBootImage() {
        if (bootVolDesc == null) {
            return false;
        }

        byte[] bootCatalog = getBootCatalog();
        if (bootCatalog == null) {
            return false;
        }

        BootValidationEntry entry = new BootValidationEntry(bootCatalog, 0);
        return entry.getChecksumValid();
    }

    /**
     * Gets the Volume Identifier.
     */
    @Override public String getVolumeLabel() {
        return getContext().getVolumeDescriptor().volumeIdentifier;
    }

    @Override public long getClusterSize() {
        return IsoUtilities.SectorSize;
    }

    @Override public long getTotalClusters() {
        return getContext().getVolumeDescriptor().volumeSpaceSize;
    }

    @Override public long clusterToOffset(long cluster) {
        return cluster * getClusterSize();
    }

    @Override public long offsetToCluster(long offset) {
        return offset / getClusterSize();
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override public long getSize() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override public long getUsedSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override public long getAvailableSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    @Override public List<Range> pathToClusters(String path) {
        ReaderDirEntry entry = getDirectoryEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("File not found" + path);
        }

        if (entry.getRecord().fileUnitSize != 0 || entry.getRecord().interleaveGapSize != 0) {
            throw new UnsupportedOperationException("Non-contiguous extents not supported");
        }

        return Collections.singletonList(new Range(entry.getRecord().locationOfExtent,
                MathUtilities.ceil(entry.getRecord().dataLength, IsoUtilities.SectorSize)));
    }

    @Override public List<StreamExtent> pathToExtents(String path) {
        ReaderDirEntry entry = getDirectoryEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("File not found " + path);
        }

        if (entry.getRecord().fileUnitSize != 0 || entry.getRecord().interleaveGapSize != 0) {
            throw new UnsupportedOperationException("Non-contiguous extents not supported");
        }

        return Collections.singletonList(new StreamExtent((long) entry.getRecord().locationOfExtent * IsoUtilities.SectorSize,
                entry.getRecord().dataLength));
    }

    @Override public ClusterMap buildClusterMap() {
        long totalClusters = getTotalClusters();
        @SuppressWarnings("unchecked")
        EnumSet<ClusterRoles>[] clusterToRole = new EnumSet[(int) totalClusters];
        Object[] clusterToFileId = new Object[(int) totalClusters];
        Map<Object, String[]> fileIdToPaths = new HashMap<>();
        forAllDirEntries("", (path, entry) -> {
            String[] paths = null;
            if (fileIdToPaths.containsKey(entry.getUniqueCacheId())) {
                paths = fileIdToPaths.get(entry.getUniqueCacheId());
            }

            if (paths == null) {
                fileIdToPaths.put(entry.getUniqueCacheId(),
                                  new String[] {
                                      path
                });
            } else {
                String[] newPaths = new String[paths.length + 1];
                System.arraycopy(paths, 0, newPaths, 0, paths.length);
                newPaths[paths.length] = path;
                fileIdToPaths.put(entry.getUniqueCacheId(), newPaths);
            }
            if (((ReaderDirEntry) entry).getRecord().fileUnitSize != 0 ||
                ((ReaderDirEntry) entry).getRecord().interleaveGapSize != 0) {
                throw new UnsupportedOperationException("Non-contiguous extents not supported");
            }

            long clusters = MathUtilities.ceil(((ReaderDirEntry) entry).getRecord().dataLength,
                                               IsoUtilities.SectorSize);
            for (long i = 0; i < clusters; ++i) {
                clusterToRole[(int) i + ((ReaderDirEntry) entry).getRecord().locationOfExtent] = EnumSet
                        .of(ClusterRoles.DataFile);
                clusterToFileId[(int) i + ((ReaderDirEntry) entry).getRecord().locationOfExtent] = entry
                        .getUniqueCacheId();
            }
        });
        return new ClusterMap(clusterToRole, clusterToFileId, fileIdToPaths);
    }

    @Override public UnixFileSystemInfo getUnixFileInfo(String path) {
        File file = getFile(path);
        return file.getUnixFileInfo();
    }

    public Stream openBootImage() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return new SubStream(data,
                    (long) initialEntry.imageStart * IsoUtilities.SectorSize,
                    (long) initialEntry.getSectorCount() * Sizes.Sector);
        }

        throw new UnsupportedOperationException("No valid boot image");
    }

    @Override protected File convertDirEntryToFile(ReaderDirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            return new ReaderDirectory(getContext(), dirEntry);
        }

        return new File(getContext(), dirEntry);
    }

    @Override protected String formatFileName(String name) {
        if (hideVersions) {
            int pos = name.lastIndexOf(';');
            if (pos > 0) {
                return name.substring(0, pos);
            }
        }

        return name;
    }

    private static void initializeSusp(IsoContext context, DirectoryRecord rootSelfRecord) {
        // Stage 1 - SUSP present?
        List<SuspExtension> extensions = new ArrayList<>();
        if (!SuspRecords.detectSharingProtocol(rootSelfRecord.systemUseData, 0)) {
            context.setSuspExtensions(new ArrayList<>());
            context.setSuspDetected(false);
            return;
        }

        context.setSuspDetected(true);
        SuspRecords suspRecords = new SuspRecords(context, rootSelfRecord.systemUseData, 0);
        // Stage 2 - Init general SUSP params
        SharingProtocolSystemUseEntry spEntry = (SharingProtocolSystemUseEntry) suspRecords.getEntries(null, "SP").get(0);
        context.setSuspSkipBytes(spEntry.getSystemAreaSkip());
        // Stage 3 - Init extensions
        List<SystemUseEntry> extensionEntries = suspRecords.getEntries(null, "ER");
        if (extensionEntries != null) {
            for (SystemUseEntry extension : extensionEntries) {
                String extensionIdentifier = ((ExtensionSystemUseEntry) extension).extensionIdentifier;
                if (extensionIdentifier.equals("RRIP_1991A") || extensionIdentifier.equals("IEEE_P1282") ||
                    extensionIdentifier.equals("IEEE_1282")) {
                    extensions.add(new RockRidgeExtension(((ExtensionSystemUseEntry) extension).extensionIdentifier));
                    context.setRockRidgeIdentifier(((ExtensionSystemUseEntry) extension).extensionIdentifier);
                } else {
                    extensions.add(new GenericSuspExtension(((ExtensionSystemUseEntry) extension).extensionIdentifier));
                }
            }
        } else if (suspRecords.getEntries(null, "RR") != null) {
            // Some ISO creators don't add the 'ER' record for rockRidge, but
            // write the
            // (legacy)
            // RR record anyway
            extensions.add(new RockRidgeExtension("RRIP_1991A"));
            context.setRockRidgeIdentifier("RRIP_1991A");
        }

        context.setSuspExtensions(extensions);
    }

    private static DirectoryRecord readRootSelfRecord(IsoContext context) {
        context.getDataStream()
                .position((long) context.getVolumeDescriptor().rootDirectory.locationOfExtent *
                             context.getVolumeDescriptor().getLogicalBlockSize());
        byte[] firstSector = StreamUtilities.readExact(context.getDataStream(),
                                                       context.getVolumeDescriptor().getLogicalBlockSize());
        DirectoryRecord[] rootSelfRecord = new DirectoryRecord[1];
        DirectoryRecord.readFrom(firstSector, 0, context.getVolumeDescriptor().characterEncoding, rootSelfRecord);
        return rootSelfRecord[0];
    }

    private BootInitialEntry getBootInitialEntry() {
        byte[] bootCatalog = getBootCatalog();
        if (bootCatalog == null) {
            return null;
        }

        BootValidationEntry validationEntry = new BootValidationEntry(bootCatalog, 0);
        if (!validationEntry.getChecksumValid()) {
            return null;
        }

        return new BootInitialEntry(bootCatalog, 0x20);
    }

    private byte[] getBootCatalog() {
        if (bootCatalog == null && bootVolDesc != null) {
            data.position((long) bootVolDesc.getCatalogSector() * IsoUtilities.SectorSize);
            bootCatalog = StreamUtilities.readExact(data, IsoUtilities.SectorSize);
        }

        return bootCatalog;
    }
}
