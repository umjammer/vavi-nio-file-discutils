/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libchdr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import jpcsp.filesystems.umdiso.UmdIsoReader;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;


/**
 * ChdTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-17 nsano initial version <br>
 * @see "https://github.com/jpcsp/jpcsp/blob/master/src/jpcsp/filesystems/umdiso/CSOFileSectorDevice.java"
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class ChdTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "chd")
    String chdFile = "src/test/resources/test.chd";

    @BeforeEach
    void before() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    private UmdIsoReader reader;

    @Test
    void test1() throws Exception {
Debug.println(chdFile);
        reader = new UmdIsoReader(chdFile);

        System.out.println("/");
        print("");
    }

    void print(String dir) throws IOException {
        String[] files = reader.listDirectory(dir);
//Debug.print(Arrays.toString(files));
        for (String file : files) {
            if (".".equals(file)) continue;
            if ("\u0001".equals(file)) continue;
//Debug.print(file);
            int p = file.indexOf(";");
            if (p != -1) {
                System.out.println("/" + file.substring(0, file.indexOf(";")));
            } else {
                file = dir.isEmpty() ? file : dir + "/" + file;
                if (reader.isDirectory(file)) {
                    System.out.println("/" + file + "/");
                    print(file);
                } else {
                    System.out.println("/" + file);
                }
            }
        }
    }
}
