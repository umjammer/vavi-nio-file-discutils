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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import discUtils.core.DiscFileSystem;
import discUtils.core.UnixFilePermissions;
import discUtils.core.internal.Utilities;
import discUtils.streams.SparseStream;
import discUtils.streams.util.Sizes;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.SeekOrigin;
import dotnet4j.util.compat.StringUtilities;


/**
 * A file system backed by an NFS server. NFS is a common storage protocol for
 * Virtual Machines. Currently, only NFS v3 is supported.
 */
public class NfsFileSystem extends DiscFileSystem {

    private static final String FS = File.separator;

    private Nfs3Client client;

    /**
     * Initializes a new instance of the NfsFileSystem class.
     *
     * @param address The address of the NFS server (IP or DNS address).
     * @param mountPoint The mount point on the server to root the file system.
     *            The created instance uses default credentials.
     */
    public NfsFileSystem(String address, String mountPoint) {
        super(new NfsFileSystemOptions());
        client = new Nfs3Client(address, RpcUnixCredential.Default, mountPoint);
    }

    /**
     * Initializes a new instance of the NfsFileSystem class.
     *
     * @param address The address of the NFS server (IP or DNS address).
     * @param credentials The credentials to use when accessing the NFS server.
     * @param mountPoint The mount point on the server to root the file system.
     */
    public NfsFileSystem(String address, RpcCredentials credentials, String mountPoint) {
        super(new NfsFileSystemOptions());
        client = new Nfs3Client(address, credentials, mountPoint);
    }

    /**
     * Gets whether this file system supports modification (true for NFS).
     */
    @Override
    public boolean canWrite() {
        return true;
    }

    /**
     * Gets the friendly name for this file system (NFS).
     */
    @Override
    public String getFriendlyName() {
        return "NFS";
    }

    /**
     * Gets the options controlling this instance.
     */
    public NfsFileSystemOptions getNfsOptions() {
        return (NfsFileSystemOptions) getOptions();
    }

    /**
     * Gets the preferred NFS read size.
     */
    public int getPreferredReadSize() {
        return client == null ? 0 : client.getFileSystemInfo().getReadPreferredBytes();
    }

    /**
     * Gets the preferred NFS write size.
     */
    public int getPreferredWriteSize() {
        return client == null ? 0 : client.getFileSystemInfo().getWritePreferredBytes();
    }

