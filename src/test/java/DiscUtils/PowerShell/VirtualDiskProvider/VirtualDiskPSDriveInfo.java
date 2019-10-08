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

package DiscUtils.PowerShell.VirtualDiskProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Complete.SetupHelper;
import DiscUtils.Core.DiscFileSystem;
import DiscUtils.Core.FileSystemInfo;
import DiscUtils.Core.FileSystemManager;
import DiscUtils.Core.LogicalVolumeInfo;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Core.VolumeInfo;
import DiscUtils.Core.VolumeManager;


public final class VirtualDiskPSDriveInfo extends PSDriveInfo {
    private VirtualDisk _disk;

    private VolumeManager _volMgr;

    private Map<String, DiscFileSystem> _fsCache;

    public VirtualDiskPSDriveInfo(PSDriveInfo toCopy, String root, VirtualDisk disk) {
        super(toCopy.Name, toCopy.Provider, root, toCopy.Description, toCopy.Credential);
        _disk = disk;
        _volMgr = new VolumeManager(_disk);
        _fsCache = new HashMap<>();
    }

    public VirtualDisk getDisk() {
        return _disk;
    }

    public VolumeManager getVolumeManager() {
        return _volMgr;
    }

    public DiscFileSystem getFileSystem(VolumeInfo volInfo) {
//        SetupHelper.setupFileSystems();
        DiscFileSystem result;
        if (!_fsCache.containsKey(volInfo.getIdentity())) {
            List<FileSystemInfo> fsInfo = FileSystemManager.detectFileSystems(volInfo);
            if (fsInfo != null && fsInfo.size() > 0) {
                result = fsInfo.get(0).open(volInfo);
                _fsCache.put(volInfo.getIdentity(), result);
            }

        }
        result = _fsCache.get(volInfo.getIdentity());

        return result;
    }

    public void rescanVolumes() {
        VolumeManager newVolMgr = new VolumeManager(_disk);
        Map<String, DiscFileSystem> newFsCache = new HashMap<>();
        Map<String, DiscFileSystem> deadFileSystems = new HashMap<>(_fsCache);
        for (LogicalVolumeInfo volInfo : newVolMgr.getLogicalVolumes()) {
            if (_fsCache.containsKey(volInfo.getIdentity())) {
                newFsCache.put(volInfo.getIdentity(), _fsCache.get(volInfo.getIdentity()));
                deadFileSystems.remove(volInfo.getIdentity());
            }
        }
        for (DiscFileSystem deadFs : deadFileSystems.values()) {
            deadFs.close();
        }
        _volMgr = newVolMgr;
        _fsCache = newFsCache;
    }

    public void uncacheFileSystem(String volId) {
        if (_fsCache.containsKey(volId)) {
            DiscFileSystem fs = _fsCache.get(volId);
            fs.close();
            _fsCache.remove(volId);
        }
    }
}
