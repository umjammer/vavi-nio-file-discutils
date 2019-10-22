/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import DiscUtils.Streams.Util.EndianUtilities;


/**
 * RawAcl.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public class RawAcl implements Iterable<GenericAce> {

    public static final byte AclRevision = 2;

    public static final byte AclRevisionDS = 4;

    private byte revision;

    private List<GenericAce> genericAces;

    /** */
    public RawAcl(byte revision, int capacity) {
        this.revision = revision;
        genericAces = new ArrayList<>(capacity);
    }

    /** */
    public RawAcl(byte[] binaryForm, int offset) {
        if (binaryForm == null)
            throw new NullPointerException("binaryForm");

        if (offset < 0 || offset > binaryForm.length - 8)
            throw new IndexOutOfBoundsException("offset; " + offset);

        revision = binaryForm[offset];
        if (revision != AclRevision && revision != AclRevisionDS)
            throw new IllegalArgumentException("Invalid ACL - unknown revision");

        int binaryLength = EndianUtilities.toUInt16LittleEndian(binaryForm, offset + 2);
        if (offset > binaryForm.length - binaryLength)
            throw new IllegalArgumentException("Invalid ACL - truncated");

        int pos = offset + 8;
        int numAces = EndianUtilities.toUInt16LittleEndian(binaryForm, offset + 4);
        genericAces = new ArrayList<>(numAces);
        for (int i = 0; i < numAces; ++i) {
            GenericAce newAce = GenericAce.createFromBinaryForm(binaryForm, pos);
            genericAces.add(newAce);
            pos += newAce.getBinaryLength();
        }
    }

    RawAcl(byte revision, List<GenericAce> aces) {
        this.revision = revision;
        this.genericAces = aces;
    }

    /** */
    public byte getRevision() {
        return revision;
    }

    /** */
    public void getBinaryForm(byte[] binaryForm, int offset) {
        if (binaryForm == null)
            throw new NullPointerException("binaryForm");

        if (offset < 0 || offset > binaryForm.length - getBinaryLength())
            throw new IllegalArgumentException("Offset out of range");

        binaryForm[offset] = revision;
        binaryForm[offset + 1] = 0;
        EndianUtilities.writeBytesLittleEndian((short) getBinaryLength(), binaryForm, offset + 2);
        EndianUtilities.writeBytesLittleEndian((short) genericAces.size(), binaryForm, offset + 4);
        EndianUtilities.writeBytesLittleEndian((short) 0, binaryForm, offset + 6);

        int pos = offset + 8;
        for (GenericAce ace : genericAces) {
            ace.getBinaryForm(binaryForm, pos);
            pos += ace.getBinaryLength();
        }
    }

    /** */
    public int getBinaryLength() {
        int len = 8;
        for (GenericAce ace : genericAces) {
            len += ace.getBinaryLength();
        }
        return len;
    }

    /** */
    public int getCount() {
        return genericAces.size();
    }

    /** */
    public void insertAce(int index, GenericAce newAce) {
        genericAces.add(index, newAce);
    }

    @Override
    public Iterator<GenericAce> iterator() {
        return genericAces.iterator();
    }

    /** */
    public String getSddlForm(EnumSet<ControlFlags> sdFlags, boolean isDacl) {
        StringBuilder result = new StringBuilder();

        if (isDacl) {
            if (sdFlags.contains(ControlFlags.DiscretionaryAclProtected))
                result.append("P");
            if (sdFlags.contains(ControlFlags.DiscretionaryAclAutoInheritRequired))
                result.append("AR");
            if (sdFlags.contains(ControlFlags.DiscretionaryAclAutoInherited))
                result.append("AI");
        } else {
            if (sdFlags.contains(ControlFlags.SystemAclProtected))
                result.append("P");
            if (sdFlags.contains(ControlFlags.SystemAclAutoInheritRequired))
                result.append("AR");
            if (sdFlags.contains(ControlFlags.SystemAclAutoInherited))
                result.append("AI");
        }

        for (GenericAce ace : genericAces) {
            result.append(ace.getSddlForm());
        }

        return result.toString();
    }

    /** */
    static RawAcl parseSddlForm(String sddlForm, boolean isDacl, EnumSet<ControlFlags> sdFlags, int[] pos) {
        parseFlags(sddlForm, isDacl, sdFlags, pos);

        byte revision = AclRevision;
        List<GenericAce> aces = new ArrayList<>();
        while (pos[0] < sddlForm.length() && sddlForm.charAt(pos[0]) == '(') {
            GenericAce ace = GenericAce.createFromSddlForm(sddlForm, pos);
//            if (ObjectAce.class.isInstance(ace)) { TODO
//                revision = AclRevisionDS;
//            }
            aces.add(ace);
        }

        return new RawAcl(revision, aces);
    }

    private static void parseFlags(String sddlForm, boolean isDacl, EnumSet<ControlFlags> sdFlags, int[] pos) {
        char ch = Character.toUpperCase(sddlForm.charAt(pos[0]));
        while (ch == 'P' || ch == 'A') {
            if (ch == 'P') {
                if (isDacl)
                    sdFlags.add(ControlFlags.DiscretionaryAclProtected);
                else
                    sdFlags.add(ControlFlags.SystemAclProtected);
                pos[0]++;
            } else if (sddlForm.length() > pos[0] + 1) {
                ch = Character.toUpperCase(sddlForm.charAt(pos[0] + 1));
                if (ch == 'R') {
                    if (isDacl)
                        sdFlags.add(ControlFlags.DiscretionaryAclAutoInheritRequired);
                    else
                        sdFlags.add(ControlFlags.SystemAclAutoInheritRequired);
                    pos[0] += 2;
                } else if (ch == 'I') {
                    if (isDacl)
                        sdFlags.add(ControlFlags.DiscretionaryAclAutoInherited);
                    else
                        sdFlags.add(ControlFlags.SystemAclAutoInherited);
                    pos[0] += 2;
                } else {
                    throw new IllegalArgumentException("Invalid SDDL string.");
                }
            } else {
                throw new IllegalArgumentException("Invalid SDDL string.");
            }

            ch = Character.toUpperCase(sddlForm.charAt(pos[0]));
        }
    }
}

/* */
