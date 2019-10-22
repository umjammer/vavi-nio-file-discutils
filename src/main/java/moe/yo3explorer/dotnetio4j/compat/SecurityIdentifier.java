/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Random;


/**
 * SecurityIdentifier.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/13 nsano initial version <br>
 */
public class SecurityIdentifier implements Principal {

    public static final int MaxBinaryLength = 68;

    public static final int MinBinaryLength = 8;

    private byte[] buffer;

    // @see
    // "https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-dtyp/c92a27b1-c772-4fa7-a432-15df5f1b66a1"
    static String domainSid;

    static {
        // TODO from env
        Random r = new SecureRandom();
        int a = r.nextInt();
        int b = r.nextInt();
        int c = r.nextInt();
        domainSid = String.format("%d-%d-%d", a & 0xffffffffl, b & 0xffffffffl, c & 0xffffffffl);
    }

    /** */
    public SecurityIdentifier(WellKnownSidType sidType, SecurityIdentifier domainSid) {
        WellKnownAccount acct = WellKnownAccount.lookupByType(sidType);
        if (acct == null)
            throw new IllegalArgumentException("Unable to convert SID type: " + sidType);

        if (acct.IsAbsolute) {
            buffer = parseSddlForm(acct.Sid);
        } else {
            if (domainSid == null)
                throw new NullPointerException("domainSid");

            buffer = parseSddlForm(domainSid.getValue() + "-" + acct.Rid);
        }
    }

    /** */
    public SecurityIdentifier(byte[] binaryForm, int offset) {
        if (binaryForm == null)
            throw new IllegalArgumentException("binaryForm");
        if ((offset < 0) || (offset > binaryForm.length - 2))
            throw new IllegalArgumentException("offset");

        int revision = binaryForm[offset + 0];
        int numSubAuthorities = binaryForm[offset + 1];
        if (revision != 1 || numSubAuthorities > 15)
            throw new IllegalArgumentException("Value was invalid.");
        if (binaryForm.length - offset < (8 + (numSubAuthorities * 4)))
            throw new IllegalArgumentException("offset");

        buffer = new byte[8 + (numSubAuthorities * 4)];
        System.arraycopy(binaryForm, offset, buffer, 0, binaryForm.length - offset);
    }

    /** */
    public SecurityIdentifier(String sddlForm) {
        if (sddlForm == null)
            throw new NullPointerException("sddlForm");
        buffer = parseSddlForm(sddlForm);
    }

    @Override
    public String getName() {
        return getValue();
    }

    /** */
    public String getValue() {
        StringBuilder s = new StringBuilder();

        long authority = getSidAuthority();
        s.append("S-1-" + authority);

        for (byte i = 0; i < getSidSubAuthorityCount(); ++i)
            s.append("-" + getSidSubAuthority(i));

        return s.toString();
    }

    private long getSidAuthority() {
        return (((long) buffer[2]) << 40) | (((long) buffer[3]) << 32) | (((long) buffer[4]) << 24) |
            (((long) buffer[5]) << 16) | (((long) buffer[6]) << 8) | (((long) buffer[7]) << 0);
    }

    private byte getSidSubAuthorityCount() {
        return buffer[1];
    }

    private int getSidSubAuthority(byte index) {
        // Note sub authorities little-endian, authority (above) is big-endian!
        int offset = 8 + (index * 4);

        return ((buffer[offset + 0] & 0xff) << 0) | ((buffer[offset + 1] & 0xff) << 8) | ((buffer[offset + 2] & 0xff) << 16) |
            ((buffer[offset + 3] & 0xff) << 24);
    }

    /** */
    public void getBinaryForm(byte[] binaryForm, int offset) {
        System.arraycopy(buffer, 0, binaryForm, offset, buffer.length);
    }

    public int getBinaryLength() {
        return buffer.length;
    }

    /** */
    public String getSddlForm() {
        String sidString = getValue();

        WellKnownAccount acct = WellKnownAccount.lookupBySid(sidString);
        if (acct == null || acct.SddlForm == null)
            return sidString;

        return acct.SddlForm;
    }

