/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.registry;

import discUtils.registry.RegistryHive;
import discUtils.registry.RegistryKey;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;


/**
 * for "regf"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-04 nsano initial version <br>
 * @see "https://github.com/msuhanov/regf/blob/master/Windows%20registry%20file%20format%20specification.md"
 */
public class TestCase {

    @Test
    @DisplayName("windows10")
    void test1() throws Exception {
        Stream stream = new FileStream("src/test/resources/ntuser.dat", FileMode.Open, FileAccess.Read);
        RegistryHive hive = new RegistryHive(stream);
Debug.println(hive);
        RegistryKey root = hive.getRoot();
        walk(root);
    }

    static void walk(RegistryKey key) {
System.err.println(key.getName() + ": " + key);
        for (RegistryKey child : key.getSubKeys()) {
            walk(child);
        }
    }

    @Test
    @DisplayName("bcd")
    void test2() throws Exception {
        Stream stream = new FileStream("src/test/resources/win10.bcd", FileMode.Open, FileAccess.Read);
        RegistryHive hive = new RegistryHive(stream);
Debug.println(hive);
        RegistryKey root = hive.getRoot();
        walk(root);
    }

    @Test
    @Disabled("doesn't work, not regf")
    @DisplayName("win95")
    void test3() throws Exception {
        Stream stream = new FileStream("../../vavi/vavi-apps-registryviewer/src/test/resources/user.dat", FileMode.Open, FileAccess.Read);
        RegistryHive hive = new RegistryHive(stream);
        Debug.println(hive);
        RegistryKey root = hive.getRoot();
        walk(root);
    }
}
