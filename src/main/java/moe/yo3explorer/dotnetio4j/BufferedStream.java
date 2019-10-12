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

    static InputStream toInputStream(Stream stream, int bufferSize) {
        if (bufferSize > 0) {
            return new BufferedInputStream(new StreamInputStream(stream), bufferSize);
        } else {
            return new BufferedInputStream(new StreamInputStream(stream));
        }
    }

    static OutputStream toOutputStream(Stream stream, int bufferSize) {
        if (bufferSize > 0) {
            return new BufferedOutputStream(new StreamOutputStream(stream), bufferSize);
        } else {
            return new BufferedOutputStream(new StreamOutputStream(stream));
        }
    }

    /** */
    public BufferedStream(Stream stream) {
        this(stream, 0);
    }

    /** */
    public BufferedStream(Stream stream, int bufferSize) {
        super(toInputStream(stream, bufferSize), toOutputStream(stream, bufferSize));
    }
}

/* */
