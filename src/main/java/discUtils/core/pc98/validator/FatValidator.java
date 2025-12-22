/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.pc98.validator;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;

import discUtils.core.pc98.Pc98FileSystemFactory.Validator;


/**
 * FatValidator.
 *
 * TODO fat12 doesn't work
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-11-30 nsano initial version <br>
 */
public class FatValidator implements Validator {

    @Override
    public int weight() {
        return 20;
    }

    @Override
    public boolean enabled() {
        return Boolean.parseBoolean(System.getProperty("discUtils.core.pc98.validator.fat", "false"));
    }

    @Override
    public boolean validate(byte[] firstSectors) {
        if (!new String(firstSectors, 0x36, 3, StandardCharsets.US_ASCII).equals("FAT")) {
logger.log(Level.TRACE, "strings FAT is not found");
            return false;
        } else {
logger.log(Level.TRACE, "validation (FAT) passed");
            return true;
        }
    }
}
