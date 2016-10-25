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

package uk.gov.gchq.gaffer.function;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public abstract class AggregateFunctionTest extends ConsumerProducerFunctionTest {
    @Test
    public void shouldReturnNullStateIfNoInputsGiven() {
        // Given
        final AggregateFunction function = getInstance();
        function.init();

        // When
        final Object[] state = function.state();

        // Then
        for (Object item : state) {
            assertNull(item);
        }
    }

    @Test
    public void shouldReturnNullStateForNullInputs() {
        // Given
        final AggregateFunction function = getInstance();
        function.init();

        // When - aggregate with null inputs
        final Object[] input = new Object[function.getInputClasses().length];
        function.aggregate(input);

        // When - call state
        final Object[] state = function.state();

        // Then
        for (Object item : state) {
            assertNull(item);
        }
    }

    @Override
    protected abstract AggregateFunction getInstance();
}
