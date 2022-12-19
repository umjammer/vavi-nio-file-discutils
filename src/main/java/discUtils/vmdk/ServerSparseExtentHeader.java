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

package discUtils.vmdk;

import discUtils.streams.util.Sizes;
import vavi.util.ByteUtil;


public class ServerSparseExtentHeader extends CommonSparseExtentHeader {

    public static final int CowdMagicNumber = 0x44574f43;

    public int flags;

    public int freeSector;

    public int numGdEntries;

    public int savedGeneration;

    public int uncleanShutdown;

    public ServerSparseExtentHeader() {
        magicNumber = CowdMagicNumber;
        version = 1;
        grainSize = 512;
        numGTEsPerGT = 4096;
        flags = 3;
    }

    public static ServerSparseExtentHeader read(byte[] buffer, int offset) {
        ServerSparseExtentHeader hdr = new ServerSparseExtentHeader();
        hdr.magicNumber = ByteUtil.readLeInt(buffer, offset + 0x00);
        hdr.version = ByteUtil.readLeInt(buffer, offset + 0x04);
        hdr.flags = ByteUtil.readLeInt(buffer, offset + 0x08);
        hdr.capacity = ByteUtil.readLeInt(buffer, offset + 0x0C);
        hdr.grainSize = ByteUtil.readLeInt(buffer, offset + 0x10);
        hdr.gdOffset = ByteUtil.readLeInt(buffer, offset + 0x14);
        hdr.numGdEntries = ByteUtil.readLeInt(buffer, offset + 0x18);
        hdr.freeSector = ByteUtil.readLeInt(buffer, offset + 0x1C);
        hdr.savedGeneration = ByteUtil.readLeInt(buffer, offset + 0x660);
        hdr.uncleanShutdown = ByteUtil.readLeInt(buffer, offset + 0x66C);
        hdr.numGTEsPerGT = 4096;
        return hdr;
    }

    public byte[] getBytes() {
        byte[] buffer = new byte[Sizes.Sector * 4];
        ByteUtil.writeLeInt(magicNumber, buffer, 0x00);
        ByteUtil.writeLeInt(version, buffer, 0x04);
        ByteUtil.writeLeInt(flags, buffer, 0x08);
        ByteUtil.writeLeInt((int) capacity, buffer, 0x0C);
        ByteUtil.writeLeInt((int) grainSize, buffer, 0x10);
        ByteUtil.writeLeInt((int) gdOffset, buffer, 0x14);
        ByteUtil.writeLeInt(numGdEntries, buffer, 0x18);
        ByteUtil.writeLeInt(freeSector, buffer, 0x1C);
        ByteUtil.writeLeInt(savedGeneration, buffer, 0x660);
        ByteUtil.writeLeInt(uncleanShutdown, buffer, 0x66C);
        return buffer;
    }
}
