/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;


/**
 * StreamOutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class StreamWriter extends Writer {

    Stream stream;

    // TODO
    Charset encoding;

    public StreamWriter(Stream stream) {
        this.stream = stream;
    }

    /** */
    public StreamWriter(Stream stream, Charset encoding) {
        this.stream = stream;
        this.encoding = encoding;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (char c : cbuf) {
            stream.writeByte((byte) c);
            stream.writeByte((byte) c); // TODO
        }
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    /** TODO */
    public void writeLine(Object obj) {
    }

    /**
     * TODO
     */
    public void println(String s) {
    }
}

/* */
