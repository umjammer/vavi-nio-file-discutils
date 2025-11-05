/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.qlgenerator;


import java.lang.System.Logger;
import java.lang.System.Logger.Level;


/**
 * Chd.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-25 nsano initial version <br>
 */
public class Chd {

    private static final Logger logger = System.getLogger(Chd.class.getName());

    static String exec(String url) {
logger.log(Level.TRACE, "@@@: " + url);
        return "Hello World!";
    }
}
