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

package DiscUtils.Iscsi;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Provides information about an iSCSI LUN.
 */
public class LunInfo {
    public LunInfo(TargetInfo targetInfo,
            long lun,
            LunClass type,
            boolean removable,
            String vendor,
            String product,
            String revision) {
        __Target = targetInfo;
        __Lun = lun;
        __DeviceType = type;
        __Removable = removable;
        __VendorId = vendor;
        __ProductId = product;
        __ProductRevision = revision;
    }

    /**
     * Gets the type (or class) of this device.
     */
    private LunClass __DeviceType = LunClass.BlockStorage;

    public LunClass getDeviceType() {
        return __DeviceType;
    }

    /**
     * Gets the Logical Unit Number of this device.
     */
    private long __Lun;

    public long getLun() {
        return __Lun;
    }

    /**
     * Gets the product id (name) for this device.
     */
    private String __ProductId;

    public String getProductId() {
        return __ProductId;
    }

    /**
     * Gets the product revision for this device.
     */
    private String __ProductRevision;

    public String getProductRevision() {
        return __ProductRevision;
    }

    /**
     * Gets a value indicating whether this Lun has removable media.
     */
    private boolean __Removable;

    public boolean getRemovable() {
        return __Removable;
    }

    /**
     * Gets info about the target hosting this LUN.
     */
    private TargetInfo __Target;

    public TargetInfo getTarget() {
        return __Target;
    }

    /**
     * Gets the vendor id (registered name) for this device.
     */
    private String __VendorId;

    public String getVendorId() {
        return __VendorId;
    }

    /**
     * Parses a URI referring to a LUN.
     *
     * @param uri The URI to parse.
     * @return The LUN info.
     *         Note the LUN info is incomplete, only as much of the information
     *         as is encoded
     *         into the URL is available.
     */
    public static LunInfo parseUri(String uri) {
        return parseUri(URI.create(uri));
    }

    /**
     * Parses a URI referring to a LUN.
     *
     * @param uri The URI to parse.
     * @return The LUN info.
     *         Note the LUN info is incomplete, only as much of the information
     *         as is encoded
     *         into the URL is available.
     */
    public static LunInfo parseUri(URI uri) {
        String address;
        int port;
        String targetGroupTag = "";
        String targetName = "";
        long lun = 0;
        if (!uri.getScheme().equals("iscsi")) {
            throwInvalidURI(uri.toString());
        }

        address = uri.getHost();
        port = uri.getPort();
        if (uri.getPort() == -1) {
            port = TargetAddress.DefaultPort;
        }

        String[] uriSegments = null; // TODO uri.Segments;
        if (uriSegments.length == 2) {
            targetName = uriSegments[1].replace("/", "");
        } else if (uriSegments.length == 3) {
            targetGroupTag = uriSegments[1].replace("/", "");
            targetName = uriSegments[2].replace("/", "");
        } else {
            throwInvalidURI(uri.toString());
        }
        TargetInfo targetInfo = new TargetInfo(targetName, Arrays.asList());
        for (String queryElem : uri.getQuery().substring(1).split("&")) {
            if (queryElem.startsWith("LUN=")) {
                lun = Long.parseLong(queryElem.substring(4));
                if (lun < 256) {
                    lun = lun << (6 * 8);
                }
            }
        }
        return new LunInfo(targetInfo, lun, LunClass.Unknown, false, "", "", "");
    }

    /**
     * Gets the LUN as a string.
     *
     * @return The LUN in string form.
     */
    public String toString() {
        if ((getLun() & 0xFF00000000000000l) == 0) {
            return String.valueOf(getLun() >>> (6 * 8));
        }

        return String.valueOf(getLun());
    }

    /**
     * Gets the URIs corresponding to this LUN.
     *
     * @return An array of URIs as strings.Multiple URIs are returned because
     *         multiple targets may serve the same LUN.
     */
    public List<String> getUris() {
        List<String> results = new ArrayList<>();
        for (TargetAddress targetAddress : getTarget().getAddresses()) {
            results.add(targetAddress.toUri() + "/" + getTarget().getName() + "?LUN=" + toString());
        }
        return results;
    }

    private static void throwInvalidURI(String uri) {
        throw new IllegalArgumentException(String.format("Not a valid iSCSI URI: %s", uri));
    }
}
