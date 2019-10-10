/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


/**
 * DeflateStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class DeflateStream extends JavaIOStream {

    static InputStream toInputStream(Stream stream) {
        return new ZipInputStream(new StreamInputStream(stream));
    }

    static OutputStream toOutputStream(Stream stream) {
        return new ZipOutputStream(new StreamOutputStream(stream));
    }

    /**
     */
    public DeflateStream(Stream stream, CompressionMode decompress) {
        super(toInputStream(stream), toOutputStream(stream));
    }

    /**
     */
    public DeflateStream(Stream stream, CompressionMode compressMode, boolean b) {
        this(stream, compressMode);
    }
}

/* */
