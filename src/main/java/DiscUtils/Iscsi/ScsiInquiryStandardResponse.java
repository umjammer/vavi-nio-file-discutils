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

package DiscUtils.Iscsi;

import DiscUtils.Streams.Util.EndianUtilities;


public class ScsiInquiryStandardResponse extends ScsiResponse {
    private boolean _truncated;

    private LunClass __DeviceType = LunClass.BlockStorage;

    public LunClass getDeviceType() {
        return __DeviceType;
    }

    public void setDeviceType(LunClass value) {
        __DeviceType = value;
    }

    public int getNeededDataLength() {
        return 36;
    }

    private String __ProductId;

    public String getProductId() {
        return __ProductId;
    }

    public void setProductId(String value) {
        __ProductId = value;
    }

    private String __ProductRevision;

    public String getProductRevision() {
        return __ProductRevision;
    }

    public void setProductRevision(String value) {
        __ProductRevision = value;
    }

    private boolean __Removable;

    public boolean getRemovable() {
        return __Removable;
    }

    public void setRemovable(boolean value) {
        __Removable = value;
    }

    private byte __SpecificationVersion;

    public byte getSpecificationVersion() {
        return __SpecificationVersion;
    }

    public void setSpecificationVersion(byte value) {
        __SpecificationVersion = value;
    }

    public boolean getTruncated() {
        return _truncated;
    }

    private String __VendorId;

    public String getVendorId() {
        return __VendorId;
    }

    public void setVendorId(String value) {
        __VendorId = value;
    }

    public void readFrom(byte[] buffer, int offset, int count) {
        if (count < 36) {
            _truncated = true;
            return;
        }

        setDeviceType(LunClass.valueOf(buffer[0] & 0x1F));
        setRemovable((buffer[1] & 0x80) != 0);
        setSpecificationVersion(buffer[2]);
        setVendorId(EndianUtilities.bytesToString(buffer, 8, 8));
        setProductId(EndianUtilities.bytesToString(buffer, 16, 16));
        setProductRevision(EndianUtilities.bytesToString(buffer, 32, 4));
    }
}
