/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;
import java.io.InputStream;

import moe.yo3explorer.dotnetio4j.Stream;


/**
 * StreamInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class StreamInputStream extends InputStream {

    private Stream stream;

    public StreamInputStream(Stream stream) {
        this.stream = stream;
    }

    @Override
    public int read() throws IOException {
         int r = stream.readByte();
//System.err.printf("%02x: %c\n", r, (r & 0xff));
         return r;
    }
}

/* */
