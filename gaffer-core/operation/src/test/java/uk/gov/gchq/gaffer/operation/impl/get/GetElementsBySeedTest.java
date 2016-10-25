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

package uk.gov.gchq.gaffer.operation.impl.get;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.operation.GetOperation;
import uk.gov.gchq.gaffer.operation.OperationTest;
import uk.gov.gchq.gaffer.operation.data.EdgeSeed;
import uk.gov.gchq.gaffer.operation.data.ElementSeed;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class GetElementsBySeedTest implements OperationTest {
    private static final JSONSerialiser serialiser = new JSONSerialiser();

    @Test
    public void shouldSetSeedMatchingTypeToEquals() {
        // Given
        final ElementSeed elementSeed1 = new EntitySeed("identifier");

        // When
        final GetElementsBySeed op = new GetElementsBySeed(Collections.singletonList(elementSeed1));

        // Then
        assertEquals(GetOperation.SeedMatchingType.EQUAL, op.getSeedMatching());
    }

    @Test
    @Override
    public void shouldSerialiseAndDeserialiseOperation() throws SerialisationException {
        // Given
        final ElementSeed elementSeed1 = new EntitySeed("identifier");
        final ElementSeed elementSeed2 = new EdgeSeed("source2", "destination2", true);
        final GetElementsBySeed op = new GetElementsBySeed(Arrays.asList(elementSeed1, elementSeed2));

        // When
        byte[] json = serialiser.serialise(op, true);
        final GetElementsBySeed deserialisedOp = serialiser.deserialise(json, GetElementsBySeed.class);

        // Then
        final Iterator itr = deserialisedOp.getSeeds().iterator();
        assertEquals(elementSeed1, itr.next());
        assertEquals(elementSeed2, itr.next());
        assertFalse(itr.hasNext());
    }

    @Test
    @Override
    public void builderShouldCreatePopulatedOperation() {
        final GetElementsBySeed<EntitySeed, Element> getElementsBySeed = new GetElementsBySeed.Builder<EntitySeed, Element>().addSeed(new EntitySeed("A"))
                .includeEdges(GetOperation.IncludeEdgeType.ALL)
                .includeEntities(false)
                .inOutType(GetOperation.IncludeIncomingOutgoingType.BOTH)
                .option("testOption", "true")
                .populateProperties(false)
                .view(new View.Builder()
                        .edge("testEdgeGroup")
                        .build())
                .build();

        assertFalse(getElementsBySeed.isIncludeEntities());
        assertFalse(getElementsBySeed.isPopulateProperties());
        assertEquals(GetOperation.IncludeIncomingOutgoingType.BOTH, getElementsBySeed.getIncludeIncomingOutGoing());
        assertEquals(GetOperation.IncludeEdgeType.ALL, getElementsBySeed.getIncludeEdges());
        assertEquals("true", getElementsBySeed.getOption("testOption"));
        assertNotNull(getElementsBySeed.getView());
    }
}
