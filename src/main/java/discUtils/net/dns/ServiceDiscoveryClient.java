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

package discUtils.net.dns;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vavi.util.Debug;


/**
 * Provides access to DNS-SD functionality.
 */
public final class ServiceDiscoveryClient implements Closeable {

    private final UnicastDnsClient dnsClient;

    private MulticastDnsClient mDnsClient;

    /**
     * Initializes a new instance of the ServiceDiscoveryClient class.
     */
    public ServiceDiscoveryClient() {
        mDnsClient = new MulticastDnsClient();
        dnsClient = new UnicastDnsClient();
    }

    /**
     * Disposes of this instance.
     */
    @Override
    public void close() throws IOException {
        if (mDnsClient != null) {
            mDnsClient.close();
            mDnsClient = null;
        }
    }

    /**
     * Flushes any cached data.
     */
    public void flushCache() {
        mDnsClient.flushCache();
        dnsClient.flushCache();
    }

    /**
     * Queries for all the different types of service available on the local
     * network.
     *
     * @return An array of service types, for example "_http._tcp".
     */
    public List<String> lookupServiceTypes() {
        return lookupServiceTypes("local.");
    }

    /**
     * Queries for all the different types of service available in a domain.
     *
     * @param domain The domain to query.
     * @return An array of service types, for example "_http._tcp".
     */
    public List<String> lookupServiceTypes(String domain) {
        List<ResourceRecord> records = doLookup("_services._dns-sd._udp" + "." + domain, RecordType.Pointer);
        List<String> result = new ArrayList<>();
        for (ResourceRecord record : records) {
            result.add(((PointerRecord) record)
                    .getTargetName()
                    .substring(0, ((PointerRecord) record).getTargetName().length() - (domain.length() + 1)));
        }
        return result;
    }

    /**
     * Queries for all instances of a particular service on the local network,
     * retrieving all details.
     *
     * @param service The service to query, for example "_http._tcp".
     * @return An array of service instances.
     */
    public List<ServiceInstance> lookupInstances(String service) {
        return lookupInstances(service, "local.", ServiceInstanceFields.All);
    }

    /**
     * Queries for all instances of a particular service on the local network.
     *
     * @param service The service to query, for example "_http._tcp".
     * @param fields The details to query.
     * @return An array of service instances.Excluding some fields (for example
     *         the IP address) may reduce the time taken.
     */
    public List<ServiceInstance> lookupInstances(String service, EnumSet<ServiceInstanceFields> fields) {
        return lookupInstances(service, "local.", fields);
    }

    /**
     * Queries for all instances of a particular service on the local network.
     *
     * @param service The service to query, for example "_http._tcp".
     * @param domain The domain to query.
     * @param fields The details to query.
     * @return An array of service instances.Excluding some fields (for example
     *         the IP address) may reduce the time taken.
     */
    public List<ServiceInstance> lookupInstances(String service, String domain, EnumSet<ServiceInstanceFields> fields) {
        List<ResourceRecord> records = doLookup(service + "." + domain, RecordType.Pointer);
        List<ServiceInstance> instances = new ArrayList<>();
        for (ResourceRecord record : records) {
            instances.add(lookupInstance(encodeName(((PointerRecord) record).getTargetName(), record.getName()),
                                         fields));
        }
        return instances;
    }

    /**
     * Queries for all instances of a particular service on the local network.
     *
     * @param name The instance to query, for example "My WebServer._http._tcp".
     * @param fields The details to query.
     * @return The service instance.Excluding some fields (for example the IP
     *         address) may reduce the time taken.
     */
    public ServiceInstance lookupInstance(String name, EnumSet<ServiceInstanceFields> fields) {
        ServiceInstance instance = new ServiceInstance(name);
        if (fields.contains(ServiceInstanceFields.DisplayName)) {
            instance.setDisplayName(decodeDisplayName(name));
        }

        if (fields.contains(ServiceInstanceFields.Parameters)) {
            instance.setParameters(lookupInstanceDetails(name));
        }

        if (fields.contains(ServiceInstanceFields.DnsAddresses) ||
            fields.contains(ServiceInstanceFields.IPAddresses)) {
            instance.setEndPoints(lookupInstanceEndpoints(name, fields));
        }

        return instance;
    }

    private static String encodeName(String fullName, String suffix) {
        String instanceName = fullName.substring(0, fullName.length() - (suffix.length() + 1));
        StringBuilder sb = new StringBuilder();
        for (char ch : instanceName.toCharArray()) {
            if (ch == '.' || ch == '\\') {
                sb.append('\\');
            }

            sb.append(ch);
        }
        return sb + "." + suffix;
    }

    private static String decodeDisplayName(String fullName) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < fullName.length()) {
            char ch = fullName.charAt(i++);
            if (ch == '.') {
                return sb.toString();
            }

            if (ch == '\\') {
                ch = fullName.charAt(i++);
            }

            sb.append(ch);
        }
        return sb.toString();
    }

    private List<ServiceInstanceEndPoint> lookupInstanceEndpoints(String name, EnumSet<ServiceInstanceFields> fields) {
        List<ResourceRecord> records = doLookup(name, RecordType.Service);

        List<ServiceInstanceEndPoint> endpoints = new ArrayList<>();

        for (ResourceRecord record : records) {
            List<InetSocketAddress> ipEndPoints = null;
            if (fields.contains(ServiceInstanceFields.IPAddresses)) {
                ipEndPoints = new ArrayList<>();

                List<ResourceRecord> ipRecords = doLookup(((ServiceRecord) record).getTarget(), RecordType.Address);

                for (ResourceRecord ipRecord : ipRecords) {
                    ipEndPoints.add(new InetSocketAddress(((IP4AddressRecord) ipRecord).getAddress(),
                                                          ((ServiceRecord) record).getPort()));
                }

                List<ResourceRecord> ip6Records = doLookup(((ServiceRecord) record).getTarget(), RecordType.IP6Address);

//              for (Ip6AddressRecord ipRecord : ipRecords) {
//                  ipEndPoints.add(new IPEndPoint(ipRecord.Address, record.Port));
//              }
            }

            endpoints.add(new ServiceInstanceEndPoint(((ServiceRecord) record).getPriority(),
                                                      ((ServiceRecord) record).getWeight(),
                                                      ((ServiceRecord) record).getPort(),
                                                      ((ServiceRecord) record).getTarget(),
                                                      ipEndPoints));
        }

        return endpoints;
    }

    private Map<String, byte[]> lookupInstanceDetails(String name) {
        List<ResourceRecord> records = doLookup(name, RecordType.Text);

        Map<String, byte[]> details = new HashMap<>();

        for (ResourceRecord record : records) {
            details.putAll(((TextRecord) record).getValues());
        }

        return details;
    }

    private List<ResourceRecord> doLookup(String name, RecordType recordType) {
        String fullName = DnsClient.normalizeDomainName(name);
Debug.println("fullname: " + fullName);

        DnsClient dnsClient;

        if (fullName.endsWith(".local.")) {
            dnsClient = mDnsClient;
        } else {
            dnsClient = this.dnsClient;
        }

        ResourceRecord[] records = dnsClient.lookup(fullName, recordType);

        List<ResourceRecord> cleanList = new ArrayList<>();
        for (ResourceRecord record : records) {
            if (record.getRecordType() == recordType && fullName.compareTo(record.getName()) == 0) {
                cleanList.add(record);
            }
        }

        return cleanList;
    }
}
