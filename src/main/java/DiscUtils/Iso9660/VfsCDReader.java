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

package DiscUtils.Iso9660;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.ClusterMap;
import DiscUtils.Core.ClusterRoles;
import DiscUtils.Core.DiscFileSystemOptions;
import DiscUtils.Core.IClusterBasedFileSystem;
import DiscUtils.Core.IUnixFileSystem;
import DiscUtils.Core.InvalidFileSystemException;
import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.Vfs.VfsReadOnlyFileSystem;
import DiscUtils.Iso9660.RockRidge.RockRidgeExtension;
import DiscUtils.Iso9660.Susp.ExtensionSystemUseEntry;
import DiscUtils.Iso9660.Susp.GenericSuspExtension;
import DiscUtils.Iso9660.Susp.SharingProtocolSystemUseEntry;
import DiscUtils.Iso9660.Susp.SuspExtension;
import DiscUtils.Iso9660.Susp.SuspRecords;
import DiscUtils.Iso9660.Susp.SystemUseEntry;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.SubStream;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Range;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.Stream;


public class VfsCDReader extends VfsReadOnlyFileSystem<ReaderDirEntry, File, ReaderDirectory, IsoContext> implements
                         IClusterBasedFileSystem,
                         IUnixFileSystem {
    private static final Iso9660Variant[] DefaultVariantsNoJoliet = {
        Iso9660Variant.RockRidge, Iso9660Variant.Iso9660
    };

    private static final Iso9660Variant[] DefaultVariantsWithJoliet = {
        Iso9660Variant.Joliet, Iso9660Variant.RockRidge, Iso9660Variant.Iso9660
    };

    private byte[] _bootCatalog;

    private BootVolumeDescriptor _bootVolDesc;

    private final Stream _data;

    private final boolean _hideVersions;

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
     *
     * @param data The stream to read the ISO image from.
     * @param variantPriorities Which possible file system variants to use, and
     *            with which priority.
     * @param hideVersions Hides version numbers (e.g. ";1") from the end of
     *            files.
     *            The implementation considers each of the file system variants
     *            in
     *            {@code variantProperties}
     *            and selects
     *            the first which is determined to be present. In this example
     *            Joliet, then Rock Ridge, then vanilla
     *            Iso9660 will be considered:
     *
     *            {@code
     *            VfsCDReader(stream, new Iso9660Variant[] {Joliet, RockRidge,
     *            Iso9660}, true);
     *            }
     *            The Iso9660 variant should normally be specified as the final
     *            entry in the list. Placing it earlier
     *            in the list will effectively mask later items and not
     *            including it may prevent some ISOs from being read.
     */
    @SuppressWarnings("incomplete-switch")
    public VfsCDReader(Stream data, Iso9660Variant[] variantPriorities, boolean hideVersions) {
        super(new DiscFileSystemOptions());
        _data = data;
        _hideVersions = hideVersions;
        long vdpos = 0x8000;
        // Skip lead-in
        byte[] buffer = new byte[IsoUtilities.SectorSize];
        long pvdPos = 0;
        long svdPos = 0;
        BaseVolumeDescriptor bvd;
        do {
            data.setPosition(vdpos);
            int numRead = data.read(buffer, 0, IsoUtilities.SectorSize);
            if (numRead != IsoUtilities.SectorSize) {
                break;
            }

            bvd = new BaseVolumeDescriptor(buffer, 0);
            if (!bvd.StandardIdentifier.equals(BaseVolumeDescriptor.Iso9660StandardIdentifier)) {
                throw new InvalidFileSystemException("Volume is not ISO-9660");
            }

            switch (bvd._VolumeDescriptorType) {
            case Boot:
                _bootVolDesc = new BootVolumeDescriptor(buffer, 0);
                if (!_bootVolDesc.getSystemId().equals(BootVolumeDescriptor.ElToritoSystemIdentifier)) {
                    _bootVolDesc = null;
                }
                break;
            case Primary:
                // Primary Vol Descriptor
                pvdPos = vdpos;
                break;
            case Supplementary:
                // Supplementary Vol Descriptor
                svdPos = vdpos;
                break;
            case Partition:
                break;
            case SetTerminator:
                break;
            }
            // Volume Partition Descriptor
            // Volume Descriptor Set Terminator
            vdpos += IsoUtilities.SectorSize;
        } while (bvd._VolumeDescriptorType != VolumeDescriptorType.SetTerminator);
        __ActiveVariant = Iso9660Variant.None;
        for (Iso9660Variant variant : variantPriorities) {
            switch (variant) {
            case Joliet:
                if (svdPos != 0) {
                    data.setPosition(svdPos);
                    data.read(buffer, 0, IsoUtilities.SectorSize);
                    SupplementaryVolumeDescriptor volDesc = new SupplementaryVolumeDescriptor(buffer, 0);
                    IsoContext context = new IsoContext();
                    context.setVolumeDescriptor(volDesc);
                    context.setDataStream(_data);
                    setContext(context);
                    setRootDirectory(new ReaderDirectory(getContext(),
                                                         new ReaderDirEntry(getContext(), volDesc.RootDirectory)));
                    __ActiveVariant = Iso9660Variant.Iso9660;
                }
                break;
            case RockRidge:
            case Iso9660:
                if (pvdPos != 0) {
                    data.setPosition(pvdPos);
                    data.read(buffer, 0, IsoUtilities.SectorSize);
                    PrimaryVolumeDescriptor volDesc = new PrimaryVolumeDescriptor(buffer, 0);
                    IsoContext context = new IsoContext();
                    context.setVolumeDescriptor(volDesc);
                    context.setDataStream(_data);
                    DirectoryRecord rootSelfRecord = readRootSelfRecord(context);
                    initializeSusp(context, rootSelfRecord);
                    if (variant == Iso9660Variant.Iso9660 ||
                        (variant == Iso9660Variant.RockRidge && context.getRockRidgeIdentifier() != null &&
                         !context.getRockRidgeIdentifier().isEmpty())) {
                        setContext(context);
                        setRootDirectory(new ReaderDirectory(context, new ReaderDirEntry(context, rootSelfRecord)));
                        __ActiveVariant = variant;
                    }
                }
                break;
            }
            if (getActiveVariant() != Iso9660Variant.None) {
                break;
            }
        }
        if (getActiveVariant() == Iso9660Variant.None) {
            throw new IOException("None of the permitted ISO9660 file system variants was detected");
        }
    }

    private Iso9660Variant __ActiveVariant = Iso9660Variant.None;

    public Iso9660Variant getActiveVariant() {
        return __ActiveVariant;
    }

    public BootDeviceEmulation getBootEmulation() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return initialEntry.BootMediaType;
        }

        return BootDeviceEmulation.NoEmulation;
    }

    public long getBootImageStart() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return initialEntry.ImageStart * IsoUtilities.SectorSize;
        }

        return 0;
    }

    public int getBootLoadSegment() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return initialEntry.LoadSegment;
        }

        return 0;
    }

    /**
     * Provides the friendly name for the CD filesystem.
     */
    public String getFriendlyName() {
        return "ISO 9660 (CD-ROM)";
    }

    public boolean getHasBootImage() {
        if (_bootVolDesc == null) {
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
    public String getVolumeLabel() {
        return getContext().getVolumeDescriptor().VolumeIdentifier;
    }

    public long getClusterSize() {
        return IsoUtilities.SectorSize;
    }

    public long getTotalClusters() {
        return getContext().getVolumeDescriptor().VolumeSpaceSize;
    }

    public long clusterToOffset(long cluster) {
        return cluster * getClusterSize();
    }

    public long offsetToCluster(long offset) {
        return offset / getClusterSize();
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

    public List<Range> pathToClusters(String path) {
        ReaderDirEntry entry = getDirectoryEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("File not found" + path);
        }

        if (entry.getRecord().FileUnitSize != 0 || entry.getRecord().InterleaveGapSize != 0) {
            throw new UnsupportedOperationException("Non-contiguous extents not supported");
        }

        return Arrays.asList(new Range(entry.getRecord().LocationOfExtent,
                                       MathUtilities.ceil(entry.getRecord().DataLength, IsoUtilities.SectorSize)));
    }

    public List<StreamExtent> pathToExtents(String path) {
        ReaderDirEntry entry = getDirectoryEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("File not found " + path);
        }

        if (entry.getRecord().FileUnitSize != 0 || entry.getRecord().InterleaveGapSize != 0) {
            throw new UnsupportedOperationException("Non-contiguous extents not supported");
        }

        return Arrays.asList(new StreamExtent(entry.getRecord().LocationOfExtent * IsoUtilities.SectorSize,
                                              entry.getRecord().DataLength));
    }

    public ClusterMap buildClusterMap() {
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
            if (ReaderDirEntry.class.cast(entry).getRecord().FileUnitSize != 0 || ReaderDirEntry.class.cast(entry).getRecord().InterleaveGapSize != 0) {
                throw new UnsupportedOperationException("Non-contiguous extents not supported");
            }

            long clusters = MathUtilities.ceil(ReaderDirEntry.class.cast(entry).getRecord().DataLength, IsoUtilities.SectorSize);
            for (long i = 0; i < clusters; ++i) {
                clusterToRole[(int) i + ReaderDirEntry.class.cast(entry).getRecord().LocationOfExtent] = EnumSet.of(ClusterRoles.DataFile);
                clusterToFileId[(int) i + ReaderDirEntry.class.cast(entry).getRecord().LocationOfExtent] = entry.getUniqueCacheId();
            }
        });
        return new ClusterMap(clusterToRole, clusterToFileId, fileIdToPaths);
    }

    public UnixFileSystemInfo getUnixFileInfo(String path) {
        File file = getFile(path);
        return file.getUnixFileInfo();
    }

    public Stream openBootImage() {
        BootInitialEntry initialEntry = getBootInitialEntry();
        if (initialEntry != null) {
            return new SubStream(_data,
                                 initialEntry.ImageStart * IsoUtilities.SectorSize,
                                 initialEntry.SectorCount * Sizes.Sector);
        }

        throw new UnsupportedOperationException("No valid boot image");
    }

    protected File convertDirEntryToFile(ReaderDirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            return new ReaderDirectory(getContext(), dirEntry);
        }

        return new File(getContext(), dirEntry);
    }

    protected String formatFileName(String name) {
        if (_hideVersions) {
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
        if (!SuspRecords.detectSharingProtocol(rootSelfRecord.SystemUseData, 0)) {
            context.setSuspExtensions(new ArrayList<SuspExtension>());
            context.setSuspDetected(false);
            return;
        }

        context.setSuspDetected(true);
        SuspRecords suspRecords = new SuspRecords(context, rootSelfRecord.SystemUseData, 0);
        // Stage 2 - Init general SUSP params
        SharingProtocolSystemUseEntry spEntry = (SharingProtocolSystemUseEntry) suspRecords.getEntries(null, "SP").get(0);
        context.setSuspSkipBytes(spEntry.SystemAreaSkip);
        // Stage 3 - Init extensions
        List<SystemUseEntry> extensionEntries = suspRecords.getEntries(null, "ER");
        if (extensionEntries != null) {
            for (SystemUseEntry extension : extensionEntries) {
                String __dummyScrutVar2 = ExtensionSystemUseEntry.class.cast(extension).ExtensionIdentifier;
                if (__dummyScrutVar2.equals("RRIP_1991A") || __dummyScrutVar2.equals("IEEE_P1282") ||
                    __dummyScrutVar2.equals("IEEE_1282")) {
                    extensions.add(new RockRidgeExtension(ExtensionSystemUseEntry.class.cast(extension).ExtensionIdentifier));
                    context.setRockRidgeIdentifier(ExtensionSystemUseEntry.class.cast(extension).ExtensionIdentifier);
                } else {
                    extensions.add(new GenericSuspExtension(ExtensionSystemUseEntry.class.cast(extension).ExtensionIdentifier));
                }
            }
        } else if (suspRecords.getEntries(null, "RR") != null) {
            // Some ISO creators don't add the 'ER' record for RockRidge, but write the (legacy)
            // RR record anyway
            extensions.add(new RockRidgeExtension("RRIP_1991A"));
            context.setRockRidgeIdentifier("RRIP_1991A");
        }

        context.setSuspExtensions(extensions);
    }

    private static DirectoryRecord readRootSelfRecord(IsoContext context) {
        context.getDataStream()
                .setPosition(context.getVolumeDescriptor().RootDirectory.LocationOfExtent *
                             context.getVolumeDescriptor().LogicalBlockSize);
        byte[] firstSector = StreamUtilities.readExact(context.getDataStream(), context.getVolumeDescriptor().LogicalBlockSize);
        DirectoryRecord[] rootSelfRecord = new DirectoryRecord[1];
        DirectoryRecord.readFrom(firstSector, 0, context.getVolumeDescriptor().CharacterEncoding, rootSelfRecord);
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
        if (_bootCatalog == null && _bootVolDesc != null) {
            _data.setPosition(_bootVolDesc.getCatalogSector() * IsoUtilities.SectorSize);
            _bootCatalog = StreamUtilities.readExact(_data, IsoUtilities.SectorSize);
        }

        return _bootCatalog;
    }
}
