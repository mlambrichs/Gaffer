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
package uk.gov.gchq.gaffer.integration;

import static org.junit.Assume.assumeTrue;

import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.commonutil.TestPropertyNames;
import uk.gov.gchq.gaffer.commonutil.TestTypes;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.function.simple.aggregate.Max;
import uk.gov.gchq.gaffer.function.simple.aggregate.StringConcat;
import uk.gov.gchq.gaffer.function.simple.aggregate.Sum;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.data.EdgeSeed;
import uk.gov.gchq.gaffer.operation.data.ElementSeed;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.serialisation.implementation.StringSerialiser;
import uk.gov.gchq.gaffer.serialisation.implementation.raw.CompactRawIntegerSerialiser;
import uk.gov.gchq.gaffer.serialisation.implementation.raw.CompactRawLongSerialiser;
import uk.gov.gchq.gaffer.store.StoreProperties;
import uk.gov.gchq.gaffer.store.StoreTrait;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaEdgeDefinition;
import uk.gov.gchq.gaffer.store.schema.SchemaEntityDefinition;
import uk.gov.gchq.gaffer.store.schema.TypeDefinition;
import uk.gov.gchq.gaffer.user.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Logic/config for setting up and running store integration tests.
 * All tests will be skipped if the storeProperties variable has not been set
 * prior to running the tests.
 */
public abstract class AbstractStoreIT {
    protected static final String USER_01 = "user01";

    // Identifier prefixes
    protected static final String SOURCE = "source";
    protected static final String DEST = "dest";
    protected static final String SOURCE_DIR = "sourceDir";
    protected static final String DEST_DIR = "destDir";
    protected static final String A = "A";
    protected static final String B = "B";
    protected static final String C = "C";
    protected static final String D = "D";
    protected static final String[] VERTEX_PREFIXES = new String[]{A, B, C, D};

    // Identifiers
    protected static final String SOURCE_1 = SOURCE + 1;
    protected static final String DEST_1 = DEST + 1;

    protected static final String SOURCE_2 = SOURCE + 2;
    protected static final String DEST_2 = DEST + 2;

    protected static final String SOURCE_3 = SOURCE + 3;
    protected static final String DEST_3 = DEST + 3;

    protected static final String SOURCE_DIR_0 = SOURCE_DIR + 0;

    protected static final String SOURCE_DIR_1 = SOURCE_DIR + 1;
    protected static final String DEST_DIR_1 = DEST_DIR + 1;

    protected static final String SOURCE_DIR_2 = SOURCE_DIR + 2;
    protected static final String DEST_DIR_2 = DEST_DIR + 2;

    protected static final String SOURCE_DIR_3 = SOURCE_DIR + 3;
    protected static final String DEST_DIR_3 = DEST_DIR + 3;

    protected static Graph graph;
    private static Schema storeSchema = new Schema();
    private static StoreProperties storeProperties;

    private final Map<EntitySeed, Entity> entities = createEntities();
    private final Map<EdgeSeed, Edge> edges = createEdges();

    @Rule
    public TestName name = new TestName();
    private static Map<? extends Class<? extends AbstractStoreIT>, String> skippedTests;


    public static void setStoreProperties(final StoreProperties storeProperties) {
        AbstractStoreIT.storeProperties = storeProperties;
    }

    public static StoreProperties getStoreProperties() {
        return storeProperties;
    }

    public static Schema getStoreSchema() {
        return storeSchema;
    }

    public static void setStoreSchema(final Schema storeSchema) {
        AbstractStoreIT.storeSchema = storeSchema;
    }

    public static void setSkipTests(final Map<? extends Class<? extends AbstractStoreIT>, String> skippedTests) {
        AbstractStoreIT.skippedTests = skippedTests;
    }

    /**
     * Setup the Parameterised Graph for each type of Store.
     * Excludes tests where the graph's Store doesn't implement the required StoreTraits.
     *
     * @throws Exception should never be thrown
     */
    @Before
    public void setup() throws Exception {
        assumeTrue("Skipping test as no store properties have been defined.", null != storeProperties);

        graph = new Graph.Builder()
                .storeProperties(storeProperties)
                .addSchema(createSchema())
                .addSchema(storeSchema)
                .build();

        final String originalMethodName = name.getMethodName().endsWith("]")
                ? name.getMethodName().substring(0, name.getMethodName().indexOf("["))
                : name.getMethodName();
        final Method testMethod = this.getClass().getMethod(originalMethodName);
        final Collection<StoreTrait> requiredTraits = new ArrayList<>();

        for (Annotation annotation : testMethod.getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(TraitRequirement.class)) {
                final TraitRequirement traitRequirement = (TraitRequirement) annotation;
                requiredTraits.addAll(Arrays.asList(traitRequirement.value()));
            }
        }

        for (StoreTrait requiredTrait : requiredTraits) {
            assumeTrue("Skipping test as the store does not implement all required traits.", graph.hasTrait(requiredTrait));
        }

