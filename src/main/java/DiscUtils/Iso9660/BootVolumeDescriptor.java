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

package DiscUtils.Iso9660;

import DiscUtils.Streams.Util.EndianUtilities;


public class BootVolumeDescriptor extends BaseVolumeDescriptor {
    public static final String ElToritoSystemIdentifier = "EL TORITO SPECIFICATION";

    public BootVolumeDescriptor(int catalogSector) {
        super(VolumeDescriptorType.Boot, (byte) 1);
        _catalogSector = catalogSector;
    }

    public BootVolumeDescriptor(byte[] src, int offset) {
        super(src, offset);
        _systemId = EndianUtilities.bytesToString(src, offset + 0x7, 0x20).replaceFirst("\0*$", "");
        _catalogSector = EndianUtilities.toUInt32LittleEndian(src, offset + 0x47);
    }

    private int _catalogSector;

    public int getCatalogSector() {
        return _catalogSector;
    }

    private String _systemId;

    public String getSystemId() {
        return _systemId;
    }

    public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        EndianUtilities.stringToBytes(ElToritoSystemIdentifier, buffer, offset + 7, 0x20);
        EndianUtilities.writeBytesLittleEndian(getCatalogSector(), buffer, offset + 0x47);
    }
}
