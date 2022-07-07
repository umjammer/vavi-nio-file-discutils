/**
 * IContentCmdletProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/10/08 umjammer initial version <br>
 */

package DiscUtils.PowerShell.Conpat;

import DiscUtils.Core.CoreCompat.IContentReader;
import DiscUtils.Core.CoreCompat.IContentWriter;

public interface IContentCmdletProvider {

    void clearContent(String path);

    Object clearContentDynamicParameters(String path);

    IContentReader getContentReader(String path);

    Object getContentReaderDynamicParameters(String path);

    IContentWriter getContentWriter(String path);

    Object getContentWriterDynamicParameters(String path);
}
