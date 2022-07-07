/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.coreCompat;

import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import discUtils.core.coreCompat.EncodingHelper.CodePage;


/**
 * EncodingHelperTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/07 umjammer initial version <br>
 */
class EncodingHelperTest {

    @Test
    void test() {
        List<CodePage> codePages = EncodingHelper.codePages;
//        codePages.forEach(System.err::println);
        assertEquals(Charset.forName("IBM855"), EncodingHelper.forCodePage(855));
    }
}

/* */
