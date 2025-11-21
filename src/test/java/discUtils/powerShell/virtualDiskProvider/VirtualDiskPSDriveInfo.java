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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.core.DiscFileSystem;
import discUtils.core.FileSystemInfo;
import discUtils.core.FileSystemManager;
import discUtils.core.LogicalVolumeInfo;
import discUtils.core.VirtualDisk;
import discUtils.core.VolumeInfo;
import discUtils.core.VolumeManager;
import discUtils.powerShell.conpat.PSDriveInfo;


public final class VirtualDiskPSDriveInfo extends PSDriveInfo {

    private final VirtualDisk disk;

    private VolumeManager volMgr;

    private Map<String, DiscFileSystem> fsCache;

    public VirtualDiskPSDriveInfo(PSDriveInfo toCopy, String root, VirtualDisk disk) {
        super(toCopy.getName(), toCopy.getProvider(), root, toCopy.getDescription(), toCopy.getCredential());
        this.disk = disk;
        volMgr = new VolumeManager(this.disk);
        fsCache = new HashMap<>();
    }

    public VirtualDisk getDisk() {
        return disk;
    }

    public VolumeManager getVolumeManager() {
        return volMgr;
    }

    public DiscFileSystem getFileSystem(VolumeInfo volInfo) {
//        SetupHelper.setupFileSystems();
        DiscFileSystem result;
        if (!fsCache.containsKey(volInfo.getIdentity())) {
            List<FileSystemInfo> fsInfo = FileSystemManager.detectFileSystems(volInfo);
            if (fsInfo != null && !fsInfo.isEmpty()) {
                result = fsInfo.get(0).open(volInfo);
                fsCache.put(volInfo.getIdentity(), result);
            }

        }
        result = fsCache.get(volInfo.getIdentity());

        return result;
    }

    public void rescanVolumes() throws IOException {
        VolumeManager newVolMgr = new VolumeManager(disk);
        Map<String, DiscFileSystem> newFsCache = new HashMap<>();
        Map<String, DiscFileSystem> deadFileSystems = new HashMap<>(fsCache);
        for (LogicalVolumeInfo volInfo : newVolMgr.getLogicalVolumes()) {
            if (fsCache.containsKey(volInfo.getIdentity())) {
                newFsCache.put(volInfo.getIdentity(), fsCache.get(volInfo.getIdentity()));
                deadFileSystems.remove(volInfo.getIdentity());
            }
        }
        for (DiscFileSystem deadFs : deadFileSystems.values()) {
            deadFs.close();
        }
        volMgr = newVolMgr;
        fsCache = newFsCache;
    }

    public void uncacheFileSystem(String volId) throws IOException {
        if (fsCache.containsKey(volId)) {
            DiscFileSystem fs = fsCache.get(volId);
            fs.close();
            fsCache.remove(volId);
        }
    }
}
