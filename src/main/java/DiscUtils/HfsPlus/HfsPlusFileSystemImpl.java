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

package DiscUtils.HfsPlus;

import DiscUtils.Core.DiscFileSystemOptions;
import DiscUtils.Core.IUnixFileSystem;
import DiscUtils.Core.UnixFileSystemInfo;
import DiscUtils.Core.Vfs.VfsFileSystem;
import DiscUtils.Streams.Util.StreamUtilities;
import moe.yo3explorer.dotnetio4j.FileNotFoundException;
import moe.yo3explorer.dotnetio4j.Stream;


public final class HfsPlusFileSystemImpl extends VfsFileSystem<DirEntry, File, Directory, Context> implements IUnixFileSystem {
    public HfsPlusFileSystemImpl(Stream s) {
        super(new DiscFileSystemOptions());
        s.setPosition(1024);
        byte[] headerBuf = StreamUtilities.readExact(s, 512);
        VolumeHeader hdr = new VolumeHeader();
        hdr.readFrom(headerBuf, 0);
        setContext(new Context());
        getContext().setVolumeStream(s);
        getContext().setVolumeHeader(hdr);
        FileBuffer catalogBuffer = new FileBuffer(getContext(), hdr.CatalogFile, CatalogNodeId.CatalogFileId);
        getContext().setCatalog(new BTree<CatalogKey>(catalogBuffer));
        FileBuffer extentsBuffer = new FileBuffer(getContext(), hdr.ExtentsFile, CatalogNodeId.ExtentsFileId);
        getContext().setExtentsOverflow(new BTree<ExtentKey>(extentsBuffer));
        FileBuffer attributesBuffer = new FileBuffer(getContext(), hdr.AttributesFile, CatalogNodeId.AttributesFileId);
        getContext().setAttributes(new BTree<AttributeKey>(attributesBuffer));
        // Establish Root directory
        byte[] rootThreadData = getContext().getCatalog().find(new CatalogKey(CatalogNodeId.RootFolderId, ""));
        CatalogThread rootThread = new CatalogThread();
        rootThread.readFrom(rootThreadData, 0);
        byte[] rootDirEntryData = getContext().getCatalog().find(new CatalogKey(rootThread.ParentId, rootThread.Name));
        DirEntry rootDirEntry = new DirEntry(rootThread.Name, rootDirEntryData);
        setRootDirectory((Directory) getFile(rootDirEntry));
    }

    public String getFriendlyName() {
        return "Apple HFS+";
    }

    public String getVolumeLabel() {
        byte[] rootThreadData = getContext().getCatalog().find(new CatalogKey(CatalogNodeId.RootFolderId, ""));
        CatalogThread rootThread = new CatalogThread();
        rootThread.readFrom(rootThreadData, 0);
        return rootThread.Name;
    }

    public boolean canWrite() {
        return false;
    }

    public UnixFileSystemInfo getUnixFileInfo(String path) {
        DirEntry dirEntry = getDirectoryEntry(path);
        if (dirEntry == null) {
            throw new FileNotFoundException("No such file or directory " + path);
        }

        return dirEntry.getCatalogFileInfo().FileSystemInfo;
    }

    protected File convertDirEntryToFile(DirEntry dirEntry) {
        if (dirEntry.isDirectory()) {
            return new Directory(getContext(), dirEntry.getNodeId(), dirEntry.getCatalogFileInfo());
        }

        if (dirEntry.isSymlink()) {
            return new Symlink(getContext(), dirEntry.getNodeId(), dirEntry.getCatalogFileInfo());
        }

        return new File(getContext(), dirEntry.getNodeId(), dirEntry.getCatalogFileInfo());
    }

    /**
     * Size of the Filesystem in bytes
     */
    public long getSize() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Used space of the Filesystem in bytes
     */
    public long getUsedSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }

    /**
     * Available space of the Filesystem in bytes
     */
    public long getAvailableSpace() {
        throw new UnsupportedOperationException("Filesystem size is not (yet) supported");
    }
}
