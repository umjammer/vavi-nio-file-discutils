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

package discUtils.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import discUtils.core.FileSystemParameters;
import discUtils.core.GenericDiskAdapterType;
import discUtils.core.VirtualDisk;
import discUtils.core.VirtualDiskManager;
import discUtils.core.VirtualDiskParameters;
import discUtils.streams.PumpProgressEventArgs;
import org.klab.commons.cli.HelpOption;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@HelpOption(option = "h", argName = "help", description = "Show this help.", helpHandler = ProgramBase.class)
public class ProgramBase implements Options.ExceptionHandler<ProgramBase> {

    private String outputDiskType;

    private String outputDiskVariant;

    protected String getUserName() {
        return userName;
    }

    protected String getPassword() {
        return password;
    }

    protected String getOutputDiskType() {
        return outputDiskType;
    }

    protected String getOutputDiskVariant() {
        return outputDiskVariant;
    }

    protected GenericDiskAdapterType getAdapterType() {
        return adapterType;
    }

    protected boolean getQuiet() {
        return quiet;
    }

    protected boolean getVerbose() {
        return verbose;
    }

    protected int getPartition() {
        return partition;
    }

    protected String getVolumeId() {
        return volumeId;
    }

    protected long getDiskSize() {
        return diskSize;
    }

    protected VirtualDiskParameters getDiskParameters() {
        return new VirtualDiskParameters();
    }

    protected FileSystemParameters getFileSystemParameters() {
        return new FileSystemParameters();
    }

    protected void doRun() throws IOException {
    }

    @Option(option = "a", argName = "adapterType", args = 1,
            description = "Some disk formats encode the disk type (IDE or SCSI) into the disk image, this parameter specifies the type of adaptor to encode.")
    GenericDiskAdapterType adapterType = GenericDiskAdapterType.Ide;

    @Option(option = "sz", argName = "diskSize", args = 1,
            description = "The size of the output disk.  Use B, KB, MB, GB to specify units (units default to bytes if not specified).")
    int diskSize;

    @Option(option = "ne", argName = "filenameEncoding", args = 1,
            description = "The encoding used for filenames in the file system (aka the codepage), e.g. UTF-8 or IBM437.  This is ignored for file systems have fixed/defined encodings.")
    String filenameEncoding;

    @Option(option = "p", argName = "partition", args = 1,
            description = "The number of the partition to inspect, in the range 0-n.  If not specified, 0 (the first partition) is the default.")
    int partition = -1;

    @Option(option = "i", argName = "volumeId", args = 1,
            description = "The volume id of the volume to access, use the volInfo tool to discover this id.  If specified, the partition parameter is ignored.")
    String volumeId;

    @Option(option = "u", argName = "userName", args = 1,
            description = "If using an iSCSI source or target, optionally use this parameter to specify the user name to authenticate with.  If this parameter is specified without a password, you will be prompted to supply the password.")
    String userName;

    @Option(option = "pw", argName = "password", args = 1,
            description = "If using an iSCSI source or target, optionally use this parameter to specify the password to authenticate with.")
    String password;

    @Option(option = "v", argName = "verbose", description = "Show detailed information.")
    boolean verbose;

    @Option(option = "q", argName = "quiet", description = "Run quietly.")
    boolean quiet;

    @Option(option = "time", argName = "time", description = "Times how long this program takes to execute.")
    boolean time;

    @Option(option = "of", argName = "outFormat", args = 1,
            description = "Mandatory - the type of disk to output, one of ..." + " or " + ".")
    String outFormat;

    protected void run(String[] args) throws IOException {
        if (!quiet) {
            displayHeader();
        }

        if (outFormat != null) {
            String[] typeAndVariant = outFormat.split("-");
            outputDiskType = typeAndVariant[0];
            outputDiskVariant = (typeAndVariant.length > 1) ? typeAndVariant[1] : "";
        }

        if (time) {
            long stopWatch = System.currentTimeMillis();
            doRun();
            System.err.println();
            System.err.println("Time taken: " + (System.currentTimeMillis() - stopWatch));

        } else {
            doRun();
        }
    }

    @Override
    public void handleException(Context<ProgramBase> context) {
        context.printHelp();
    }

    protected void displayHeader() {
        System.err.printf("%s v%s, available from http://discutils.codeplex.com\n", getClass().getPackage().getName(), "0.14.3");
        System.err.println("Copyright (c) Kenneth Bell, 2008-2013");
        System.err.println("Free software issued under the MIT License, see LICENSE.TXT for details.");
        System.err.println();
    }

    @Option(option = "x",
            argName = "fileOrUriParameter", args = 1,
            description = "This can be a file path or an iSCSI, NFS or ODS URL.  " +
                          "URLs for iSCSI LUNs are of the form: iscsi://192.168.1.2/iqn.2002-2004.example.com:port1?LUN=2.  " +
                          "Use the iSCSIBrowse utility to discover iSCSI URLs.  " +
                          "NFS URLs are of the form: nfs://host/a/path.vhd.  " +
                          "ODS URLs are of the form: ods://domain/host/volumename.")
    String fileOrUriParameter;

    @Option(option = "y",
            argName = "fileOrUriMultiParameter", args = 1,
            description = "This can be a file path or an iSCSI, NFS or ODS URL.  " +
                          "URLs for iSCSI LUNs are of the form: iscsi://192.168.1.2/iqn.2002-2004.example.com:port1?LUN=2.  " +
                          "Use the iSCSIBrowse utility to discover iSCSI URLs.  " +
                          "NFS URLs are of the form: nfs://host/a/path.vhd.  " +
                          "ODS URLs are of the form: ods://domain/host/volumename.")
    String fileOrUriMultiParameter;

    protected static void showProgress(String label,
                                       long totalBytes,
                                       long startTime,
                                       Object sourceObject,
                                       PumpProgressEventArgs e) {
        int progressLen = 55 - label.length();
        int numProgressChars = (int) ((e.getBytesRead() * progressLen) / totalBytes);
        String progressBar = new String(new char[numProgressChars]).replace("\0", "=") +
                             new String(new char[progressLen - numProgressChars]).replace("\0", " ");
        long now = System.currentTimeMillis();
        long timeSoFar = now - startTime;
        long remaining = (long) ((timeSoFar / (double) e.getBytesRead()) * (totalBytes - e.getBytesRead()));
        System.err.printf("\n%s (%03d%%)  |%s| %4$tT.%4$tL\n",
                          label,
                          (e.getBytesRead() * 100) / totalBytes,
                          progressBar,
                          remaining);
    }

    void outputFormatSwitch() {
        List<String> outputTypes = new ArrayList<>();
        for (String type : VirtualDiskManager.getSupportedDiskTypes()) {
            List<String> variants = Arrays.asList(VirtualDisk.getSupportedDiskVariants(type));
            if (variants.isEmpty()) {
                outputTypes.add(type.toUpperCase());
            } else {
                for (String variant : variants) {
                    outputTypes.add(type.toUpperCase() + "-" + variant.toLowerCase());
                }
            }
        }
        Collections.sort(outputTypes);
        System.err.println(String.join(", ", outputTypes));
    }
}
