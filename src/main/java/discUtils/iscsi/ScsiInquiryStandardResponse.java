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

    private boolean truncated;

    private LunClass deviceType = LunClass.BlockStorage;

    public LunClass getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(LunClass value) {
        deviceType = value;
    }

    public int getNeededDataLength() {
        return 36;
    }

    private String productId;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String value) {
        productId = value;
    }

    private String productRevision;

    public String getProductRevision() {
        return productRevision;
    }

    public void setProductRevision(String value) {
        productRevision = value;
    }

    private boolean removable;

    public boolean getRemovable() {
        return removable;
    }

    public void setRemovable(boolean value) {
        removable = value;
    }

    private byte specificationVersion;

    public byte getSpecificationVersion() {
        return specificationVersion;
    }

    public void setSpecificationVersion(byte value) {
        specificationVersion = value;
    }

    public boolean getTruncated() {
        return truncated;
    }

    private String vendorId;

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String value) {
        vendorId = value;
    }

    public void readFrom(byte[] buffer, int offset, int count) {
        if (count < 36) {
            truncated = true;
            return;
        }

        deviceType = LunClass.valueOf(buffer[0] & 0x1F);
        removable = (buffer[1] & 0x80) != 0;
        specificationVersion = buffer[2];
        vendorId = EndianUtilities.bytesToString(buffer, 8, 8);
        productId = EndianUtilities.bytesToString(buffer, 16, 16);
        productRevision = EndianUtilities.bytesToString(buffer, 32, 4);
    }
}
