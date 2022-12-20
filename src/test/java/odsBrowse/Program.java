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

package odsBrowse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import discUtils.common.ProgramBase;
import discUtils.opticalDiscSharing.DiscInfo;
import discUtils.opticalDiscSharing.OpticalDiscService;
import discUtils.opticalDiscSharing.OpticalDiscServiceClient;
import dotnet4j.util.compat.Utilities;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@Options
public class Program extends ProgramBase {

    @Option(option = "host",
            description = "The name of a Mac / PC sharing its optical disk(s).  For example \"My Computer\".",
            required = true)
    private String host;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    @Override
    protected void doRun() throws IOException {
        OpticalDiscServiceClient odsClient = new OpticalDiscServiceClient();
        if (host != null) {
            boolean found = false;
            for (OpticalDiscService service : odsClient.lookupServices()) {
                if (Utilities.equals(host, service.getDisplayName()) ||
                    Utilities.equals(host, URLEncoder.encode(service.getDisplayName(), StandardCharsets.UTF_8.name()))) {
                    found = true;

                    System.err.println("Connecting to " + service.getDisplayName() + " - the owner may need to accept...");
                    service.connect(System.getProperty("user.name"), InetAddress.getLocalHost().getHostName(), 30);

                    showService(service);

                    break;
                }
            }
            if (!found) {
                System.err.println("Host not found");
            }
        } else {
            for (OpticalDiscService service : odsClient.lookupServices()) {
                showService(service);
                System.err.println();
            }
        }
        odsClient.close();
    }

    private static void showService(OpticalDiscService service) throws IOException {
        System.err.println();
        System.err.println("Service: " + service.getDisplayName());
        System.err.println("  Safe Name: " + URLEncoder.encode(service.getDisplayName(), StandardCharsets.UTF_8.name()) +
                           "  (for URLs, copy+paste)");
        System.err.println();

        boolean foundDisk = false;
        for (DiscInfo disk : service.getAdvertisedDiscs()) {
            foundDisk = true;
            System.err.println("  Disk: " + disk.getVolumeLabel());
            System.err.println("    Name: " + disk.getName());
            System.err.println("    Type: " + disk.getVolumeType());
            System.err.println("     Url: " +
                               URLEncoder.encode("ods://local/" + service.getDisplayName() + "/" + disk.getVolumeLabel(),
                                                 StandardCharsets.UTF_8.name()));
        }

        if (!foundDisk) {
            System.err.println("  [No disks found - try specifying host to connect for full list]");
        }
    }
}
