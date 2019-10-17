/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.nio.charset.Charset;
import java.security.Permission;
import java.util.EnumSet;

import moe.yo3explorer.dotnetio4j.AccessControlSections;


/**
 * RawSecurityDescriptor.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/14 nsano initial version <br>
 */
public class RawSecurityDescriptor extends Permission {

    private String sddl;

    /** */
    public RawSecurityDescriptor(String sddl) {
        super("sddl");
        this.sddl = sddl;
        this.binaryForm = sddl.getBytes(Charset.forName("ASCII"));
    }

    private byte[] binaryForm;

    /** */
    public RawSecurityDescriptor(byte[] bytes, int offset) {
        super("binaryForm");
        this.binaryForm = new byte[bytes.length - offset];
        System.arraycopy(this.binaryForm, 0, bytes, offset, bytes.length);
    }

    private SecurityIdentifier owner;
    private SecurityIdentifier group;
    private RawAcl sacl;
    private RawAcl dacl;

    /** */
    public RawSecurityDescriptor(EnumSet<ControlFlags> controlFlags,
            SecurityIdentifier owner,
            SecurityIdentifier group,
            RawAcl sacl,
            RawAcl dacl) {
        super("acl");
        this.owner = owner;
        this.group = group;
        this.sacl = sacl;
        this.dacl = dacl;
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return sddl.equals(RawSecurityDescriptor.class.cast(obj).sddl);
    }

    @Override
    public int hashCode() {
        return sddl.hashCode();
    }

    @Override
    public String getActions() {
        return sddl;
    }

    /** */
    public String getSddlForm(AccessControlSections all) {
        return sddl;
    }

    /** */
    public long getBinaryLength() {
        return binaryForm == null ? 0 : binaryForm.length;
    }

    public SecurityIdentifier getOwner() {
        return owner;
    }

    public SecurityIdentifier getGroup() {
        return group;
    }

    /**
     * @return
     */
    public EnumSet<ControlFlags> getControlFlags() {
        return EnumSet.noneOf(ControlFlags.class);
    }

    /**
     * @return
     */
    public int getResourceManagerControl() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @return
     */
    public RawAcl getDiscretionaryAcl() {
        return dacl;
    }

    /**
     * @return
     */
    public RawAcl getSystemAcl() {
        return sacl;
    }
}
