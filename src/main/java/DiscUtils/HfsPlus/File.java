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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import DiscUtils.Core.Compression.SizedDeflateStream;
import DiscUtils.Core.Compression.ZlibBuffer;
import DiscUtils.Core.Compression.ZlibStream;
import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Internal.Utilities;
import DiscUtils.Core.Vfs.IVfsFileWithStreams;
import DiscUtils.Streams.ConcatStream;
import DiscUtils.Streams.SparseStream;
import DiscUtils.Streams.StreamBuffer;
import DiscUtils.Streams.Buffer.BufferStream;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Buffer.SubBuffer;
import DiscUtils.Streams.Util.Ownership;
import dotnet4j.io.FileAccess;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.compression.CompressionMode;
import dotnet4j.io.compression.DeflateStream;


public class File implements IVfsFileWithStreams {
    private static final String CompressionAttributeName = "com.apple.decmpfs";

    private final CommonCatalogFileInfo _catalogInfo;

    private final boolean _hasCompressionAttribute;

    public File(Context context, CatalogNodeId nodeId, CommonCatalogFileInfo catalogInfo) {
        __Context = context;
        __NodeId = nodeId;
        _catalogInfo = catalogInfo;
        _hasCompressionAttribute = getContext().getAttributes()
                .find(new AttributeKey(getNodeId(), CompressionAttributeName)) != null;
    }

    private Context __Context;

    protected Context getContext() {
        return __Context;
    }

    private CatalogNodeId __NodeId;

    protected CatalogNodeId getNodeId() {
        return __NodeId;
    }

    public long getLastAccessTimeUtc() {
        return _catalogInfo.AccessTime;
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return _catalogInfo.ContentModifyTime;
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        return _catalogInfo.CreateTime;
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        return Utilities.fileAttributesFromUnixFileType(_catalogInfo.FileSystemInfo.getFileType());
    }

    public void setFileAttributes(Map<String, Object> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        CatalogFileInfo fileInfo = _catalogInfo instanceof CatalogFileInfo ? (CatalogFileInfo) _catalogInfo
                                                                           : (CatalogFileInfo) null;
        if (fileInfo == null) {
            throw new UnsupportedOperationException();
        }

        return fileInfo.DataFork.LogicalSize;
    }

    public IBuffer getFileContent() {
        CatalogFileInfo fileInfo = _catalogInfo instanceof CatalogFileInfo ? (CatalogFileInfo) _catalogInfo
                                                                           : (CatalogFileInfo) null;
        if (fileInfo == null) {
            throw new UnsupportedOperationException();
        }

        if (_hasCompressionAttribute) {
            // Open the compression attribute
            byte[] compressionAttributeData = getContext().getAttributes()
                    .find(new AttributeKey(_catalogInfo.FileId, "com.apple.decmpfs"));
            CompressionAttribute compressionAttribute = new CompressionAttribute();
            compressionAttribute.readFrom(compressionAttributeData, 0);
            // There are three possibilities:
            // - The file is very small and embedded "as is" in the compression attribute
            // - The file is small and is embedded as a compressed stream in the compression attribute
            // - The file is large and is embedded as a compressed stream in the resource fork
            if (compressionAttribute.getCompressionType() == 3 &&
                compressionAttribute.getUncompressedSize() == compressionAttribute.getAttrSize() - 0x11) {
                // Inline, no compression, very small file
                MemoryStream stream = new MemoryStream(compressionAttributeData,
                                                       CompressionAttribute.getSize() + 1,
                                                       compressionAttribute.getUncompressedSize(),
                                                       false);
                return new StreamBuffer(stream, Ownership.Dispose);
            }

            if (compressionAttribute.getCompressionType() == 3) {
                // Inline, but we must decompress
                MemoryStream stream = new MemoryStream(compressionAttributeData,
                                                       CompressionAttribute.getSize(),
                                                       compressionAttributeData.length - CompressionAttribute.getSize(),
                                                       false);
                // The usage upstream will want to seek or set the position, the ZlibBuffer
                // wraps around a zlibstream and allows for this (in a limited fashion).
                ZlibStream compressedStream = new ZlibStream(stream, CompressionMode.Decompress, false);
                return new ZlibBuffer(compressedStream, Ownership.Dispose);
            }

            if (compressionAttribute.getCompressionType() == 4) {
                // The data is stored in the resource fork.
                FileBuffer buffer = new FileBuffer(getContext(), fileInfo.ResourceFork, fileInfo.FileId);
                CompressionResourceHeader compressionFork = new CompressionResourceHeader();
                byte[] compressionForkData = new byte[CompressionResourceHeader.getSize()];
                buffer.read(0, compressionForkData, 0, CompressionResourceHeader.getSize());
                compressionFork.readFrom(compressionForkData, 0);
                // The data is compressed in a number of blocks. Each block originally accounted for
                // 0x10000 bytes (that's 64 KB) of data. The compressed size may vary.
                // The data in each block can be read using a SparseStream. The first block contains
                // the zlib header but the others don't, so we read them directly as deflate streams.
                // For each block, we create a separate stream which we later aggregate.
                CompressionResourceBlockHead blockHeader = new CompressionResourceBlockHead();
                byte[] blockHeaderData = new byte[CompressionResourceBlockHead.getSize()];
                buffer.read(compressionFork.getHeaderSize(), blockHeaderData, 0, CompressionResourceBlockHead.getSize());
                blockHeader.readFrom(blockHeaderData, 0);
                int blockCount = blockHeader.getNumBlocks();
                CompressionResourceBlock[] blocks = new CompressionResourceBlock[blockCount];
                SparseStream[] streams = new SparseStream[blockCount];
                for (int i = 0; i < blockCount; i++) {
                    // Read the block data, first into a buffer and the into the class.
                    blocks[i] = new CompressionResourceBlock();
                    byte[] blockData = new byte[CompressionResourceBlock.getSize()];
                    buffer.read(compressionFork.getHeaderSize() + CompressionResourceBlockHead.getSize() +
                                i * CompressionResourceBlock.getSize(),
                                blockData,
                                0,
                                blockData.length);
                    blocks[i].readFrom(blockData, 0);
                    // Create a SubBuffer which points to the data window that corresponds to the block.
                    SubBuffer subBuffer = new SubBuffer(buffer,
                                                        compressionFork.getHeaderSize() + blocks[i].getOffset() + 6,
                                                        blocks[i].getDataSize());
                    // ... convert it to a stream
                    BufferStream stream = new BufferStream(subBuffer, FileAccess.Read);
                    // ... and create a deflate stream. Because we will concatenate the streams, the streams
                    // must report on their size. We know the size (0x10000) so we pass it as a parameter.
                    DeflateStream s = new SizedDeflateStream(stream, CompressionMode.Decompress, false, 0x10000);
                    streams[i] = SparseStream.fromStream(s, Ownership.Dispose);
                }
                // Finally, concatenate the streams together and that's about it.
                ConcatStream concatStream = new ConcatStream(Ownership.Dispose, Arrays.asList(streams));
                return new ZlibBuffer(concatStream, Ownership.Dispose);
            }

            return new FileBuffer(getContext(), fileInfo.DataFork, fileInfo.FileId);
        }

        return new FileBuffer(getContext(), fileInfo.DataFork, fileInfo.FileId);
    }

    // Fall back to the default behavior.
    public SparseStream createStream(String name) {
        throw new UnsupportedOperationException();
    }

    public SparseStream openExistingStream(String name) {
        throw new UnsupportedOperationException();
    }
}
