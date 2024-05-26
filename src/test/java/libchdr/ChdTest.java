/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package libchdr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dotnet4j.io.FileAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static libchdr.Cdrom.CD_FRAMES_PER_HUNK;
import static libchdr.Cdrom.CD_MAX_SECTOR_DATA;
import static libchdr.Cdrom.CD_MAX_SUBCODE_DATA;
import static libchdr.ChdHeader.CDROM_TRACK_METADATA2_TAG;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;


/**
 * ChdTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-17 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file://${user.dir}/local.properties")
class ChdTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "chd")
    String chdFile;

    @BeforeEach
    void before() throws IOException {
        PropsEntity.Util.bind(this);
    }

    @Test
    void test1() throws Exception {
        ChdHeader.ChdError err;

        Chd chd = new Chd();
        Chd.ChdFile parent = null;

        Chd.ChdFile[] res = new Chd.ChdFile[1];
        err = chd.chd_open_file(chdFile, FileAccess.Read, parent, res);
        Chd.ChdFile chdFile = res[0];
Debug.printf("chdFile=%s, err=%s", chdFile, err);

        ChdHeader header = chd.chd_get_header(chdFile);
        int numberFrames = header.totalhunks * CD_FRAMES_PER_HUNK;
        int frameSize = CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA;
Debug.printf("numberFrames=%d, frameSize=%d", numberFrames, frameSize);

        byte[] metadata = new byte[512];
        int[] resultLength = new int[1];
        err = chd.chd_get_metadata(chdFile, CDROM_TRACK_METADATA2_TAG, 0, metadata, metadata.length, resultLength, null, null);
        if (err == CHDERR_NONE) {
            int metadataLength = resultLength[0];
            String metadataString = new String(metadata, 0, metadataLength);
Debug.println("metadataString: " + metadataString);
            Pattern p = Pattern.compile("TRACK:(\\d+) TYPE:(.*) SUBTYPE:(.*) FRAMES:(\\d+) PREGAP:(\\d+)");
            Matcher m = p.matcher(metadataString);
            if (m.find()) {
                int track = Integer.parseInt(m.group(1));
                String type = m.group(2);
                String subtype = m.group(3);
                int frames = Integer.parseInt(m.group(4));
                int pregap = Integer.parseInt(m.group(5));
Debug.printf("Track %d, type %s, subtype %s, frames %d, pregap %d", track, type, subtype, frames, pregap);

                numberFrames = frames;
                if ("MODE1".equals(type)) {
                    frameSize = 2048;
                }
            } else {
Debug.println(metadataString);
            }
        }

//Debug.printf("header.hunkbytes=%d, header.totalhunks=%d", header.hunkbytes, header.totalhunks);
//        byte[] buffer = new byte[header.hunkbytes];
//        for (int i = 0, frameCount = 0; i < header.totalhunks; i++) {
//Debug.printf(Level.FINER, "Reading hunk#%d/%d", i + 1, header.totalhunks);
//            err = chd.chd_read(chdFile, i, buffer, 0);
//            if (err != CHDERR_NONE) {
//Debug.printf("chdRead hunknum=%d, err=%s", i, err);
//            }
//        }
        chd.chd_close(chdFile);
    }
}
