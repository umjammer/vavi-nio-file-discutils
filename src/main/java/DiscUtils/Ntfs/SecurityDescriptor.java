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

package DiscUtils.Ntfs;

import java.io.PrintWriter;
import java.security.Permission;
import java.security.acl.Acl;

import DiscUtils.Core.IDiagnosticTraceable;
import DiscUtils.Streams.IByteArraySerializable;
import DiscUtils.Streams.Util.EndianUtilities;
import moe.yo3explorer.dotnetio4j.AccessControlSections;
import moe.yo3explorer.dotnetio4j.IOException;
import moe.yo3explorer.dotnetio4j.compat.RawSecurityDescriptor;


public final class SecurityDescriptor implements IByteArraySerializable, IDiagnosticTraceable {
    public SecurityDescriptor() {
    }

    public SecurityDescriptor(RawSecurityDescriptor secDesc) {
        setDescriptor(secDesc);
    }

    private RawSecurityDescriptor __Descriptor;

    public RawSecurityDescriptor getDescriptor() {
        return __Descriptor;
    }

    public void setDescriptor(RawSecurityDescriptor value) {
        __Descriptor = value;
    }

    public long getSize() {
        return getDescriptor().getBinaryLength();
    }

    public int readFrom(byte[] buffer, int offset) {
        setDescriptor(new RawSecurityDescriptor(buffer, offset));
        return (int) getDescriptor().getBinaryLength();
    }

    public void writeTo(byte[] buffer, int offset) {
        // Write out the security descriptor manually because on NTFS the DACL is written
        // before the Owner & Group.  Writing the components in the same order means the
        // hashes will match for identical Security Descriptors.
        ControlFlags controlFlags = getDescriptor().ControlFlags;
        buffer[offset + 0x00] = 1;
        buffer[offset + 0x01] = getDescriptor().ResourceManagerControl;
        EndianUtilities.writeBytesLittleEndian((short) controlFlags, buffer, offset + 0x02);
        for (int i = 0x04; i < 0x14; ++i) {
            // Blank out offsets, will fill later
            buffer[offset + i] = 0;
        }
        int pos = 0x14;
        Acl discAcl = getDescriptor().DiscretionaryAcl;
        if ((controlFlags & ControlFlags.DiscretionaryAclPresent) != 0 && discAcl != null) {
            EndianUtilities.writeBytesLittleEndian(pos, buffer, offset + 0x10);
            discAcl.GetBinaryForm(buffer, offset + pos);
            pos += getDescriptor().DiscretionaryAcl.BinaryLength;
        } else {
            EndianUtilities.writeBytesLittleEndian(0, buffer, offset + 0x10);
        }
        Acl sysAcl = getDescriptor().SystemAcl;
        if ((controlFlags & ControlFlags.SystemAclPresent) != 0 && sysAcl != null) {
            EndianUtilities.writeBytesLittleEndian(pos, buffer, offset + 0x0C);
            sysAcl.GetBinaryForm(buffer, offset + pos);
            pos += getDescriptor().SystemAcl.BinaryLength;
        } else {
            EndianUtilities.writeBytesLittleEndian(0, buffer, offset + 0x0C);
        }
        EndianUtilities.writeBytesLittleEndian(pos, buffer, offset + 0x04);
        getDescriptor().getOwner().getBinaryForm(buffer, offset + pos);
        pos += getDescriptor().getOwner().getBinaryLength();
        EndianUtilities.writeBytesLittleEndian(pos, buffer, offset + 0x08);
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
        byte[] buffer = new byte[(int)getSize()];
        writeTo(buffer, 0);
        int hash = 0;
        for (int i = 0; i < buffer.length / 4; ++i) {
            hash = EndianUtilities.toUInt32LittleEndian(buffer, i * 4) + ((hash << 3) | (hash >>> 29));
        }
        return hash;
    }

    public static RawSecurityDescriptor calcNewObjectDescriptor(RawSecurityDescriptor parent, boolean isContainer) {
        Acl sacl = inheritAcl(parent.SystemAcl, isContainer);
        Acl dacl = InheritAcl(parent.DiscretionaryAcl, isContainer);
        return new RawSecurityDescriptor(parent.ControlFlags, parent.getOwner(), parent.getGroup(), sacl, dacl);
    }

    private static Acl inheritAcl(Acl parentAcl, boolean isContainer) {
        AceFlags inheritTest = isContainer ? AceFlags.ContainerInherit : AceFlags.ObjectInherit;
        Acl newAcl = null;
        if (parentAcl != null) {
            newAcl = new Acl(parentAcl.Revision, parentAcl.size());
            for (GenericAce ace : parentAcl) {
                if ((ace.AceFlags & inheritTest) != 0) {
                    GenericAce newAce = ace.Copy();
                    AceFlags newFlags = ace.AceFlags;
                    if ((newFlags & AceFlags.NoPropagateInherit) != 0) {
                        newFlags &= ~(AceFlags.ContainerInherit | AceFlags.ObjectInherit | AceFlags.NoPropagateInherit);
                    }

                    newFlags &= ~AceFlags.InheritOnly;
                    newFlags |= AceFlags.Inherited;
                    newAce.AceFlags = newFlags;
                    newAcl.InsertAce(newAcl.size(), newAce);
                }
            }
        }

        return newAcl;
    }
}
