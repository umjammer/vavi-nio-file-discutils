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

package discUtils.udf;

import java.util.EnumSet;
import java.util.List;

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.IVfsFile;
import discUtils.streams.buffer.IBuffer;
import discUtils.streams.util.EndianUtilities;


public class File implements IVfsFile {

    protected int blockSize;

    protected IBuffer content;

    protected UdfContext context;

    protected FileEntry fileEntry;

    protected Partition partition;

    public File(UdfContext context, Partition partition, FileEntry fileEntry, int blockSize) {
        this.context = context;
        this.partition = partition;
        this.fileEntry = fileEntry;
        this.blockSize = blockSize;
    }

    public List<ExtendedAttributeRecord> getExtendedAttributes() {
        return fileEntry.extendedAttributes;
    }

    public IBuffer getFileContent() {
        if (content != null) {
            return content;
        }

        content = new FileContentBuffer(context, partition, fileEntry, blockSize);
        return content;
    }

    public long getLastAccessTimeUtc() {
        return fileEntry.accessTime;
    }

    public void setLastAccessTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        return fileEntry.modificationTime;
    }

    public void setLastWriteTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public long getCreationTimeUtc() {
        ExtendedFileEntry efe = fileEntry instanceof ExtendedFileEntry ? (ExtendedFileEntry) fileEntry
                                                                        : null;
        if (efe != null) {
            return efe.creationTime;
        }

        return getLastWriteTimeUtc();
    }

    public void setCreationTimeUtc(long value) {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        EnumSet<FileAttributes> attribs = EnumSet.noneOf(FileAttributes.class);
        EnumSet<InformationControlBlockFlags> flags = fileEntry.informationControlBlock.flags;
        if (fileEntry.informationControlBlock.fileType == FileType.Directory) {
            attribs.add(FileAttributes.Directory);
        } else if (fileEntry.informationControlBlock.fileType == FileType.Fifo ||
                   fileEntry.informationControlBlock.fileType == FileType.Socket ||
                   fileEntry.informationControlBlock.fileType == FileType.SpecialBlockDevice ||
                   fileEntry.informationControlBlock.fileType == FileType.SpecialCharacterDevice ||
                   fileEntry.informationControlBlock.fileType == FileType.TerminalEntry) {
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
        return fileEntry.informationLength;
    }

    public static File fromDescriptor(UdfContext context, LongAllocationDescriptor icb) {
        LogicalPartition partition = context.logicalPartitions.get(icb.extentLocation.getPartition());
        byte[] rootDirData = UdfUtilities.readExtent(context, icb);
        DescriptorTag rootDirTag = EndianUtilities.toStruct(DescriptorTag.class, rootDirData, 0);
        if (rootDirTag.tagIdentifier == TagIdentifier.ExtendedFileEntry) {
            ExtendedFileEntry fileEntry = EndianUtilities.toStruct(ExtendedFileEntry.class, rootDirData, 0);
            if (fileEntry.informationControlBlock.fileType == FileType.Directory) {
                return new Directory(context, partition, fileEntry);
            }

            return new File(context, partition, fileEntry, (int) partition.getLogicalBlockSize());
        }

        if (rootDirTag.tagIdentifier == TagIdentifier.FileEntry) {
            FileEntry fileEntry = EndianUtilities.toStruct(FileEntry.class, rootDirData, 0);
            if (fileEntry.informationControlBlock.fileType == FileType.Directory) {
                return new Directory(context, partition, fileEntry);
            }

            return new File(context, partition, fileEntry, (int) partition.getLogicalBlockSize());
        }

        throw new UnsupportedOperationException("Only ExtendedFileEntries implemented");
    }
}
