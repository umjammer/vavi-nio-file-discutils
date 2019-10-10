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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import DiscUtils.Core.Plist;
import DiscUtils.Core.VirtualDisk;
import DiscUtils.Net.Dns.ServiceDiscoveryClient;
import DiscUtils.Net.Dns.ServiceInstance;
import DiscUtils.Net.Dns.ServiceInstanceEndPoint;
import DiscUtils.Net.Dns.ServiceInstanceFields;
import moe.yo3explorer.dotnetio4j.Stream;


/**
 * Represents a particular Optical Disc Sharing service (typically a Mac or PC).
 */
public final class OpticalDiscService {
    private String _askToken;

    private ServiceInstance _instance;

    private final ServiceDiscoveryClient _sdClient;

    private String _userName;

    public OpticalDiscService(ServiceInstance instance, ServiceDiscoveryClient sdClient) {
        _sdClient = sdClient;
        _instance = instance;
    }

    /**
     * Gets information about the optical discs advertised by this service.
     */
    public List<DiscInfo> getAdvertisedDiscs() {
        List<DiscInfo> result = new ArrayList<>();
        for (Map.Entry<String, byte[]> sdParam : _instance.getParameters().entrySet()) {
            if (sdParam.getKey().startsWith("disk")) {
                Map<String, String> diskParams = getParams(sdParam.getKey());
                String infoVal;
                DiscInfo info = new DiscInfo();
                if (diskParams.containsKey("adVN")) {
                    infoVal = diskParams.get("adVN");
                    info.setVolumeLabel(infoVal);
                }

                if (diskParams.containsKey("adVT")) {
                    infoVal = diskParams.get("adVT");
                    info.setVolumeType(infoVal);
                }

                result.add(info);
            }
        }
        return result;
    }

    /**
     * Gets the display name of this service.
     */
    public String getDisplayName() {
        return _instance.getDisplayName();
    }

    /**
     * Connects to the service.
     *
     * @param userName The username to use, if the owner of the Mac / PC is
     *            prompted.
     * @param computerName The computer name to use, if the owner of the Mac /
     *            PC is prompted.
     * @param maxWaitSeconds The maximum number of seconds to wait to be granted
     *            access.
     */
    public void connect(String userName, String computerName, int maxWaitSeconds) {
        Map<String, String> sysParams = getParams("sys");
        int volFlags = 0;
        String volFlagsStr;
        if (sysParams.containsKey("adVF")) {
            volFlagsStr = sysParams.get("adVF");
            volFlags = parseInt(volFlagsStr);
        }

        if ((volFlags & 0x200) != 0) {
            _userName = userName;
            askForAccess(userName, computerName, maxWaitSeconds);
            // Flush any stale mDNS data - the server advertises extra info (such as the discs available)
            // after a client is granted permission to access a disc.
            _sdClient.flushCache();
            _instance = _sdClient.lookupInstance(_instance.getName(), ServiceInstanceFields.All);
        }
    }

    /**
     * Opens a shared optical disc as a virtual disk.
     *
     * @param name The name of the disc, from the Name field of DiscInfo.
     * @return The virtual disk.
     */
    public VirtualDisk openDisc(String name) {
        ServiceInstanceEndPoint siep = _instance.getEndPoints().get(0);
        List<InetSocketAddress> ipAddrs = new ArrayList<>(siep.getInetSocketAddresss());
        URI uri = URI
                .create("http" + "://" + ipAddrs.get(0).getAddress() + ":" + ipAddrs.get(0).getPort() + "/" + name + ".dmg");
        return new Disc(uri, _userName, _askToken);
    }

    private static String getAskToken(String askId, URI uri, int maxWaitSecs) {
        URI newURI = URI.create(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/ods-ask-status" + "?" +
                                "askID=" + askId);
        boolean askBusy = true;
        String askStatus = "unknown";
        String askToken = null;
        Instant start = Instant.now();
        Duration maxWait = Duration.ofSeconds(maxWaitSecs);
        while (askStatus.equals("unknown") && maxWait.compareTo(Duration.between(Instant.now(), start)) > 0) {
            Thread.sleep(1000);
            WebRequest wreq = WebRequest.Create(uri);
            wreq.Method = "GET";
            WebResponse wrsp = wreq.GetResponse();
            Stream inStream = wrsp.GetResponseStream();
            try {
                Map<String, Object> plist = Plist.parse(inStream);
                askBusy = (boolean) plist.get("askBusy");
                askStatus = plist.get("askStatus") instanceof String ? (String) plist.get("askStatus") : (String) null;
                if (askStatus.equals("accepted")) {
                    askToken = plist.get("askToken") instanceof String ? (String) plist.get("askToken") : (String) null;
                }
            } finally {
                if (inStream != null)
                    inStream.close();
            }
        }
        if (askToken == null) {
            throw new IllegalAccessException("Access not granted");
        }

        return askToken;
    }

    private static String initiateAsk(String userName, String computerName, URI uri) {
        URI newURI = URI.create(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/ods-ask");
        HttpWebRequest wreq = (HttpWebRequest) WebRequest.Create(uri);
        wreq.Method = "POST";
        Map<String, Object> req = new HashMap<>();
        req.put("askDevice", "");
        req.put("computer", computerName);
        req.put("user", userName);
        Stream outStream = wreq.GetRequestStream();
        try {
            Plist.write(outStream, req);
        } finally {
            if (outStream != null)
                outStream.close();

        }
        String askId;
        WebResponse wrsp = wreq.GetResponse();
        Stream inStream = wrsp.GetResponseStream();
        try {
            Map<String, Object> plist = Plist.parse(inStream);
            askId = String.valueOf((int) plist.get("askID"));
        } finally {
            if (inStream != null)
                inStream.close();
        }
        return askId;
    }

    private static int parseInt(String volFlagsStr) {
        if (volFlagsStr.startsWith("0x")) {
            return Integer.parseInt(volFlagsStr.substring(2), 16);
        }

        return Integer.parseInt(volFlagsStr);
    }

    private void askForAccess(String userName, String computerName, int maxWaitSecs) {
        ServiceInstanceEndPoint siep = _instance.getEndPoints().get(0);
        List<InetSocketAddress> ipAddrs = new ArrayList<>(siep.getInetSocketAddresss());
        URI uri = URI.create("http" + "://" + ipAddrs.get(0).getAddress() + ":" + ipAddrs.get(0).getPort());
        String askId = initiateAsk(userName, computerName, uri);
        _askToken = getAskToken(askId, uri, maxWaitSecs);
    }

    private Map<String, String> getParams(String section) {
        Map<String, String> result = new HashMap<>();
        if (_instance.getParameters().containsKey(section)) {
            byte[] data = _instance.getParameters().get(section);
            String asString = new String(data, Charset.forName("ASCII"));
            String[] nvPairs = asString.split(",");
            for (String nvPair : nvPairs) {
                String[] parts = nvPair.split("=");
                result.put(parts[0], parts[1]);
            }
        }

        return result;
    }
}