    private static byte[] parseSddlForm(String sddlForm) {
        String sid = sddlForm;

        // If only 2 characters long, can't be a full SID string - so assume
        // it's an attempted alias. Do that conversion first.
        if (sddlForm.length() == 2) {
            WellKnownAccount acct = WellKnownAccount.lookupBySddlForm(sddlForm);
            if (acct == null) {
                throw new IllegalArgumentException("Invalid SDDL string - unrecognized account: " + sddlForm);
            }
            if (!acct.IsAbsolute) {
                if (acct.Sid != null) {
                    sid = String.format(acct.Sid, domainSid);
                } else {
                    throw new IllegalArgumentException(SecurityIdentifier.class + " unable to convert account to SID: " +
                        (acct.Name != null ? acct.Name : sddlForm));
                }
            } else {
                sid = acct.Sid;
            }
        }

        String[] elements = sid.toUpperCase().split("-");
        int numSubAuthorities = elements.length - 3;

        if (elements.length < 3 || !elements[0].equals("S") || numSubAuthorities > 15)
            throw new IllegalArgumentException("Value was invalid.");

        if (!elements[1].equals("1"))
            throw new IllegalArgumentException("Only SIDs with revision 1 are supported");

        byte[] buffer = new byte[8 + (numSubAuthorities * 4)];
        buffer[0] = 1;
        buffer[1] = (byte) numSubAuthorities;

        long authority;
        try {
            authority = tryParseAuthority(elements[2]);
        } catch (NumberFormatException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Value was invalid.").initCause(e);
        }
        buffer[2] = (byte) ((authority >> 40) & 0xFF);
        buffer[3] = (byte) ((authority >> 32) & 0xFF);
        buffer[4] = (byte) ((authority >> 24) & 0xFF);
        buffer[5] = (byte) ((authority >> 16) & 0xFF);
        buffer[6] = (byte) ((authority >> 8) & 0xFF);
        buffer[7] = (byte) ((authority >> 0) & 0xFF);

        for (int i = 0; i < numSubAuthorities; ++i) {
            int subAuthority;
            try {
                subAuthority = tryParseSubAuthority(elements[i + 3]);
            } catch (NumberFormatException e) {
                throw (IllegalArgumentException) new IllegalArgumentException("Value was invalid.").initCause(e);
            }

            // Note sub authorities little-endian!
            int offset = 8 + (i * 4);
            buffer[offset + 0] = (byte) (subAuthority >> 0);
            buffer[offset + 1] = (byte) (subAuthority >> 8);
            buffer[offset + 2] = (byte) (subAuthority >> 16);
            buffer[offset + 3] = (byte) (subAuthority >> 24);
        }

        return buffer;
    }

    private static long tryParseAuthority(String s) {
        if (s.startsWith("0X")) {
            return Long.parseLong(s.substring(2), 16);
        } else {
            return Long.parseLong(s);
        }
    }

    private static int tryParseSubAuthority(String s) {
        if (s.startsWith("0X")) {
            return (int) Long.parseLong(s.substring(2), 16);
        } else {
            return (int) Long.parseLong(s);
        }
    }

    /** */
    static SecurityIdentifier parseSddlForm(String sddlForm, int[] pos) {
        if (sddlForm.length() - pos[0] < 2)
            throw new IllegalArgumentException("Invalid SDDL string.");

        String sid;
        int len;

        String prefix = sddlForm.substring(pos[0], pos[0] + 2).toUpperCase();
        if (prefix.equals("S-")) {
            // Looks like a SID, try to parse it.
            int endPos = pos[0];

            char ch = Character.toUpperCase(sddlForm.charAt(endPos));
            while (ch == 'S' || ch == '-' || ch == 'X' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) {
                ++endPos;
                ch = Character.toUpperCase(sddlForm.charAt(endPos));
            }

            sid = sddlForm.substring(pos[0], endPos);
            len = endPos - pos[0];
        } else {
            sid = prefix;
            len = 2;
        }

        SecurityIdentifier ret = new SecurityIdentifier(sid);
        pos[0] += len;
        return ret;
    }
}

/* */
