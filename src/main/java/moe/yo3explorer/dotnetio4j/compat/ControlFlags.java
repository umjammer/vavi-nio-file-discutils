/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.EnumSet;
import java.util.stream.Collectors;


/**
 * ControlFlags.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public enum ControlFlags {
    DiscretionaryAclPresent(4),
    SystemAclPresent(16);

    private int value;

    public int getValue() {
        return value;
    }

    private ControlFlags(int value) {
        this.value = value;
    }

    public static long valueOf(EnumSet<ControlFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.getValue())).getSum();
    }
}

/* */
