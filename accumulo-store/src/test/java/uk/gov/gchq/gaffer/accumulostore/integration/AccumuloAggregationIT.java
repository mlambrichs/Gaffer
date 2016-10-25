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
package uk.gov.gchq.gaffer.accumulostore.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import uk.gov.gchq.gaffer.accumulostore.utils.AccumuloPropertyNames;
import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.commonutil.TestTypes;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.data.elementdefinition.view.ViewElementDefinition;
import uk.gov.gchq.gaffer.function.simple.aggregate.StringConcat;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.graph.Graph.Builder;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetEntitiesBySeed;
import uk.gov.gchq.gaffer.serialisation.implementation.StringSerialiser;
import uk.gov.gchq.gaffer.store.StoreProperties;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaEntityDefinition;
import uk.gov.gchq.gaffer.store.schema.TypeDefinition;
import uk.gov.gchq.gaffer.user.User;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class AccumuloAggregationIT {
    private static final StoreProperties STORE_PROPERTIES = StoreProperties.loadStoreProperties(StreamUtil.storeProps(AccumuloStoreITs.class));
    private static final String VERTEX = "vertex";
    private static final String PUBLIC_VISIBILITY = "publicVisibility";
    private static final String PRIVATE_VISIBILITY = "privateVisibility";
    private static final User USER = new User.Builder()
            .dataAuth(PUBLIC_VISIBILITY)
            .dataAuth(PRIVATE_VISIBILITY)
            .build();

    @Test
    public void shouldOnlyAggregateVisibilityWhenGroupByIsNull() throws OperationException, UnsupportedEncodingException {
        final Graph graph = createGraph();
        final Entity entity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "value 3a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "value 4")
                .property(AccumuloPropertyNames.VISIBILITY, PUBLIC_VISIBILITY)
                .build();
        final Entity entity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "value 3a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "value 4")
                .property(AccumuloPropertyNames.VISIBILITY, PRIVATE_VISIBILITY)
                .build();
        final Entity entity3 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "value 3b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "value 4")
                .property(AccumuloPropertyNames.VISIBILITY, PRIVATE_VISIBILITY)
                .build();

        graph.execute(new AddElements(Arrays.asList((Element) entity1, entity2, entity3)), USER);

        // Given
        final GetEntitiesBySeed getElements = new GetEntitiesBySeed.Builder()
                .addSeed(new EntitySeed(VERTEX))
                .view(new View())
                .build();

        // When
        final List<Entity> results = Lists.newArrayList(graph.execute(getElements, USER));

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        final Entity expectedSummarisedEntity = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "value 3a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "value 4")
                .property(AccumuloPropertyNames.VISIBILITY, PRIVATE_VISIBILITY + "," + PUBLIC_VISIBILITY)
                .build();
        assertThat(results, IsCollectionContaining.hasItems(
                expectedSummarisedEntity, entity3
        ));
    }

    @Test
    public void shouldAggregateOverAllPropertiesExceptForGroupByProperties() throws OperationException, UnsupportedEncodingException {
        final Graph graph = createGraph();
        final Entity entity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "some value")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "some value 2")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "some value 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "some value 4")
                .property(AccumuloPropertyNames.VISIBILITY, PUBLIC_VISIBILITY)
                .build();
        final Entity entity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "some value")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "some value 2b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "some value 3b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "some value 4b")
                .property(AccumuloPropertyNames.VISIBILITY, PRIVATE_VISIBILITY)
                .build();
        final Entity entity3 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "some value c")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "some value 2c")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "some value 3c")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "some value 4c")
                .property(AccumuloPropertyNames.VISIBILITY, PRIVATE_VISIBILITY)
                .build();

        graph.execute(new AddElements(Arrays.asList((Element) entity1, entity2, entity3)), USER);

        // Given
        final GetEntitiesBySeed getElements = new GetEntitiesBySeed.Builder()
                .addSeed(new EntitySeed(VERTEX))
                .view(new View.Builder()
                        .entity(TestGroups.ENTITY, new ViewElementDefinition.Builder()
                                .groupBy(AccumuloPropertyNames.COLUMN_QUALIFIER)
                                .build())
                        .build())
                .build();

        // When
        final List<Entity> results = Lists.newArrayList(graph.execute(getElements, USER));

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());

        final Entity expectedEntity = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "some value")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "some value 2,some value 2b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "some value 3,some value 3b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "some value 4,some value 4b")
                .property(AccumuloPropertyNames.VISIBILITY, PUBLIC_VISIBILITY + "," + PRIVATE_VISIBILITY)
                .build();

        assertThat(results, IsCollectionContaining.hasItems(
                expectedEntity,
                entity3
        ));
    }

    @Test
    public void shouldHandleAggregatationWhenGroupByPropertiesAreNull() throws OperationException, UnsupportedEncodingException {
        final Graph graph = createGraph();
        final Entity entity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, null)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, null)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, null)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, null)
                .build();
        final Entity entity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();

        graph.execute(new AddElements(Arrays.asList((Element) entity1, entity2)), USER);

        // Given
        final GetEntitiesBySeed getElements = new GetEntitiesBySeed.Builder()
                .addSeed(new EntitySeed(VERTEX))
                .view(new View.Builder()
                        .entity(TestGroups.ENTITY, new ViewElementDefinition.Builder()
                                .groupBy()
                                .build())
                        .build())
                .build();

        // When
        final List<Entity> results = Lists.newArrayList(graph.execute(getElements, USER));

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        final Entity expectedEntity = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        assertEquals(expectedEntity, results.get(0));
    }

    @Test
    public void shouldHandleAggregatationWhenAllColumnQualifierPropertiesAreGroupByProperties() throws OperationException, UnsupportedEncodingException {
        final Graph graph = createGraph();
        final Entity entity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "test 4")
                .build();
        final Entity entity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "test 4")
                .build();

        graph.execute(new AddElements(Arrays.asList((Element) entity1, entity2)), USER);

        // Given
        final GetEntitiesBySeed getElements = new GetEntitiesBySeed.Builder()
                .addSeed(new EntitySeed(VERTEX))
                .view(new View.Builder()
                        .entity(TestGroups.ENTITY, new ViewElementDefinition.Builder()
                                .groupBy(AccumuloPropertyNames.COLUMN_QUALIFIER, AccumuloPropertyNames.COLUMN_QUALIFIER_2)
                                .build())
                        .build())
                .build();

        // When
        final List<Entity> results = Lists.newArrayList(graph.execute(getElements, USER));

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        final Entity expectedEntity = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "test 4")
                .build();
        assertEquals(expectedEntity, results.get(0));
    }

    @Test
    public void shouldHandleAggregatationWhenGroupByPropertiesAreNotSet() throws OperationException, UnsupportedEncodingException {
        final Graph graph = createGraph();
        final Entity entity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        final Entity entity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();

        graph.execute(new AddElements(Arrays.asList((Element) entity1, entity2)), USER);

        // Given
        final GetEntitiesBySeed getElements = new GetEntitiesBySeed.Builder()
                .addSeed(new EntitySeed(VERTEX))
                .view(new View.Builder()
                        .entity(TestGroups.ENTITY, new ViewElementDefinition.Builder()
                                .groupBy(AccumuloPropertyNames.COLUMN_QUALIFIER, AccumuloPropertyNames.COLUMN_QUALIFIER_2)
                                .build())
                        .build())
                .build();

        // When
        final List<Entity> results = Lists.newArrayList(graph.execute(getElements, USER));

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());

        final Entity expectedEntity = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        assertEquals(expectedEntity, results.get(0));
    }

    @Test
    public void shouldHandleAggregatationWithMultipleCombinations() throws OperationException, UnsupportedEncodingException {
        final Graph graph = createGraph();
        final Entity entity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        final Entity entity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, null)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        final Entity entity3 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test1a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        final Entity entity4 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test1b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();
        final Entity entity5 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test1a")
                .build();
        final Entity entity6 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test1b")
                .build();
        final Entity entity7 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "test2a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .build();

        graph.execute(new AddElements(
                Arrays.asList(
                        (Element) entity1,
                        entity2,
                        entity3,
                        entity4,
                        entity5,
                        entity6,
                        entity7
                )), USER);

        // Duplicate the entities to check they are aggregated properly
        graph.execute(new AddElements(
                Arrays.asList(
                        (Element) entity1,
                        entity2,
                        entity3,
                        entity4,
                        entity5,
                        entity6,
                        entity7
                )), USER);

        // Given
        final GetEntitiesBySeed getElements = new GetEntitiesBySeed.Builder()
                .addSeed(new EntitySeed(VERTEX))
                .view(new View.Builder()
                        .entity(TestGroups.ENTITY, new ViewElementDefinition.Builder()
                                .groupBy(AccumuloPropertyNames.COLUMN_QUALIFIER, AccumuloPropertyNames.COLUMN_QUALIFIER_2)
                                .build())
                        .build())
                .build();

        // When
        final List<Entity> results = Lists.newArrayList(graph.execute(getElements, USER));

        // Then
        assertNotNull(results);
        assertEquals(4, results.size());

        final Entity expectedEntity1 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();

        final Entity expectedEntity2 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test1a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();

        final Entity expectedEntity3 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "test1b")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "test 4")
                .build();

        final Entity expectedEntity4 = new Entity.Builder()
                .vertex(VERTEX)
                .group(TestGroups.ENTITY)
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "test2a")
                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "test 3")
                .build();

        assertThat(results, IsCollectionContaining.hasItems(
                expectedEntity1,
                expectedEntity2,
                expectedEntity3,
                expectedEntity4
        ));
    }

    protected Graph createGraph() {
        return new Builder()
                .storeProperties(STORE_PROPERTIES)
                .addSchema(new Schema.Builder()
                        .type(TestTypes.ID_STRING, new TypeDefinition.Builder()
                                .clazz(String.class)
                                .build())
                        .type("colQual", new TypeDefinition.Builder()
                                .clazz(String.class)
                                .aggregateFunction(new StringConcat())
                                .serialiser(new StringSerialiser())
                                .build())
                        .type("visibility", new TypeDefinition.Builder()
                                .clazz(String.class)
                                .aggregateFunction(new StringConcat())
                                .serialiser(new StringSerialiser())
                                .build())
                        .entity(TestGroups.ENTITY, new SchemaEntityDefinition.Builder()
                                .vertex(TestTypes.ID_STRING)
                                .property(AccumuloPropertyNames.COLUMN_QUALIFIER, "colQual")
                                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_2, "colQual")
                                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_3, "colQual")
                                .property(AccumuloPropertyNames.COLUMN_QUALIFIER_4, "colQual")
                                .property(AccumuloPropertyNames.VISIBILITY, "visibility")
                                .groupBy(AccumuloPropertyNames.COLUMN_QUALIFIER,
                                        AccumuloPropertyNames.COLUMN_QUALIFIER_2,
                                        AccumuloPropertyNames.COLUMN_QUALIFIER_3,
                                        AccumuloPropertyNames.COLUMN_QUALIFIER_4)
                                .build())
                        .visibilityProperty(AccumuloPropertyNames.VISIBILITY)
                        .build())
                .build();
    }
}
