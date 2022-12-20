//
// Copyright (c) 2008-2011, Kenneth Bell
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
//

package discUtils.iscsi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;


@Target({
    ElementType.FIELD, ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtocolKeyAttribute {

    String defaultValue() default "";

    boolean leadingConnectionOnly() default false;

    String name();

    KeyUsagePhase phase();

    KeySender sender();

    KeyType type();

    boolean usedForDiscovery() default false;

    class Util {
        public static String getValueAsString(Object value, Class<?> valueType) {
            try {
                if (valueType == Boolean.TYPE || valueType == Boolean.class) {
                    return (Boolean) value ? "Yes" : "No";
                }

                if (valueType == String.class) {
                    return (String) value;
                }

                if (valueType == int.class || valueType == Integer.class) {
                    return ((Integer) value).toString();
                }

                if (valueType.isEnum()) {
                    Field[] infos = valueType.getFields();
                    for (Field info : infos) {
                        if (info.isEnumConstant()) {
                            Object literalValue = info.get(null);
                            if (literalValue.equals(value)) {
                                ProtocolKeyValueAttribute attr = info.getAnnotation(ProtocolKeyValueAttribute.class);
                                return attr.name();
                            }
                        }
                    }
                    throw new UnsupportedOperationException();
                }
                throw new UnsupportedOperationException("Unknown property type: " + valueType);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public static Object getValueAsObject(String value, Class<?> valueType) {
            try {
                if (valueType == Boolean.TYPE || valueType == Boolean.class) {
                    return value.equals("Yes");
                }

                if (valueType == String.class) {
                    return value;
                }

                if (valueType == int.class || valueType == Integer.class) {
                    return Integer.parseInt(value);
                }

                if (valueType.isEnum()) {
                    Field[] infos = valueType.getFields();
                    for (Field info : infos) {
                        if (info.isEnumConstant()) {
                            ProtocolKeyValueAttribute attr = info.getAnnotation(ProtocolKeyValueAttribute.class);
                            if (attr != null && attr.name().equals(value)) {
                                return info.get(null);
                            }
                        }
                    }
                    throw new UnsupportedOperationException();
                }
                throw new UnsupportedOperationException("Unknown property type: " + valueType);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public static boolean shouldTransmit(ProtocolKeyAttribute attr, Object currentValue, Class<?> valueType, KeyUsagePhase phase, boolean discoverySession) {
            return (attr.phase().ordinal() & phase.ordinal()) != 0 && (discoverySession ? attr.usedForDiscovery() : true) &&
                   currentValue != null && !getValueAsString(currentValue, valueType).equals(attr.defaultValue()) &&
                   (attr.sender().ordinal() & KeySender.Initiator.ordinal()) != 0;
        }
    }
}
