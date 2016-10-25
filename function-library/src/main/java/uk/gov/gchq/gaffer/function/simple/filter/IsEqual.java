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
package uk.gov.gchq.gaffer.function.simple.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import uk.gov.gchq.gaffer.function.SimpleFilterFunction;
import uk.gov.gchq.gaffer.function.annotation.Inputs;

/**
 * An <code>IsEqual</code> is a {@link SimpleFilterFunction} that checks that the input object is
 * equal to a control value.
 */
@Inputs(Object.class)
public class IsEqual extends SimpleFilterFunction<Object> {
    private Object controlValue;

    public IsEqual() {
        // Required for serialisation
    }

    public IsEqual(final Object controlValue) {
        this.controlValue = controlValue;
    }

    @Override
    public IsEqual statelessClone() {
        return new IsEqual(controlValue);
    }

    @JsonProperty("value")
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_OBJECT)
    public Object getControlValue() {
        return controlValue;
    }

    public void setControlValue(final Object controlValue) {
        this.controlValue = controlValue;
    }

    @Override
    public boolean isValid(final Object input) {
        if (null == controlValue) {
            return null == input;
        }

        return controlValue.equals(input);
    }
}
