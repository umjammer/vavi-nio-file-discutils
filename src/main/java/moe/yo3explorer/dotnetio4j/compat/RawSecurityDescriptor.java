/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.security.Permission;

import moe.yo3explorer.dotnetio4j.AccessControlSections;


/**
 * RawSecurityDescriptor.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/14 nsano initial version <br>
 */
public class RawSecurityDescriptor extends Permission {

    /** */
    public RawSecurityDescriptor(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    /** */
    public RawSecurityDescriptor(byte[] readBytes, int i) {
        super(null);
    }

    @Override
    public boolean implies(Permission permission) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getActions() {
        // TODO Auto-generated method stub
        return null;
    }

    /** */
    public String getSddlForm(AccessControlSections all) {
        // TODO Auto-generated method stub
        return null;
    }

    /** */
    public long getBinaryLength() {
        return 0;
    }

    public SecurityIdentifier getOwner() {
        return null;
    }

    public SecurityIdentifier getGroup() {
        return null;
    }
}
