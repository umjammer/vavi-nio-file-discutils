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

package discUtils.squashFs;

import discUtils.streams.util.EndianUtilities;


public final class DeviceInode extends Inode {

    private int deviceId;

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int value) {
        deviceId = value;
    }

    public int size() {
        return 24;
    }

    public int readFrom(byte[] buffer, int offset) {
        super.readFrom(buffer, offset);
        numLinks = EndianUtilities.toInt32LittleEndian(buffer, offset + 16);
        deviceId = EndianUtilities.toUInt32LittleEndian(buffer, offset + 20);
        return 24;
    }
}
