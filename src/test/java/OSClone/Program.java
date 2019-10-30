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

package OSClone;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import DiscUtils.BootConfig.BcdObject;
import DiscUtils.BootConfig.Element;
import DiscUtils.BootConfig.Store;
import DiscUtils.Common.ProgramBase;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Core.Partitions.WellKnownPartitionType;
import DiscUtils.Ntfs.NtfsFileSystem;
import DiscUtils.Ntfs.ShortFileNameOption;
import DiscUtils.Registry.RegistryHive;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.Utilities;


@Options
public class Program extends ProgramBase {
    // Shared to avoid continual re-allocation of a large buffer
    private static byte[] _copyBuffer = new byte[10 * 1024 * 1024];

    private static String[] _excludedFiles = new String[] {
        "\\PAGEFILE.SYS", "\\HIBERFIL.SYS", "\\SYSTEM VOLUME INFORMATION"
    };

    @Option(option = "in_file",
            description = "The disk image containing the Operating System image to be cloned.",
            required = false)
    private String _sourceFile;

    @Option(option = "out_file", description = "The path to the output disk image.", required = false)
    private String _destFile;

    @Option(option = "l", argName = "label", description = "The volume label for the NTFS file system created.")
    private String _labelSwitch = "name";

    private Map<Long, String> _uniqueFiles = new HashMap<>();

    public static void Main(String[] args) throws Exception {
        Program program = new Program();
        program.run(args);
    }

    // return StandardSwitches.OutputFormatAndAdapterType |
    // StandardSwitches.UserAndPassword | StandardSwitches.DiskSize;

    private Random random = new Random();

