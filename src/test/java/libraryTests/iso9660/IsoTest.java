/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libraryTests.iso9660;

import java.nio.file.Files;
import java.nio.file.Paths;

import discUtils.core.DiscFileSystem;
import discUtils.iso9660.CDReader;
import discUtils.iso9660.VfsCDReader;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * IsoTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-23 nsano initial version <br>
 * @see discUtils.iso9660.CommonVolumeDescriptor
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class IsoTest {
    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @BeforeEach
    public void before() throws Exception {
        PropsEntity.Util.bind(this);
    }

    @Property(name = "iso")
    String image;

    /*
CD-ROM is in ISO 9660 format
System id: LINUX
Volume id: SONY-CELL-Linux-CL_20080201
Volume set id:
Publisher id:
Data preparer id:
Application id: MKISOFS ISO 9660/HFS FILESYSTEM BUILDER & CDRECORD CD-R/DVD CREATOR (C) 1993 E.YOUNGDALE (C) 1997 J.PEARSON/J.SCHILLING
Copyright File id:
Abstract File id:
Bibliographic File id:
Volume set size is: 1
Volume set sequence number is: 1
Logical block size is: 2048
Volume size is: 93360

Joliet with UCS level 3 found.
SUSP signatures version 1 found
Rock Ridge signatures version 1 found
Rock Ridge id 'RRIP_1991A'
$ /usr/local/bin/isoinfo -d -i /Volumes/GoogleDrive/My Drive/Downloads/games/PS3/CellSDK21.iso
CD-ROM is in ISO 9660 format
System id: LINUX
Volume id: Cell_SDK_2.1
Volume set id:
Publisher id:
Data preparer id:
Application id: MKISOFS ISO 9660/HFS FILESYSTEM BUILDER & CDRECORD CD-R/DVD CREATOR (C) 1993 E.YOUNGDALE (C) 1997 J.PEARSON/J.SCHILLING
Copyright File id: (c) 2006 IBM
Abstract File id:
Bibliographic File id:
Volume set size is: 1
Volume set sequence number is: 1
Logical block size is: 2048
Volume size is: 123668

Joliet with UCS level 3 found.
SUSP signatures version 1 found
Rock Ridge signatures version 1 found
Rock Ridge id 'RRIP_1991A'

     */
    @Test
    void test1() throws Exception {
        CDReader fs = new CDReader(new FileStream(image, FileMode.Open, FileAccess.Read), true) {
            {{
                VfsCDReader vfs = (VfsCDReader) (DiscFileSystem) getRealFileSystem();
            }}
        };
    }
}
