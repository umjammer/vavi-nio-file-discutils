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

import java.nio.charset.Charset;
import java.util.Arrays;


public class BaseVolumeDescriptor {
    public static final String Iso9660StandardIdentifier = "CD001";

    public final String StandardIdentifier;

    public final VolumeDescriptorType _VolumeDescriptorType;

    public final byte VolumeDescriptorVersion;

    public BaseVolumeDescriptor(VolumeDescriptorType type, byte version) {
        _VolumeDescriptorType = type;
        StandardIdentifier = "CD001";
        VolumeDescriptorVersion = version;
    }

    public BaseVolumeDescriptor(byte[] src, int offset) {
        _VolumeDescriptorType = VolumeDescriptorType.valueOf(src[offset + 0] & 0xff);
        StandardIdentifier = new String(src, offset + 1, 5, Charset.forName("ASCII"));
        VolumeDescriptorVersion = src[offset + 6];
    }

    public void writeTo(byte[] buffer, int offset) {
        Arrays.fill(buffer, offset, offset + IsoUtilities.SectorSize, (byte) 0);
        buffer[offset] = (byte) _VolumeDescriptorType.getValue();
        IsoUtilities.writeAChars(buffer, offset + 1, 5, StandardIdentifier);
        buffer[offset + 6] = VolumeDescriptorVersion;
    }
}
