/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package DiscUtils.Core.CoreCompat;

import java.util.List;

import org.junit.jupiter.api.Test;

import DiscUtils.Core.CoreCompat.EncodingHelper.CodePage;


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
        codePages.forEach(System.err::println);
    }
}

/* */
