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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Streams.Builder.BuilderBufferExtent;
import DiscUtils.Streams.Builder.BuilderExtent;
import DiscUtils.Streams.Builder.BuilderStreamExtent;
import DiscUtils.Streams.Builder.StreamBuilder;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;
import DiscUtils.Streams.Util.Sizes;
import DiscUtils.Streams.Util.StreamUtilities;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.StringUtilities;


/**
 * Class that creates ISO images.
 * 
 * <pre>
 * {@code
 * CDBuilder builder = new CDBuilder();
 * builder.VolumeIdentifier = "MYISO";
 * builder.UseJoliet = true;
 * builder.AddFile("Hello.txt", Encoding("ASCII").GetBytes("hello world!"));
 * builder.Build(@"C:\TEMP\myiso.iso");
 * }
 * </pre>
 */
public final class CDBuilder extends StreamBuilder {
    private static final long DiskStart = 0x8000;

    private BootInitialEntry _bootEntry;

    private Stream _bootImage;

    private final BuildParameters _buildParams;

    private final List<BuildDirectoryInfo> _dirs;

    private final List<BuildFileInfo> _files;

    private final BuildDirectoryInfo _rootDirectory;

    /**
     * Initializes a new instance of the CDBuilder class.
     */
    public CDBuilder() {
        _files = new ArrayList<>();
        _dirs = new ArrayList<>();
        _rootDirectory = new BuildDirectoryInfo("\0", null);
        _dirs.add(_rootDirectory);

        _buildParams = new BuildParameters();
        _buildParams.setUseJoliet(true);
    }

    /**
     * Gets or sets a value indicating whether to update the ISOLINUX info table
     * at the start of the boot image. Use with ISOLINUX only.
     * <p>
     * ISOLINUX has an 'information table' at the start of the boot loader that
     * verifies the CD has been loaded correctly by the BIOS. This table needs
     * to be updated to match the actual ISO.
     */
    private boolean _updateIsolinuxBootTable;

    public boolean getUpdateIsolinuxBootTable() {
        return _updateIsolinuxBootTable;
    }

    public void setUpdateIsolinuxBootTable(boolean value) {
        _updateIsolinuxBootTable = value;
    }

    /**
     * Gets or sets a value indicating whether Joliet file-system extensions
     * should be used.
     */
    public boolean useJoliet() {
        return _buildParams.useJoliet();
    }

    public void setUseJoliet(boolean value) {
        _buildParams.setUseJoliet(value);
    }

    /**
     * Gets or sets the Volume Identifier for the ISO file.
     * <p>
     * Must be a valid identifier, i.e. max 32 characters in the range A-Z, 0-9
     * or _. Lower-case characters are not permitted.
     */
    public String getVolumeIdentifier() {
        return _buildParams.getVolumeIdentifier();
    }

    public void setVolumeIdentifier(String value) {
        if (value.length() > 32) {
            throw new IllegalArgumentException("Not a valid volume identifier");
        }
        _buildParams.setVolumeIdentifier(value);
    }

    /**
     * Sets the boot image for the ISO image.
     *
     * @param image Stream containing the boot image.
     * @param emulation The type of emulation requested of the BIOS.
     * @param loadSegment The memory segment to load the image to (0 for
     *            default).
     */
    public void setBootImage(Stream image, BootDeviceEmulation emulation, int loadSegment) {
        if (_bootEntry != null) {
            throw new UnsupportedOperationException("Boot image already set");
        }

        _bootEntry = new BootInitialEntry();
        _bootEntry.BootIndicator = (byte) 0x88;
        _bootEntry.BootMediaType = emulation;
        _bootEntry.setLoadSegment((short) loadSegment);
        _bootEntry.SystemType = 0;
        _bootImage = image;
    }

