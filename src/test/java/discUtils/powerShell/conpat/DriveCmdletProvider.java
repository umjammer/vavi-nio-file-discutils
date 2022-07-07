
package discUtils.powerShell.conpat;

import java.util.Collection;

public abstract class DriveCmdletProvider extends CmdletProvider {

    protected DriveCmdletProvider() {
    }

    protected abstract Collection<PSDriveInfo> InitializeDefaultDrives();

    protected abstract PSDriveInfo NewDrive(PSDriveInfo drive);

    protected abstract Object NewDriveDynamicParameters();

    protected abstract PSDriveInfo RemoveDrive(PSDriveInfo drive);
}
