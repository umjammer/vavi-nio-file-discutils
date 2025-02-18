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

package discUtils.vdi;

import java.nio.charset.StandardCharsets;

import discUtils.streams.util.EndianUtilities;
import discUtils.streams.util.StreamUtilities;
import dotnet4j.io.Stream;
import vavi.util.ByteUtil;


public class PreHeaderRecord {

    public static final int VdiSignature = 0xbeda107f;

    public static final int Size = 72;

    public String fileInfo;

    public int signature;

    public FileVersion version;

    public static PreHeaderRecord initialized() {
        PreHeaderRecord result = new PreHeaderRecord();
        result.fileInfo = "<<< Sun xVM VirtualBox Disk Image >>>\n";
        result.signature = VdiSignature;
        result.version = new FileVersion(0x00010001);
        return result;
    }

    public int read(byte[] buffer, int offset) {
        fileInfo = new String(buffer, offset + 0, 64, StandardCharsets.US_ASCII).replaceFirst("\0*$", "");
        signature = ByteUtil.readLeInt(buffer, offset + 64);
        version = new FileVersion(ByteUtil.readLeInt(buffer, offset + 68));
        return Size;
    }

    public void read(Stream s) {
        byte[] buffer = StreamUtilities.readExact(s, 72);
        read(buffer, 0);
    }

    public void write(Stream s) {
        byte[] buffer = new byte[Size];
        write(buffer, 0);
        s.write(buffer, 0, buffer.length);
    }

    public void write(byte[] buffer, int offset) {
        EndianUtilities.stringToBytes(fileInfo, buffer, offset + 0, 64);
        ByteUtil.writeLeInt(signature, buffer, offset + 64);
        ByteUtil.writeLeInt(version.getValue(), buffer, offset + 68);
    }
}
