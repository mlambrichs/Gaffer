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

import uk.gov.gchq.gaffer.function.SimpleFilterFunction;
import uk.gov.gchq.gaffer.function.annotation.Inputs;
import java.util.Map;

/**
 * An <code>MapContains</code> is a {@link SimpleFilterFunction} that checks
 * whether a {@link Map} contains a provided key.
 */
@Inputs(Map.class)
public class MapContains extends SimpleFilterFunction<Map> {
    private String key;

    public MapContains() {
        // Required for serialisation
    }

    public MapContains(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public MapContains statelessClone() {
        return new MapContains(key);
    }

    @Override
    public boolean isValid(final Map input) {
        return input.containsKey(key);
    }
}
