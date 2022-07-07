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
        _target = targetInfo;
        _lun = lun;
        _deviceType = type;
        _removable = removable;
        _vendorId = vendor;
        _productId = product;
        _productRevision = revision;
    }

    /**
     * Gets the type (or class) of this device.
     */
    private LunClass _deviceType = LunClass.BlockStorage;

    public LunClass getDeviceType() {
        return _deviceType;
    }

    /**
     * Gets the Logical Unit Number of this device.
     */
    private long _lun;

    public long getLun() {
        return _lun;
    }

    /**
     * Gets the product id (name) for this device.
     */
    private String _productId;

    public String getProductId() {
        return _productId;
    }

    /**
     * Gets the product revision for this device.
     */
    private String _productRevision;

    public String getProductRevision() {
        return _productRevision;
    }

    /**
     * Gets a value indicating whether this Lun has removable media.
     */
    private boolean _removable;

    public boolean getRemovable() {
        return _removable;
    }

    /**
     * Gets info about the target hosting this LUN.
     */
    private TargetInfo _target;

    public TargetInfo getTarget() {
        return _target;
    }

    /**
     * Gets the vendor id (registered name) for this device.
     */
    private String _vendorId;

    public String getVendorId() {
        return _vendorId;
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
        if ((getLun() & 0xFF00000000000000L) == 0) {
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
