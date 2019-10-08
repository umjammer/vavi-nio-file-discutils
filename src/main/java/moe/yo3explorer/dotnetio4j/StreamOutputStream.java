/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;
import java.io.OutputStream;

import moe.yo3explorer.dotnetio4j.Stream;


/**
 * StreamOutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class StreamOutputStream extends OutputStream {

    Stream stream;

    public StreamOutputStream(Stream stream) {
        this.stream = stream;
    }

    @Override
    public void write(int b) throws IOException {
        stream.writeByte((byte) b);
    }
}

/* */
