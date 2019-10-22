//
// Copyright (C) 2006-2007 Novell, Inc (http://www.novell.com)
// Copyright (C) 2012      James Bellinger
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
import java.util.UUID;

import DiscUtils.Streams.Util.EndianUtilities;


/**
 * System.Security.AccessControl.ObjectAce implementation
 *
 * @author Dick Porter <dick@ximian.com>
 * @author Atsushi Enomoto <atsushi@ximian.com>
 * @author Kenneth Bell
 * @author James Bellinger <jfb@zer7.com>
 */
public class ObjectAce extends QualifiedAce {
    private UUID objectAceType;

    private UUID inheritedObjectAceType;

    private EnumSet<ObjectAceFlags> objectAceFlags;

    public ObjectAce(EnumSet<AceFlags> aceFlags,
            AceQualifier qualifier,
            int accessMask,
            SecurityIdentifier sid,
            EnumSet<ObjectAceFlags> flags,
            UUID type,
            UUID inheritedType,
            boolean isCallback,
            byte[] opaque) {
        super(convertType(qualifier, isCallback), aceFlags, opaque);

        this.accessMask = accessMask;
        securityIdentifier = sid;
        objectAceFlags = flags;
        objectAceType = type;
        inheritedObjectAceType = inheritedType;
    }

    ObjectAce(AceType type,
            EnumSet<AceFlags> flags,
            int accessMask,
            SecurityIdentifier sid,
            EnumSet<ObjectAceFlags> objFlags,
            UUID objType,
            UUID inheritedType,
            byte[] opaque) {
        super(type, flags, opaque);

        this.accessMask = accessMask;
        securityIdentifier = sid;
        objectAceFlags = objFlags;
        objectAceType = objType;
        inheritedObjectAceType = inheritedType;
    }

    ObjectAce(byte[] binaryForm, int offset) {
        super(binaryForm, offset);

        int len = EndianUtilities.toUInt16LittleEndian(binaryForm, offset + 2);
        int lenMinimum = 12 + SecurityIdentifier.MinBinaryLength;

        if (offset > binaryForm.length - len)
            throw new IllegalArgumentException("Invalid ACE - truncated");
        if (len < lenMinimum)
            throw new IllegalArgumentException("Invalid ACE");

        accessMask = EndianUtilities.toInt32LittleEndian(binaryForm, offset + 4);
        objectAceFlags = ObjectAceFlags.valueOf(EndianUtilities.toInt32LittleEndian(binaryForm, offset + 8));

        if (getObjectAceTypePresent())
            lenMinimum += 16;
        if (isInheritedObjectAceTypePresent())
            lenMinimum += 16;
        if (len < lenMinimum)
            throw new IllegalArgumentException("Invalid ACE");

        int pos = 12;
        if (getObjectAceTypePresent()) {
            objectAceType = EndianUtilities.toGuidLittleEndian(binaryForm, offset + pos);
            pos += 16;
        }
        if (isInheritedObjectAceTypePresent()) {
            inheritedObjectAceType = EndianUtilities.toGuidLittleEndian(binaryForm, offset + pos);
            pos += 16;
        }

        securityIdentifier = new SecurityIdentifier(binaryForm, offset + pos);
        pos += securityIdentifier.getBinaryLength();

        int opaqueLen = len - pos;
        if (opaqueLen > 0) {
            byte[] opaque = new byte[opaqueLen];
            System.arraycopy(binaryForm, offset + pos, opaque, 0, opaqueLen);
            setOpaque(opaque);
        }
    }

    public int getBinaryLength() {
        int length = 12 + securityIdentifier.getBinaryLength() + getOpaqueLength();
        if (getObjectAceTypePresent())
            length += 16;
        if (isInheritedObjectAceTypePresent())
            length += 16;
        return length;
    }

    public UUID getInheritedObjectAceType() {
        return inheritedObjectAceType;
    }

