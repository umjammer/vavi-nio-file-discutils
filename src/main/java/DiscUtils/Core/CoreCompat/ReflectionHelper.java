
package DiscUtils.Core.CoreCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


public class ReflectionHelper {
    public static boolean isEnum(Class<?> type) {
        return type.isEnum();
    }

    public static <T extends Annotation> T getCustomAttribute(Method property, Class<T> attributeType) {
        return property.getAnnotation(attributeType);
    }

    public static <T extends Annotation> T getCustomAttribute(Method property, Class<T> attributeType, boolean inherit) {
        return property.getAnnotation(attributeType);
    }

    public static <T extends Annotation> T getCustomAttribute(Field field, Class<T> attributeType) {
        return field.getAnnotation(attributeType);
    }

    public static <T extends Annotation> T getCustomAttribute(Class<?> type, Class<T> attributeType) {
        return type.getAnnotation(attributeType);
    }

    public static <T extends Annotation> T getCustomAttribute(Class<?> type, Class<T> attributeType, boolean inherit) {
        return type.getAnnotation(attributeType);
    }

    public static <T extends Annotation> List<T> getCustomAttributes(Class<?> type, Class<T> attributeType, boolean inherit) {
        return Arrays.asList(type.getAnnotation(attributeType));
    }

    public static List<Class<?>> getAssembly(Class<?> type) {
        return Arrays.asList(type);
    }

    public static <T extends Serializable> int sizeOf(Class<T> c) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(c.newInstance());
            oos.flush();
            oos.close();
            return baos.size();
        } catch(InstantiationException | IllegalAccessException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
