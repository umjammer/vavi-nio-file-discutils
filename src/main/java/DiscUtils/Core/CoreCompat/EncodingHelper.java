
package DiscUtils.Core.CoreCompat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.klab.commons.csv.CsvColumn;
import org.klab.commons.csv.CsvEntity;

import vavi.util.StringUtil;


public class EncodingHelper {
    private static boolean _registered;

    static List<CodePage> codePages;

    static {
        try {
            codePages = CsvEntity.Util.read(CodePage.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void registerEncodings() {
        if (_registered)
            return;

        _registered = true;
    }

    @CsvEntity(url = "classpath:codepage.csv")
    public static class CodePage {
        @CsvColumn(sequence = 1)
        int codePage;

        @CsvColumn(sequence = 2)
        String identifierAndName;

        @CsvColumn(sequence = 3)
        boolean brDisp;

        @CsvColumn(sequence = 4)
        boolean brSave;

        @CsvColumn(sequence = 5)
        boolean mNDisp;

        @CsvColumn(sequence = 6)
        boolean mNSave;

        @CsvColumn(sequence = 7)
        boolean oneByte;

        @CsvColumn(sequence = 8)
        boolean readOnly;

        public String toString() {
            return StringUtil.paramString(this);
        }
    }

    // TODO check error
    public static boolean isSingleByte(String encoding) {
        return codePages.stream()
                .filter(cp -> cp.identifierAndName.toLowerCase().equals(encoding.toLowerCase()))
                .map(cp -> cp.oneByte)
                .findFirst()
                .get();
    }

    // TODO check error
    public static Charset forCodePage(int codePage) {
        return Charset.forName(codePages.stream()
                .filter(cp -> cp.codePage == codePage)
                .map(cp -> cp.identifierAndName)
                .findFirst()
                .get());
    }
}
