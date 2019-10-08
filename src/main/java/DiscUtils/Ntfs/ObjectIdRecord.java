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

package DiscUtils.Ntfs;

import java.util.UUID;

import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;


public final class ObjectIdRecord implements IByteArraySerializable {
    public UUID BirthDomainId;

    public UUID BirthObjectId;

    public UUID BirthVolumeId;

    public FileRecordReference MftReference = new FileRecordReference();

    public long getSize() {
        return 0x38;
    }

    public int readFrom(byte[] buffer, int offset) {
        MftReference = new FileRecordReference();
        MftReference.readFrom(buffer, offset);
        BirthVolumeId = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x08);
        BirthObjectId = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x18);
        BirthDomainId = EndianUtilities.toGuidLittleEndian(buffer, offset + 0x28);
        return 0x38;
    }

    public void writeTo(byte[] buffer, int offset) {
        MftReference.writeTo(buffer, offset);
        EndianUtilities.writeBytesLittleEndian(BirthVolumeId, buffer, offset + 0x08);
        EndianUtilities.writeBytesLittleEndian(BirthObjectId, buffer, offset + 0x18);
        EndianUtilities.writeBytesLittleEndian(BirthDomainId, buffer, offset + 0x28);
    }

    public String toString() {
        return String.format("[Data-MftRef:%s,BirthVolId:%s,BirthObjId:%s,BirthDomId:%s]",
                             MftReference,
                             BirthVolumeId,
                             BirthObjectId,
                             BirthDomainId);
    }
}
