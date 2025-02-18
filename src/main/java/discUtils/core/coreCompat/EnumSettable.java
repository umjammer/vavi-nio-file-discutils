/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package discUtils.core.coreCompat;

import java.util.function.Function;
import java.util.function.Supplier;


/**
 * EnumSettable.
 *
 * TODO should consider more
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/07 umjammer initial version <br>
 */
public interface EnumSettable {

    Supplier<Integer> supplier();

    Function<Integer, Boolean> function();
}
