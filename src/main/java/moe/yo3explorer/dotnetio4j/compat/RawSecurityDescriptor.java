/**
 *
 */
package moe.yo3explorer.dotnetio4j.compat;

import java.security.Permission;

import moe.yo3explorer.dotnetio4j.AccessControlSections;

/**
 * ${type_name}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 ${id:date('YYYY/MM/dd')} umjammer initial version <br>
 */
public class RawSecurityDescriptor extends Permission {

    /**
     * @param name
     */
    public RawSecurityDescriptor(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param readBytes
     * @param i
     */
    public RawSecurityDescriptor(byte[] readBytes, int i) {
        super(null);
    }

    /* ${see_to_overridden} */
    @Override
    public boolean implies(Permission permission) {
        // TODO Auto-generated method stub
        return false;
    }

    /* ${see_to_overridden} */
    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    /* ${see_to_overridden} */
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* ${see_to_overridden} */
    @Override
    public String getActions() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param all
     * @return
     */
    public String getSddlForm(AccessControlSections all) {
        // TODO Auto-generated method stub
        return null;
    }

}