    protected void doRun() throws IOException {

        try (VirtualDisk sourceDisk = VirtualDisk.openDisk(_sourceFile, FileAccess.Read, getUserName(), getPassword());
                VirtualDisk destDisk = VirtualDisk.createDisk(getOutputDiskType(),
                                                              getOutputDiskVariant(),
                                                              _destFile,
                                                              getDiskParameters(),
                                                              getUserName(),
                                                              getPassword())) {
            if (destDisk instanceof DiscUtils.Vhd.Disk) {
                ((DiscUtils.Vhd.Disk) destDisk).setAutoCommitFooter(false);
            }

            // Copy the MBR from the source disk, and invent a new signature
            // for this new disk
            destDisk.setMasterBootRecord(sourceDisk.getMasterBootRecord());
            destDisk.setSignature(random.nextInt());
            SparseStream sourcePartStream = SparseStream.fromStream(sourceDisk.getPartitions().get___idx(0).open(),
                                                                    Ownership.None);
            NtfsFileSystem sourceNtfs = new NtfsFileSystem(sourcePartStream);
            // Copy the OS boot code into memory, so we can apply it when
            // formatting the new disk
            byte[] bootCode;

            try (Stream bootStream = sourceNtfs.openFile("$Boot", FileMode.Open, FileAccess.Read)) {
                bootCode = new byte[(int) bootStream.getLength()];
                int totalRead = 0;
                while (totalRead < bootCode.length) {
                    totalRead += bootStream.read(bootCode, totalRead, bootCode.length - totalRead);
                }
            }
            // Partition the new disk with a single NTFS partition
            BiosPartitionTable.initialize(destDisk, WellKnownPartitionType.WindowsNtfs);
            VolumeManager volMgr = new VolumeManager(destDisk);
            String label = _labelSwitch != null ? _labelSwitch : sourceNtfs.getVolumeLabel();

            try (NtfsFileSystem destNtfs = NtfsFileSystem.format(volMgr.getLogicalVolumes().get(0), label, bootCode)) {
                destNtfs.setSecurity("\\", sourceNtfs.getSecurity("\\"));
                destNtfs.getNtfsOptions().setShortNameCreation(ShortFileNameOption.Disabled);
                sourceNtfs.getNtfsOptions().setHideHiddenFiles(false);
                sourceNtfs.getNtfsOptions().setHideSystemFiles(false);
                copyFiles(sourceNtfs, destNtfs, "\\", true);
                if (destNtfs.fileExists("\\boot\\BCD")) {
                    // Force all boot entries in the BCD to point to the
                    // newly created NTFS partition - does _not_ cope with
                    // complex multi-volume / multi-boot scenarios at all.

                    try (Stream bcdStream = destNtfs.openFile("\\boot\\BCD", FileMode.Open, FileAccess.ReadWrite);
                            RegistryHive hive = new RegistryHive(bcdStream)) {
                        Store store = new Store(hive.getRoot());
                        for (BcdObject obj : store.getObjects()) {
                            for (Element elem : obj.getElements()) {
                                if (elem.getFormat() == DiscUtils.BootConfig.ElementFormat.Device) {
                                    elem.setValue(DiscUtils.BootConfig.ElementValue
                                            .forDevice(elem.getValue().getParentObject(), volMgr.getPhysicalVolumes().get(0)));
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private void copyFiles(NtfsFileSystem sourceNtfs, NtfsFileSystem destNtfs, String path, boolean subs) throws IOException {
        if (subs) {
            for (String dir : sourceNtfs.getDirectories(path)) {
                if (!isExcluded(dir)) {
                    int hardLinksRemaining = sourceNtfs.getHardLinkCount(dir) - 1;
                    boolean newDir = false;
                    long sourceFileId = sourceNtfs.getFileId(dir);
                    String refPath;
                    if (_uniqueFiles.containsKey(sourceFileId)) {
                        refPath = _uniqueFiles.get(sourceFileId);
                        // If this is another name for a known dir, recreate the
                        // hard link
                        destNtfs.createHardLink(refPath, dir);
                    } else {
                        destNtfs.createDirectory(dir);
                        newDir = true;
                        Map<String, Object> fileAttrs = sourceNtfs.getAttributes(dir);
                        if (fileAttrs.containsKey(FileAttributes.ReparsePoint.name())) {
                            destNtfs.setReparsePoint(dir, sourceNtfs.getReparsePoint(dir));
                        }

                        destNtfs.setAttributes(dir, fileAttrs);
                        destNtfs.setSecurity(dir, sourceNtfs.getSecurity(dir));
                    }
                    // File may have a short name
                    String shortName = sourceNtfs.getShortName(dir);
                    if (shortName != null && !shortName.isEmpty() && !Utilities.equals(shortName, dir)) {
                        destNtfs.setShortName(dir, shortName);
                        --hardLinksRemaining;
                    }

                    if (newDir) {
                        if (hardLinksRemaining > 0) {
                            _uniqueFiles.put(sourceFileId, dir);
                        }

                        copyFiles(sourceNtfs, destNtfs, dir, subs);
                    }

                    // Set standard information last (includes modification
                    // timestamps)
                    destNtfs.setFileStandardInformation(dir, sourceNtfs.getFileStandardInformation(dir));
                }

            }
        }

        for (String file : sourceNtfs.getFiles(path)) {
            System.err.println(file);
            int hardLinksRemaining = sourceNtfs.getHardLinkCount(file) - 1;
            long sourceFileId = sourceNtfs.getFileId(file);
            String refPath;
            if (_uniqueFiles.containsKey(sourceFileId)) {
                refPath = _uniqueFiles.get(sourceFileId);
                // If this is another name for a known file, recreate the hard
                // link
                destNtfs.createHardLink(refPath, file);
            } else {
                copyFile(sourceNtfs, destNtfs, file);
                if (hardLinksRemaining > 0) {
                    _uniqueFiles.put(sourceFileId, file);
                }

            }
            // File may have a short name
            String shortName = sourceNtfs.getShortName(file);
            if (shortName != null && !shortName.isEmpty() && !Utilities.equals(shortName, file)) {
                destNtfs.setShortName(file, shortName);
            }

        }
    }

    private void copyFile(NtfsFileSystem sourceNtfs, NtfsFileSystem destNtfs, String path) throws IOException {
        if (isExcluded(path)) {
            return;
        }

        try (Stream s = sourceNtfs.openFile(path, FileMode.Open, FileAccess.Read);
                Stream d = destNtfs.openFile(path, FileMode.Create, FileAccess.ReadWrite)) {
            d.setLength(s.getLength());
            int numRead = s.read(_copyBuffer, 0, _copyBuffer.length);
            while (numRead > 0) {
                d.write(_copyBuffer, 0, numRead);
                numRead = s.read(_copyBuffer, 0, _copyBuffer.length);
            }
        }

        destNtfs.setSecurity(path, sourceNtfs.getSecurity(path));
        destNtfs.setFileStandardInformation(path, sourceNtfs.getFileStandardInformation(path));
    }

    private static boolean isExcluded(String path) {
        String pathUpper = path.toUpperCase();
        for (int i = 0; i < _excludedFiles.length; ++i) {
            if (Utilities.equals(pathUpper, _excludedFiles[i])) {
                return true;
            }

        }
        return false;
    }
}
