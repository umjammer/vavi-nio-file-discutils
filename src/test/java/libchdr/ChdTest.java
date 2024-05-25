/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package aaru.image.chd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * ChdTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-17 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class ChdTest {

    @Property
    String chd;

    @BeforeEach
    void before() throws IOException {
        PropsEntity.Util.bind(this);
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Test
    void test1() throws Exception {
        Chd chd = new Chd();
        Debug.println(this.chd);
        chd.open(new FileStream(this.chd, FileMode.Open, FileAccess.Read));
        Debug.println(chd.getInfo());
    }
}
