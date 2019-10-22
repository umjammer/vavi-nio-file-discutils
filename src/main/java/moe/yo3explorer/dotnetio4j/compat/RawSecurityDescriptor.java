/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.security.Permission;
import java.util.EnumSet;

import DiscUtils.Streams.Util.EndianUtilities;
import moe.yo3explorer.dotnetio4j.AccessControlSections;


/**
 * RawSecurityDescriptor.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/14 nsano initial version <br>
 */
public class RawSecurityDescriptor extends Permission {

    private EnumSet<ControlFlags> controlFlags;

    private SecurityIdentifier owner;

    private SecurityIdentifier group;

    private RawAcl genericSacl;

    private RawAcl genericDacl;

    private byte resourcemgrControl;

    /** */
    public RawSecurityDescriptor(String sddlForm) {
        super("RawSecurityDescriptor");
        parseSddl(sddlForm.replace(" ", ""));
        controlFlags.add(ControlFlags.SelfRelative);
    }

    private void parseSddl(String sddlForm) {
        EnumSet<ControlFlags> flags = EnumSet.noneOf(ControlFlags.class);

        int[] pos = new int[1];
        while (pos[0] < sddlForm.length() - 2) {
            switch (sddlForm.substring(pos[0], pos[0] + 2)) {
            case "O:":
                pos[0] += 2;
                owner = SecurityIdentifier.parseSddlForm(sddlForm, pos);
                break;
            case "G:":
                pos[0] += 2;
                group = SecurityIdentifier.parseSddlForm(sddlForm, pos);
                break;
            case "D:":
                pos[0] += 2;
                genericDacl = RawAcl.parseSddlForm(sddlForm, true, flags, pos);
                flags.add(ControlFlags.DiscretionaryAclPresent);
                break;
            case "S:":
                pos[0] += 2;
                genericSacl = RawAcl.parseSddlForm(sddlForm, false, flags, pos);
                flags.add(ControlFlags.SystemAclPresent);
                break;
            default:
                throw new IllegalArgumentException("Invalid SDDL.");
            }
        }

        if (pos[0] != sddlForm.length()) {
            throw new IllegalArgumentException("Invalid SDDL.");
        }

        setFlags(flags);
    }

    /** */
    public RawSecurityDescriptor(byte[] binaryForm, int offset) {
        super("RawSecurityDescriptor");

        resourcemgrControl = binaryForm[offset + 0x01];
        controlFlags = ControlFlags.valueOf(EndianUtilities.toUInt16LittleEndian(binaryForm, offset + 0x02));

        int ownerPos = EndianUtilities.toUInt32LittleEndian(binaryForm, offset + 0x04);
        int groupPos = EndianUtilities.toUInt32LittleEndian(binaryForm, offset + 0x08);
        int saclPos = EndianUtilities.toUInt32LittleEndian(binaryForm, offset + 0x0C);
        int daclPos = EndianUtilities.toUInt32LittleEndian(binaryForm, offset + 0x10);

        if (ownerPos != 0)
            owner = new SecurityIdentifier(binaryForm, ownerPos);

        if (groupPos != 0)
            group = new SecurityIdentifier(binaryForm, groupPos);

        if (saclPos != 0)
            genericSacl = new RawAcl(binaryForm, saclPos);

        if (daclPos != 0)
            genericDacl = new RawAcl(binaryForm, daclPos);
    }

    private static final int HeaderLength = 20;

    /** */
    public RawSecurityDescriptor(EnumSet<ControlFlags> controlFlags,
            SecurityIdentifier owner,
            SecurityIdentifier group,
            RawAcl sacl,
            RawAcl dacl) {
        super("RawSecurityDescriptor");
        this.controlFlags = controlFlags;
        this.owner = owner;
        this.group = group;
        this.genericSacl = sacl;
        this.genericDacl = dacl;
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == null ? false : getSddlForm(AccessControlSections.All)
                .equals(RawSecurityDescriptor.class.cast(obj).getSddlForm(AccessControlSections.All));
    }

    @Override
    public int hashCode() {
        return getSddlForm(AccessControlSections.All).hashCode();
    }

    @Override
    public String getActions() {
        return null;
    }

    public void setFlags(EnumSet<ControlFlags> flags) {
        controlFlags = flags;
        controlFlags.add(ControlFlags.SelfRelative);
    }

    /** */
    public String getSddlForm(EnumSet<AccessControlSections> includeSections) {
        StringBuilder result = new StringBuilder();

        if (includeSections.contains(AccessControlSections.Owner) && owner != null) {
            result.append("O:" + owner.getSddlForm());
        }

        if (includeSections.contains(AccessControlSections.Group) && group != null) {
            result.append("G:" + group.getSddlForm());
        }

        if (includeSections.contains(AccessControlSections.Access) && genericDacl != null) {
            result.append("D:" + genericDacl.getSddlForm(controlFlags, true));
        }

        if (includeSections.contains(AccessControlSections.Audit) && genericSacl != null) {
            result.append("S:" + genericSacl.getSddlForm(controlFlags, false));
        }

        return result.toString();
    }

    /** */
    public long getBinaryLength() {
        int result = HeaderLength;

        if (owner != null) {
            result += owner.getBinaryLength();
        }

        if (group != null) {
            result += group.getBinaryLength();
        }

        if (controlFlags.contains(ControlFlags.SystemAclPresent) && genericSacl != null) {
            result += genericSacl.getBinaryLength();
        }

        if (controlFlags.contains(ControlFlags.DiscretionaryAclPresent) && genericDacl != null) {
            result += genericDacl.getBinaryLength();
        }

        return result;
    }

    public SecurityIdentifier getOwner() {
        return owner;
    }

    public SecurityIdentifier getGroup() {
        return group;
    }

    /** */
    public EnumSet<ControlFlags> getControlFlags() {
        return controlFlags;
    }

    /** */
    public int getResourceManagerControl() {
        return resourcemgrControl;
    }

    /** */
    public RawAcl getDiscretionaryAcl() {
        return genericDacl;
    }

    /** */
    public RawAcl getSystemAcl() {
        return genericSacl;
    }
}
