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

import vavi.io.InputEngine;
import vavi.io.InputEngineOutputStream;
import vavi.io.OutputEngine;
import vavi.io.OutputEngineInputStream;

import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


/**
 * GZipStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/11 umjammer initial version <br>
 */
public class GZipStream extends JavaIOStream {

    @SuppressWarnings("resource")
    static InputStream toInputStream(Stream stream, CompressionMode compressionMode) {
        try {
            InputStream is = new StreamInputStream(stream);
            return compressionMode == CompressionMode.Decompress ? new GZIPInputStream(is)
                                                                 : new OutputEngineInputStream(new OutputEngine() {
                                                                     OutputStream out;

                                                                     public void initialize(OutputStream out) throws IOException {
                                                                         this.out = new GZIPOutputStream(out);
                                                                     }

                                                                     byte[] buf = new byte[8192];

                                                                     public void execute() throws IOException {
                                                                         int r = is.read(buf);
                                                                         out.write(buf, 0, r);
                                                                     }

                                                                     public void finish() throws IOException {
                                                                     }
                                                                 });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    @SuppressWarnings("resource")
    static OutputStream toOutputStream(Stream stream, CompressionMode compressionMode) {
        try {
            OutputStream os = new StreamOutputStream(stream);
            return compressionMode == CompressionMode.Compress ? new GZIPOutputStream(os)
                                                               : new InputEngineOutputStream(new InputEngine() {
                                                                   InputStream in;

                                                                   public void initialize(InputStream in) throws IOException {
                                                                       this.in = new GZIPInputStream(in);
                                                                   }

                                                                   byte[] buf = new byte[8192];

                                                                   public void execute() throws IOException {
                                                                       int r = in.read(buf);
                                                                       os.write(buf, 0, r);
                                                                   }

                                                                   public void finish() throws IOException {
                                                                   }
                                                               });
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     */
    public GZipStream(Stream stream, CompressionMode compressionMode) {
        super(toInputStream(stream, compressionMode), toOutputStream(stream, compressionMode));
    }
}

/* */
