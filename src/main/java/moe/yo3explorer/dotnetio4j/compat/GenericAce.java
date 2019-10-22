/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.EnumSet;
import java.util.UUID;


/**
 * GenericAce.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public abstract class GenericAce implements Cloneable {

    protected AceType aceType;

    protected EnumSet<AceFlags> aceFlags;

    GenericAce(AceType type, EnumSet<AceFlags> flags) {
        this.aceType = type;
        this.aceFlags = flags;
    }

    GenericAce(byte[] binaryForm, int offset) {
        if (binaryForm == null)
            throw new NullPointerException("binaryForm");

        if (offset < 0 || offset > binaryForm.length - 2)
            throw new IndexOutOfBoundsException("offset: " + offset);

        aceType = AceType.values()[binaryForm[offset]];
        aceFlags = AceFlags.valueOf(binaryForm[offset + 1]);
    }

    /** */
    public EnumSet<AceFlags> getAceFlags() {
        return aceFlags;
    }

    /** */
    public void setAceFlags(EnumSet<AceFlags> aceFlags) {
        this.aceFlags = aceFlags;
    }

    /** */
    public abstract int getBinaryLength();

    public Object clone() {
        byte[] buffer = new byte[getBinaryLength()];
        getBinaryForm(buffer, 0);
        return createFromBinaryForm(buffer, 0);
    }

    /** */
    public static GenericAce createFromBinaryForm(byte[] binaryForm, int offset) {
        if (binaryForm == null)
            throw new NullPointerException("binaryForm");

        if (offset < 0 || offset > binaryForm.length - 1)
            throw new IndexOutOfBoundsException("offset: " + offset);

        AceType type = AceType.values()[binaryForm[offset]];
        if (isObjectType(type))
            return new ObjectAce(binaryForm, offset);
        else
            return new CommonAce(binaryForm, offset);
    }

    /** */
    public abstract void getBinaryForm(byte[] binaryForm, int offset);

    /** */
    abstract String getSddlForm();

    /** */
    static GenericAce createFromSddlForm(String sddlForm, int[] pos) {
        if (sddlForm.charAt(pos[0]) != '(')
            throw new IllegalArgumentException("Invalid SDDL string.");

        int endPos = sddlForm.indexOf(')', pos[0]);
        if (endPos < 0)
            throw new IllegalArgumentException("Invalid SDDL string.");

        int count = endPos - (pos[0] + 1);
        String elementsStr = sddlForm.substring(pos[0] + 1, pos[0] + 1 + count);
        elementsStr = elementsStr.toUpperCase();
        String[] elements = elementsStr.split(";");
        if (elements.length != 6)
            throw new IllegalArgumentException("Invalid SDDL string.");

        EnumSet<ObjectAceFlags> objFlags = EnumSet.noneOf(ObjectAceFlags.class);

        AceType type = parseSddlAceType(elements[0]);

        EnumSet<AceFlags> flags = parseSddlAceFlags(elements[1]);

        int accessMask = parseSddlAccessRights(elements[2]);

        UUID objectType = new UUID(0, 0);
        if (elements[3] != null && !elements[3].isEmpty()) {
            objectType = UUID.fromString(elements[3]);
            objFlags.add(ObjectAceFlags.ObjectAceTypePresent);
        }

        UUID inhObjectType = new UUID(0, 0);
        if (elements[4] != null && !elements[4].isEmpty()) {
            inhObjectType = UUID.fromString(elements[4]);
            objFlags.add(ObjectAceFlags.InheritedObjectAceTypePresent);
        }

        SecurityIdentifier sid = new SecurityIdentifier(elements[5]);

        if (type == AceType.AccessAllowedCallback || type == AceType.AccessDeniedCallback)
            throw new UnsupportedOperationException("Conditional ACEs not supported");

        pos[0] = endPos + 1;

        if (isObjectType(type)) {
            return new ObjectAce(type, flags, accessMask, sid, objFlags, objectType, inhObjectType, null);
        } else {
            if (!objFlags.isEmpty())
                throw new IllegalArgumentException("Invalid SDDL string.");
            return new CommonAce(type, flags, accessMask, sid, null);
        }
    }

    private static boolean isObjectType(AceType type) {
        return type == AceType.AccessAllowedCallbackObject || type == AceType.AccessAllowedObject
                || type == AceType.AccessDeniedCallbackObject || type == AceType.AccessDeniedObject
                || type == AceType.SystemAlarmCallbackObject || type == AceType.SystemAlarmObject
                || type == AceType.SystemAuditCallbackObject || type == AceType.SystemAuditObject;
    }

    static String getSddlAceType(AceType type) {
        switch (type) {
        case AccessAllowed:
            return "A";
        case AccessDenied:
            return "D";
        case AccessAllowedObject:
            return "OA";
        case AccessDeniedObject:
            return "OD";
        case SystemAudit:
            return "AU";
        case SystemAlarm:
            return "AL";
        case SystemAuditObject:
            return "OU";
        case SystemAlarmObject:
            return "OL";
        case AccessAllowedCallback:
            return "XA";
        case AccessDeniedCallback:
            return "XD";
        default:
            throw new IllegalArgumentException("Unable to convert to SDDL ACE type: " + type);
        }
    }

    static AceType parseSddlAceType(String type) {
        switch (type) {
        case "A":
            return AceType.AccessAllowed;
        case "D":
            return AceType.AccessDenied;
        case "OA":
            return AceType.AccessAllowedObject;
        case "OD":
            return AceType.AccessDeniedObject;
        case "AU":
            return AceType.SystemAudit;
        case "AL":
            return AceType.SystemAlarm;
        case "OU":
            return AceType.SystemAuditObject;
        case "OL":
            return AceType.SystemAlarmObject;
        case "XA":
            return AceType.AccessAllowedCallback;
        case "XD":
            return AceType.AccessDeniedCallback;
        default:
            throw new IllegalArgumentException("Unable to convert SDDL to ACE type: " + type);
        }
    }

    static String getSddlAceFlags(EnumSet<AceFlags> flags) {
        StringBuilder result = new StringBuilder();
        if (flags.contains(AceFlags.ObjectInherit))
            result.append("OI");
        if (flags.contains(AceFlags.ContainerInherit))
            result.append("CI");
        if (flags.contains(AceFlags.NoPropagateInherit))
            result.append("NP");
        if (flags.contains(AceFlags.InheritOnly))
            result.append("IO");
        if (flags.contains(AceFlags.Inherited))
            result.append("ID");
        if (flags.contains(AceFlags.SuccessfulAccess))
            result.append("SA");
        if (flags.contains(AceFlags.FailedAccess))
            result.append("FA");
        return result.toString();
    }

    static EnumSet<AceFlags> parseSddlAceFlags(String flags) {
        EnumSet<AceFlags> ret = EnumSet.noneOf(AceFlags.class);

        int pos = 0;
        while (pos < flags.length() - 1) {
            String flag = flags.substring(pos, pos + 2);
            switch (flag) {
            case "CI":
                ret.add(AceFlags.ContainerInherit);
                break;
            case "OI":
                ret.add(AceFlags.ObjectInherit);
                break;
            case "NP":
                ret.add(AceFlags.NoPropagateInherit);
                break;
            case "IO":
                ret.add(AceFlags.InheritOnly);
                break;
            case "ID":
                ret.add(AceFlags.Inherited);
                break;
            case "SA":
                ret.add(AceFlags.SuccessfulAccess);
                break;
            case "FA":
                ret.add(AceFlags.FailedAccess);
                break;
            default:
                throw new IllegalArgumentException("Invalid SDDL string.");
            }

            pos += 2;
        }

        if (pos != flags.length())
            throw new IllegalArgumentException("Invalid SDDL string.");

        return ret;
    }

    static int parseSddlAccessRights(String accessMask) {
        if (accessMask.startsWith("0X")) {
            return Integer.parseInt(accessMask.substring(2), 16);
        } else if (Character.isDigit(accessMask.charAt(0))) {
            return Integer.parseInt(accessMask);
        } else {
            return parseSddlAliasRights(accessMask);
        }
    }

    static int parseSddlAliasRights(String accessMask) {
        int ret = 0;

        int pos = 0;
        while (pos < accessMask.length() - 1) {
            String flag = accessMask.substring(pos, pos + 2);
            SddlAccessRight right = SddlAccessRight.lookupByName(flag);
            if (right == null)
                throw new IllegalArgumentException("Invalid SDDL string.");

            ret |= right.Value;
            pos += 2;
        }

        if (pos != accessMask.length())
            throw new IllegalArgumentException("Invalid SDDL string.");

        return ret;
    }
}

/* */
