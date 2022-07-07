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

package discUtils.powerShell;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;

import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeInfo;
import discUtils.hfsPlus.FileInfo;
import discUtils.powerShell.conpat.ErrorRecord;
import discUtils.powerShell.conpat.PSCmdlet;
import discUtils.powerShell.virtualDiskProvider.OnDemandVirtualDisk;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.Path;
import dotnet4j.io.FileAccess;


public class NewVirtualDiskCommand extends PSCmdlet {
    private String literalPath;

    public String getLiteralPath() {
        return literalPath;
    }

    public void setLiteralPath(String value) {
        literalPath = value;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String value) {
        type = value;
    }

    private String size;

    public String getSize() {
        return size;
    }

    public void setSize(String value) {
        size = value;
    }

    private SwitchParameter differencing;

    public SwitchParameter getDifferencing() {
        return differencing;
    }

    public void setDifferencing(SwitchParameter value) {
        differencing = value;
    }

    private String baseDisk;

    public String getBaseDisk() {
        return baseDisk;
    }

    public void setBaseDisk(String value) {
        baseDisk = value;
    }

    protected void processRecord() throws IOException {
        if (ParameterSetName.equals("New")) {
            createNewDisk();
        } else {
            createDiffDisk();
        }
    }

    private void createNewDisk() throws IOException {
        String[] typeAndVariant = getType().split("-");
        if (typeAndVariant.length < 1 || typeAndVariant.length > 2) {
            writeError(new ErrorRecord(new IllegalArgumentException("Invalid Type of disk"),
                                       "BadDiskType",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        long size;
        Long[] refVar___0 = new Long[1];
        boolean boolVar___0 = !discUtils.common.Utilities.tryParseDiskSize(getSize(), refVar___0);
        size = refVar___0.getValue();
        if (boolVar___0) {
            writeError(new ErrorRecord(new IllegalArgumentException("Unable to parse the disk size"),
                                       "BadDiskSize",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }

        String type = typeAndVariant[0];
        String variant = typeAndVariant.length > 1 ? typeAndVariant[1] : null;
        String child;
        String[] refVar___1 = new String[1];
        PSObject parentObj = resolveNewDiskPath(refVar___1);
        child = refVar___1.getValue();
        VirtualDisk disk = null;
        if (parentObj.BaseObject instanceof DirectoryInfo) {
            String path = Path.combine(((DirectoryInfo) parentObj.BaseObject).FullName, child);
            try (VirtualDisk realDisk = VirtualDisk.createDisk(type, variant, path, size, null, null)) {
            }
            disk = new OnDemandVirtualDisk(path, FileAccess.ReadWrite);
        } else if (parentObj.BaseObject instanceof DiscDirectoryInfo) {
            DiscDirectoryInfo ddi = (DiscDirectoryInfo) parentObj.BaseObject;
            String path = Paths.get(ddi.getFullName(), child).toString();
            try (VirtualDisk realDisk = VirtualDisk.createDisk(ddi.getFileSystem(), type, variant, path, size, null, null)) {
            }
            disk = new OnDemandVirtualDisk(ddi.getFileSystem(), path, FileAccess.ReadWrite);
        } else {
            writeError(new ErrorRecord(new FileNotFoundException("Cannot create a virtual disk in that location"),
                                       "BadDiskLocation",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return;
        }
        writeObject(disk, false);
    }

    private void createDiffDisk() throws IOException {
        String child;
        String[] refVar___2 = new String[1];
        PSObject parentObj = resolveNewDiskPath(refVar___2);
        child = refVar___2.getValue();
        PSObject baseDiskObj = SessionState.InvokeProvider.Item.get(new String[] {
            getBaseDisk()
        }, false, true)[0];
        VirtualDisk baseDisk = null;
        try {
            if (baseDiskObj.BaseObject instanceof FileInfo) {
                baseDisk = VirtualDisk.openDisk(((FileInfo) baseDiskObj.BaseObject).FullName, FileAccess.Read);
            } else if (baseDiskObj.BaseObject instanceof DiscFileInfo) {
                DiscFileInfo dfi = (DiscFileInfo) baseDiskObj.BaseObject;
                baseDisk = VirtualDisk.openDisk(dfi.getFileSystem(), dfi.getFullName(), FileAccess.Read);
            } else {
                writeError(new ErrorRecord(new FileNotFoundException("The file specified by the BaseDisk parameter doesn't exist"),
                                           "BadBaseDiskLocation",
                                           ErrorCategory.InvalidArgument,
                                           null));
                return;
            }
            VirtualDisk newDisk = null;
            if (parentObj.BaseObject instanceof DirectoryInfo) {
                String path = Path.combine(((DirectoryInfo) parentObj.BaseObject).FullName, child);
                try (Closeable __newVar0 = baseDisk.createDifferencingDisk(path)) {
                }
                newDisk = new OnDemandVirtualDisk(path, FileAccess.ReadWrite);
            } else if (parentObj.BaseObject instanceof DiscDirectoryInfo) {
                DiscDirectoryInfo ddi = (DiscDirectoryInfo) parentObj.BaseObject;
                String path = Paths.get(ddi.getFullName(), child).toString();
                try (Closeable __newVar1 = baseDisk.createDifferencingDisk(ddi.getFileSystem(), path)) {
                }
                newDisk = new OnDemandVirtualDisk(ddi.getFileSystem(), path, FileAccess.ReadWrite);
            } else {
                writeError(new ErrorRecord(new FileNotFoundException("Cannot create a virtual disk in that location"),
                                           "BadDiskLocation",
                                           ErrorCategory.InvalidArgument,
                                           null));
                return;
            }
            writeObject(newDisk, false);
        } finally {
            if (baseDisk != null) {
                baseDisk.close();
            }
        }
    }

    private PSObject resolveNewDiskPath(String[] child) {
        PSObject parentObj = new PSObject();
        child.setValue(SessionState.Path.ParseChildName(getLiteralPath()));
        String parent = SessionState.Path.ParseParent(getLiteralPath(), null);
        PathInfo parentPath = this.SessionState.Path.getResolvedPSPathFromPSPath(parent)[0];
        parentObj = SessionState.InvokeProvider.Item.get(new String[] {
            parentPath.Path
        }, false, true)[0];
        // If we got a Volume, then we need to send a special marker to the provider indicating that we
        // actually wanted the root directory on the volume, not the volume itself.
        if (parentObj.BaseObject instanceof VolumeInfo) {
            parentObj = SessionState.InvokeProvider.Item.get(new String[] {
                Path.combine(parentPath.Path, "$Root")
            }, false, true)[0];
        }

        return parentObj;
    }
}
