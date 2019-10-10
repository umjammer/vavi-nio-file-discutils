/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


/**
 * BufferedStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class BufferedStream extends JavaIOStream {

    static InputStream toInputStream(Stream stream) {
        return new BufferedInputStream(new StreamInputStream(stream));
    }

    static OutputStream toOutputStream(Stream stream) {
        return new BufferedOutputStream(new StreamOutputStream(stream));
    }

    /**
     */
    public BufferedStream(Stream stream) {
        super(toInputStream(stream), toOutputStream(stream));
    }
}

/* */
