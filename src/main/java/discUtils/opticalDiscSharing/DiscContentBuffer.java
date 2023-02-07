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

package discUtils.opticalDiscSharing;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discUtils.streams.StreamExtent;
import discUtils.streams.buffer.Buffer;
import dotnet4j.io.Stream;
import dotnet4j.io.compat.JavaIOStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public final class DiscContentBuffer extends Buffer {

    private String authHeader;

    private String password;

    private URI uri;

    private String userName;

    OkHttpClient client;

    public DiscContentBuffer(URI uri, String userName, String password) {
        this.uri = uri;
        this.userName = userName;
        this.password = password;
        client = new OkHttpClient().newBuilder().followRedirects(false).followSslRedirects(false).build();
        Response response = sendRequest(() -> new Request.Builder().url(uri.toString()).head().build());
        capacity = response.body().contentLength();
    }

    @Override public boolean canRead() {
        return true;
    }

    @Override public boolean canWrite() {
        return false;
    }

    private long capacity;

    @Override public long getCapacity() {
        return capacity;
    }

    @Override public int read(long pos, byte[] buffer, int offset, int count) {
        Response response = sendRequest(() -> new Request.Builder().url(uri.toString())
                .get()
                .addHeader("Range", String.format("bytes=%d-%d", (int) pos, (int) (pos + count - 1)))
                .build());
        try (Stream s = new JavaIOStream(response.body().byteStream(), null)) {
            int total = (int) response.body().contentLength();
            int read = 0;
            while (read < Math.min(total, count)) {
                read += s.read(buffer, offset + read, count - read);
            }
            return read;
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    @Override public void write(long pos, byte[] buffer, int offset, int count) {
        throw new UnsupportedOperationException("Attempt to write to shared optical disc");
    }

    @Override public void setCapacity(long value) {
        throw new UnsupportedOperationException("Attempt to change size of shared optical disc");
    }

    @Override public List<StreamExtent> getExtentsInRange(long start, long count) {
        return StreamExtent.intersect(Collections.singletonList(new StreamExtent(0, capacity)),
                Collections.singletonList(new StreamExtent(start, count)));
    }

    private static String toHexString(byte[] p) {
        StringBuilder result = new StringBuilder();
        for (byte b : p) {
            int j = (b >>> 4) & 0xf;
            result.append((char) (j <= 9 ? '0' + j : 'a' + (j - 10)));
            j = b & 0xf;
            result.append((char) (j <= 9 ? '0' + j : 'a' + (j - 10)));
        }
        return result.toString();
    }

    /**
     * @param authMethod {@cs out}
     */
    private static Map<String, String> parseAuthenticationHeader(String header, String[] authMethod) {
        Map<String, String> result = new HashMap<>();

        String[] elements = header.split(" ");

        authMethod[0] = elements[0];

        for (int i = 1; i < elements.length; ++i) {
            String[] nvPair = elements[i].split("=", 2);
            result.put(nvPair[0], nvPair[1].replaceAll("(^\"*|\"*$)", ""));
        }

        return result;
    }

    private Response sendRequest(WebRequestCreator wrc) {
        Request wr = wrc.invoke();
        if (authHeader != null) {
            wr = wr.newBuilder().addHeader("Authorization", authHeader).build();
        }

        Response wresp;
        try {
            wresp = client.newCall(wr).execute();
            if (wresp.isSuccessful()) {
                return wresp;
            } else if (wresp.code() == 401) {
                String[] authMethod = new String[1];
                Map<String, String> authParams = parseAuthenticationHeader(wresp.header("WWW-Authenticate"), authMethod);
                if (!authMethod[0].equals("Digest")) {
                    throw new dotnet4j.io.IOException("status: " + wresp.code());
                }

                String resp = calcDigestResponse(authParams.get("nonce"),
                                                 wr.url().uri().getPath(),
                                                 wr.method(),
                                                 authParams.get("realm"));
                authHeader = "Digest username=\"" + userName + "\", realm=\"ODS\", nonce=\"" + authParams.get("nonce") +
                              "\", uri=\"" + wr.url().uri().getPath() + "\", response=\"" + resp + "\"";
                wr = wrc.invoke();
                wr = wr.newBuilder().addHeader("Authorization", authHeader).build();
                try {
                    return client.newCall(wr).execute();
                } catch (IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            } else {
                throw new dotnet4j.io.IOException("status: " + wresp.code());
            }
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }

    private String calcDigestResponse(String nonce, String uriPath, String method, String realm) {
        try {
            String a2 = method + ":" + uriPath;
            MessageDigest ha2hash = MessageDigest.getInstance("MD5");
            String ha2 = toHexString(ha2hash.digest(a2.getBytes(StandardCharsets.US_ASCII)));
            String a1 = userName + ":" + realm + ":" + password;
            MessageDigest ha1hash = MessageDigest.getInstance("MD5");
            String ha1 = toHexString(ha1hash.digest(a1.getBytes(StandardCharsets.US_ASCII)));
            String toHash = ha1 + ":" + nonce + ":" + ha2;
            MessageDigest respHas = MessageDigest.getInstance("MD5");
            byte[] hash = respHas.digest(toHash.getBytes(StandardCharsets.US_ASCII));
            return toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @FunctionalInterface
    public interface WebRequestCreator {

        Request invoke();
    }
}
