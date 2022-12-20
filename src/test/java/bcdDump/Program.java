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

package bcdDump;

import java.io.IOException;

import discUtils.bootConfig.BcdObject;
import discUtils.bootConfig.Element;
import discUtils.bootConfig.ObjectType;
import discUtils.bootConfig.Store;
import discUtils.common.ProgramBase;
import discUtils.registry.RegistryHive;
import dotnet4j.io.File;
import dotnet4j.io.Stream;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;


@Options
public class Program extends ProgramBase {

    @Option(option = "bcd_file", description = "Path to the BCD file to inspect.", args = 1, required = true)
    private String bcdFile;

    public static void main(String[] args) throws Exception {
        Program program = new Program();
        Options.Util.bind(args, program);
        program.run(args);
    }

    @Override
    protected void doRun() throws IOException {
        try (Stream fileStream = File.openRead(bcdFile);
             RegistryHive hive = new RegistryHive(fileStream)) {

            Store bcdDb = new Store(hive.getRoot());
            for (BcdObject obj : bcdDb.getObjects()) {
                System.err.println(obj.getFriendlyName() + ":");
                System.err.println("               Id: " + obj);
                System.err.println("             Type: " + obj.getObjectType());
                System.err.println("   App Image Type: " + obj.getApplicationImageType());
                System.err.println("         App Type: " + obj.getApplicationType());
                System.err.println("  App can inherit: " + obj.isInheritableBy(ObjectType.Application));
                System.err.println("  Dev can inherit: " + obj.isInheritableBy(ObjectType.Device));
                System.err.println("  ELEMENTS");
                for (Element elem : obj.getElements()) {
                    System.err.println("    " + elem.getFriendlyName() + ":");
                    System.err.println("          Id: " + elem);
                    System.err.println("       Class: " + elem.getClass());
                    System.err.println("      Format: " + elem.getFormat());
                    System.err.println("       Value: " + elem.getValue());
                }

                System.err.println();
            }
        }
    }
}
