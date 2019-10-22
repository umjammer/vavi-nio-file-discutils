/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.nio.charset.Charset;
import java.security.Permission;
import java.util.Arrays;
import java.util.EnumSet;

import moe.yo3explorer.dotnetio4j.AccessControlSections;


/**
 * RegistrySecurity.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public class RegistrySecurity extends Permission {

    RawSecurityDescriptor descriptor;

    /**
     */
    public RegistrySecurity() {
        super("RegistrySecurity");
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        return Arrays.equals(binaryForm, RegistrySecurity.class.cast(obj).binaryForm);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(binaryForm);
    }

    @Override
    public String getActions() {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO impl
    private byte[] binaryForm;

    /** TODO impl */
    public byte[] getSecurityDescriptorBinaryForm() {
        return binaryForm;
    }

    /** TODO impl */
    public void setSecurityDescriptorBinaryForm(byte[] form) {
        this.binaryForm = form;
    }

    /** TODO impl */
    public String getSecurityDescriptorSddlForm(EnumSet<AccessControlSections> sections) {
        return new String(binaryForm, Charset.forName("ASCII"));
    }

    /** TODO impl */
    public void setSecurityDescriptorSddlForm(String form, EnumSet<AccessControlSections> sections) {
//System.err.println(form);
        binaryForm = form.getBytes(Charset.forName("ASCII"));
    }
}
