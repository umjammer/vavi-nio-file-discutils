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

package discUtils.nfs;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class Nfs3Client implements Closeable {

    private final Map<Nfs3FileHandle, Nfs3FileAttributes> cachedAttributes;

    private final Map<Nfs3FileHandle, Nfs3FileSystemStat> cachedStats;

    private final Nfs3Mount mountClient;

    private final Nfs3 nfsClient;

    private IRpcClient rpcClient;

    public Nfs3Client(String address, RpcCredentials credentials, String mountPoint) {
        this(new RpcClient(address, credentials), mountPoint);
    }

    public Nfs3Client(IRpcClient rpcClient, String mountPoint) {
        this.rpcClient = rpcClient;
        mountClient = new Nfs3Mount(this.rpcClient);
        rootHandle = mountClient.mount(mountPoint).getFileHandle();
        nfsClient = new Nfs3(this.rpcClient);
        Nfs3FileSystemInfoResult fsiResult = nfsClient.fileSystemInfo(getRootHandle());
        fileSystemInfo = fsiResult.getFileSystemInfo();
        cachedAttributes = new HashMap<>();
        cachedAttributes.put(getRootHandle(), fsiResult.getPostOpAttributes());
        cachedStats = new HashMap<>();
    }

    private Nfs3FileSystemInfo fileSystemInfo;

    public Nfs3FileSystemInfo getFileSystemInfo() {
        return fileSystemInfo;
    }

    private Nfs3FileHandle rootHandle;

    public Nfs3FileHandle getRootHandle() {
        return rootHandle;
    }

    public void close() throws IOException {
        if (rpcClient != null) {
            rpcClient.close();
            rpcClient = null;
        }
    }

    public Nfs3FileAttributes getAttributes(Nfs3FileHandle handle) {
        if (cachedAttributes.containsKey(handle)) {
            Nfs3FileAttributes result = cachedAttributes.get(handle);
            return result;
        }

        Nfs3GetAttributesResult getResult = nfsClient.getAttributes(handle);
        if (getResult.getStatus() == Nfs3Status.Ok) {
            cachedAttributes.put(handle, getResult.getAttributes());
            return getResult.getAttributes();
        }

        throw new Nfs3Exception(getResult.getStatus());
    }

    public void setAttributes(Nfs3FileHandle handle, Nfs3SetAttributes newAttributes) {
        Nfs3ModifyResult result = nfsClient.setAttributes(handle, newAttributes);
        cachedAttributes.put(handle, result.getCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public Nfs3FileHandle lookup(Nfs3FileHandle dirHandle, String name) {
        Nfs3LookupResult result = nfsClient.lookup(dirHandle, name);
        if (result.getObjectAttributes() != null && result.getObjectHandle() != null) {
            cachedAttributes.put(result.getObjectHandle(), result.getObjectAttributes());
        }

        if (result.getDirAttributes() != null) {
            cachedAttributes.put(dirHandle, result.getDirAttributes());
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
        Nfs3AccessResult result = nfsClient.access(handle, requested);
        if (result.getObjectAttributes() != null) {
            cachedAttributes.put(handle, result.getObjectAttributes());
        }

        if (result.getStatus() == Nfs3Status.Ok) {
            return result.getAccess();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public Nfs3ReadResult read(Nfs3FileHandle fileHandle, long position, int count) {
        Nfs3ReadResult result = nfsClient.read(fileHandle, position, count);
        if (result.getFileAttributes() != null) {
            cachedAttributes.put(fileHandle, result.getFileAttributes());
        }

        if (result.getStatus() == Nfs3Status.Ok) {
            return result;
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public int write(Nfs3FileHandle fileHandle, long position, byte[] buffer, int offset, int count) {
        Nfs3WriteResult result = nfsClient.write(fileHandle, position, buffer, offset, count);
        cachedAttributes.put(fileHandle, result.getCacheConsistency().getAfter());
        if (result.getStatus() == Nfs3Status.Ok) {
            return result.getCount();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public Nfs3FileHandle create(Nfs3FileHandle dirHandle, String name, boolean createNew, Nfs3SetAttributes attributes) {
        Nfs3CreateResult result = nfsClient.create(dirHandle, name, createNew, attributes);
        if (result.getStatus() == Nfs3Status.Ok) {
            cachedAttributes.put(result.getFileHandle(), result.getFileAttributes());
            return result.getFileHandle();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public Nfs3FileHandle makeDirectory(Nfs3FileHandle dirHandle, String name, Nfs3SetAttributes attributes) {
        Nfs3CreateResult result = nfsClient.makeDirectory(dirHandle, name, attributes);
        if (result.getStatus() == Nfs3Status.Ok) {
            cachedAttributes.put(result.getFileHandle(), result.getFileAttributes());
            return result.getFileHandle();
        }

        throw new Nfs3Exception(result.getStatus());
    }

    public void remove(Nfs3FileHandle dirHandle, String name) {
        Nfs3ModifyResult result = nfsClient.remove(dirHandle, name);
        cachedAttributes.put(dirHandle, result.getCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public void removeDirectory(Nfs3FileHandle dirHandle, String name) {
        Nfs3ModifyResult result = nfsClient.removeDirectory(dirHandle, name);
        cachedAttributes.put(dirHandle, result.getCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public void rename(Nfs3FileHandle fromDirHandle, String fromName, Nfs3FileHandle toDirHandle, String toName) {
        Nfs3RenameResult result = nfsClient.rename(fromDirHandle, fromName, toDirHandle, toName);
        cachedAttributes.put(fromDirHandle, result.getFromDirCacheConsistency().getAfter());
        cachedAttributes.put(toDirHandle, result.getToDirCacheConsistency().getAfter());
        if (result.getStatus() != Nfs3Status.Ok) {
            throw new Nfs3Exception(result.getStatus());
        }
    }

    public Nfs3FileSystemStat fsStat(Nfs3FileHandle handle) {
        if (cachedStats.containsKey(handle)) {
            Nfs3FileSystemStat result = cachedStats.get(handle);
            //increase caching to at least one second to prevent multiple RPC calls for single Size calculation
            if (result.getInvariantUntil() > System.currentTimeMillis() - 1)
                return result;
        }

        Nfs3FileSystemStatResult getResult = nfsClient.fileSystemStat(handle);
        if (getResult.getStatus() == Nfs3Status.Ok) {
            cachedStats.put(handle, getResult.getFileSystemStat());
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
            result = nfsClient.readDirPlus(parent,
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
                cachedAttributes.put(entry.getFileHandle(), entry.getFileAttributes());
                entries.add(entry);
                cookie = entry.getCookie();
            }
            cookieVerifier = result.getCookieVerifier();
        } while (!result.getEof());
        return entries;
    }
}
