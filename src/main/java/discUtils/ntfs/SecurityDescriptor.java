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

package discUtils.ntfs;

import java.io.PrintWriter;
import java.util.EnumSet;

import discUtils.core.IDiagnosticTraceable;
import discUtils.streams.IByteArraySerializable;
import dotnet4j.io.IOException;
import dotnet4j.security.accessControl.AccessControlSections;
import dotnet4j.security.accessControl.AceFlags;
import dotnet4j.security.accessControl.ControlFlags;
import dotnet4j.security.accessControl.GenericAce;
import dotnet4j.security.accessControl.RawAcl;
import dotnet4j.security.accessControl.RawSecurityDescriptor;
import vavi.util.ByteUtil;


public final class SecurityDescriptor implements IByteArraySerializable, IDiagnosticTraceable {

    public SecurityDescriptor() {
    }

    public SecurityDescriptor(RawSecurityDescriptor secDesc) {
        setDescriptor(secDesc);
    }

    private RawSecurityDescriptor descriptor;

    public RawSecurityDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(RawSecurityDescriptor value) {
        descriptor = value;
    }

    public int size() {
        return (int) getDescriptor().getBinaryLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        setDescriptor(new RawSecurityDescriptor(buffer, offset));
        return (int) getDescriptor().getBinaryLength();
    }

    public void writeTo(byte[] buffer, int offset) {
        // Write out the security descriptor manually because on NTFS the DACL is written
        // before the Owner & Group.  Writing the components in the same order means the
        // hashes will match for identical Security Descriptors.
        EnumSet<ControlFlags> controlFlags = getDescriptor().getControlFlags();
        buffer[offset + 0x00] = 1;
        buffer[offset + 0x01] = (byte) getDescriptor().getResourceManagerControl();
        ByteUtil.writeLeShort((short) ControlFlags.valueOf(controlFlags), buffer, offset + 0x02);

        // Blank out offsets, will fill later
        for (int i = 0x04; i < 0x14; ++i) {
            buffer[offset + i] = 0;
        }

        int pos = 0x14;

        RawAcl discAcl = getDescriptor().getDiscretionaryAcl();
        if (controlFlags.contains(ControlFlags.DiscretionaryAclPresent) && discAcl != null) {
            ByteUtil.writeLeInt(pos, buffer, offset + 0x10);
            discAcl.getBinaryForm(buffer, offset + pos);
            pos += getDescriptor().getDiscretionaryAcl().getBinaryLength();
        } else {
            ByteUtil.writeLeInt(0, buffer, offset + 0x10);
        }

        RawAcl sysAcl = getDescriptor().getSystemAcl();
        if (controlFlags.contains(ControlFlags.SystemAclPresent) && sysAcl != null) {
            ByteUtil.writeLeInt(pos, buffer, offset + 0x0C);
            sysAcl.getBinaryForm(buffer, offset + pos);
            pos += getDescriptor().getSystemAcl().getBinaryLength();
        } else {
            ByteUtil.writeLeInt(0, buffer, offset + 0x0C);
        }

        ByteUtil.writeLeInt(pos, buffer, offset + 0x04);
        getDescriptor().getOwner().getBinaryForm(buffer, offset + pos);
        pos += getDescriptor().getOwner().getBinaryLength();

        ByteUtil.writeLeInt(pos, buffer, offset + 0x08);
        getDescriptor().getGroup().getBinaryForm(buffer, offset + pos);
        pos += getDescriptor().getGroup().getBinaryLength();

        if (pos != getDescriptor().getBinaryLength()) {
            throw new IOException("Failed to write Security Descriptor correctly");
        }
    }

    public void dump(PrintWriter writer, String indent) {
        writer.println(indent + "Descriptor: " + getDescriptor().getSddlForm(AccessControlSections.All));
    }

    public int calcHash() {
        byte[] buffer = new byte[size()];
        writeTo(buffer, 0);
        int hash = 0;
        for (int i = 0; i < buffer.length / 4; ++i) {
            hash = ByteUtil.readLeInt(buffer, i * 4) + ((hash << 3) | (hash >>> 29));
        }
        return hash;
    }

    public static RawSecurityDescriptor calcNewObjectDescriptor(RawSecurityDescriptor parent, boolean isContainer) {
        RawAcl sacl = inheritAcl(parent.getSystemAcl(), isContainer);
        RawAcl dacl = inheritAcl(parent.getDiscretionaryAcl(), isContainer);
        return new RawSecurityDescriptor(parent.getControlFlags(), parent.getOwner(), parent.getGroup(), sacl, dacl);
    }

    private static RawAcl inheritAcl(RawAcl parentAcl, boolean isContainer) {
        AceFlags inheritTest = isContainer ? AceFlags.ContainerInherit : AceFlags.ObjectInherit;

        RawAcl newAcl = null;
        if (parentAcl != null) {
            newAcl = new RawAcl(parentAcl.getRevision(), parentAcl.getCount());
            for (GenericAce ace : parentAcl) {
                if (ace.getAceFlags().contains(inheritTest)) {
                    GenericAce newAce = (GenericAce) ace.clone();

                    EnumSet<AceFlags> newFlags = ace.getAceFlags();
                    if (newFlags.contains(AceFlags.NoPropagateInherit)) {
                        newFlags.remove(AceFlags.ContainerInherit);
                        newFlags.remove(AceFlags.ObjectInherit);
                        newFlags.remove(AceFlags.NoPropagateInherit);
                    }

                    newFlags.remove(AceFlags.InheritOnly);
                    newFlags.add(AceFlags.Inherited);
                    newAce.setAceFlags(newFlags);
                    newAcl.insertAce(newAcl.getCount(), newAce);
                }
            }
        }

        return newAcl;
    }
}
