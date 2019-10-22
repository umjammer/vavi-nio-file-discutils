/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


/**
 * DeflateStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/09/30 umjammer initial version <br>
 */
public class DeflateStream extends JavaIOStream {

    static InputStream toInputStream(Stream stream, CompressionMode compressionMode) {
        InputStream is = new StreamInputStream(stream);
        return compressionMode == CompressionMode.Decompress ? new InflaterInputStream(is) : new DeflaterInputStream(is);
    }

    static OutputStream toOutputStream(Stream stream, CompressionMode compressionMode) {
        OutputStream os = new StreamOutputStream(stream);
        return compressionMode == CompressionMode.Compress ? new DeflaterOutputStream(os) : new DeflaterOutputStream(os);
    }

    /**
     */
    public DeflateStream(Stream stream, CompressionMode compressionMode) {
        this(stream, compressionMode, false);
    }

    /**
     */
    public DeflateStream(Stream stream, CompressionMode compressionMode, boolean leaveOpen) {
        super(toInputStream(stream, compressionMode), toOutputStream(stream, compressionMode), false);
    }
}

/* */
