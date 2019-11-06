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

package FileRecover;

import java.io.IOException;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import DiscUtils.Common.ProgramBase;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeInfo;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Ntfs.AttributeType;
import DiscUtils.Ntfs.NtfsFileSystem;
import DiscUtils.Ntfs.Internals.EntryStates;
import DiscUtils.Ntfs.Internals.FileNameAttribute;
import DiscUtils.Ntfs.Internals.GenericAttribute;
import DiscUtils.Ntfs.Internals.MasterFileTable;
import DiscUtils.Ntfs.Internals.MasterFileTableEntry;
import DiscUtils.Ntfs.Internals.MasterFileTableEntryFlags;
import DiscUtils.Ntfs.Internals.NtfsNamespace;
import DiscUtils.Streams.Buffer.IBuffer;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;


@Options
public class Program extends ProgramBase {
    @Option(option = "disk", description = "The disks to inspect.", required = false)
    private String[] _diskFiles;

    @Option(option = "r", argName = "recover",
//          "file_index",
            description = "Tries to recover the file at MFT index file_index from the disk.")
    private String _recoverFile;

    @Option(option = "M", argName = "meta",
//          null,
            description = "Don't hide files and directories that are part of the file system itself.")
    private boolean _showMeta;

    @Option(option = "Z",
            argName = "empty",
            // null,
            description = "Don't hide files shown as zero size.")
    private boolean _showZeroSize;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        program.run(args);
    }

//        return StandardSwitches.UserAndPassword | StandardSwitches.PartitionOrVolume;

    protected String[] getHelpRemarks() {
        return new String[] {
            "By default this utility shows the files that it may be possible to recover from an NTFS formatted virtual disk.  Once a " +
                              "candidate file is determined, use the '-r' option to attempt recovery of the file's contents."
        };
    }

    protected void doRun() throws IOException {
        VolumeManager volMgr = new VolumeManager();
        for (String disk : _diskFiles) {
            volMgr.addDisk(VirtualDisk.openDisk(disk, FileAccess.Read, getUserName(), getPassword()));
        }
        VolumeInfo volInfo = null;
        if (getVolumeId() != null && !getVolumeId().isEmpty()) {
            volInfo = volMgr.getVolume(getVolumeId());
        } else if (getPartition() >= 0) {
            volInfo = volMgr.getPhysicalVolumes().get(getPartition());
        } else {
            volInfo = volMgr.getLogicalVolumes().get(0);
        }

        try (NtfsFileSystem fs = new NtfsFileSystem(volInfo.open())) {
            MasterFileTable mft = fs.getMasterFileTable();
            if (_recoverFile != null) {
                MasterFileTableEntry entry = mft.get___idx(Long.parseLong(_recoverFile));
                IBuffer content = getContent(entry);
                if (content == null) {
                    System.err.println("Sorry, unable to recover content");
                    System.exit(1);
                }

                String outFile = _recoverFile + "__recovered.bin";
                if (File.exists(outFile)) {
                    System.err.println("Sorry, the file already exists: " + outFile);
                    System.exit(1);
                }

                FileStream outFileStream = new FileStream(outFile, FileMode.CreateNew, FileAccess.Write);
                try {
                    {
                        pump(content, outFileStream);
                    }
                } finally {
                    if (outFileStream != null)
                        outFileStream.close();

                }
                System.err.println("Possible file contents saved as: " + outFile);
                System.err.println();
                System.err.println("Caution! It is rare for the file contents of deleted files to be intact - most");
                System.err.println("likely the contents recovered are corrupt as the space has been reused.");
            } else {
                for (MasterFileTableEntry entry : mft.getEntries(EntryStates.NotInUse)) {
                    // Skip entries with no attributes, they've probably
                    // never been used. We're certainly
                    // not going to manage to recover any useful data from
                    // them.
                    if (entry.getAttributes().size() == 0)
                        continue;

                    // Skip directories - any useful files inside will be
                    // found separately
                    if (entry.getFlags().contains(MasterFileTableEntryFlags.IsDirectory))
                        continue;

                    long size = getSize(entry);
                    String path = getPath(mft, entry);
                    if (!_showMeta && path.startsWith("<root>\\$Extend"))
                        continue;

                    if (!_showZeroSize && size == 0)
                        continue;

                    System.err.printf("Index: {0,-4}   Size: {1,-8}   Path: %d\n",
                                      entry.getIndex(),
                                      DiscUtils.Common.Utilities.approximateDiskSize(size),
                                      path);
                }
            }
        }
    }

    static IBuffer getContent(MasterFileTableEntry entry) {
        for (GenericAttribute attr : entry.getAttributes()) {
            if (attr.getAttributeType() == AttributeType.Data) {
                return attr.getContent();
            }
        }
        return null;
    }

    static long getSize(MasterFileTableEntry entry) {
        for (GenericAttribute attr : entry.getAttributes()) {
            FileNameAttribute fna = attr instanceof FileNameAttribute ? (FileNameAttribute) attr : (FileNameAttribute) null;
            if (fna != null) {
                return fna.getRealSize();
            }
        }
        return 0;
    }

    static String getPath(MasterFileTable mft, MasterFileTableEntry entry) {
        if (entry.getSequenceNumber() == MasterFileTable.RootDirectoryIndex) {
            return "<root>";
        }

        FileNameAttribute fna = null;
        for (GenericAttribute attr : entry.getAttributes()) {
            FileNameAttribute thisFna = attr instanceof FileNameAttribute ? (FileNameAttribute) attr : (FileNameAttribute) null;
            if (thisFna != null) {
                if (fna == null || thisFna.getFileNameNamespace() == NtfsNamespace.Win32 ||
                    thisFna.getFileNameNamespace() == NtfsNamespace.Win32AndDos) {
                    fna = thisFna;
                }

            }

        }
        if (fna == null) {
            return "<unknown>";
        }

        String parentPath = "<unknown>";
        MasterFileTableEntry parentEntry = mft.get___idx(fna.getParentDirectory().getRecordIndex());
        if (parentEntry != null) {
            if (parentEntry.getSequenceNumber() == fna.getParentDirectory().getRecordSequenceNumber() ||
                parentEntry.getSequenceNumber() == fna.getParentDirectory().getRecordSequenceNumber() + 1) {
                parentPath = getPath(mft, parentEntry);
            }

        }

        return parentPath + "\\" + fna.getFileName();
    }

    private static void pump(IBuffer inBuffer, Stream outStream) {
        long inPos = 0;
        byte[] buffer = new byte[4096];
        int bytesRead = inBuffer.read(inPos, buffer, 0, 4096);
        while (bytesRead != 0 && inPos < inBuffer.getCapacity()) {
            inPos += bytesRead;
            outStream.write(buffer, 0, bytesRead);
            bytesRead = inBuffer.read(inPos, buffer, 0, 4096);
        }
    }
}