    /**
     * Adds a directory to the ISO image.
     * <p>
     * The name is the full path to the directory, for example:
     *
     * <pre>
     * {@code builder.addDirectory("DIRA\\DIRB\\DIRC");}
     * </pre>
     *
     * @param name The name of the directory on the ISO image.
     * @return The object representing this directory.
     */
    public BuildDirectoryInfo addDirectory(String name) {
        String[] nameElements = Arrays.stream(name.split(StringUtilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        return getDirectory(nameElements, nameElements.length, true);
    }

    /**
     * Adds a byte array to the ISO image as a file.
     * <p>
     * The name is the full path to the file, for example:
     *
     * <pre>
     * {@code builder.addFile("DIRA\\DIRB\\FILE.TXT;1", new byte[] { 0, 1, 2 }); }
     * </pre>
     *
     * Note the version number at the end of the file name is optional, if not
     * specified the default of 1 will be used.
     *
     * @param name The name of the file on the ISO image.
     * @param content The contents of the file.
     * @return The object representing this file.
     */
    public BuildFileInfo addFile(String name, byte[] content) {
        String[] nameElements = Arrays.stream(name.split(StringUtilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        BuildDirectoryInfo dir = getDirectory(nameElements, nameElements.length - 1, true);

        BuildDirectoryMember[] existing = new BuildDirectoryMember[1];
        if (dir.tryGetMember(nameElements[nameElements.length - 1], existing)) {
            throw new IOException("File already exists");
        }
        BuildFileInfo fi = new BuildFileInfo(nameElements[nameElements.length - 1], dir, content);
        _files.add(fi);
        dir.add(fi);
        return fi;
    }

    /**
     * Adds a disk file to the ISO image as a file.
     * <p>
     * The name is the full path to the file, for example:
     *
     * <pre>
     * {@code builder.addFile("DIRA\\DIRB\\FILE.TXT;1", "C:\\temp\\tempfile.bin"); }
     * </pre>
     *
     * Note the version number at the end of the file name is optional, if not
     * specified the default of 1 will be used.
     *
     * @param name The name of the file on the ISO image.
     * @param sourcePath The name of the file on disk.
     * @return The object representing this file.
     */
    public BuildFileInfo addFile(String name, String sourcePath) {
        String[] nameElements = Arrays.stream(name.split(StringUtilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        BuildDirectoryInfo dir = getDirectory(nameElements, nameElements.length - 1, true);

        BuildDirectoryMember[] existing = new BuildDirectoryMember[1];
        if (dir.tryGetMember(nameElements[nameElements.length - 1], existing)) {
            throw new IOException("File already exists");
        }
        BuildFileInfo fi = new BuildFileInfo(nameElements[nameElements.length - 1], dir, sourcePath);
        _files.add(fi);
        dir.add(fi);
        return fi;
    }

    /**
     * Adds a stream to the ISO image as a file.
     * <p>
     * The name is the full path to the file, for example:
     *
     * <pre>
     * {@code builder.addFile("DIRA\\DIRB\\FILE.TXT;1", stream); }
     * </pre>
     * 
     * Note the version number at the end of the file name is optional, if not
     * specified the default of 1 will be used.
     *
     * @param name The name of the file on the ISO image.
     * @param source The contents of the file.
     * @return The object representing this file.
     */
    public BuildFileInfo addFile(String name, Stream source) {
        if (!source.canSeek()) {
            throw new IllegalArgumentException("source doesn't support seeking " + source);
        }

        String[] nameElements = Arrays.stream(name.split(StringUtilities.escapeForRegex("\\")))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        BuildDirectoryInfo dir = getDirectory(nameElements, nameElements.length - 1, true);

        BuildDirectoryMember[] existing = new BuildDirectoryMember[1];
        if (dir.tryGetMember(nameElements[nameElements.length - 1], existing)) {
            throw new IOException("File already exists");
        }
        BuildFileInfo fi = new BuildFileInfo(nameElements[nameElements.length - 1], dir, source);
        _files.add(fi);
        dir.add(fi);
        return fi;
    }

    /**
     * @param totalLength {@cs out}
     */
    protected List<BuilderExtent> fixExtents(long[] totalLength) {
        List<BuilderExtent> fixedRegions = new ArrayList<>();

        long buildTime = System.currentTimeMillis();

        Charset suppEncoding = _buildParams.useJoliet() ? Charset.forName("UTF-16BE") : Charset.forName("ASCII");

        Map<BuildDirectoryMember, Integer> primaryLocationTable = new HashMap<>();
        Map<BuildDirectoryMember, Integer> supplementaryLocationTable = new HashMap<>();

        long focus = DiskStart + 3 * IsoUtilities.SectorSize;
        // Primary, Supplementary, End (fixed at end...)
        if (_bootEntry != null) {
            focus += IsoUtilities.SectorSize;
        }

        // ####################################################################
        // # 0. Fix boot image location
        // ####################################################################
        long bootCatalogPos = 0;
        if (_bootEntry != null) {
            long bootImagePos = focus;
            Stream realBootImage = patchBootImage(_bootImage,
                                                  (int) (DiskStart / IsoUtilities.SectorSize),
                                                  (int) (bootImagePos / IsoUtilities.SectorSize));
            BuilderStreamExtent bootImageExtent = new BuilderStreamExtent(focus, realBootImage);
            fixedRegions.add(bootImageExtent);
            focus += MathUtilities.roundUp(bootImageExtent.getLength(), IsoUtilities.SectorSize);
            bootCatalogPos = focus;

            byte[] bootCatalog = new byte[IsoUtilities.SectorSize];
            BootValidationEntry bve = new BootValidationEntry();
            bve.writeTo(bootCatalog, 0x00);
            _bootEntry.ImageStart = (int) MathUtilities.ceil(bootImagePos, IsoUtilities.SectorSize);
            _bootEntry.setSectorCount((short) MathUtilities.ceil(_bootImage.getLength(), Sizes.Sector));
            _bootEntry.writeTo(bootCatalog, 0x20);
            fixedRegions.add(new BuilderBufferExtent(bootCatalogPos, bootCatalog));
            focus += IsoUtilities.SectorSize;
        }

        // ####################################################################
        // # 1. Fix file locations
        // ####################################################################

        // Find end of the file data, fixing the files in place as we go
        for (BuildFileInfo fi : _files) {
            primaryLocationTable.put(fi, (int) (focus / IsoUtilities.SectorSize));
            supplementaryLocationTable.put(fi, (int) (focus / IsoUtilities.SectorSize));
            FileExtent extent = new FileExtent(fi, focus);

            // Only remember files of non-zero length (otherwise we'll stomp on
            // a valid file)
            if (extent.getLength() != 0) {
                fixedRegions.add(extent);
            }

            focus += MathUtilities.roundUp(extent.getLength(), IsoUtilities.SectorSize);
        }

        // ####################################################################
        // # 2. Fix directory locations
        // ####################################################################

        // There are two directory tables
        // 1. Primary (std ISO9660)
        // 2. Supplementary (Joliet)

        // Find start of the second set of directory data, fixing ASCII
        // directories in place.
        long startOfFirstDirData = focus;
        for (BuildDirectoryInfo di : _dirs) {
            primaryLocationTable.put(di, (int) (focus / IsoUtilities.SectorSize));
            DirectoryExtent extent = new DirectoryExtent(di, primaryLocationTable, Charset.forName("ASCII"), focus);
            fixedRegions.add(extent);
            focus += MathUtilities.roundUp(extent.getLength(), IsoUtilities.SectorSize);
        }

        // Find end of the second directory table, fixing supplementary
        // directories in place.
        long startOfSecondDirData = focus;
        for (BuildDirectoryInfo di : _dirs) {
            supplementaryLocationTable.put(di, (int) (focus / IsoUtilities.SectorSize));
            DirectoryExtent extent = new DirectoryExtent(di, supplementaryLocationTable, suppEncoding, focus);
            fixedRegions.add(extent);
            focus += MathUtilities.roundUp(extent.getLength(), IsoUtilities.SectorSize);
        }

        // ####################################################################
        // # 3. Fix path tables
        // ####################################################################

        // There are four path tables:
        // 1. LE, ASCII
        // 2. BE, ASCII
        // 3. LE, Supp Charset (Joliet)
        // 4. BE, Supp Charset (Joliet)

        // Find end of the path table
        long startOfFirstPathTable = focus;
        PathTable pathTable = new PathTable(false, Charset.forName("ASCII"), _dirs, primaryLocationTable, focus);
        fixedRegions.add(pathTable);
        focus += MathUtilities.roundUp(pathTable.getLength(), IsoUtilities.SectorSize);
        long primaryPathTableLength = pathTable.getLength();

        long startOfSecondPathTable = focus;
        pathTable = new PathTable(true, Charset.forName("ASCII"), _dirs, primaryLocationTable, focus);
        fixedRegions.add(pathTable);
        focus += MathUtilities.roundUp(pathTable.getLength(), IsoUtilities.SectorSize);

        long startOfThirdPathTable = focus;
        pathTable = new PathTable(false, suppEncoding, _dirs, supplementaryLocationTable, focus);
        fixedRegions.add(pathTable);
        focus += MathUtilities.roundUp(pathTable.getLength(), IsoUtilities.SectorSize);
        long supplementaryPathTableLength = pathTable.getLength();

        long startOfFourthPathTable = focus;
        pathTable = new PathTable(true, suppEncoding, _dirs, supplementaryLocationTable, focus);
        fixedRegions.add(pathTable);
        focus += MathUtilities.roundUp(pathTable.getLength(), IsoUtilities.SectorSize);

        // Find the end of the disk
        totalLength[0] = focus;

        // ####################################################################
        // # 4. Prepare volume descriptors now other structures are fixed
        // ####################################################################
        int regionIdx = 0;
        focus = DiskStart;
        PrimaryVolumeDescriptor pvDesc = new PrimaryVolumeDescriptor((int) (totalLength[0] / IsoUtilities.SectorSize), // VolumeSpaceSize
                                                                     (int) primaryPathTableLength, // PathTableSize
                                                                     (int) (startOfFirstPathTable / IsoUtilities.SectorSize), // TypeLPathTableLocation
                                                                     (int) (startOfSecondPathTable / IsoUtilities.SectorSize), // TypeMPathTableLocation
                                                                     (int) (startOfFirstDirData / IsoUtilities.SectorSize), // RootDirectory.LocationOfExtent
                                                                     (int) _rootDirectory.getDataSize(Charset.forName("ASCII")), // RootDirectory.DataLength
                                                                     buildTime);
        pvDesc.VolumeIdentifier = _buildParams.getVolumeIdentifier();
        PrimaryVolumeDescriptorRegion pvdr = new PrimaryVolumeDescriptorRegion(pvDesc, focus);
        fixedRegions.add(regionIdx++, pvdr);
        focus += IsoUtilities.SectorSize;

        if (_bootEntry != null) {
            BootVolumeDescriptor bvDesc = new BootVolumeDescriptor((int) (bootCatalogPos / IsoUtilities.SectorSize));
            BootVolumeDescriptorRegion bvdr = new BootVolumeDescriptorRegion(bvDesc, focus);
            fixedRegions.add(regionIdx++, bvdr);
            focus += IsoUtilities.SectorSize;
        }

        SupplementaryVolumeDescriptor svDesc = new SupplementaryVolumeDescriptor((int) (totalLength[0] /
                                                                                        IsoUtilities.SectorSize), // VolumeSpaceSize
                                                                                 (int) supplementaryPathTableLength, // PathTableSize
                                                                                 (int) (startOfThirdPathTable /
                                                                                        IsoUtilities.SectorSize), // TypeLPathTableLocation
                                                                                 (int) (startOfFourthPathTable /
                                                                                        IsoUtilities.SectorSize), // TypeMPathTableLocation
                                                                                 (int) (startOfSecondDirData /
                                                                                        IsoUtilities.SectorSize), // RootDirectory.LocationOfExtent
                                                                                 (int) _rootDirectory.getDataSize(suppEncoding), // RootDirectory.DataLength
                                                                                 buildTime,
                                                                                 suppEncoding);
        svDesc.VolumeIdentifier = _buildParams.getVolumeIdentifier();
        SupplementaryVolumeDescriptorRegion svdr = new SupplementaryVolumeDescriptorRegion(svDesc, focus);
        fixedRegions.add(regionIdx++, svdr);
        focus += IsoUtilities.SectorSize;

        VolumeDescriptorSetTerminator evDesc = new VolumeDescriptorSetTerminator();
        VolumeDescriptorSetTerminatorRegion evdr = new VolumeDescriptorSetTerminatorRegion(evDesc, focus);
        fixedRegions.add(regionIdx++, evdr);

        return fixedRegions;
    }

    /**
     * Patches a boot image (esp. for ISOLINUX) before it is written to the
     * disk.
     *
     * @param bootImage The original (master) boot image.
     * @param pvdLba The logical block address of the primary volume descriptor.
     * @param bootImageLba The logical block address of the boot image itself.
     * @return A stream containing the patched boot image - does not need to be
     *         disposed.
     */
    private Stream patchBootImage(Stream bootImage, int pvdLba, int bootImageLba) {
        // Early-exit if no patching to do...
        if (!getUpdateIsolinuxBootTable()) {
            return bootImage;
        }

        byte[] bootData = StreamUtilities.readExact(bootImage, (int) bootImage.getLength());

        Arrays.fill(bootData, 8, 8 + 56, (byte) 0);

        int checkSum = 0;
        for (int i = 64; i < bootData.length; i += 4) {
            checkSum += EndianUtilities.toUInt32LittleEndian(bootData, i);
        }

        EndianUtilities.writeBytesLittleEndian(pvdLba, bootData, 8);
        EndianUtilities.writeBytesLittleEndian(bootImageLba, bootData, 12);
        EndianUtilities.writeBytesLittleEndian(bootData.length, bootData, 16);
        EndianUtilities.writeBytesLittleEndian(checkSum, bootData, 20);

        return new MemoryStream(bootData, false);
    }

    private BuildDirectoryInfo getDirectory(String[] path, int pathLength, boolean createMissing) {
        BuildDirectoryInfo di = tryGetDirectory(path, pathLength, createMissing);

        if (di == null) {
            throw new dotnet4j.io.FileNotFoundException("Directory not found");
        }

        return di;
    }

    private BuildDirectoryInfo tryGetDirectory(String[] path, int pathLength, boolean createMissing) {
        BuildDirectoryInfo focus = _rootDirectory;

        for (int i = 0; i < pathLength; ++i) {
            BuildDirectoryMember[] next = new BuildDirectoryMember[1];
            if (!focus.tryGetMember(path[i], next)) {
                if (createMissing) {
                    // This directory doesn't exist, create it...
                    BuildDirectoryInfo di = new BuildDirectoryInfo(path[i], focus);
                    focus.add(di);
                    _dirs.add(di);
                    focus = di;
                } else {
                    return null;
                }
            } else {
                if (!BuildDirectoryInfo.class.isInstance(next[0])) {
                    throw new IOException("File with conflicting name exists");
                }
                BuildDirectoryInfo nextAsBuildDirectoryInfo = BuildDirectoryInfo.class.cast(next[0]);
                focus = nextAsBuildDirectoryInfo;
            }
        }

        return focus;
    }
}
