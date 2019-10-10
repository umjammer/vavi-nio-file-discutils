
package DiscUtils.PowerShell;

import java.util.Random;

import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.Partitions.BiosPartitionTable;
import DiscUtils.Core.Partitions.GuidPartitionTable;
import DiscUtils.Core.Partitions.PartitionTable;
import DiscUtils.PowerShell.Conpat.ErrorRecord;
import DiscUtils.PowerShell.Conpat.PSCmdlet;
import DiscUtils.PowerShell.VirtualDiskProvider.VirtualDiskPSDriveInfo;


public class InitializeVirtualDiskCommand extends PSCmdlet {
    private String __LiteralPath;

    public String getLiteralPath() {
        return __LiteralPath;
    }

    public void setLiteralPath(String value) {
        __LiteralPath = value;
    }

    private PSObject __InputObject;

    public PSObject getInputObject() {
        return __InputObject;
    }

    public void setInputObject(PSObject value) {
        __InputObject = value;
    }

    private VolumeManagerType __VolumeManager = VolumeManagerType.Bios;

    public VolumeManagerType getVolumeManager() {
        return __VolumeManager;
    }

    public void setVolumeManager(VolumeManagerType value) {
        __VolumeManager = value;
    }

    private int __Signature;

    public int getSignature() {
        return __Signature;
    }

    public void setSignature(int value) {
        __Signature = value;
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
            diskObject = SessionState.InvokeProvider.Item.get(getLiteralPath())[0];
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
                                                                        : (VirtualDiskPSDriveInfo) null;
        if (drive != null) {
            drive.rescanVolumes();
        }

        writeObject(disk);
    }
}
