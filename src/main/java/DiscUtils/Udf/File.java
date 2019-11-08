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

package DiscUtils.Udf;

import java.util.EnumSet;
import java.util.List;

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.IVfsFile;
import DiscUtils.Streams.Buffer.IBuffer;
import DiscUtils.Streams.Util.EndianUtilities;


public class File implements IVfsFile {
    protected int _blockSize;

    protected IBuffer _content;

    protected UdfContext _context;

    protected FileEntry _fileEntry;

    protected Partition _partition;

    public File(UdfContext context, Partition partition, FileEntry fileEntry, int blockSize) {
        _context = context;
        _partition = partition;
        _fileEntry = fileEntry;
        _blockSize = blockSize;
    }

    public List<ExtendedAttributeRecord> getExtendedAttributes() {
        return _fileEntry.ExtendedAttributes;
    }

    public IBuffer getFileContent() {
        if (_content != null) {
            return _content;
        }

        _content = new FileContentBuffer(_context, _partition, _fileEntry, _blockSize);
        return _content;
    }

    public long getLastAccessTimeUtc() {
        return _fileEntry.AccessTime;
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return _fileEntry.ModificationTime;
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        ExtendedFileEntry efe = _fileEntry instanceof ExtendedFileEntry ? (ExtendedFileEntry) _fileEntry
                                                                        : (ExtendedFileEntry) null;
        if (efe != null) {
            return efe.CreationTime;
        }

        return getLastWriteTimeUtc();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        EnumSet<FileAttributes> attribs = EnumSet.noneOf(FileAttributes.class);
        EnumSet<InformationControlBlockFlags> flags = _fileEntry.InformationControlBlock.Flags;
        if (_fileEntry.InformationControlBlock._FileType == FileType.Directory) {
            attribs.add(FileAttributes.Directory);
        } else if (_fileEntry.InformationControlBlock._FileType == FileType.Fifo ||
                   _fileEntry.InformationControlBlock._FileType == FileType.Socket ||
                   _fileEntry.InformationControlBlock._FileType == FileType.SpecialBlockDevice ||
                   _fileEntry.InformationControlBlock._FileType == FileType.SpecialCharacterDevice ||
                   _fileEntry.InformationControlBlock._FileType == FileType.TerminalEntry) {
            attribs.add(FileAttributes.Device);
        }

        if (flags.contains(InformationControlBlockFlags.Archive)) {
            attribs.add(FileAttributes.Archive);
        }

        if (flags.contains(InformationControlBlockFlags.System)) {
            attribs.add(FileAttributes.System);
            attribs.add(FileAttributes.Hidden);
        }

        if (attribs.size() == 0) {
            attribs.add(FileAttributes.Normal);
        }

        return attribs;
    }

    public void setFileAttributes(EnumSet<FileAttributes> value) {
        throw new UnsupportedOperationException();
    }

    public long getFileLength() {
        return _fileEntry.InformationLength;
    }

    public static File fromDescriptor(UdfContext context, LongAllocationDescriptor icb) {
        LogicalPartition partition = context.LogicalPartitions.get(icb.ExtentLocation.getPartition());
        byte[] rootDirData = UdfUtilities.readExtent(context, icb);
        DescriptorTag rootDirTag = EndianUtilities.<DescriptorTag> toStruct(DescriptorTag.class, rootDirData, 0);
        if (rootDirTag._TagIdentifier == TagIdentifier.ExtendedFileEntry) {
            ExtendedFileEntry fileEntry = EndianUtilities.<ExtendedFileEntry> toStruct(ExtendedFileEntry.class, rootDirData, 0);
            if (fileEntry.InformationControlBlock._FileType == FileType.Directory) {
                return new Directory(context, partition, fileEntry);
            }

            return new File(context, partition, fileEntry, (int) partition.getLogicalBlockSize());
        }

        if (rootDirTag._TagIdentifier == TagIdentifier.FileEntry) {
            FileEntry fileEntry = EndianUtilities.<FileEntry> toStruct(FileEntry.class, rootDirData, 0);
            if (fileEntry.InformationControlBlock._FileType == FileType.Directory) {
                return new Directory(context, partition, fileEntry);
            }

            return new File(context, partition, fileEntry, (int) partition.getLogicalBlockSize());
        }

        throw new UnsupportedOperationException("Only ExtendedFileEntries implemented");
    }
}
