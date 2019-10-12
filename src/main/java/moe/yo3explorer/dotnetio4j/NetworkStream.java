/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package moe.yo3explorer.dotnetio4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import moe.yo3explorer.dotnetio4j.compat.JavaIOStream;


/**
 * NetworkStream.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @author <a href="mailto:sano-n@klab.org">Naohide Sano</a> (nsano)
 * @version 0.00 2019/10/12 nsano initial version <br>
 */
public class NetworkStream extends JavaIOStream {

    static InputStream toInputStream(Socket socket) {
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    static OutputStream toOutputStream(Socket socket) {
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            throw new moe.yo3explorer.dotnetio4j.IOException(e);
        }
    }

    /**
     */
    public NetworkStream(Socket socket, boolean b) {
        super(toInputStream(socket), toOutputStream(socket));
    }

    /**
     */
    public NetworkStream(Socket socket) {
        this(socket, false);
    }
}
