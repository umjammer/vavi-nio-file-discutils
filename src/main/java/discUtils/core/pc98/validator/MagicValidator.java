/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98.validator;

import java.lang.System.Logger.Level;

import discUtils.core.pc98.Pc98FileSystemFactory.Validator;


/**
 * MagicValidator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-11-30 nsano initial version <br>
 */
public class MagicValidator implements Validator {

    @Override
    public int weight() {
        return 0;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public boolean validate(byte[] firstSectors) {
        if (firstSectors[0xfe] != 0x55 ||
                firstSectors[0xff] != (byte) 0xaa) {
            // Missing magic number
logger.log(Level.TRACE, "Missing magic number 0x55, 0xaa");
            return false;
        } else {
logger.log(Level.TRACE, "validation (55aa) passed");
            return true;
        }
    }
}
