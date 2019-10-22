//
// Copyright (C) 2006-2007 Novell, Inc (http://www.novell.com)
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

package moe.yo3explorer.dotnetio4j.compat;

import java.util.EnumSet;

import DiscUtils.Streams.Util.EndianUtilities;


/**
 * System.Security.AccessControl.CommonAce implementation
 *
 * @author Dick Porter <dick@ximian.com>
 * @author Atsushi Enomoto <atsushi@ximian.com>
 * @author Kenneth Bell
 */
public class CommonAce extends QualifiedAce {
    public CommonAce(EnumSet<AceFlags> flags,
            AceQualifier qualifier,
            int accessMask,
            SecurityIdentifier sid,
            boolean isCallback,
            byte[] opaque) {
        super(convertType(qualifier, isCallback), flags, opaque);

        this.accessMask = accessMask;
        securityIdentifier = sid;
    }

    CommonAce(AceType type, EnumSet<AceFlags> flags, int accessMask, SecurityIdentifier sid, byte[] opaque) {
        super(type, flags, opaque);

        this.accessMask = accessMask;
        securityIdentifier = sid;
    }

    CommonAce(byte[] binaryForm, int offset) {
        super(binaryForm, offset);

        int len = EndianUtilities.toUInt16LittleEndian(binaryForm, offset + 2);
        if (offset > binaryForm.length - len)
            throw new IllegalArgumentException("Invalid ACE - truncated");
        if (len < 8 + SecurityIdentifier.MinBinaryLength)
            throw new IllegalArgumentException("Invalid ACE");

        accessMask = EndianUtilities.toInt32LittleEndian(binaryForm, offset + 4);
        securityIdentifier = new SecurityIdentifier(binaryForm, offset + 8);

        int opaqueLen = len - (8 + securityIdentifier.getBinaryLength());
        if (opaqueLen > 0) {
            byte[] opaque = new byte[opaqueLen];
            System.arraycopy(binaryForm, offset + 8 + securityIdentifier.getBinaryLength(), opaque, 0, opaqueLen);
            setOpaque(opaque);
        }
    }

    public int getBinaryLength() {
        return 8 + securityIdentifier.getBinaryLength() + getOpaqueLength();
    }

    public void getBinaryForm(byte[] binaryForm, int offset) {
        int len = getBinaryLength();
        binaryForm[offset] = (byte) this.aceType.ordinal();
        binaryForm[offset + 1] = (byte) AceFlags.valueOf(this.aceFlags);
        EndianUtilities.writeBytesLittleEndian((short) len, binaryForm, offset + 2);
        EndianUtilities.writeBytesLittleEndian(accessMask, binaryForm, offset + 4);

        securityIdentifier.getBinaryForm(binaryForm, offset + 8);

        byte[] opaque = getOpaque();
        if (opaque != null)
            System.arraycopy(opaque, 0, binaryForm, offset + 8 + securityIdentifier.getBinaryLength(), opaque.length);
    }

    public static int maxOpaqueLength(boolean isCallback) {
        // Varies by platform?
        return 65459;
    }

    /** */
    public String getSddlForm() {
        if (getOpaqueLength() != 0)
            throw new UnsupportedOperationException("Unable to convert conditional ACEs to SDDL");

        return String.format("(%s;%s;%s;;;%s)",
                             getSddlAceType(aceType),
                             getSddlAceFlags(aceFlags),
                             getSddlAccessRights(accessMask),
                             securityIdentifier.getSddlForm());
    }

    private static AceType convertType(AceQualifier qualifier, boolean isCallback) {
        switch (qualifier) {
        case AccessAllowed:
            if (isCallback)
                return AceType.AccessAllowedCallback;
            else
                return AceType.AccessAllowed;
        case AccessDenied:
            if (isCallback)
                return AceType.AccessDeniedCallback;
            else
                return AceType.AccessDenied;
        case SystemAlarm:
            if (isCallback)
                return AceType.SystemAlarmCallback;
            else
                return AceType.SystemAlarm;
        case SystemAudit:
            if (isCallback)
                return AceType.SystemAuditCallback;
            else
                return AceType.SystemAudit;
        default:
            throw new IllegalArgumentException("Unrecognized ACE qualifier: " + qualifier);
        }
    }
}
