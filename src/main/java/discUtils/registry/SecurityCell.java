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

package discUtils.registry;

import discUtils.streams.util.EndianUtilities;
import dotnet4j.security.accessControl.AccessControlSections;
import dotnet4j.security.accessControl.RegistrySecurity;
import vavi.util.ByteUtil;


public final class SecurityCell extends Cell {
    public SecurityCell(RegistrySecurity secDesc) {
        this(-1);
        setSecurityDescriptor(secDesc);
    }

    public SecurityCell(int index) {
        super(index);
        setPreviousIndex(-1);
        setNextIndex(-1);
    }

    private int nextIndex;

    public int getNextIndex() {
        return nextIndex;
    }

    public void setNextIndex(int value) {
        nextIndex = value;
    }

    private int previousIndex;

    public int getPreviousIndex() {
        return previousIndex;
    }

    public void setPreviousIndex(int value) {
        previousIndex = value;
    }

    private RegistrySecurity securityDescriptor;

    public RegistrySecurity getSecurityDescriptor() {
        return securityDescriptor;
    }

    public void setSecurityDescriptor(RegistrySecurity value) {
        securityDescriptor = value;
    }

    public int size() {
        int sdLen = getSecurityDescriptor().getSecurityDescriptorBinaryForm().length;
        return 0x14 + sdLen;
    }

    private int usageCount;

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int value) {
        usageCount = value;
    }

    public int readFrom(byte[] buffer, int offset) {
        previousIndex = ByteUtil.readLeInt(buffer, offset + 0x04);
        nextIndex = ByteUtil.readLeInt(buffer, offset + 0x08);
        usageCount = ByteUtil.readLeInt(buffer, offset + 0x0C);
        int secDescSize = ByteUtil.readLeInt(buffer, offset + 0x10);
        byte[] secDesc = new byte[secDescSize];
        System.arraycopy(buffer, offset + 0x14, secDesc, 0, secDescSize);
        securityDescriptor = new RegistrySecurity();
        securityDescriptor.setSecurityDescriptorBinaryForm(secDesc);
        return 0x14 + secDescSize;
    }

    public void writeTo(byte[] buffer, int offset) {
        byte[] sd = securityDescriptor.getSecurityDescriptorBinaryForm();
        EndianUtilities.stringToBytes("sk", buffer, offset, 2);
        ByteUtil.writeLeInt(previousIndex, buffer, offset + 0x04);
        ByteUtil.writeLeInt(nextIndex, buffer, offset + 0x08);
        ByteUtil.writeLeInt(usageCount, buffer, offset + 0x0C);
        ByteUtil.writeLeInt(sd.length, buffer, offset + 0x10);
        System.arraycopy(sd, 0, buffer, offset + 0x14, sd.length);
    }

    public String toString() {
        return "SecDesc:" + securityDescriptor.getSecurityDescriptorSddlForm(AccessControlSections.All) +
                " (refCount:" + usageCount + ")";
    }
}
