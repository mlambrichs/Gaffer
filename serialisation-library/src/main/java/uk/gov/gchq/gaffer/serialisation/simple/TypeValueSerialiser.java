/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.gaffer.serialisation.simple;

import uk.gov.gchq.gaffer.commonutil.ByteArrayEscapeUtils;
import uk.gov.gchq.gaffer.commonutil.CommonConstants;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.serialisation.Serialisation;
import uk.gov.gchq.gaffer.types.simple.TypeValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class TypeValueSerialiser implements Serialisation {

    private static final long serialVersionUID = 8675867261911636738L;

    @Override
    public boolean canHandle(final Class clazz) {
        return TypeValue.class.equals(clazz);
    }

    @Override
    public byte[] serialise(final Object object) throws SerialisationException {
        TypeValue typeValue = (TypeValue) object;
        String type = typeValue.getType();
        String value = typeValue.getValue();
        if ((null == type || type.isEmpty()) && (null == value || value.isEmpty())) {
            throw new SerialisationException("TypeValue passed to serialiser is blank");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (type != null) {
            try {
                out.write(ByteArrayEscapeUtils.escape(type.getBytes(CommonConstants.UTF_8)));
            } catch (IOException e) {
                throw new SerialisationException("Failed to serialise the Type from TypeValue Object", e);
            }
        }
        out.write(ByteArrayEscapeUtils.DELIMITER);
        if (value != null) {
            try {
                out.write(ByteArrayEscapeUtils.escape(value.getBytes(CommonConstants.UTF_8)));
            } catch (IOException e) {
                throw new SerialisationException("Failed to serialise the Value from TypeValue Object", e);
            }
        }
        return out.toByteArray();
    }

    @Override
    public Object deserialise(final byte[] bytes) throws SerialisationException {
        int lastDelimiter = 0;
        TypeValue typeValue = new TypeValue();
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == ByteArrayEscapeUtils.DELIMITER) {
                if (i > 0) {
                    try {
                        typeValue.setType(new String(ByteArrayEscapeUtils.unEscape(Arrays.copyOfRange(bytes, lastDelimiter, i)), CommonConstants.UTF_8));
                    } catch (UnsupportedEncodingException e) {
                        throw new SerialisationException("Failed to deserialise the Type from TypeValue Object", e);
                    }
                }
                lastDelimiter = i + 1;
                break;
            }
        }
        if (bytes.length > lastDelimiter) {
            try {
                typeValue.setValue(new String(ByteArrayEscapeUtils.unEscape(Arrays.copyOfRange(bytes, lastDelimiter, bytes.length)), CommonConstants.UTF_8));
            } catch (UnsupportedEncodingException e) {
                throw new SerialisationException("Failed to deserialise the Value from TypeValue Object", e);
            }
        }
        return typeValue;
    }

    @Override
    public boolean isByteOrderPreserved() {
        return true;
    }
}
