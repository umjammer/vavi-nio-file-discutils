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

package discUtils.powerShell.virtualDiskProvider;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.List;

import discUtils.complete.SetupHelper;
import discUtils.core.coreCompat.IContentReader;
import discUtils.core.coreCompat.IContentWriter;
import discUtils.core.DiscDirectoryInfo;
import discUtils.core.DiscFileInfo;
import discUtils.core.DiscFileSystem;
import discUtils.core.DiscFileSystemInfo;
import discUtils.core.IWindowsFileSystem;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.ntfs.NtfsFileSystem;
import discUtils.powerShell.conpat.ErrorRecord;
import discUtils.powerShell.conpat.NavigationCmdletProvider;
import discUtils.powerShell.conpat.PSDriveInfo;
import discUtils.powerShell.conpat.ReturnContainers;
import discUtils.powerShell.Utilities;
import discUtils.powerShell.conpat.IContentCmdletProvider;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.IOException;
import dotnet4j.io.Stream;


public final class Provider extends NavigationCmdletProvider implements IContentCmdletProvider {

    private static final String FS = java.io.File.separator;

    protected PSDriveInfo newDrive(PSDriveInfo drive) {
        SetupHelper.setupComplete();
        NewDriveParameters dynParams = DynamicParameters instanceof NewDriveParameters ? (NewDriveParameters) DynamicParameters
                                                                                       : null;
        if (drive == null) {
            writeError(new ErrorRecord(new NullPointerException(nameof(drive)),
                                       "NullDrive",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return null;
        }

        if (String.IsNullOrEmpty(drive.Root)) {
            writeError(new ErrorRecord(new IllegalArgumentException("drive"), "NoRoot", ErrorCategory.InvalidArgument, drive));
            return null;
        }

        String[] mountPaths = Utilities.NormalizePath(drive.Root).split('!');
        if (mountPaths.length < 1 || mountPaths.length > 2) {
            writeError(new ErrorRecord(new IllegalArgumentException("drive"),
                                       "InvalidRoot",
                                       ErrorCategory.InvalidArgument,
                                       drive));
        }

        //return null;
        String diskPath = mountPaths[0];
        String relPath = mountPaths.length > 1 ? mountPaths[1] : "";
        String user = null;
        String password = null;
        if (drive.Credential != null && drive.Credential.UserName != null) {
            NetworkCredential netCred = drive.Credential.getNetworkCredential();
            user = netCred.UserName;
            password = netCred.Password;
        }

        try {
            String fullPath = Utilities.denormalizePath(diskPath);
            /* [UNSUPPORTED] 'var' as type is unsupported "var" */ resolvedPath = SessionState.Path
                    .getResolvedPSPathFromPSPath(fullPath)[0];
            if (resolvedPath.Provider.Name.equals("FileSystem")) {
                fullPath = resolvedPath.ProviderPath;
            }

            FileAccess access = dynParams.getReadWrite().IsPresent ? FileAccess.ReadWrite : FileAccess.Read;
            VirtualDisk disk = VirtualDisk.openDisk(fullPath, dynParams.getDiskType(), access, user, password);
            return new VirtualDiskPSDriveInfo(drive, makePath(Utilities.normalizePath(fullPath) + "!", relPath), disk);
        } catch (IOException ioe) {
            writeError(new ErrorRecord(ioe, "DiskAccess", ErrorCategory.ResourceUnavailable, drive.Root));
            return null;
        }
    }

    protected Object newDriveDynamicParameters() {
        return new NewDriveParameters();
    }

    protected PSDriveInfo removeDrive(PSDriveInfo drive) {
        if (drive == null) {
            writeError(new ErrorRecord(new NullPointerException(nameof(drive)),
                                       "NullDrive",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return null;
        }

        VirtualDiskPSDriveInfo vdDrive = drive instanceof VirtualDiskPSDriveInfo ? (VirtualDiskPSDriveInfo) drive
                                                                                 : null;
        if (vdDrive == null) {
            writeError(new ErrorRecord(new IllegalArgumentException("invalid type of drive"),
                                       "BadDrive",
                                       ErrorCategory.InvalidArgument,
                                       null));
            return null;
        }

        vdDrive.close();
        return vdDrive;
    }

    protected void getItem(String path) {
        GetItemParameters dynParams = DynamicParameters instanceof GetItemParameters ? (GetItemParameters) DynamicParameters
                                                                                     : null;
        boolean readOnly = !(dynParams != null && dynParams.getReadWrite().IsPresent);
        Object obj = findItemByPath(Utilities.normalizePath(path), false, readOnly);
        if (obj != null) {
            writeItemObject(obj, path.replaceAll("(^\\*|\\*$)", ""), true);
        }

    }

    protected Object getItemDynamicParameters(String path) {
        return new GetItemParameters();
    }

    protected void setItem(String path, Object value) {
        throw new UnsupportedOperationException();
    }

    protected boolean itemExists(String path) {
        boolean result = findItemByPath(Utilities.normalizePath(path), false, true) != null;
        return result;
    }

    protected boolean isValidPath(String path) {
        return path != null && !path.isEmpty();
    }

    protected void getChildItems(String path, boolean recurse) {
        getChildren(Utilities.normalizePath(path), recurse, false);
    }

    protected void getChildNames(String path, ReturnContainers returnContainers) {
        // TODO: returnContainers
        getChildren(Utilities.normalizePath(path), false, true);
    }

    protected boolean hasChildItems(String path) {
        Object obj = findItemByPath(Utilities.normalizePath(path), true, true);
        if (obj instanceof DiscFileInfo) {
            return false;
        } else if (obj instanceof DiscDirectoryInfo) {
            return ((DiscDirectoryInfo) obj).getFileSystemInfos().size() > 0;
        } else {
            return true;
        }
    }

    protected void removeItem(String path, boolean recurse) {
        Object obj = findItemByPath(Utilities.normalizePath(path), false, false);
        if (obj instanceof DiscDirectoryInfo) {
            ((DiscDirectoryInfo) obj).delete(true);
        } else if (obj instanceof DiscFileInfo) {
            ((DiscFileInfo) obj).delete();
        } else {
            writeError(new ErrorRecord(new UnsupportedOperationException("Cannot delete items of this type: " +
                                                                         (obj != null ? obj.getClass() : null)),
                                       "UnknownObjectTypeToRemove",
                                       ErrorCategory.InvalidOperation,
                                       obj));
        }
    }

    protected void newItem(String path, String itemTypeName, Object newItemValue) {
        String parentPath = getParentPath(path, null);
        if (itemTypeName == null || itemTypeName.isEmpty()) {
            writeError(new ErrorRecord(new UnsupportedOperationException("No type specified.  Specify \"file\" or \"directory\" as the type."),
                                       "NoTypeForNewItem",
                                       ErrorCategory.InvalidArgument,
                                       itemTypeName));
            return;
        }

        String itemTypeUpper = itemTypeName.toUpperCase();
        Object obj = findItemByPath(Utilities.normalizePath(parentPath), true, false);
        if (obj instanceof DiscDirectoryInfo) {
            DiscDirectoryInfo dirInfo = (DiscDirectoryInfo) obj;
            switch (itemTypeUpper) {
            case "FILE":
                try (Closeable ignored = dirInfo.getFileSystem()
                        .openFile(Path.Combine(dirInfo.getFullName(), getChildName(path)), FileMode.Create)) {
                    {
                    }
                }
                break;
            case "DIRECTORY":
                dirInfo.getFileSystem().createDirectory(Path.Combine(dirInfo.getFullName(), getChildName(path)));
                break;
            case "HARDLINK":
                NtfsFileSystem ntfs = dirInfo.getFileSystem() instanceof NtfsFileSystem
                        ? (NtfsFileSystem) dirInfo
                        .getFileSystem()
                        : null;
                if (ntfs != null) {
                    NewHardLinkDynamicParameters hlParams = (NewHardLinkDynamicParameters) DynamicParameters;
                    Object srcItems = SessionState.InvokeProvider.Item.get(hlParams.getSourcePath());
                    if (srcItems.size() != 1) {
                        writeError(new ErrorRecord(new UnsupportedOperationException("The type is unknown for this provider.  Only \"file\" and \"directory\" can be specified."),
                                "UnknownTypeForNewItem",
                                ErrorCategory.InvalidArgument,
                                itemTypeName));
                        return;
                    }

                    DiscFileSystemInfo srcFsi = srcItems[0].BaseObject instanceof DiscFileSystemInfo ? (DiscFileSystemInfo) srcItems[0].BaseObject
                            : null;
                    ntfs.CreateHardLink(srcFsi.getFullName(), Path.Combine(dirInfo.getFullName(), getChildName(path)));
                }

                break;
            default:
                writeError(new ErrorRecord(new UnsupportedOperationException("The type is unknown for this provider.  Only \"file\" and \"directory\" can be specified."),
                        "UnknownTypeForNewItem",
                        ErrorCategory.InvalidArgument,
                        itemTypeName));
                break;
            }
        } else {
            writeError(new ErrorRecord(new UnsupportedOperationException("Cannot create items in an object of this type: " +
                                                                         (obj != null ? obj.getClass() : null)),
                                       "UnknownObjectTypeForNewItemParent",
                                       ErrorCategory.InvalidOperation,
                                       obj));
        }
    }

    protected Object newItemDynamicParameters(String path, String itemTypeName, Object newItemValue) {
        if (itemTypeName == null || itemTypeName.isEmpty()) {
            return null;
        }

        String itemTypeUpper = itemTypeName.toUpperCase();
        if (itemTypeUpper.equals("HARDLINK")) {
            return new NewHardLinkDynamicParameters();
        }

        return null;
    }

    protected void renameItem(String path, String newName) {
        Object obj = findItemByPath(Utilities.normalizePath(path), true, false);
        DiscFileSystemInfo fsiObj = obj instanceof DiscFileSystemInfo ? (DiscFileSystemInfo) obj : null;
        if (fsiObj == null) {
            writeError(new ErrorRecord(new UnsupportedOperationException("Cannot move items to this location"),
                                       "BadParentForNewItem",
                                       ErrorCategory.InvalidArgument,
                                       newName));
            return;
        }

        String newFullName = Paths.get(Paths.get(fsiObj.getFullName().replaceFirst("\\*$", "")).getParent().toString(), newName)
                .toString();
        if (obj instanceof DiscDirectoryInfo) {
            DiscDirectoryInfo dirObj = (DiscDirectoryInfo) obj;
            dirObj.moveTo(newFullName);
        } else {
            DiscFileInfo fileObj = (DiscFileInfo) obj;
            fileObj.moveTo(newFullName);
        }
    }

    protected void copyItem(String path, String copyPath, boolean recurse) {
        DiscDirectoryInfo destDir;
        String destFileName = null;
        Object destObj = findItemByPath(Utilities.normalizePath(copyPath), true, false);
        destDir = destObj instanceof DiscDirectoryInfo ? (DiscDirectoryInfo) destObj : null;
        if (destDir != null) {
            destFileName = getChildName(path);
        } else if (destObj == null || destObj instanceof DiscFileInfo) {
            destObj = FindItemByPath(Utilities.NormalizePath(getParentPath(copyPath, null)), true, false);
            destDir = destObj instanceof DiscDirectoryInfo ? (DiscDirectoryInfo) destObj : null;
            destFileName = getChildName(copyPath);
        }

        if (destDir == null) {
            writeError(new ErrorRecord(new UnsupportedOperationException("Cannot copy items to this location"),
                                       "BadParentForNewItem",
                                       ErrorCategory.InvalidArgument,
                                       copyPath));
            return;
        }

        Object srcDirObj = FindItemByPath(Utilities.NormalizePath(getParentPath(path, null)), true, true);
        DiscDirectoryInfo srcDir = srcDirObj instanceof DiscDirectoryInfo ? (DiscDirectoryInfo) srcDirObj
                                                                          : null;
        String srcFileName = getChildName(path);
        if (srcDir == null) {
            writeError(new ErrorRecord(new UnsupportedOperationException("Cannot copy items from this location"),
                                       "BadParentForNewItem",
                                       ErrorCategory.InvalidArgument,
                                       copyPath));
            return;
        }

        doCopy(srcDir, srcFileName, destDir, destFileName, recurse);
    }

    protected boolean isItemContainer(String path) {
        Object obj = findItemByPath(Utilities.normalizePath(path), false, true);
        boolean result = false;
        if (obj instanceof VirtualDisk) {
            result = true;
        } else if (obj instanceof LogicalVolumeInfo) {
            result = true;
        } else if (obj instanceof DiscDirectoryInfo) {
            result = true;
        }

        return result;
    }

    protected String makePath(String parent, String child) {
        return Utilities.NormalizePath(super.makePath(Utilities.denormalizePath(parent), Utilities.denormalizePath(child)));
    }

    public void clearContent(String path) {
        Object destObj = findItemByPath(Utilities.normalizePath(path), true, false);
        if (destObj instanceof DiscFileInfo) {
            try (Stream s = ((DiscFileInfo) destObj).open(FileMode.Open, FileAccess.ReadWrite)) {
                {
                    s.setLength(0);
                }
            }
        } else {
            writeError(new ErrorRecord(new IOException("Cannot write to this item"),
                                       "BadContentDestination",
                                       ErrorCategory.InvalidOperation,
                                       destObj));
        }
    }

    public Object clearContentDynamicParameters(String path) {
        return null;
    }

    public IContentReader getContentReader(String path) {
        Object destObj = findItemByPath(Utilities.normalizePath(path), true, false);
        if (destObj instanceof DiscFileInfo) {
            return new FileContentReaderWriter(this,
                                               ((DiscFileInfo) destObj).open(FileMode.Open, FileAccess.Read),
                                               DynamicParameters instanceof ContentParameters ? (ContentParameters) DynamicParameters
                                                                                              : null);
        } else {
            writeError(new ErrorRecord(new IOException("Cannot read from this item"),
                                       "BadContentSource",
                                       ErrorCategory.InvalidOperation,
                                       destObj));
            return null;
        }
    }

    public Object getContentReaderDynamicParameters(String path) {
        return new ContentParameters();
    }

    public IContentWriter getContentWriter(String path) {
        Object destObj = findItemByPath(Utilities.normalizePath(path), true, false);
        if (destObj instanceof DiscFileInfo) {
            return new FileContentReaderWriter(this,
                                               ((DiscFileInfo) destObj).open(FileMode.Open, FileAccess.ReadWrite),
                                               DynamicParameters instanceof ContentParameters ? (ContentParameters) DynamicParameters
                                                                                              : null);
        } else {
            writeError(new ErrorRecord(new IOException("Cannot write to this item"),
                                       "BadContentDestination",
                                       ErrorCategory.InvalidOperation,
                                       destObj));
            return null;
        }
    }

    public Object getContentWriterDynamicParameters(String path) {
        return new ContentParameters();
    }

    public static String mode(PSObject instance) {
        if (instance == null) {
            return "";
        }

        DiscFileSystemInfo fsi = instance.BaseObject instanceof DiscFileSystemInfo ? (DiscFileSystemInfo) instance.BaseObject
                                                                                   : null;
        if (fsi == null) {
            return "";
        }

        StringBuilder result = new StringBuilder(5);
        result.append(fsi.getAttributes().containsKey("Directory") ? "d" : "-");
        result.append(fsi.getAttributes().containsKey("Archive") ? "a" : "-");
        result.append(fsi.getAttributes().containsKey("ReadOnly") ? "r" : "-");
        result.append(fsi.getAttributes().containsKey("Hidden") ? "h" : "-");
        result.append(fsi.getAttributes().containsKey("System") ? "s" : "-");
        return result.toString();
    }

    private VirtualDiskPSDriveInfo getDriveInfo() {
        return PSDriveInfo instanceof VirtualDiskPSDriveInfo ? (VirtualDiskPSDriveInfo) PSDriveInfo
                                                             : null;
    }

    private VirtualDisk getDisk() {
        VirtualDiskPSDriveInfo driveInfo = getDriveInfo();
        return (driveInfo != null) ? driveInfo.getDisk() : null;
    }

    private Object findItemByPath(String path, boolean preferFs, boolean readOnly) throws java.io.IOException {
        FileAccess fileAccess = readOnly ? FileAccess.Read : FileAccess.ReadWrite;
        String diskPath;
        String relPath;
        int mountSepIdx = path.indexOf('!');
        if (mountSepIdx < 0) {
            diskPath = path;
            relPath = "";
        } else {
            diskPath = path.substring(0, mountSepIdx);
            relPath = path.substring(mountSepIdx + 1);
        }
        VirtualDisk disk = getDisk();
        if (disk == null) {
            OnDemandVirtualDisk odvd = new OnDemandVirtualDisk(Utilities.denormalizePath(diskPath), fileAccess);
            if (odvd.getIsValid()) {
                disk = odvd;
                showSlowDiskWarning();
            } else {
                return null;
            }
        }

        List<String> pathElems = Arrays.asList(relPath.split(discUtils.core.internal.Utilities.escapeForRegex(FS)));
        if (pathElems.size() == 0) {
            return disk;
        }

        VolumeInfo volInfo = null;
        VolumeManager volMgr = getDriveInfo() != null ? getDriveInfo().getVolumeManager() : new VolumeManager(disk);
        List<LogicalVolumeInfo> volumes = volMgr.getLogicalVolumes();
        String volNumStr = pathElems.get(0).startsWith("Volume") ? pathElems.get(0).substring(6) : null;
        int volNum = 0;
        boolean r;
        try {
            volNum = Integer.parseInt(volNumStr);
            r = true;
        } catch (NumberFormatException e) {
            r = false;
        }
        if (r || volNum < 0 || volNum >= volumes.size()) {
            volInfo = volumes.get(volNum);
        } else {
            volInfo = volMgr.getVolume(Utilities.denormalizePath(pathElems.get(0)));
        }
        pathElems.remove(0);
        if (volInfo == null || (pathElems.size() == 0 && !preferFs)) {
            return volInfo;
        }

        boolean[] disposeFs = new boolean[1];
        DiscFileSystem fs = getFileSystem(volInfo, disposeFs);
        try {
            if (fs == null) {
                return null;
            }

            // Special marker in the path - disambiguates the root folder from the volume
            // containing it.  By this point it's done it's job (we didn't return volInfo),
            // so we just remove it.
            if (pathElems.size() > 0 && pathElems.get(0).equals("$Root")) {
                pathElems.remove(0);
            }

            String fsPath = String.join(FS, pathElems.toArray(new String[0]));
            if (fs.directoryExists(fsPath)) {
                return fs.getDirectoryInfo(fsPath);
            } else if (fs.fileExists(fsPath)) {
                return fs.getFileInfo(fsPath);
            }
        } finally {
            if (disposeFs[0] && fs != null) {
                fs.close();
            }
        }
        return null;
    }

    private void showSlowDiskWarning() {
        final String varName = "DiscUtils_HideSlowDiskWarning";
        PSVariable psVar = this.SessionState.PSVariable.get(varName);
        if (psVar != null && psVar.Value != null) {
            boolean warningHidden;
            String valStr = psVar.Value.toString();
            warningHidden = Boolean.parseBoolean(valStr);
            if (warningHidden) {
                return;
            }

        }

        writeWarning("Slow disk access.  Mount the disk using New-PSDrive to improve performance.  This message will not show again.");
        this.SessionState.PSVariable.set(varName, Boolean.TRUE.toString());
    }

    /**
     * @param dispose {@cs out}
     */
    private DiscFileSystem getFileSystem(VolumeInfo volInfo, boolean[] dispose) {
        if (getDriveInfo() != null) {
            dispose[0] = false;
            return getDriveInfo().getFileSystem(volInfo);
        } else {
            // TODO: proper file system detection
            if (volInfo.getBiosType() == 7) {
                dispose[0] = true;
                return new NtfsFileSystem(volInfo.open());
            }

        }
        dispose[0] = false;
        return null;
    }

    private void getChildren(String path, boolean recurse, boolean namesOnly) throws java.io.IOException {
        if (path == null || path.isEmpty()) {
            return;
        }

        Object obj = findItemByPath(path, false, true);
        if (obj instanceof VirtualDisk) {
            VirtualDisk vd = (VirtualDisk) obj;
            enumerateDisk(vd, path, recurse, namesOnly);
        } else if (obj instanceof LogicalVolumeInfo) {
            LogicalVolumeInfo lvi = (LogicalVolumeInfo) obj;
            boolean[] dispose = new boolean[1];
            DiscFileSystem fs = getFileSystem(lvi, dispose);
            try {
                if (fs != null) {
                    enumerateDirectory(fs.getRoot(), path, recurse, namesOnly);
                }

            } finally {
                if (dispose[0] && fs != null) {
                    fs.close();
                }
            }
        } else if (obj instanceof DiscDirectoryInfo) {
            DiscDirectoryInfo ddi = (DiscDirectoryInfo) obj;
            enumerateDirectory(ddi, path, recurse, namesOnly);
        } else {
            writeError(new ErrorRecord(new UnsupportedOperationException("Unrecognized object type: " +
                                                                         (obj != null ? obj.getClass() : null)),
                                       "UnknownObjectType",
                                       ErrorCategory.ParserError,
                                       obj));
        }
    }

    private void enumerateDisk(VirtualDisk vd, String path, boolean recurse, boolean namesOnly) throws java.io.IOException {
        if (!path.replaceFirst("\\*$", "").endsWith("!")) {
            path += "!";
        }

        VolumeManager volMgr = getDriveInfo() != null ? getDriveInfo().getVolumeManager() : new VolumeManager(vd);
        List<LogicalVolumeInfo> volumes = volMgr.getLogicalVolumes();
        for (int i = 0; i < volumes.size(); ++i) {
            String name = "Volume" + i;
            String volPath = makePath(path, name);
            // new PathInfo(PathInfo.Parse(path, true).MountParts, "" + i).toString();
            writeItemObject(namesOnly ? name : volumes.get(i), volPath, true);
            if (recurse) {
                getChildren(volPath, recurse, namesOnly);
            }
        }
    }

    private void enumerateDirectory(DiscDirectoryInfo parent, String basePath, boolean recurse, boolean namesOnly) {
        for (DiscDirectoryInfo dir : parent.getDirectories()) {
            writeItemObject(namesOnly ? dir.getName() : dir, makePath(basePath, dir.getName()), true);
            if (recurse) {
                enumerateDirectory(dir, makePath(basePath, dir.getName()), recurse, namesOnly);
            }

        }
        for (DiscDirectoryInfo file : parent.getFiles()) {
            writeItemObject(namesOnly ? file.getName() : file, makePath(basePath, file.getName()), false);
        }
    }

    private void doCopy(DiscDirectoryInfo srcDir,
                        String srcFileName,
                        DiscDirectoryInfo destDir,
                        String destFileName,
                        boolean recurse) {
        String srcPath = Paths.get(srcDir.getFullName(), srcFileName).toString();
        String destPath = Paths.get(destDir.getFullName(), destFileName).toString();
        if (srcDir.getFileSystem().getAttributes(srcPath).containsKey("Directory")) {
            doCopyFile(srcDir.getFileSystem(), srcPath, destDir.getFileSystem(), destPath);
        } else {
            doCopyDirectory(srcDir.getFileSystem(), srcPath, destDir.getFileSystem(), destPath);
            if (recurse) {
                doRecursiveCopy(srcDir.getFileSystem(), srcPath, destDir.getFileSystem(), destPath);
            }
        }
    }

    private void doRecursiveCopy(DiscFileSystem srcFs, String srcPath, DiscFileSystem destFs, String destPath) {
        for (String dir : srcFs.getDirectories(srcPath)) {
            String srcDirPath = Paths.get(srcPath, dir).toString();
            String destDirPath = Paths.get(destPath, dir).toString();
            doCopyDirectory(srcFs, srcDirPath, destFs, destDirPath);
            doRecursiveCopy(srcFs, srcDirPath, destFs, destDirPath);
        }
        for (String file : srcFs.getFiles(srcPath)) {
            String srcFilePath = Paths.get(srcPath, file).toString();
            String destFilePath = Paths.get(destPath, file).toString();
            doCopyFile(srcFs, srcFilePath, destFs, destFilePath);
        }
    }

    private void doCopyDirectory(DiscFileSystem srcFs, String srcPath, DiscFileSystem destFs, String destPath) {
        IWindowsFileSystem destWindowsFs = destFs instanceof IWindowsFileSystem ? (IWindowsFileSystem) destFs
                                                                                : null;
        IWindowsFileSystem srcWindowsFs = srcFs instanceof IWindowsFileSystem ? (IWindowsFileSystem) srcFs
                                                                              : null;
        destFs.createDirectory(destPath);
        if (srcWindowsFs != null && destWindowsFs != null) {
            if (srcWindowsFs.getAttributes(srcPath).containsKey("ReparsePoint")) {
                destWindowsFs.setReparsePoint(destPath, srcWindowsFs.getReparsePoint(srcPath));
            }

            destWindowsFs.setSecurity(destPath, srcWindowsFs.getSecurity(srcPath));
        }

        destFs.setAttributes(destPath, srcFs.getAttributes(srcPath));
    }

    private void doCopyFile(DiscFileSystem srcFs, String srcPath, DiscFileSystem destFs, String destPath) throws java.io.IOException {
        IWindowsFileSystem destWindowsFs = destFs instanceof IWindowsFileSystem ? (IWindowsFileSystem) destFs
                                                                                : null;
        IWindowsFileSystem srcWindowsFs = srcFs instanceof IWindowsFileSystem ? (IWindowsFileSystem) srcFs
                                                                              : null;
        try (Stream src = srcFs.openFile(srcPath, FileMode.Open, FileAccess.Read)) {
            Stream dest = destFs.openFile(destPath, FileMode.Create, FileAccess.ReadWrite);
            try {
                dest.setLength(src.getLength());
                byte[] buffer = new byte[1024 * 1024];
                int numRead = src.read(buffer, 0, buffer.length);
                while (numRead > 0) {
                    dest.write(buffer, 0, numRead);
                    numRead = src.read(buffer, 0, buffer.length);
                }
            } finally {
                if (dest != null)
                    dest.close();
            }
        }
        if (srcWindowsFs != null && destWindowsFs != null) {
            if (srcWindowsFs.getAttributes(srcPath).containsKey("ReparsePoint")) {
                destWindowsFs.setReparsePoint(destPath, srcWindowsFs.getReparsePoint(srcPath));
            }

            SecurityPermission sd = srcWindowsFs.getSecurity(srcPath);
            if (sd != null) {
                destWindowsFs.setSecurity(destPath, sd);
            }
        }

        destFs.setAttributes(destPath, srcFs.getAttributes(srcPath));
        destFs.setCreationTimeUtc(destPath, srcFs.getCreationTimeUtc(srcPath));
    }
}
