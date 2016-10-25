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

package uk.gov.gchq.gaffer.rest.service;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.rest.GraphFactory;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaEdgeDefinition;
import uk.gov.gchq.gaffer.store.schema.SchemaEntityDefinition;
import uk.gov.gchq.gaffer.store.schema.ViewValidator;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;


public class ExamplesServiceTest {
    private static final JSONSerialiser serialiser = new JSONSerialiser();
    private SimpleExamplesService service;

    private Schema schema;

    @Before
    public void setup() {
        schema = new Schema.Builder()
                .entity(TestGroups.ENTITY, new SchemaEntityDefinition.Builder()
                        .property("entityProperties", String.class)
                        .vertex(String.class)
                        .build())
                .edge(TestGroups.EDGE, new SchemaEdgeDefinition.Builder()
                        .property("edgeProperties", String.class)
                        .source(String.class)
                        .destination(String.class)
                        .directed(Boolean.class)
                        .build())
                .build();

        final GraphFactory graphFactory = mock(GraphFactory.class);
        final Store store = mock(Store.class);
        given(store.getSchema()).willReturn(schema);
        final Graph graph = new Graph.Builder().store(store).build();
        given(graphFactory.getGraph()).willReturn(graph);

        service = new SimpleExamplesService(graphFactory);
    }

    @Test
    public void shouldSerialiseAndDeserialiseAddElements() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.addElements());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetElementsBySeed() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getElementsBySeed());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetRelatedElements() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getRelatedElements());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetEntitiesBySeed() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getEntitiesBySeed());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetRelatedEntities() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getRelatedEntities());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetEdgesBySeed() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getEdgesBySeed());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetRelatedEdges() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getRelatedEdges());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetAllElements() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getAllElements());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetAllEntities() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getAllEntities());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGetAllEdges() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.getAllEdges());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGenerateObjects() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.generateObjects());
    }

    @Test
    public void shouldSerialiseAndDeserialiseGenerateElements() throws IOException {
        shouldSerialiseAndDeserialiseOperation(service.generateElements());
    }

    @Test
    public void shouldSerialiseAndDeserialiseOperationChain() throws IOException {
        //Given
        final OperationChain opChain = service.execute();

        // When
        byte[] bytes = serialiser.serialise(opChain);
        final OperationChain deserialisedOp = serialiser.deserialise(bytes, opChain.getClass());

        // Then
        assertNotNull(deserialisedOp);
    }

    @Test
    public void shouldCreateViewForEdges() {
        final View.Builder builder = service.generateViewBuilder();
        final View view = builder.build();
        assertNotNull(view);

        final ViewValidator viewValidator = new ViewValidator();
        final boolean validate = viewValidator.validate(view, schema, false);
        assertTrue(validate);
    }

    private void shouldSerialiseAndDeserialiseOperation(Operation operation) throws IOException {
        //Given

        // When
        byte[] bytes = serialiser.serialise(operation);
        final Operation deserialisedOp = serialiser.deserialise(bytes, operation.getClass());

        // Then
        assertNotNull(deserialisedOp);
    }
}
