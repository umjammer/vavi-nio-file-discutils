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

package virtualDiskConvert;

import java.io.IOException;

import discUtils.common.ProgramBase;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.Geometry;
import discUtils.core.GeometryTranslation;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskParameters;
import discUtils.core.VirtualDiskTypeInfo;
import discUtils.core.VolumeManager;
import discUtils.core.partitions.BiosPartitionTable;
import discUtils.ntfs.NtfsFileSystem;
import discUtils.streams.SnapshotStream;
import discUtils.streams.SparseStream;
import discUtils.streams.StreamExtent;
import discUtils.streams.StreamPump;
import discUtils.streams.util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.util.compat.Utilities;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@Options
public class Program extends ProgramBase {

    @Option(option = "in_file", description = "Path to the source disk.", args = 1, required = true)
    private String inFile;

    @Option(option = "out_file", description = "Path to the output disk.", args = 1, required = true)
    private String outFile;

    @Option(option = "t",
            argName = "translation {mode}",
            description = "Indicates the geometry adjustment to apply for bootable disks.  " +
                          "Set this parameter to match the translation configured in the BIOS of the machine " +
                          "that will boot from the disk - auto should work in most cases for modern BIOS.")
    private GeometryTranslation translation = GeometryTranslation.None;

    @Option(option = "w",
            argName = "wipe",
            description = "Write zero's to all unused parts of the disk.  " +
                          "This option only makes sense when converting to an iSCSI LUN which may be dirty.")
    private boolean wipe;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    @Override
    protected void doRun() throws IOException {

        try (VirtualDisk inDisk = VirtualDisk.openDisk(inFile, FileAccess.Read, getUserName(), getPassword())) {
            VirtualDiskParameters diskParams = inDisk.getParameters();
            diskParams.adapterType = getAdapterType();
            VirtualDiskTypeInfo diskTypeInfo = VirtualDisk.getDiskType(getOutputDiskType(), getOutputDiskVariant());
            if (diskTypeInfo.getDeterministicGeometry()) {
                diskParams.geometry = diskTypeInfo.getCalcGeometry().invoke(diskParams.getCapacity());
            }

            if (translation != null && translation != GeometryTranslation.None) {
                diskParams.setBiosGeometry(diskParams.getGeometry().translateToBios(diskParams.getCapacity(), translation));
            } else if (!inDisk.getDiskTypeInfo().getPreservesBiosGeometry()) {
                // In case the BIOS geometry was just a default, it's better
                // to override based on the physical geometry
                // of the new disk.
                diskParams.setBiosGeometry(Geometry.makeBiosSafe(diskParams.getGeometry(), diskParams.getCapacity()));
            }

            try (VirtualDisk outDisk = VirtualDisk.createDisk(getOutputDiskType(),
                                                              getOutputDiskVariant(),
                    outFile,
                                                              diskParams,
                                                              getUserName(),
                                                              getPassword())) {
                if (outDisk.getCapacity() < inDisk.getCapacity()) {
                    System.err.println("ERROR: The output disk is smaller than the input disk, conversion aborted");
                }

                SparseStream contentStream = inDisk.getContent();
                if (translation != null && translation != GeometryTranslation.None) {
                    SnapshotStream ssStream = new SnapshotStream(contentStream, Ownership.None);
                    ssStream.snapshot();
                    updateBiosGeometry(ssStream, inDisk.getBiosGeometry(), diskParams.getBiosGeometry());
                    contentStream = ssStream;
                }

                StreamPump pump = new StreamPump();
                if (!getQuiet()) {
                    long totalBytes = contentStream.getLength();
                    if (!wipe) {
                        totalBytes = 0;
                        for (StreamExtent se : contentStream.getExtents()) {
                            totalBytes += se.getLength();
                        }
                    }

                    long now = System.currentTimeMillis();
                    long totalBytes_ = totalBytes;
                    pump.progressEvent = (o, e) -> showProgress("Progress", totalBytes_, now, o, e);
                }

                pump.run();
            }
        }
    }

    protected String[] getHelpRemarks() {
        return new String[] {
            "This utility flattens disk hierarchies (VMDK linked-clones, VHD differencing disks) " +
                              "into a single disk image, but does preserve sparseness where the output disk format " +
                              "supports it."
        };
    }

    private static void updateBiosGeometry(SparseStream contentStream,
                                           Geometry oldGeometry,
                                           Geometry newGeometry) throws IOException {
        BiosPartitionTable partTable = new BiosPartitionTable(contentStream, oldGeometry);
        partTable.updateBiosGeometry(newGeometry);
        VolumeManager volMgr = new VolumeManager(contentStream);
        for (LogicalVolumeInfo volume : volMgr.getLogicalVolumes()) {
            for (FileSystemInfo fsInfo : FileSystemManager.detectFileSystems(volume.open())) {
                if (Utilities.equals(fsInfo.getName(), "NTFS")) {

                    try (NtfsFileSystem fs = new NtfsFileSystem(volume.open())) {
                        fs.updateBiosGeometry(newGeometry);
                    }
                }
            }
        }
    }
}
