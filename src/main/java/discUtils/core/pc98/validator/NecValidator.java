/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98.validator;

import java.lang.System.Logger.Level;

import discUtils.core.pc98.Pc98FileSystemFactory.Validator;


/**
 * NecValidator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-11-30 nsano initial version <br>
 */
public class NecValidator implements Validator {

    @Override
    public int weight() {
        return 10;
    }

    @Override
    public boolean enabled() {
        return Boolean.parseBoolean(System.getProperty("discUtils.core.pc98.validator.nec", "true"));
    }

    @Override
    public boolean validate(byte[] firstSectors) {
        if (firstSectors[0x3] != 'N' ||
                firstSectors[0x4] != 'E' ||
                firstSectors[0x5] != 'C') {
            // Missing magic number
logger.log(Level.TRACE, "Missing magic number 'NEC': %c%c%c".formatted(firstSectors[0x3] & 0xff, firstSectors[0x4] & 0xff, firstSectors[0x5] & 0xff));
            return false;
        } else {
logger.log(Level.TRACE, "validation (NEC) passed");
            return true;
        }
    }
}
