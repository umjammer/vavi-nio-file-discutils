
package DiscUtils.PowerShell;

import java.io.IOException;
import java.util.Random;

import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Core.Partitions.GuidPartitionTable;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.PowerShell.Conpat.ErrorRecord;
import DiscUtils.PowerShell.Conpat.PSCmdlet;
import DiscUtils.PowerShell.VirtualDiskProvider.VirtualDiskPSDriveInfo;


public class InitializeVirtualDiskCommand extends PSCmdlet {
    private String literalPath;

    public String getLiteralPath() {
        return literalPath;
    }

    public void setLiteralPath(String value) {
        literalPath = value;
    }

    private PSObject inputObject;

    public PSObject getInputObject() {
        return inputObject;
    }

    public void setInputObject(PSObject value) {
        inputObject = value;
    }

    private VolumeManagerType volumeManager = VolumeManagerType.Bios;

    public VolumeManagerType getVolumeManager() {
        return volumeManager;
    }

    public void setVolumeManager(VolumeManagerType value) {
        volumeManager = value;
    }

    private int signature;

    public int getSignature() {
        return signature;
    }

    public void setSignature(int value) {
        signature = value;
    }

    protected void processRecord() throws IOException {
        PSObject diskObject = null;
        VirtualDisk disk = null;
        if (getInputObject() != null) {
            diskObject = getInputObject();
            disk = diskObject.BaseObject instanceof VirtualDisk ? (VirtualDisk) diskObject.BaseObject : null;
        }

        if (disk == null && (getLiteralPath() == null || getLiteralPath().isEmpty())) {
            writeError(new ErrorRecord(new IllegalArgumentException("No disk specified"),
                                       "NoDiskSpecified",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        if (disk == null) {
            diskObject = SessionState.InvokeProvider.Item.get(getLiteralPath())[0];
            VirtualDisk vdisk = diskObject.BaseObject instanceof VirtualDisk ? (VirtualDisk) diskObject.BaseObject
                                                                             : null;
            if (vdisk == null) {
                writeError(new ErrorRecord(new IllegalArgumentException("Path specified is not a virtual disk"),
                                           "BadDiskSpecified",
                                           ErrorCategory.InvalidArgument,
                                           null));
                return;
            }

            disk = vdisk;
        }

        PartitionTable pt = null;
        if (getVolumeManager() == VolumeManagerType.Bios) {
            pt = BiosPartitionTable.initialize(disk);
        } else {
            pt = GuidPartitionTable.initialize(disk);
        }
        if (getSignature() != 0) {
            disk.setSignature(getSignature());
        } else {
            disk.setSignature((new Random()).nextInt());
        }
        // Changed volume layout, force a rescan
        VirtualDiskPSDriveInfo drive = diskObject.Properties
                .get("PSDrive").Value instanceof VirtualDiskPSDriveInfo ? (VirtualDiskPSDriveInfo) diskObject.Properties.get("PSDrive").Value
                                                                        : null;
        if (drive != null) {
            drive.rescanVolumes();
        }

        writeObject(disk);
    }
}
