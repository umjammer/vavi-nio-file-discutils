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

package VolInfo;

import java.io.IOException;

import org.klab.commons.cli.Option;

import DiscUtils.Common.ProgramBase;
import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.PhysicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeManager;
import dotnet4j.io.FileAccess;


public class Program extends ProgramBase {
    @Option(option = "disk", description = "Paths to the disks to inspect.", required = false)
    private String[] _inFiles;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        program.run(args);
    }

    protected void doRun() throws IOException {
        VolumeManager volMgr = new VolumeManager();
        for (String path : _inFiles) {
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
