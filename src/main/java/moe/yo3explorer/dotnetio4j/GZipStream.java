/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;

/**
 * GZipStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/11 umjammer initial version <br>
 */
public class GZipStream extends JavaIOStream {

    static InputStream toInputStream(Stream stream) {
        try {
            return new GZIPInputStream(new StreamInputStream(stream));
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    static OutputStream toOutputStream(Stream stream) {
        try {
            return new GZIPOutputStream(new StreamOutputStream(stream));
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     */
    public GZipStream(Stream stream, CompressionMode decompress) {
        super(toInputStream(stream), toOutputStream(stream));
    }
}

/* */
