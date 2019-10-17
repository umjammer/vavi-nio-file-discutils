/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.EnumSet;

/**
 * GenericAce.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public class GenericAce implements Cloneable {

    public Object clone() {
        return null;
    }

    private EnumSet<AceFlags> aceFlags;

    /** */
    public EnumSet<AceFlags> getAceFlags() {
        return aceFlags;
    }

    /** */
    public void setAceFlags(EnumSet<AceFlags> aceFlags) {
        this.aceFlags = aceFlags;
    }
}

/* */
