/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package DiscUtils.Core.CoreCompat;


/**
 * Tuple.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/25 umjammer initial version <br>
 */
public class Tuple<A, B> {
    public A Item1;

    public B Item2;

    public Tuple(A a, B b) {
        Item1 = a;
        Item2 = b;
    }

    public A getKey() {
        return Item1;
    }

    public B getValue() {
        return Item2;
    }
}

/* */
