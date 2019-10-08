//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package DiscUtils.OpticalDiscSharing;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.CoreCompat.ListSupport;
import DiscUtils.Streams.StreamExtent;
import DiscUtils.Streams.Buffer.Buffer;
import moe.yo3explorer.dotnetio4j.Stream;


public final class DiscContentBuffer extends Buffer {
    private String _authHeader;

    private String _password;

    private URI _uri;

    private String _userName;

    public DiscContentBuffer(URI uri, String userName, String password) {
        _uri = uri;
        _userName = userName;
        _password = password;
        HttpWebResponse response = sendRequest(() -> {
            HttpWebRequest wr = (HttpWebRequest) WebRequest.Create(uri);
            wr.Method = "HEAD";
            return wr;
        });
        __Capacity = response.ContentLength;
    }

    public boolean canRead() {
        return true;
    }

    public boolean canWrite() {
        return false;
    }

    private long __Capacity;

    public long getCapacity() {
        return __Capacity;
    }

    public int read(long pos, byte[] buffer, int offset, int count) {
        HttpWebResponse response = sendRequest(() -> {
            HttpWebRequest wr = (HttpWebRequest) WebRequest.Create(_uri);
            wr.Method = "GET";
            wr.AddRange((int) pos, (int) (pos + count - 1));
            return wr;
        });
        Stream s = response.getResponseStream();
        try {
            int total = (int) response.ContentLength;
            int read = 0;
            while (read < Math.min(total, count)) {
                read += s.read(buffer, offset + read, count - read);
            }
            return read;
        } finally {
            if (s != null)
                s.close();
        }
    }

    public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException("Attempt to write to shared optical disc");
    }

    public void setCapacity(long value) {
        throw new UnsupportedOperationException("Attempt to change size of shared optical disc");
    }

    public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect();
    }

    private static String toHexString(byte[] p) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < p.length; ++i) {
            int j = (p[i] >> 4) & 0xf;
            result.append((char) (j <= 9 ? '0' + j : 'a' + (j - 10)));
            j = p[i] & 0xf;
            result.append((char) (j <= 9 ? '0' + j : 'a' + (j - 10)));
        }
        return result.toString();
    }

    private static Map<String, String> parseAuthenticationHeader(String header, String[] authMethod) {
        Map<String, String> result = new HashMap<>();
        String[] elements = header.split(" ");
        authMethod[0] = elements[0];
        for (int i = 1; i < elements.length; ++i) {
            String[] nvPair = elements[i].split("=", 2);
            result.put(nvPair[0], nvPair[1].replaceAll("(^\"*|\"*$", ""));
        }
        return result;
    }

    private HttpWebResponse sendRequest(WebRequestCreator wrc) {
        HttpWebRequest wr = wrc.invoke();
        if (_authHeader != null) {
            wr.Headers.put("Authorization", _authHeader);
        }

        try {
            return (HttpWebResponse) wr.GetResponse();
        } catch (WebException we) {
            HttpWebResponse wresp = (HttpWebResponse) we.Response;
            if (wresp.StatusCode == HttpStatusCode.Unauthorized) {
                String authMethod;
                RefSupport<String> refVar___0 = new RefSupport<String>();
                Map<String, String> authParams = ParseAuthenticationHeader(wresp.Headers.get("WWW-Authenticate"), refVar___0);
                authMethod = refVar___0.getValue();
                if (!authMethod.equals("Digest")) {
                    throw we;
                }

                String resp = CalcDigestResponse(authParams.get("nonce"),
                                                 wr.RequestUri.AbsolutePath,
                                                 wr.Method,
                                                 authParams.get("realm"));
                _authHeader = "Digest username=\"" + _userName + "\", realm=\"ODS\", nonce=\"" + authParams.get("nonce") +
                              "\", uri=\"" + wr.RequestUri.AbsolutePath + "\", response=\"" + resp + "\"";
                (wresp instanceof Closeable ? (Closeable) wresp : (Closeable) null).close();
                wr = wrc.invoke();
                wr.Headers.put("Authorization", _authHeader);
                return (HttpWebResponse) wr.GetResponse();
            }

            throw we;
        }
    }

    private String calcDigestResponse(String nonce, String uriPath, String method, String realm) {
        try {
            String a2 = method + ":" + uriPath;
            MessageDigest ha2hash = MessageDigest.getInstance("MD5");
            String ha2 = toHexString(ha2hash.digest(a2.getBytes(Charset.forName("ASCII"))));
            String a1 = _userName + ":" + realm + ":" + _password;
            MessageDigest ha1hash = MessageDigest.getInstance("MD5");
            String ha1 = toHexString(ha1hash.digest(a1.getBytes(Charset.forName("ASCII"))));
            String toHash = ha1 + ":" + nonce + ":" + ha2;
            MessageDigest respHas = MessageDigest.getInstance("MD5");
            byte[] hash = respHas.digest(toHash.getBytes(Charset.forName("ASCII")));
            return toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

//    public static class __MultiWebRequestCreator implements WebRequestCreator {
//        public HttpWebRequest invoke() {
//            List<WebRequestCreator> copy = new ArrayList<>(), members = this.getInvocationList();
//            synchronized (members) {
//                copy = new LinkedList<>(members);
//            }
//            WebRequestCreator prev = null;
//            for (WebRequestCreator d : copy) {
//                if (prev != null)
//                    prev.invoke();
//
//                prev = d;
//            }
//            return prev.invoke();
//        }
//
//        private List<WebRequestCreator> _invocationList = new ArrayList<>();
//
//        public static WebRequestCreator combine(WebRequestCreator a, WebRequestCreator b) {
//            if (a == null)
//                return b;
//
//            if (b == null)
//                return a;
//
//            __MultiWebRequestCreator ret = new __MultiWebRequestCreator();
//            ret._invocationList = a.getInvocationList();
//            ret._invocationList.addAll(b.getInvocationList());
//            return ret;
//        }
//
//        public static WebRequestCreator remove(WebRequestCreator a, WebRequestCreator b) {
//            if (a == null || b == null)
//                return a;
//
//            List<WebRequestCreator> aInvList = a.getInvocationList();
//            List<WebRequestCreator> newInvList = ListSupport.removeFinalStretch(aInvList, b.getInvocationList());
//            if (aInvList == newInvList) {
//                return a;
//            } else {
//                __MultiWebRequestCreator ret = new __MultiWebRequestCreator();
//                ret._invocationList = newInvList;
//                return ret;
//            }
//        }
//
//        public List<WebRequestCreator> getInvocationList() {
//            return _invocationList;
//        }
//    }

    @FunctionalInterface
    public static interface WebRequestCreator {
        HttpWebRequest invoke();

//        List<WebRequestCreator> getInvocationList();
    }
}
