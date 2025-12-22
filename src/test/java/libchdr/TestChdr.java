/*
 * This file is part of jpcsp.
 *
 * Jpcsp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpcsp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */

package libchdr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Stream;
import libchdr.ChdHeader.ChdError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static libchdr.Cdrom.CD_FRAMES_PER_HUNK;
import static libchdr.Cdrom.CD_MAX_SECTOR_DATA;
import static libchdr.Cdrom.CD_MAX_SUBCODE_DATA;
import static libchdr.ChdHeader.CDROM_TRACK_METADATA2_TAG;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;


@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class TestChdr {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "chd")
    String chdFile = "src/test/resources/test.chd";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    String outputFileName = "tmp/out.iso";

    @Test
    @DisplayName("convert chd -> iso")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test() throws Exception {
        ChdError err;

        long start = System.currentTimeMillis();

        Files.delete(Path.of(outputFileName));
        Stream vFileOut = new FileStream(outputFileName, FileMode.Open, FileAccess.ReadWrite);
Debug.printf("out: %s", outputFileName);

        Chd chd = new Chd();
        Chd.ChdFile parent = null;

        Chd.ChdFile[] res = new Chd.ChdFile[1];
Debug.printf("in: %s", chdFile);
        err = chd.chd_open_file(chdFile, FileAccess.Read, parent, res);
        Chd.ChdFile chdFile = res[0];
Debug.printf("chdFile=%s, err=%s", chdFile, err);

        ChdHeader header = Chd.chd_get_header(chdFile);
        int numberFrames = header.totalhunks * CD_FRAMES_PER_HUNK;
        int frameSize = CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA;

        byte[] metadata = new byte[512];
        int[] resultLength = new int[1];
        err = chd.chd_get_metadata(chdFile, CDROM_TRACK_METADATA2_TAG, 0, metadata, metadata.length, resultLength, null, null);
        if (err == CHDERR_NONE) {
            int metadataLength = resultLength[0];
            String metadataString = new String(metadata, 0, metadataLength);
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

        int totalBytes = header.hunkbytes * header.totalhunks;
        byte[] buffer = new byte[header.hunkbytes];
        for (int i = 0, frameCount = 0; i < header.totalhunks; i++) {
Debug.printf(Level.FINER, "Reading hunk#%d/%d", i + 1, header.totalhunks);
            err = chd.chd_read(chdFile, i, buffer, 0);
            if (err != CHDERR_NONE) {
Debug.printf("chdRead hunknum=%d, err=%s", i, err);
            } else {
                for (int frame = 0; frame < CD_FRAMES_PER_HUNK && frameCount < numberFrames; frame++, frameCount++) {
                    vFileOut.write(buffer, frame * (CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA), frameSize);
                }
            }
        }
        Chd.chd_close(chdFile);
        vFileOut.close();

        long end = System.currentTimeMillis();
        double timeTaken = (end - start) / 1000.0;
Debug.printf("Read %d bytes in %f seconds", totalBytes, timeTaken);
    }
}
