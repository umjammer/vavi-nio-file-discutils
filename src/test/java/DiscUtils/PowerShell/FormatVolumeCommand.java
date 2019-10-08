
package DiscUtils.PowerShell;

import DiscUtils.Core.VolumeInfo;
import DiscUtils.Ntfs.NtfsFileSystem;
import DiscUtils.PowerShell.Conpat.ErrorRecord;
import DiscUtils.PowerShell.Conpat.PSCmdlet;
import DiscUtils.PowerShell.VirtualDiskProvider.VirtualDiskPSDriveInfo;


public class FormatVolumeCommand extends PSCmdlet {
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

    private FileSystemType __Filesystem = FileSystemType.Ntfs;

    public FileSystemType getFilesystem() {
        return __Filesystem;
    }

    public void setFilesystem(FileSystemType value) {
        __Filesystem = value;
    }

    private String __Label;

    public String getLabel() {
        return __Label;
    }

    public void setLabel(String value) {
        __Label = value;
    }

    public FormatVolumeCommand() {
    }

    protected void processRecord() {
        PSObject volInfoObj = null;
        VolumeInfo volInfo = null;
        if (getInputObject() != null) {
            volInfoObj = getInputObject();
            volInfo = volInfoObj.BaseObject instanceof VolumeInfo ? (VolumeInfo) volInfoObj.BaseObject : (VolumeInfo) null;
        }

        if (volInfo == null && (getLiteralPath() == null || getLiteralPath().isEmpty())) {
            WriteError(new ErrorRecord(new IllegalArgumentException("No volume specified"),
                                       "NoVolumeSpecified",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        if (getFilesystem() != FileSystemType.Ntfs) {
            WriteError(new ErrorRecord(new IllegalArgumentException("Unknown filesystem type"),
                                       "BadFilesystem",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        if (volInfo == null) {
            volInfoObj = SessionState.InvokeProvider.Item.Get(getLiteralPath())[0];
            volInfo = volInfoObj.BaseObject instanceof VolumeInfo ? (VolumeInfo) volInfoObj.BaseObject : (VolumeInfo) null;
        }

        if (volInfo == null) {
            WriteError(new ErrorRecord(new IllegalArgumentException("Path specified is not a disk volume"),
                                       "BadVolumeSpecified",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        Object driveProp = volInfoObj.Properties.get("PSDrive");
        if (driveProp != null) {
            VirtualDiskPSDriveInfo drive = driveProp.Value instanceof VirtualDiskPSDriveInfo ? (VirtualDiskPSDriveInfo) driveProp.Value
                                                                                             : (VirtualDiskPSDriveInfo) null;
            if (drive != null) {
                drive.uncacheFileSystem(volInfo.getIdentity());
            }
        }

        NtfsFileSystem.format(volInfo, getLabel());
    }
}
