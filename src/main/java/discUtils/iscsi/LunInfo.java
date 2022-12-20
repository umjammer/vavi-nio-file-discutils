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

package discUtils.iscsi;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
        target = targetInfo;
        this.lun = lun;
        deviceType = type;
        this.removable = removable;
        vendorId = vendor;
        productId = product;
        productRevision = revision;
    }

    /**
     * Gets the type (or class) of this device.
     */
    private LunClass deviceType;

    public LunClass getDeviceType() {
        return deviceType;
    }

    /**
     * Gets the Logical Unit Number of this device.
     */
    private long lun;

    public long getLun() {
        return lun;
    }

    /**
     * Gets the product id (name) for this device.
     */
    private String productId;

    public String getProductId() {
        return productId;
    }

    /**
     * Gets the product revision for this device.
     */
    private String productRevision;

    public String getProductRevision() {
        return productRevision;
    }

    /**
     * Gets a value indicating whether this Lun has removable media.
     */
    private boolean removable;

    public boolean getRemovable() {
        return removable;
    }

    /**
     * Gets info about the target hosting this LUN.
     */
    private TargetInfo target;

    public TargetInfo getTarget() {
        return target;
    }

    /**
     * Gets the vendor id (registered name) for this device.
     */
    private String vendorId;

    public String getVendorId() {
        return vendorId;
    }

    /**
     * Parses a URI referring to a LUN.
     *
     * @param uri The URI to parse.
     * @return The LUN info. Note the LUN info is incomplete, only as much of the
     *         information as is encoded into the URL is available.
     */
    public static LunInfo parseUri(String uri) {
        return parseUri(URI.create(uri));
    }

    /**
     * Parses a URI referring to a LUN.
     *
     * @param uri The URI to parse.
     * @return The LUN info. Note the LUN info is incomplete, only as much of the
     *         information as is encoded into the URL is available.
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

        String[] uriSegments = uri.getPath().split("/"); // TODO check
        if (uriSegments.length == 1) {
            targetName = uriSegments[0];
        } else if (uriSegments.length == 2) {
            targetGroupTag = uriSegments[0];
            targetName = uriSegments[1];
        } else {
            throwInvalidURI(uri.toString());
        }

        TargetInfo targetInfo = new TargetInfo(targetName, Collections.singletonList(new TargetAddress(address, port, targetGroupTag)));

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
        if ((getLun() & 0xFF00_0000_0000_0000L) == 0) {
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
            results.add(targetAddress.toUri() + "/" + getTarget().getName() + "?LUN=" + this);
        }

        return results;
    }

    private static void throwInvalidURI(String uri) {
        throw new IllegalArgumentException(String.format("Not a valid iSCSI URI: %s", uri));
    }
}
