
package DiscUtils.PowerShell.Conpat;

public class PSDriveInfo implements Comparable<PSDriveInfo> {
    public PSDriveInfo(String name, ProviderInfo provider, String root, String description, PSCredential credential) {
    }

    public PSDriveInfo(String name,
            ProviderInfo provider,
            String root,
            String description,
            PSCredential credential,
            boolean persist) {
    }

    public PSDriveInfo(String name,
            ProviderInfo provider,
            String root,
            String description,
            PSCredential credential,
            String displayRoot) {
    }

    protected PSDriveInfo(PSDriveInfo driveInfo) {
        
    }

    public getPSCredential Credential() {
        return null;
    }

    public String getCurrentLocation() {
    }

    public void setCurrentLocation(String v) {
    }

    public String getDescription() {
        return null;
    }

    public void setDescription(String v) {
        ;
    }

    public String getDisplayRoot() {
        return null;
    }

    public String getName() {
        return null;
    }

    public ProviderInfo getProvider() {
        return null;
    }

    public String getRoot() {
        return null;
    }

    public int compareTo(PSDriveInfo drive) {
        return 0;
    }

    public boolean equals(PSDriveInfo drive) {
        return false;
    }

    public static boolean operatorEqual(PSDriveInfo drive1, PSDriveInfo drive2) {
        return false;
    }

    public static boolean operatorNotEqual(PSDriveInfo drive1, PSDriveInfo drive2) {
        return false;
    }

    public static boolean operatorLT(PSDriveInfo drive1, PSDriveInfo drive2) {
        return false;
    }

    public static boolean operatorGT(PSDriveInfo drive1, PSDriveInfo drive2) {
        return false;
    }
}
