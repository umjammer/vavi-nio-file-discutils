/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;


/**
 * WellKnownSidType.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/13 nsano initial version <br>
 */
public enum WellKnownSidType {
    /** Administrator アカウントに一致する SID を示します。 */
    BuiltinAdministratorsSid(26),
    /** Account Administrators グループに一致する SID を示します。 */
    AccountAdministratorSid(38);

    private int value;

    public int getValue() {
        return value;
    }

    private WellKnownSidType(int value) {
        this.value = value;
    }
}

/* */
