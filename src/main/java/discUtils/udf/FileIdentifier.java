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
import vavi.util.ByteUtil;


public class FileIdentifier extends VfsDirEntry implements IByteArraySerializable {

    public DescriptorTag descriptorTag;

    public EnumSet<FileCharacteristic> fileCharacteristics;

    public LongAllocationDescriptor fileLocation;

    public short fileVersionNumber;

    public byte[] implementationUse;

    public short implementationUseLength;

    public String name;

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
        return name;
    }

    public boolean hasVfsFileAttributes() {
        return false;
    }

    public boolean hasVfsTimeInfo() {
        return false;
    }

    public boolean isDirectory() {
        return fileCharacteristics.contains(FileCharacteristic.Directory);
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
        return ((long) fileLocation.extentLocation.getPartition()) << 32 | fileLocation.extentLocation.logicalBlock;
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public int readFrom(byte[] buffer, int offset) {
        descriptorTag = EndianUtilities.toStruct(DescriptorTag.class, buffer, offset);
        fileVersionNumber = ByteUtil.readLeShort(buffer, offset + 16);
        fileCharacteristics = FileCharacteristic.valueOf(buffer[offset + 18]);
        nameLength = buffer[offset + 19];
        fileLocation = EndianUtilities.toStruct(LongAllocationDescriptor.class, buffer, offset + 20);
        implementationUseLength = ByteUtil.readLeShort(buffer, offset + 36);
        implementationUse = EndianUtilities.toByteArray(buffer, offset + 38, implementationUseLength);
        name = UdfUtilities.readDCharacters(buffer, offset + 38 + implementationUseLength, getNameLength());
        return MathUtilities.roundUp(38 + implementationUseLength + getNameLength(), 4);
    }

    public void writeTo(byte[] buffer, int offset) {
        throw new UnsupportedOperationException();
    }
}
