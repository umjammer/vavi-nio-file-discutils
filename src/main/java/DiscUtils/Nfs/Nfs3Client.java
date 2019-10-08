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

package DiscUtils.Nfs;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class Nfs3Client implements Closeable {
    private final Map<Nfs3FileHandle, Nfs3FileAttributes> _cachedAttributes;

    private final Map<Nfs3FileHandle, Nfs3FileSystemStat> _cachedStats;

    private final Nfs3Mount _mountClient;

    private final Nfs3 _nfsClient;

    private IRpcClient _rpcClient;

    public Nfs3Client(String address, RpcCredentials credentials, String mountPoint) {
        this(new RpcClient(address, credentials), mountPoint);
    }

    public Nfs3Client(IRpcClient rpcClient, String mountPoint) {
        _rpcClient = rpcClient;
        _mountClient = new Nfs3Mount(_rpcClient);
        __RootHandle = _mountClient.mount(mountPoint).getFileHandle();
        _nfsClient = new Nfs3(_rpcClient);
        Nfs3FileSystemInfoResult fsiResult = _nfsClient.fileSystemInfo(getRootHandle());
        __FileSystemInfo = fsiResult.getFileSystemInfo();
        _cachedAttributes = new HashMap<>();
        _cachedAttributes.put(getRootHandle(), fsiResult.getPostOpAttributes());
        _cachedStats = new HashMap<>();
    }

    private Nfs3FileSystemInfo __FileSystemInfo;

    public Nfs3FileSystemInfo getFileSystemInfo() {
        return __FileSystemInfo;
    }

    private Nfs3FileHandle __RootHandle;

    public Nfs3FileHandle getRootHandle() {
        return __RootHandle;
    }

    public void close() throws IOException {
        if (_rpcClient != null) {
            _rpcClient.close();
            _rpcClient = null;
        }
    }

    public Nfs3FileAttributes getAttributes(Nfs3FileHandle handle) {
        if (_cachedAttributes.containsKey(handle)) {
            Nfs3FileAttributes result = _cachedAttributes.get(handle);
            return result;
        }

        Nfs3GetAttributesResult getResult = _nfsClient.getAttributes(handle);
        if (getResult.getStatus() == Nfs3Status.Ok) {
            _cachedAttributes.put(handle, getResult.getAttributes());
            return getResult.getAttributes();
        }

        throw new Nfs3Exception(getResult.getStatus());
    }

    public void setAttributes(Nfs3FileHandle handle, Nfs3SetAttributes newAttributes) {
        Nfs3ModifyResult result = _nfsClient.setAttributes(handle, newAttributes);
        _cachedAttributes.put(handle, result.getCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }

    }

    public Nfs3FileHandle lookup(Nfs3FileHandle dirHandle, String name) {
        Nfs3LookupResult result = _nfsClient.lookup(dirHandle, name);
        if (result.getObjectAttributes() != null && result.getObjectHandle() != null) {
            _cachedAttributes.put(result.getObjectHandle(), result.getObjectAttributes());
        }

        if (result.getDirAttributes() != null) {
            _cachedAttributes.put(dirHandle, result.getDirAttributes());
        }

        if (result.getStatus() == Nfs3Status.Ok) {
            return result.getObjectHandle();
        }

        if (result.getStatus() == Nfs3Status.NoSuchEntity) {
            return null;
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public EnumSet<Nfs3AccessPermissions> access(Nfs3FileHandle handle, EnumSet<Nfs3AccessPermissions> requested) {
        Nfs3AccessResult result = _nfsClient.access(handle, requested);
        if (result.getObjectAttributes() != null) {
            _cachedAttributes.put(handle, result.getObjectAttributes());
        }

        if (result.getStatus() == Nfs3Status.Ok) {
            return result.getAccess();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public Nfs3ReadResult read(Nfs3FileHandle fileHandle, long position, int count) {
        Nfs3ReadResult result = _nfsClient.read(fileHandle, position, count);
        if (result.getFileAttributes() != null) {
            _cachedAttributes.put(fileHandle, result.getFileAttributes());
        }

        if (result.getStatus() == Nfs3Status.Ok) {
            return result;
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public int write(Nfs3FileHandle fileHandle, long position, byte[] buffer, int offset, int count) {
        Nfs3WriteResult result = _nfsClient.write(fileHandle, position, buffer, offset, count);
        _cachedAttributes.put(fileHandle, result.getCacheConsistency().getAfter());
        if (result.getStatus() == Nfs3Status.Ok) {
            return result.getCount();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public Nfs3FileHandle create(Nfs3FileHandle dirHandle, String name, boolean createNew, Nfs3SetAttributes attributes) {
        Nfs3CreateResult result = _nfsClient.create(dirHandle, name, createNew, attributes);
        if (result.getStatus() == Nfs3Status.Ok) {
            _cachedAttributes.put(result.getFileHandle(), result.getFileAttributes());
            return result.getFileHandle();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public Nfs3FileHandle makeDirectory(Nfs3FileHandle dirHandle, String name, Nfs3SetAttributes attributes) {
        Nfs3CreateResult result = _nfsClient.makeDirectory(dirHandle, name, attributes);
        if (result.getStatus() == Nfs3Status.Ok) {
            _cachedAttributes.put(result.getFileHandle(), result.getFileAttributes());
            return result.getFileHandle();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public void remove(Nfs3FileHandle dirHandle, String name) {
        Nfs3ModifyResult result = _nfsClient.remove(dirHandle, name);
        _cachedAttributes.put(dirHandle, result.getCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public void removeDirectory(Nfs3FileHandle dirHandle, String name) {
        Nfs3ModifyResult result = _nfsClient.removeDirectory(dirHandle, name);
        _cachedAttributes.put(dirHandle, result.getCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public void rename(Nfs3FileHandle fromDirHandle, String fromName, Nfs3FileHandle toDirHandle, String toName) {
        Nfs3RenameResult result = _nfsClient.rename(fromDirHandle, fromName, toDirHandle, toName);
        _cachedAttributes.put(fromDirHandle, result.getFromDirCacheConsistency().getAfter());
        _cachedAttributes.put(toDirHandle, result.getToDirCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public Nfs3FileSystemStat fsStat(Nfs3FileHandle handle) {
        if (_cachedStats.containsKey(handle)) {
            Nfs3FileSystemStat result = _cachedStats.get(handle);
            //increase caching to at least one second to prevent multiple RPC calls for single Size calculation
            if (result.getInvariantUntil() > System.currentTimeMillis() - 1)
                return result;
        }

        Nfs3FileSystemStatResult getResult = _nfsClient.fileSystemStat(handle);
        if (getResult.getStatus() == Nfs3Status.Ok) {
            _cachedStats.put(handle, getResult.getFileSystemStat());
            return getResult.getFileSystemStat();
        } else {
            throw new Nfs3Exception(getResult.getStatus());
        }
    }

    public List<Nfs3DirectoryEntry> readDirectory(Nfs3FileHandle parent, boolean silentFail) {
        List<Nfs3DirectoryEntry> entries = new ArrayList<>();
        long cookie = 0;
        long cookieVerifier = 0;
        Nfs3ReadDirPlusResult result;
        do {
            result = _nfsClient.readDirPlus(parent,
                                            cookie,
                                            cookieVerifier,
                                            getFileSystemInfo().getDirectoryPreferredBytes(),
                                            getFileSystemInfo().getReadMaxBytes());
            if (result.getStatus() == Nfs3Status.AccessDenied && silentFail) {
                break;
            }

            if (result.getStatus() != Nfs3Status.Ok) {
                throw new Nfs3Exception(result.getStatus());
            }

            for (Nfs3DirectoryEntry entry : result.getDirEntries()) {
                _cachedAttributes.put(entry.getFileHandle(), entry.getFileAttributes());
                entries.add(entry);
                cookie = entry.getCookie();
            }
            cookieVerifier = result.getCookieVerifier();
        } while (!result.getEof());
        return entries;
    }
}
