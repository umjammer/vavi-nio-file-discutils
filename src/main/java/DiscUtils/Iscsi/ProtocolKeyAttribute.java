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

package DiscUtils.Iscsi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.google.api.client.util.FieldInfo;

import DiscUtils.Core.CoreCompat.ReflectionHelper;


public @interface ProtocolKeyAttribute {

    String getDefaultValue();

    boolean getLeadingConnectionOnly();

    String getName();

    KeyUsagePhase getPhase();

    KeySender getSender();

    KeyType getType();

    boolean getUsedForDiscovery();

    class Util {
        public static String getValueAsString(Object value, Class<?> valueType) {
            if (valueType == Boolean.TYPE) {
                return (Boolean) value ? "Yes" : "No";
            }

            if (valueType == String.class) {
                return (String) value;
            }

            if (valueType == int.class) {
                return ((Integer) value).toString();
            }

            if (ReflectionHelper.isEnum(valueType)) {
                Field[] infos = valueType.getFields();
                for (Field info : infos) {
                    if (info.isLiteral) {
                        Object literalValue = info.getValue(null);
                        if (literalValue.equals(value)) {
                            Annotation attr = ReflectionHelper.getCustomAttribute(info, ProtocolKeyValueAttribute.class);
                            return ((ProtocolKeyValueAttribute) attr).getName();
                        }
                    }
                }
                throw new UnsupportedOperationException();
            }

            throw new UnsupportedOperationException("Unknown property type: " + valueType);
        }

        public static Object getValueAsObject(String value, Class<?> valueType) {
            if (valueType == Boolean.TYPE) {
                return value.equals("Yes");
            }

            if (valueType == String.class) {
                return value;
            }

            if (valueType == int.class) {
                return Integer.parseInt(value);
            }

            if (ReflectionHelper.isEnum(valueType)) {
                Field[] infos = valueType.getFields();
                for (Field info : infos) {
                    if (info.isLiteral) {
                        Annotation attr = ReflectionHelper.getCustomAttribute(info, ProtocolKeyValueAttribute.class);
                        if (attr != null && ((ProtocolKeyValueAttribute) attr).getName().equals(value)) {
                            return info.getValue(null);
                        }
                    }
                }
                throw new UnsupportedOperationException();
            }

            throw new UnsupportedOperationException("Unknown property type: " + valueType);
        }

        public boolean shouldTransmit(Object currentValue, Class<?> valueType, KeyUsagePhase phase, boolean discoverySession) {
            return (getPhase().ordinal() & phase.ordinal()) != 0 && (discoverySession ? getUsedForDiscovery() : true) &&
                   currentValue != null && !getValueAsString(currentValue, valueType).equals(getDefaultValue()) &&
                   (getSender().ordinal() & KeySender.Initiator.ordinal()) != 0;
        }
    }
}
