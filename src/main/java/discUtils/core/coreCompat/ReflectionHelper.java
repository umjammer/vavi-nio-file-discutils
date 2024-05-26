
package discUtils.core.coreCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static java.lang.System.getLogger;


public class ReflectionHelper {

    private static final Logger logger = getLogger(ReflectionHelper.class.getName());

    public static <T extends Serializable> int sizeOf(Class<T> c) {
        try {
            if (Integer.class == c || Integer.TYPE == c) {
                return Integer.BYTES;
            } else if (Long.class == c || Long.TYPE == c) {
                return Long.BYTES;
            } else if (UUID.class == c) {
                return Long.BYTES * 2;
            } else {
logger.log(Level.DEBUG, c);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(c.getDeclaredConstructor().newInstance());
                oos.flush();
                oos.close();
                return baos.size();
            }
        } catch (InstantiationException | IllegalAccessException | IOException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
