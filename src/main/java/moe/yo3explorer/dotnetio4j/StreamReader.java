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

    Stream stream;

    public StreamReader(Stream stream) {
        this.stream = stream;
    }

    /** TODO */
    public StreamReader(Stream stream, boolean b) {
        this.stream = stream;
    }

    /**
     * @param _contentStream
     * @param forName
     * @param b
     */
    public StreamReader(Stream _contentStream, Charset forName, boolean b) {
        // TODO Auto-generated constructor stub
    }

    /**
     * @param _contentStream
     * @param encoding
     */
    public StreamReader(Stream _contentStream, Charset encoding) {
        // TODO Auto-generated constructor stub
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