        assumeTrue("Skipping test. Justification: " + skippedTests.get(getClass()), !skippedTests.containsKey(getClass()));
    }

    protected Schema createSchema() {
        return new Schema.Builder()
                .type(TestTypes.ID_STRING, new TypeDefinition.Builder()
                        .clazz(String.class)
                        .build())
                .type(TestTypes.DIRECTED_EITHER, new TypeDefinition.Builder()
                        .clazz(Boolean.class)
                        .build())
                .type(TestTypes.PROP_STRING, new TypeDefinition.Builder()
                        .clazz(String.class)
                        .aggregateFunction(new StringConcat())
                        .serialiser(new StringSerialiser())
                        .build())
                .type(TestTypes.PROP_INTEGER, new TypeDefinition.Builder()
                        .clazz(Integer.class)
                        .aggregateFunction(new Max())
                        .serialiser(new CompactRawIntegerSerialiser())
                        .build())
                .type(TestTypes.PROP_COUNT, new TypeDefinition.Builder()
                        .clazz(Long.class)
                        .aggregateFunction(new Sum())
                        .serialiser(new CompactRawLongSerialiser())
                        .build())
                .type(TestTypes.TIMESTAMP, new TypeDefinition.Builder()
                        .clazz(Long.class)
                        .aggregateFunction(new Max())
                        .build())
                .entity(TestGroups.ENTITY, new SchemaEntityDefinition.Builder()
                        .vertex(TestTypes.ID_STRING)
                        .property(TestPropertyNames.STRING, TestTypes.PROP_STRING)
                        .groupBy(TestPropertyNames.INT)
                        .build())
                .edge(TestGroups.EDGE, new SchemaEdgeDefinition.Builder()
                        .source(TestTypes.ID_STRING)
                        .destination(TestTypes.ID_STRING)
                        .directed(TestTypes.DIRECTED_EITHER)
                        .property(TestPropertyNames.INT, TestTypes.PROP_INTEGER)
                        .property(TestPropertyNames.COUNT, TestTypes.PROP_COUNT)
                        .groupBy(TestPropertyNames.INT)
                        .build())
                .vertexSerialiser(new StringSerialiser())
                .build();
    }

    @After
    public void tearDown() {
        graph = null;
    }

    public void addDefaultElements() throws OperationException {
        graph.execute(new AddElements.Builder()
                .elements((Iterable) getEntities().values())
                .build(), getUser());

        graph.execute(new AddElements.Builder()
                .elements((Iterable) getEdges().values())
                .build(), getUser());
    }

    public Map<EntitySeed, Entity> getEntities() {
        return entities;
    }

    public Map<EdgeSeed, Edge> getEdges() {
        return edges;
    }

    public Entity getEntity(final Object vertex) {
        return entities.get(new EntitySeed(vertex));
    }

    public Edge getEdge(final Object source, final Object dest, final boolean isDirected) {
        return edges.get(new EdgeSeed(source, dest, isDirected));
    }

    protected Map<EdgeSeed, Edge> createEdges() {
        final Map<EdgeSeed, Edge> edges = new HashMap<>();
        for (int i = 0; i <= 10; i++) {
            for (int j = 0; j < VERTEX_PREFIXES.length; j++) {
                final Edge edge = new Edge(TestGroups.EDGE, VERTEX_PREFIXES[0] + i, VERTEX_PREFIXES[j] + i, false);
                edge.putProperty(TestPropertyNames.INT, 1);
                edge.putProperty(TestPropertyNames.COUNT, 1L);
                addToMap(edge, edges);
            }

            final Edge firstEdge = new Edge(TestGroups.EDGE, SOURCE + i, DEST + i, false);
            firstEdge.putProperty(TestPropertyNames.INT, 1);
            firstEdge.putProperty(TestPropertyNames.COUNT, 1L);
            addToMap(firstEdge, edges);

            final Edge secondEdge = new Edge(TestGroups.EDGE, SOURCE_DIR + i, DEST_DIR + i, true);
            secondEdge.putProperty(TestPropertyNames.INT, 1);
            secondEdge.putProperty(TestPropertyNames.COUNT, 1L);
            addToMap(secondEdge, edges);
        }

        return edges;
    }

    protected Map<EntitySeed, Entity> createEntities() {
        final Map<EntitySeed, Entity> entities = new HashMap<>();
        for (int i = 0; i <= 10; i++) {
            for (int j = 0; j < VERTEX_PREFIXES.length; j++) {
                final Entity entity = new Entity(TestGroups.ENTITY, VERTEX_PREFIXES[j] + i);
                entity.putProperty(TestPropertyNames.STRING, "3");
                addToMap(entity, entities);
            }

            final Entity secondEntity = new Entity(TestGroups.ENTITY, SOURCE + i);
            secondEntity.putProperty(TestPropertyNames.STRING, "3");
            addToMap(secondEntity, entities);

            final Entity thirdEntity = new Entity(TestGroups.ENTITY, DEST + i);
            thirdEntity.putProperty(TestPropertyNames.STRING, "3");
            addToMap(thirdEntity, entities);

            final Entity fourthEntity = new Entity(TestGroups.ENTITY, SOURCE_DIR + i);
            fourthEntity.putProperty(TestPropertyNames.STRING, "3");
            addToMap(fourthEntity, entities);

            final Entity fifthEntity = new Entity(TestGroups.ENTITY, DEST_DIR + i);
            fifthEntity.putProperty(TestPropertyNames.STRING, "3");
            addToMap(fifthEntity, entities);
        }

        return entities;
    }

    protected void addToMap(final Edge element, final Map<EdgeSeed, Edge> edges) {
        edges.put(ElementSeed.createSeed(element), element);
    }

    protected void addToMap(final Entity element, final Map<EntitySeed, Entity> entities) {
        entities.put(ElementSeed.createSeed(element), element);
    }

    protected User getUser() {
        return new User(USER_01);
    }
}
