/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package DiscUtils.Core.CoreCompat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * FileAttributes.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/09 umjammer initial version <br>
 */
public enum FileAttributes {

    ReadOnly(0x1),
    Hidden(0x2),
    System(0x4),
    Directory(0x10),
    Archive(0x20),
    Device(0x40),
    Normal(0x80),
    Temporary(0x100),
    SparseFile(0x200),
    ReparsePoint(0x400),
    Compressed(0x800),
    Offline(0x1000),
    NotContentIndexed(0x2000),
    Encrypted(0x4000),
    IntegrityStream(0x8000),
    NoScrubData(0x20000);

    private int value;

    public int getValue() {
        return value;
    }

    private FileAttributes(int value) {
        this.value = value;
    }

    // TODO EnumMap?
    public static Map<String, Object> all() {
        Map<String, Object> attrs = new HashMap<>();
        Arrays.asList(values()).stream().forEach(v -> attrs.put(v.name(), true));
        return attrs;
    }

    public static Map<String, Object> not(Map<String, Object> attrs, FileAttributes key) {
        attrs.entrySet().stream().filter(e -> e.getKey().equals(key.name())).forEach(e -> attrs.put(key.name(), false));
        return attrs;
    }
}

/* */
