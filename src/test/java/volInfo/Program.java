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

package volInfo;

import java.io.IOException;

import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import discUtils.common.ProgramBase;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.PhysicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeManager;
import dotnet4j.io.FileAccess;


@Options
public class Program extends ProgramBase {

    @Option(option = "disk", description = "Paths to the disks to inspect.", args = 1, required = true)
    private String inFile; // TODO array

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    protected void doRun() throws IOException {
        VolumeManager volMgr = new VolumeManager();
String[] inFiles = new String[] {inFile};
        for (String path : inFiles) {
            volMgr.addDisk(VirtualDisk.openDisk(path, FileAccess.Read, getUserName(), getPassword()));
        }
        System.err.println("PHYSICAL VOLUMES");
        for (PhysicalVolumeInfo physVol : volMgr.getPhysicalVolumes()) {
            System.err.println("      Identity: " + physVol.getIdentity());
            System.err.println("          Type: " + physVol.getVolumeType());
            System.err.println("       Disk Id: " + physVol.getDiskIdentity());
            System.err.println("      Disk Sig: " + String.format("%8x", physVol.getDiskSignature()));
            System.err.println("       Part Id: " + physVol.getPartitionIdentity());
            System.err.println("        Length: " + physVol.getLength() + " bytes");
            System.err.println(" Disk Geometry: " + physVol.getPhysicalGeometry());
            System.err.println("  First Sector: " + physVol.getPhysicalStartSector());
            System.err.println();
        }
        System.err.println("LOGICAL VOLUMES");
        for (LogicalVolumeInfo logVol : volMgr.getLogicalVolumes()) {
            System.err.println("      Identity: " + logVol.getIdentity());
            System.err.println("        Length: " + logVol.getLength() + " bytes");
            System.err.println(" Disk Geometry: " + logVol.getPhysicalGeometry());
            System.err.println("  First Sector: " + logVol.getPhysicalStartSector());
            System.err.println();
        }
    }
}
