/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.security.Permission;
import java.security.Principal;


/**
 * SecurityIdentifier.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/13 nsano initial version <br>
 */
public class SecurityIdentifier implements Principal {

    /**
     * @param builtinadministratorssid
     * @param object
     */
    public SecurityIdentifier(WellKnownSidType builtinadministratorssid, Permission object) {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param buffer
     * @param offset
     */
    public SecurityIdentifier(byte[] buffer, int offset) {
        // TODO Auto-generated constructor stub
    }

    /* @see java.security.Principal#getName() */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param buffer
     * @param offset
     */
    public void getBinaryForm(byte[] buffer, int offset) {
        // TODO Auto-generated method stub
        
    }

    public int getBinaryLength() {
        return 0;
    }
}

/* */
