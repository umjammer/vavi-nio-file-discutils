
package discUtils.core.coreCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import vavi.util.Debug;


public class ReflectionHelper {
    @Deprecated
    public static boolean isEnum(Class<?> type) {
        return type.isEnum();
    }

    @Deprecated
    public static <T extends Annotation> T getCustomAttribute(Method property, Class<T> attributeType) {
        return property.getAnnotation(attributeType);
    }

    @Deprecated
    public static <T extends Annotation> T getCustomAttribute(Method property, Class<T> attributeType, boolean inherit) {
        return property.getAnnotation(attributeType);
    }

    @Deprecated
    public static <T extends Annotation> T getCustomAttribute(Field field, Class<T> attributeType) {
        return field.getAnnotation(attributeType);
    }

    @Deprecated
    public static <T extends Annotation> T getCustomAttribute(Class<?> type, Class<T> attributeType) {
        return type.getAnnotation(attributeType);
    }

    @Deprecated
    public static <T extends Annotation> T getCustomAttribute(Class<?> type, Class<T> attributeType, boolean inherit) {
        return type.getAnnotation(attributeType);
    }

    @Deprecated
    public static <T extends Annotation> List<T> getCustomAttributes(Class<?> type, Class<T> attributeType, boolean inherit) {
        return Arrays.asList(type.getAnnotationsByType(attributeType));
    }

    @Deprecated
    public static List<Class<?>> getAssembly(Class<?> type) {
        return Collections.singletonList(type);
    }

    public static <T extends Serializable> int sizeOf(Class<T> c) {
        try {
            if (Integer.class == c || Integer.TYPE == c) {
                return Integer.BYTES;
            } else if (Long.class == c || Long.TYPE == c) {
                return Long.BYTES;
            } else if (UUID.class == c) {
                return Long.BYTES * 2;
            } else {
Debug.println(c);
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