    void setInheritedObjectAceType(UUID value) {
        inheritedObjectAceType = value;
    }

    boolean isInheritedObjectAceTypePresent() {
        return objectAceFlags.contains(ObjectAceFlags.InheritedObjectAceTypePresent);
    }

    public EnumSet<ObjectAceFlags> getObjectAceFlags() {
        return objectAceFlags;
    }

    void setObjectAceFlags(EnumSet<ObjectAceFlags> value) {
        objectAceFlags = value;
    }

    UUID getObjectAceType() {
        return objectAceType;
    }

    void setObjectAceType(UUID value) {
        objectAceType = value;
    }

    boolean getObjectAceTypePresent() {
        return objectAceFlags.contains(ObjectAceFlags.ObjectAceTypePresent);
    }

    public void getBinaryForm(byte[] binaryForm, int offset) {
        int len = getBinaryLength();
        binaryForm[offset++] = (byte) this.aceType.ordinal();
        binaryForm[offset++] = (byte) AceFlags.valueOf(this.aceFlags);
        EndianUtilities.writeBytesLittleEndian((short) len, binaryForm, offset);
        offset += 2;
        EndianUtilities.writeBytesLittleEndian(accessMask, binaryForm, offset);
        offset += 4;
        EndianUtilities.writeBytesLittleEndian((int) ObjectAceFlags.valueOf(objectAceFlags), binaryForm, offset);
        offset += 4;

        if (objectAceFlags.contains(ObjectAceFlags.ObjectAceTypePresent)) {
            EndianUtilities.writeBytesLittleEndian(objectAceType, binaryForm, offset);
            offset += 16;
        }
        if (objectAceFlags.contains(ObjectAceFlags.InheritedObjectAceTypePresent)) {
            EndianUtilities.writeBytesLittleEndian(inheritedObjectAceType, binaryForm, offset);
            offset += 16;
        }

        securityIdentifier.getBinaryForm(binaryForm, offset);
        offset += securityIdentifier.getBinaryLength();

        byte[] opaque = getOpaque();
        if (opaque != null) {
            System.arraycopy(opaque, 0, binaryForm, offset, opaque.length);
            offset += opaque.length;
        }
    }

    public static int maxOpaqueLength(boolean isCallback) {
        // Varies by platform?
        return 65423;
    }

    String getSddlForm() {
        if (getOpaqueLength() != 0)
            throw new UnsupportedOperationException("Unable to convert conditional ACEs to SDDL");

        String objType = "";
        if (objectAceFlags.contains( ObjectAceFlags.ObjectAceTypePresent) )
            objType = objectAceType.toString();

        String inhObjType = "";
        if (objectAceFlags.contains( ObjectAceFlags.InheritedObjectAceTypePresent) )
            inhObjType = inheritedObjectAceType.toString();

        return String.format("(%s;%s;%s;%s;%s;%s)",
                             getSddlAceType(aceType),
                             getSddlAceFlags(aceFlags),
                             getSddlAccessRights(accessMask),
                             objType,
                             inhObjType,
                             securityIdentifier.getSddlForm());
    }

    private static AceType convertType(AceQualifier qualifier, boolean isCallback) {
        switch (qualifier) {
        case AccessAllowed:
            if (isCallback)
                return AceType.AccessAllowedCallbackObject;
            else
                return AceType.AccessAllowedObject;
        case AccessDenied:
            if (isCallback)
                return AceType.AccessDeniedCallbackObject;
            else
                return AceType.AccessDeniedObject;
        case SystemAlarm:
            if (isCallback)
                return AceType.SystemAlarmCallbackObject;
            else
                return AceType.SystemAlarmObject;
        case SystemAudit:
            if (isCallback)
                return AceType.SystemAuditCallbackObject;
            else
                return AceType.SystemAuditObject;
        default:
            throw new IllegalArgumentException("Unrecognized ACE qualifier: " + qualifier);
        }
    }
}
