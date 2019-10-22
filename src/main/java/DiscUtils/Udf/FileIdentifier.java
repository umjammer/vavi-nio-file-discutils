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

import DiscUtils.Core.CoreCompat.FileAttributes;
import DiscUtils.Core.Vfs.VfsDirEntry;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import DiscUtils.Streams.Util.MathUtilities;


public class FileIdentifier extends VfsDirEntry implements IByteArraySerializable {
    public DescriptorTag DescriptorTag;

    public EnumSet<FileCharacteristic> _FileCharacteristics;

    public LongAllocationDescriptor FileLocation;

    public short FileVersionNumber;

    public byte[] ImplementationUse;

    public short ImplementationUseLength;

    public String Name;

    public byte NameLength;

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
        return _FileCharacteristics.contains(FileCharacteristic.Directory);
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
        return (long) FileLocation.ExtentLocation.Partition << 32 | FileLocation.ExtentLocation.LogicalBlock;
    }

    public long getSize() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        DescriptorTag = EndianUtilities.<DescriptorTag> toStruct(DescriptorTag.class, buffer, offset);
        FileVersionNumber = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 16);
        _FileCharacteristics = FileCharacteristic.valueOf(buffer[offset + 18]);
        NameLength = buffer[offset + 19];
        FileLocation = EndianUtilities.<LongAllocationDescriptor> toStruct(LongAllocationDescriptor.class, buffer, offset + 20);
        ImplementationUseLength = (short) EndianUtilities.toUInt16LittleEndian(buffer, offset + 36);
        ImplementationUse = EndianUtilities.toByteArray(buffer, offset + 38, ImplementationUseLength);
        Name = UdfUtilities.readDCharacters(buffer, offset + 38 + ImplementationUseLength, NameLength);
        return MathUtilities.roundUp(38 + ImplementationUseLength + NameLength, 4);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
