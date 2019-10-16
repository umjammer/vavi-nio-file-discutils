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
public class RegistrySecurity extends Permission {

    /**
     */
    public RegistrySecurity() {
        super(null); // TODO
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
     * @return
     */
    public String getSecurityDescriptorBinaryForm() {
        // TODO Auto-generated method stub
        return "";
    }

    /**
     * @param secDesc
     */
    public void setSecurityDescriptorBinaryForm(String form) {
        // TODO Auto-generated method stub
        
    }

    /**
     */
    public String getSecurityDescriptorSddlForm(AccessControlSections sections) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     */
    public void setSecurityDescriptorSddlForm(String form, AccessControlSections sections) {
        // TODO Auto-generated method stub
        
    }
}
