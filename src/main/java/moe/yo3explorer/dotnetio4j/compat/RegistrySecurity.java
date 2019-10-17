/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.security.Permission;

import moe.yo3explorer.dotnetio4j.AccessControlSections;


/**
 * RegistrySecurity.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public class RegistrySecurity extends Permission {

    /**
     */
    public RegistrySecurity() {
        super(null); // TODO
    }

    @Override
    public boolean implies(Permission permission) {
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

    private String binaryForm;

    /**
     * @return
     */
    public String getSecurityDescriptorBinaryForm() {
        return binaryForm;
    }

    /**
     * @param secDesc
     */
    public void setSecurityDescriptorBinaryForm(String binaryForm) {
        this.binaryForm = binaryForm;
    }

    /**
     */
    public String getSecurityDescriptorSddlForm(AccessControlSections sections) {
        // TODO Auto-generated method stub
        return binaryForm;
    }

    /**
     */
    public void setSecurityDescriptorSddlForm(String form, AccessControlSections sections) {
        // TODO Auto-generated method stub
        
    }
}
