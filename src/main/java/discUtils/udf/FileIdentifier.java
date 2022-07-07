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

import discUtils.core.coreCompat.FileAttributes;
import discUtils.core.vfs.VfsDirEntry;
import discUtils.streams.IByteArraySerializable;
import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.MathUtilities;


public class FileIdentifier extends VfsDirEntry implements IByteArraySerializable {
    public DescriptorTag DescriptorTag;

    public EnumSet<FileCharacteristic> _fileCharacteristics;

    public LongAllocationDescriptor FileLocation;

    public short FileVersionNumber;

    public byte[] ImplementationUse;

    public short ImplementationUseLength;

    public String Name;

    private byte nameLength;

    public int getNameLength() {
        return nameLength & 0xff;
    }

    public long getCreationTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public EnumSet<FileAttributes> getFileAttributes() {
        throw new UnsupportedOperationException();
    }

    public String getFileName() {
        return Name;
    }

    public boolean hasVfsFileAttributes() {
        return false;
    }

    public boolean hasVfsTimeInfo() {
        return false;
    }

    public boolean isDirectory() {
        return _fileCharacteristics.contains(FileCharacteristic.Directory);
    }

    public boolean isSymlink() {
        return false;
    }

    public long getLastAccessTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public long getLastWriteTimeUtc() {
        throw new UnsupportedOperationException();
    }

    public long getUniqueCacheId() {
        return ((long) FileLocation.ExtentLocation.getPartition()) << 32 | FileLocation.ExtentLocation.LogicalBlock;
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        DescriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        FileVersionNumber = EndianUtilities.toUInt16LittleEndian(buffer, offset + 16);
        _fileCharacteristics = FileCharacteristic.valueOf(buffer[offset + 18]);
        nameLength = buffer[offset + 19];
        FileLocation = EndianUtilities.toStruct(LongAllocationDescriptor.class, buffer, offset + 20);
        ImplementationUseLength = EndianUtilities.toUInt16LittleEndian(buffer, offset + 36);
        ImplementationUse = EndianUtilities.toByteArray(buffer, offset + 38, ImplementationUseLength);
        Name = UdfUtilities.readDCharacters(buffer, offset + 38 + ImplementationUseLength, getNameLength());
        return MathUtilities.roundUp(38 + ImplementationUseLength + getNameLength(), 4);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
