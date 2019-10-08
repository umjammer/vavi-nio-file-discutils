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

package DiscUtils.PowerShell;

import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeManager;
import DiscUtils.Core.Partitions.WellKnownPartitionType;
import DiscUtils.PowerShell.Conpat.ErrorRecord;
import DiscUtils.PowerShell.Conpat.PSCmdlet;
import DiscUtils.PowerShell.VirtualDiskProvider.VirtualDiskPSDriveInfo;


public class NewVolumeCommand extends PSCmdlet {
    private PSObject __InputObject;

    public PSObject getInputObject() {
        return __InputObject;
    }

    public void setInputObject(PSObject value) {
        __InputObject = value;
    }

    private String __LiteralPath;

    public String getLiteralPath() {
        return __LiteralPath;
    }

    public void setLiteralPath(String value) {
        __LiteralPath = value;
    }

    private String __Size;

    public String getSize() {
        return __Size;
    }

    public void setSize(String value) {
        __Size = value;
    }

    private WellKnownPartitionType __Type;

    public WellKnownPartitionType getType() {
        return __Type;
    }

    public void setType(WellKnownPartitionType value) {
        __Type = value;
    }

    private SwitchParameter __Active;

    public SwitchParameter getActive() {
        return __Active;
    }

    public void setActive(SwitchParameter value) {
        __Active = value;
    }

    public NewVolumeCommand() {
        setType(WellKnownPartitionType.WindowsNtfs);
    }

    protected void processRecord() {
        PSObject diskObject = null;
        VirtualDisk disk = null;
        if (getInputObject() != null) {
            diskObject = getInputObject();
            disk = diskObject.BaseObject instanceof VirtualDisk ? (VirtualDisk) diskObject.BaseObject : (VirtualDisk) null;
        }

        if (disk == null && (getLiteralPath() == null || getLiteralPath().isEmpty())) {
            writeError(new ErrorRecord(new IllegalArgumentException("No disk specified"),
                                       "NoDiskSpecified",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        if (disk == null) {
            diskObject = SessionState.InvokeProvider.Item.Get(getLiteralPath())[0];
            VirtualDisk vdisk = diskObject.BaseObject instanceof VirtualDisk ? (VirtualDisk) diskObject.BaseObject
                                                                             : (VirtualDisk) null;
            if (vdisk == null) {
                writeError(new ErrorRecord(new IllegalArgumentException("Path specified is not a virtual disk"),
                                           "BadDiskSpecified",
                                           ErrorCategory.InvalidArgument,
                                           null));
                return;
            }

            disk = vdisk;
        }

        int newIndex;
        if (getSize() == null || getSize().isEmpty()) {
            newIndex = disk.getPartitions().create(getType(), getActive());
        } else {
            long size;
            RefSupport<Long> refVar___0 = new RefSupport<Long>();
            boolean boolVar___0 = !DiscUtils.Common.Utilities.tryParseDiskSize(getSize(), refVar___0);
            size = refVar___0.getValue();
            if (boolVar___0) {
                writeError(new ErrorRecord(new IllegalArgumentException("Unable to parse the volume size"),
                                           "BadVolumeSize",
                                           ErrorCategory.InvalidArgument,
                                           null));
                return;
            }

            newIndex = disk.getPartitions().create(size, getType(), getActive());
        }
        long startSector = disk.getPartitions().get___idx(newIndex).getFirstSector();
        VolumeManager volMgr = null;
        // Changed volume layout, force a rescan
        VirtualDiskPSDriveInfo drive = diskObject.Properties
                .get("PSDrive").Value instanceof VirtualDiskPSDriveInfo ? (VirtualDiskPSDriveInfo) diskObject.Properties.get("PSDrive").Value
                                                                        : (VirtualDiskPSDriveInfo) null;
        if (drive != null) {
            drive.rescanVolumes();
            volMgr = drive.getVolumeManager();
        } else {
            volMgr = new VolumeManager(disk);
        }
        for (LogicalVolumeInfo vol : volMgr.getLogicalVolumes()) {
            if (vol.getPhysicalStartSector() == startSector) {
                writeObject(vol);
            }
        }
    }
}
