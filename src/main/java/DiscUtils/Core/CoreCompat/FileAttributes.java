/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package DiscUtils.Core.CoreCompat;

import java.util.Arrays;
import java.util.EnumSet;
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

    /** */
    public static Map<String, Object> toMap(EnumSet<FileAttributes> of) {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO EnumMap?
    public static Map<String, Object> all() {
        Map<String, Object> attrs = new HashMap<>();
        Arrays.stream(values()).forEach(v -> attrs.put(v.name(), true));
        return attrs;
    }

    public static Map<String, Object> not(Map<String, Object> attrs, FileAttributes key) {
        attrs.entrySet().stream().filter(e -> e.getKey().equals(key.name())).forEach(e -> attrs.put(key.name(), false));
        return attrs;
    }

    public static Map<String, Object> or(Map<String, Object> attrs, FileAttributes key) {
        attrs.entrySet().stream().filter(e -> e.getKey().equals(key.name())).forEach(e -> attrs.put(key.name(), true));
        return attrs;
    }

    /** TODO filter? */
    public static Map<String, Object> xor(Map<String, Object> attrs1, Map<String, Object> attrs2) {
        attrs2.entrySet().stream().forEach(e2 -> {
            attrs1.entrySet().stream().filter(e1 -> e1.getKey().equals(e2.getKey())).forEach(e1 -> attrs1.put(e1.getKey(), Boolean.class.cast(e1.getValue()) ^ Boolean.class.cast(e2.getValue())));
        });
        return attrs1;
    }

    /** */
    public static int count(Map<String, Object> attrs) {
        return (int) attrs.entrySet().stream().filter(e -> Boolean.class.cast(e.getValue())).count();
    }
}

/* */
