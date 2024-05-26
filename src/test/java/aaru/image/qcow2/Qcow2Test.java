/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package aaru.image.qcow2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Qcow2Test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-19 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class Qcow2Test {

    @Property
    String qcow2;

    @BeforeEach
    void before() throws IOException {
        PropsEntity.Util.bind(this);
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        Qcow2 qcow2 = new Qcow2();
        Debug.println(this.qcow2);
        qcow2.open(new FileStream(this.qcow2, FileMode.Open, FileAccess.Read));
Debug.println(qcow2.getInfo());
    }
}
