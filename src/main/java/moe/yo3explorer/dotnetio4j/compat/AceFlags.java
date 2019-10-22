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
 * AceFlags.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/17 nsano initial version <br>
 */
public enum AceFlags {
    ObjectInherit(0x01),
    ContainerInherit(0x02),
    NoPropagateInherit(0x04),
    InheritOnly(0x08),
    Inherited(0x10),
    SuccessfulAccess(0x40),
    FailedAccess(0x80);

    public static final EnumSet<AceFlags> InheritanceFlags = EnumSet
            .of(ObjectInherit, ContainerInherit, NoPropagateInherit, InheritOnly);

    public static final EnumSet<AceFlags> AuditFlags = EnumSet.of(SuccessfulAccess, FailedAccess);

    private int value;

    public int getValue() {
        return value;
    }

    private AceFlags(int value) {
        this.value = value;
    }

    public static EnumSet<AceFlags> valueOf(int value) {
        return Arrays.stream(values())
                .filter(v -> (v.getValue() & value) != 0)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AceFlags.class)));
    }

    public static long valueOf(EnumSet<AceFlags> flags) {
        return flags.stream().collect(Collectors.summarizingInt(e -> e.getValue())).getSum();
    }
}

/* */
