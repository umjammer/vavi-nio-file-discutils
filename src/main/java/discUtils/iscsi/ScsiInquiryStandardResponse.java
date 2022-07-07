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

package discUtils.iscsi;

import discUtils.streams.util.EndianUtilities;


public class ScsiInquiryStandardResponse extends ScsiResponse {
    private boolean _truncated;

    private LunClass _deviceType = LunClass.BlockStorage;

    public LunClass getDeviceType() {
        return _deviceType;
    }

    public void setDeviceType(LunClass value) {
        _deviceType = value;
    }

    public int getNeededDataLength() {
        return 36;
    }

    private String _productId;

    public String getProductId() {
        return _productId;
    }

    public void setProductId(String value) {
        _productId = value;
    }

    private String _productRevision;

    public String getProductRevision() {
        return _productRevision;
    }

    public void setProductRevision(String value) {
        _productRevision = value;
    }

    private boolean _removable;

    public boolean getRemovable() {
        return _removable;
    }

    public void setRemovable(boolean value) {
        _removable = value;
    }

    private byte _specificationVersion;

    public byte getSpecificationVersion() {
        return _specificationVersion;
    }

    public void setSpecificationVersion(byte value) {
        _specificationVersion = value;
    }

    public boolean getTruncated() {
        return _truncated;
    }

    private String _vendorId;

    public String getVendorId() {
        return _vendorId;
    }

    public void setVendorId(String value) {
        _vendorId = value;
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
