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

import DiscUtils.Core.VolumeInfo;
import DiscUtils.Ntfs.NtfsFileSystem;
import DiscUtils.PowerShell.Conpat.ErrorRecord;
import DiscUtils.PowerShell.Conpat.PSCmdlet;
import DiscUtils.PowerShell.VirtualDiskProvider.VirtualDiskPSDriveInfo;

enum FileSystemType {
    Ntfs
}

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
            volInfo = volInfoObj.BaseObject instanceof VolumeInfo ? (VolumeInfo) volInfoObj.BaseObject : null;
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
            volInfoObj = SessionState.InvokeProvider.Item.get(getLiteralPath())[0];
            volInfo = volInfoObj.BaseObject instanceof VolumeInfo ? (VolumeInfo) volInfoObj.BaseObject : null;
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
                                                                                             : null;
            if (drive != null) {
                drive.uncacheFileSystem(volInfo.getIdentity());
            }
        }

        NtfsFileSystem.format(volInfo, getLabel());
    }
}