    /**
     * Gets the folders exported by a server.
     *
     * @param address The address of the server.
     * @return An enumeration of exported folders.
     */
    public static List<String> getExports(String address) {
        try (RpcClient rpcClient = new RpcClient(address, null)) {
            List<String> result = new ArrayList<>();
            Nfs3Mount mountClient = new Nfs3Mount(rpcClient);
            for (Nfs3Export export : mountClient.exports()) {
                result.add(export.getDirPath());
            }
            return result;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    /**
     * Copies a file from one location to another.
     *
     * @param sourceFile The source file to copy.
     * @param destinationFile The destination path.
     * @param overwrite Whether to overwrite any existing file (true), or fail
     *            if such a file exists.
     */
    @Override
    public void copyFile(String sourceFile, String destinationFile, boolean overwrite) {
        try {
            Nfs3FileHandle sourceParent = getParentDirectory(sourceFile);
            Nfs3FileHandle destParent = getParentDirectory(destinationFile);

            String sourceFileName = Utilities.getFileFromPath(sourceFile);
            String destFileName = Utilities.getFileFromPath(destinationFile);

            Nfs3FileHandle sourceFileHandle = client.lookup(sourceParent, sourceFileName);
            if (sourceFileHandle == null) {
                throw new FileNotFoundException(String.format("The file '%s' does not exist", sourceFile));
            }

            Nfs3FileAttributes sourceAttrs = client.getAttributes(sourceFileHandle);
            if ((sourceAttrs.type.ordinal() & Nfs3FileType.Directory.ordinal()) != 0) {
                throw new FileNotFoundException(String.format("The path '%s' is not a file", sourceFile));
            }

            Nfs3FileHandle destFileHandle = client.lookup(destParent, destFileName);
            if (destFileHandle != null) {
                if (!overwrite) {
                    throw new dotnet4j.io.IOException(String.format("The destination file '%s' already exists",
                                                                    destinationFile));
                }
            }

            // Create the file, with temporary permissions
            Nfs3SetAttributes setAttrs = new Nfs3SetAttributes();
            setAttrs.setMode(EnumSet.of(UnixFilePermissions.OwnerRead, UnixFilePermissions.OwnerWrite));
            setAttrs.setSetMode(true);
            setAttrs.setSize(sourceAttrs.size);
            setAttrs.setSetSize(true);
            destFileHandle = client.create(destParent, destFileName, !overwrite, setAttrs);

            // Copy the file contents
            try (Nfs3FileStream sourceFs = new Nfs3FileStream(client, sourceFileHandle, FileAccess.Read);
                 Nfs3FileStream destFs = new Nfs3FileStream(client, destFileHandle, FileAccess.Write)) {
                int bufferSize = (int) Math.max(1 * Sizes.OneMiB,
                                                Math.min(client.getFileSystemInfo().getWritePreferredBytes(),
                                                         client.getFileSystemInfo().getReadPreferredBytes()));
                byte[] buffer = new byte[bufferSize];

                int numRead = sourceFs.read(buffer, 0, bufferSize);
                while (numRead > 0) {
                    destFs.write(buffer, 0, numRead);
                    numRead = sourceFs.read(buffer, 0, bufferSize);
                }
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }

            // Set the new file's attributes based on the source file
            setAttrs = new Nfs3SetAttributes();
            setAttrs.setMode(sourceAttrs.mode);
            setAttrs.setSetMode(true);
            setAttrs.setAccessTime(sourceAttrs.accessTime);
            setAttrs.setSetAccessTime(Nfs3SetTimeMethod.ClientTime);
            setAttrs.setModifyTime(sourceAttrs.modifyTime);
            setAttrs.setSetModifyTime(Nfs3SetTimeMethod.ClientTime);
            setAttrs.setGid(sourceAttrs.gid);
            setAttrs.setSetGid(true);
            client.setAttributes(destFileHandle, setAttrs);
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Creates a directory at the specified path.
     *
     * @param path The path of the directory to create.
     */
    @Override
    public void createDirectory(String path) {
        try {
            Nfs3FileHandle parent = getParentDirectory(path);

            Nfs3SetAttributes setAttrs = new Nfs3SetAttributes();
            setAttrs.setMode(getNfsOptions().getNewDirectoryPermissions());
            setAttrs.setSetMode(true);

            client.makeDirectory(parent, Utilities.getFileFromPath(path), setAttrs);
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Deletes a directory at the specified path.
     *
     * @param path The directory to delete.
     */
    @Override
    public void deleteDirectory(String path) {
        try {
            Nfs3FileHandle handle = getFile(path);
            if (handle != null && client.getAttributes(handle).type != Nfs3FileType.Directory) {
                throw new FileNotFoundException("No such directory: " + path);
            }

            Nfs3FileHandle parent = getParentDirectory(path);
            if (handle != null) {
                client.removeDirectory(parent, Utilities.getFileFromPath(path));
            }
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Deletes a file at the specified path.
     *
     * @param path The path of the file to delete.
     */
    @Override
    public void deleteFile(String path) {
        try {
            Nfs3FileHandle handle = getFile(path);
            if (handle != null && client.getAttributes(handle).type == Nfs3FileType.Directory) {
                throw new FileNotFoundException("No such file " + path);
            }

            Nfs3FileHandle parent = getParentDirectory(path);
            if (handle != null) {
                client.remove(parent, Utilities.getFileFromPath(path));
            }
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Indicates whether a specified path exists, and refers to a directory.
     *
     * @param path The path to inspect.
     * @return {@code true} if the path is a directory, else {@code false}.
     */
    @Override
    public boolean directoryExists(String path) {
        return getAttributes(path).containsKey("Directory") && (Boolean) getAttributes(path).get("Directory");
    }

    /**
     * Indicates whether a specified path exists, and refers to a directory.
     *
     * @param path The path to inspect.
     * @return {@code true} if the path is a file, else {@code false} .
     */
    @Override
    public boolean fileExists(String path) {
        return getAttributes(path).containsKey("Normal") && (Boolean) getAttributes(path).get("Normal");
    }

    /**
     * Gets the names of subdirectories in a specified directory matching a
     * specified search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return list of directories matching the search pattern.
     */
    @Override
    public List<String> getDirectories(String path, String searchPattern, String searchOption) {
        try {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> dirs = new ArrayList<>();
            doSearch(dirs, path, re, "AllDirectories".equalsIgnoreCase(searchOption), true, false);
            return dirs;
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Gets the names of files in a specified directory matching a specified
     * search pattern, using a value to determine whether to search
     * subdirectories.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @param searchOption Indicates whether to search subdirectories.
     * @return list of files matching the search pattern.
     */
    @Override
    public List<String> getFiles(String path, String searchPattern, String searchOption) {
        try {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> results = new ArrayList<>();
            doSearch(results, path, re, "AllDirectories".equalsIgnoreCase(searchOption), false, true);
            return results;
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Gets the names of all files and subdirectories in a specified directory.
     *
     * @param path The path to search.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override
    public List<String> getFileSystemEntries(String path) {
        try {
            Pattern re = Utilities.convertWildcardsToRegEx("*.*");
            List<String> results = new ArrayList<>();
            doSearch(results, path, re, false, true, true);
            return results;
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Gets the names of files and subdirectories in a specified directory
     * matching a specified search pattern.
     *
     * @param path The path to search.
     * @param searchPattern The search string to match against.
     * @return list of files and subdirectories matching the search pattern.
     */
    @Override
    public List<String> getFileSystemEntries(String path, String searchPattern) {
        try {
            Pattern re = Utilities.convertWildcardsToRegEx(searchPattern);
            List<String> results = new ArrayList<>();
            doSearch(results, path, re, false, true, true);
            return results;
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Moves a directory.
     *
     * @param sourceDirectoryName The directory to move.
     * @param destinationDirectoryName The target directory name.
     */
    @Override
    public void moveDirectory(String sourceDirectoryName, String destinationDirectoryName) {
        try {
            Nfs3FileHandle sourceParent = getParentDirectory(sourceDirectoryName);
            Nfs3FileHandle destParent = getParentDirectory(destinationDirectoryName);

            String sourceName = Utilities.getFileFromPath(sourceDirectoryName);
            String destName = Utilities.getFileFromPath(destinationDirectoryName);

            Nfs3FileHandle fileHandle = client.lookup(sourceParent, sourceName);
            if (fileHandle == null) {
                throw new FileNotFoundException(String.format("The directory '%s' does not exist", sourceDirectoryName));
            }

            Nfs3FileAttributes sourceAttrs = client.getAttributes(fileHandle);
            if ((sourceAttrs.type.ordinal() & Nfs3FileType.Directory.ordinal()) == 0) {
                throw new FileNotFoundException(String.format("The path '%s' is not a directory", sourceDirectoryName));
            }

            client.rename(sourceParent, sourceName, destParent, destName);
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Moves a file, allowing an existing file to be overwritten.
     *
     * @param sourceName The file to move.
     * @param destinationName The target file name.
     * @param overwrite Whether to permit a destination file to be overwritten.
     */
    @Override
    public void moveFile(String sourceName, String destinationName, boolean overwrite) {
        try {
            Nfs3FileHandle sourceParent = getParentDirectory(sourceName);
            Nfs3FileHandle destParent = getParentDirectory(destinationName);

            String sourceFileName = Utilities.getFileFromPath(sourceName);
            String destFileName = Utilities.getFileFromPath(destinationName);

            Nfs3FileHandle sourceFileHandle = client.lookup(sourceParent, sourceFileName);
            if (sourceFileHandle == null) {
                throw new FileNotFoundException(String.format("The file '%s' does not exist", sourceName));
            }

            Nfs3FileAttributes sourceAttrs = client.getAttributes(sourceFileHandle);
            if ((sourceAttrs.type.ordinal() & Nfs3FileType.Directory.ordinal()) != 0) {
                throw new FileNotFoundException(String.format("The path '%s' is not a file", sourceName));
            }

            Nfs3FileHandle destFileHandle = client.lookup(destParent, destFileName);
            if (destFileHandle != null && !overwrite) {
                throw new dotnet4j.io.IOException(String.format("The destination file '%s' already exists", destinationName));
            }

            client.rename(sourceParent, sourceFileName, destParent, destFileName);
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Opens the specified file.
     *
     * @param path The full path of the file to open.
     * @param mode The file mode for the created stream.
     * @param access The access permissions for the created stream.
     * @return The new stream.
     */
    @Override
    public SparseStream openFile(String path, FileMode mode, FileAccess access) {
        try {
            EnumSet<Nfs3AccessPermissions> requested = EnumSet.noneOf(Nfs3AccessPermissions.class);
            if (access == FileAccess.Read) {
                requested.add(Nfs3AccessPermissions.Read);
            } else if (access == FileAccess.ReadWrite) {
                requested.addAll(Arrays.asList(Nfs3AccessPermissions.Read, Nfs3AccessPermissions.Modify));
            } else {
                requested.add(Nfs3AccessPermissions.Modify);
            }

            if (mode == FileMode.Create || mode == FileMode.CreateNew || (mode == FileMode.OpenOrCreate && !fileExists(path))) {
                Nfs3FileHandle parent = getParentDirectory(path);

                Nfs3SetAttributes setAttrs = new Nfs3SetAttributes();
                setAttrs.setMode(getNfsOptions().getNewFilePermissions());
                setAttrs.setSetMode(true);
                setAttrs.setSize(0);
                setAttrs.setSetSize(true);
                Nfs3FileHandle handle = client
                        .create(parent, Utilities.getFileFromPath(path), mode != FileMode.Create, setAttrs);

                return new Nfs3FileStream(client, handle, access);
            } else {
                Nfs3FileHandle handle = getFile(path);
                EnumSet<Nfs3AccessPermissions> actualPerms = client.access(handle, requested);

                if (!actualPerms.equals(requested)) {
                    throw new dotnet4j.io.IOException(String
                            .format("Access denied opening '%s'. Requested permission '%s', got '%s'",
                                    path,
                                    requested,
                                    actualPerms));
                }

                Nfs3FileStream result = new Nfs3FileStream(client, handle, access);
                if (mode == FileMode.Append) {
                    result.seek(0, SeekOrigin.End);
                } else if (mode == FileMode.Truncate) {
                    result.setLength(0);
                }

                return result;
            }
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Gets the attributes of a file or directory.
     *
     * @param path The file or directory to inspect.
     * @return The attributes of the file or directory.
     */
    @Override
    public Map<String, Object> getAttributes(String path) {
        try {
            Nfs3FileHandle handle = getFile(path);
            Nfs3FileAttributes nfsAttrs = client.getAttributes(handle);

            Map<String, Object> result = new HashMap<>();
            if (nfsAttrs.type == Nfs3FileType.Directory) {
                result.put("Directory", true);
            } else if (nfsAttrs.type == Nfs3FileType.BlockDevice || nfsAttrs.type == Nfs3FileType.CharacterDevice) {
                result.put("Device", true);
            } else {
               result.put("Normal", true);
            }

            if (Utilities.getFileFromPath(path).startsWith(".")) {
                result.put("Hidden", true);
            }

            return result;
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Sets the attributes of a file or directory.
     *
     * @param path The file or directory to change.
     * @param newValue The new attributes of the file or directory.
     */
    @Override
    public void setAttributes(String path, Map<String, Object> newValue) {
        if (!newValue.equals(getAttributes(path))) {
            throw new UnsupportedOperationException("Unable to change file attributes over NFS");
        }
    }

    /**
     * Gets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The creation time.
     */
    @Override
    public long getCreationTimeUtc(String path) {
        try {
            // Note creation time is not available, so simulating from last
            // modification time
            Nfs3FileHandle handle = getFile(path);
            Nfs3FileAttributes attrs = client.getAttributes(handle);
            return attrs.modifyTime.toDateTime();
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }

    }

    /**
     * Sets the creation time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setCreationTimeUtc(String path, long newTime) {
        // No action - creation time is not accessible over NFS
    }

    /**
     * Gets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last access time.
     */
    @Override
    public long getLastAccessTimeUtc(String path) {
        try {
            Nfs3FileHandle handle = getFile(path);
            Nfs3FileAttributes attrs = client.getAttributes(handle);
            return attrs.accessTime.toDateTime();
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Sets the last access time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastAccessTimeUtc(String path, long newTime) {
        try {
            Nfs3FileHandle handle = getFile(path);
            Nfs3SetAttributes attributes = new Nfs3SetAttributes();
            attributes.setSetAccessTime(Nfs3SetTimeMethod.ClientTime);
            attributes.setAccessTime(new Nfs3FileTime(newTime));
            client.setAttributes(handle, attributes);
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Gets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @return The last write time.
     */
    @Override
    public long getLastWriteTimeUtc(String path) {
        try {
            Nfs3FileHandle handle = getFile(path);
            Nfs3FileAttributes attrs = client.getAttributes(handle);
            return attrs.modifyTime.toDateTime();
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Sets the last modification time (in UTC) of a file or directory.
     *
     * @param path The path of the file or directory.
     * @param newTime The new time to set.
     */
    @Override
    public void setLastWriteTimeUtc(String path, long newTime) {
        try {
            Nfs3FileHandle handle = getFile(path);
            Nfs3SetAttributes attributes = new Nfs3SetAttributes();
            attributes.setSetModifyTime(Nfs3SetTimeMethod.ClientTime);
            attributes.setModifyTime(new Nfs3FileTime(newTime));
            client.setAttributes(handle, attributes);
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Gets the length of a file.
     *
     * @param path The path to the file.
     * @return The length in bytes.
     */
    @Override
    public long getFileLength(String path) {
        try {
            Nfs3FileHandle handle = getFile(path);
            Nfs3FileAttributes attrs = client.getAttributes(handle);
            return attrs.size;
        } catch (Nfs3Exception ne) {
            throw convertNfsException(ne);
        }
    }

    /**
     * Size of the Filesystem in bytes
     */
    @Override
    public long getSize() {
        return client.fsStat(client.getRootHandle()).getTotalSizeBytes();
    }

    /**
     * Used space of the Filesystem in bytes
     */
    @Override
    public long getUsedSpace() {
        return getSize() - getAvailableSpace();
    }

    /**
     * Available space of the Filesystem in bytes
     */
    @Override
    public long getAvailableSpace() {
        return client.fsStat(client.getRootHandle()).getFreeSpaceBytes();
    }

    /**
     * Disposes of this instance, freeing up any resources used.
     */
    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
            client = null;
        }

        super.close();
    }

    private static dotnet4j.io.IOException convertNfsException(Nfs3Exception ne) {
        throw new dotnet4j.io.IOException("NFS Status: " + ne.getMessage());
    }

    private void doSearch(List<String> results, String path, Pattern regex, boolean subFolders, boolean dirs, boolean files) {
        Nfs3FileHandle dir = getDirectory(path);
        for (Nfs3DirectoryEntry de : client.readDirectory(dir, true)) {
            if (de.getName().equals(".") || de.getName().equals("..")) {
                continue;
            }

            boolean isDir = de.getFileAttributes().type == Nfs3FileType.Directory;

            if ((isDir && dirs) || (!isDir && files)) {
                String searchName = de.getName().indexOf('.') == -1 ? de.getName() + "." : de.getName();

                if (regex.matcher(searchName).find()) {
                    results.add(Utilities.combinePaths(path, de.getName()));
                }
            }

            if (subFolders && isDir) {
                doSearch(results, Utilities.combinePaths(path, de.getName()), regex, subFolders, dirs, files);
            }
        }
    }

    private Nfs3FileHandle getFile(String path) {
        String file = Utilities.getFileFromPath(path);
        Nfs3FileHandle parent = getParentDirectory(path);

        Nfs3FileHandle handle = client.lookup(parent, file);
        if (handle == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return handle;
    }

    private Nfs3FileHandle getParentDirectory(String path) {
        String[] dirs = Arrays.stream(Utilities.getDirectoryFromPath(path).split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        Nfs3FileHandle parent = getDirectory(client.getRootHandle(), dirs);
        return parent;
    }

    private Nfs3FileHandle getDirectory(String path) {
        String[] dirs = Arrays.stream(path.split(StringUtilities.escapeForRegex(FS)))
                .filter(e -> !e.isEmpty())
                .toArray(String[]::new);
        return getDirectory(client.getRootHandle(), dirs);
    }

    private Nfs3FileHandle getDirectory(Nfs3FileHandle parent, String[] dirs) {
        if (dirs == null) {
            return parent;
        }

        Nfs3FileHandle handle = parent;
        for (String dir : dirs) {
            handle = client.lookup(handle, dir);
            if (handle == null || client.getAttributes(handle).type != Nfs3FileType.Directory) {
                throw new FileNotFoundException();
            }
        }

        return handle;
    }
}
