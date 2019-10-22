/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j.compat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;


/**
 * ControlFlags.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public enum ControlFlags {
    None(0x0000),
    OwnerDefaulted(0x0001),
    GroupDefaulted(0x0002),
    DiscretionaryAclPresent(0x0004),
    DiscretionaryAclDefaulted(0x0008),
    SystemAclPresent(0x0010),
    SystemAclDefaulted(0x0020),
    DiscretionaryAclUntrusted(0x0040),
    ServerSecurity(0x0080),
    DiscretionaryAclAutoInheritRequired(0x0100),
    SystemAclAutoInheritRequired(0x0200),
    DiscretionaryAclAutoInherited(0x0400),
    SystemAclAutoInherited(0x0800),
    DiscretionaryAclProtected(0x1000),
    SystemAclProtected(0x2000),
    RMControlValid(0x4000),
    SelfRelative(0x8000);

    private int value;

    public int getValue() {
        return value;
    }

    private ControlFlags(int value) {
        this.value = value;
    }

    public static EnumSet<ControlFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (value & v.getValue()) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ControlFlags.class)));
    }

    public static long valueOf(EnumSet<ControlFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.getValue())).getSum();
    }
}

/* */
