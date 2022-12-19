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

package iSCSIBrowse;

import java.io.IOException;
import java.util.List;

import discUtils.common.ProgramBase;
import discUtils.iscsi.Initiator;
import discUtils.iscsi.LoginException;
import discUtils.iscsi.LunInfo;
import discUtils.iscsi.Session;
import discUtils.iscsi.TargetAddress;
import discUtils.iscsi.TargetInfo;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@Options
public class Program extends ProgramBase {

    @Option(option = "portal",
            description = "Address of the iSCSI server (aka Portal) in the form <host>[:<port>], for example 192.168.1.2:3260 or 192.168.1.2",
            args = 1,
            required = true)
    private String portalAddress;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

//        return StandardSwitches.UserAndPassword | StandardSwitches.Verbose;

    protected void doRun() throws IOException {
        Initiator initiator = new Initiator();
        if (getUserName() != null && !getUserName().isEmpty()) {
            initiator.setCredentials(getUserName(), getPassword());
        }

        boolean foundTargets = false;
        try {
            for (TargetInfo target : initiator.getTargets(portalAddress)) {
                foundTargets = true;
                System.err.println("Target: " + target);
                if (getVerbose()) {
                    System.err.println("  Name: " + target.getName());
                    for (TargetAddress addr : target.getAddresses()) {
                        System.err.println("  Address: " + addr + "  <" + addr.toUri() + ">");
                    }
                    System.err.println();
                }

                try (Session s = initiator.connectTo(target)) {
                    for (LunInfo lun : s.getLuns()) {
                        System.err.println(lun.getDeviceType() + ": ");
                        List<String> uris = lun.getUris();
                        if (uris.size() > 1) {
                            for (int i = 0; i < uris.size(); ++i) {
                                System.err.println("  URI[" + i + "]: " + uris.get(i));
                            }
                        } else if (uris.size() > 0) {
                            System.err.println("  URI: " + uris.get(0));
                        }

                        if (getVerbose()) {
                            System.err.println("  LUN: " + String.format("%016x", lun.getLun()));
                            System.err.println("  Device Type: " + lun.getDeviceType());
                            System.err.println("  Removeable: " + (lun.getRemovable() ? "Yes" : "No"));
                            System.err.println("  Vendor: " + lun.getVendorId());
                            System.err.println("  Product: " + lun.getProductId());
                            System.err.println("  Revision: " + lun.getProductRevision());
                            System.err.println();
                        }
                    }
                }
            }
            if (!foundTargets) {
                System.err.println("No targets found");
            }
        } catch (LoginException e) {
            e.printStackTrace();
            System.err.println("ERROR: Need credentials, or the credentials specified were invalid");
        }
    }
}
