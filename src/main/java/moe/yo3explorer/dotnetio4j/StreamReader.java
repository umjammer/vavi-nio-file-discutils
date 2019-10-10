/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;


/**
 * StreamInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class StreamReader extends Reader {

    private Stream stream;

    private Charset encoding = Charset.forName("utf-8");

    public StreamReader(Stream stream) {
        this.stream = stream;
    }

    /** TODO */
    public StreamReader(Stream stream, boolean b) {
        this.stream = stream;
    }

    /**
     */
    public StreamReader(Stream stream, Charset encoding) {
        this.stream = stream;
    }

    /**
     */
    public StreamReader(Stream stream, Charset encoding, boolean b) {
        this.stream = stream;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        byte[] buf = new byte[(len - off) * 2];
        return stream.read(buf, 0, (len - off) * 2); // TODO
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    /** */
    public String readLine() {
        return null; // TODO
    }

    /** */
    public String readToEnd() {
        return null;
    }

    /**
     * @return
     */
    public boolean isEndOfStream() {
        // TODO Auto-generated method stub
        return false;
    }
}

/* */
