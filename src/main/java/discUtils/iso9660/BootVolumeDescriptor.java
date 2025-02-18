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

package discUtils.iso9660;

import java.nio.charset.StandardCharsets;

import discUtils.streams.util.EndianUtilities;
import vavi.util.ByteUtil;


public class BootVolumeDescriptor extends BaseVolumeDescriptor {

    public static final String ElToritoSystemIdentifier = "EL TORITO SPECIFICATION";

    public BootVolumeDescriptor(int catalogSector) {
        super(VolumeDescriptorType.Boot, (byte) 1);
        this.catalogSector = catalogSector;
    }

    public BootVolumeDescriptor(byte[] src, int offset) {
        super(src, offset);
        systemId = new String(src, offset + 0x7, 0x20, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        catalogSector = ByteUtil.readLeInt(src, offset + 0x47);
    }

    private final int catalogSector;

    public int getCatalogSector() {
        return catalogSector;
    }

    private String systemId;

    public String getSystemId() {
        return systemId;
    }

    @Override public void writeTo(byte[] buffer, int offset) {
        super.writeTo(buffer, offset);
        EndianUtilities.stringToBytes(ElToritoSystemIdentifier, buffer, offset + 7, 0x20);
        ByteUtil.writeLeInt(catalogSector, buffer, offset + 0x47);
    }
}
