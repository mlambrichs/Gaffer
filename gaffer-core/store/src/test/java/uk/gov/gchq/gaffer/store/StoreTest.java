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

package uk.gov.gchq.gaffer.store;

import static uk.gov.gchq.gaffer.store.StoreTrait.AGGREGATION;
import static uk.gov.gchq.gaffer.store.StoreTrait.PRE_AGGREGATION_FILTERING;
import static uk.gov.gchq.gaffer.store.StoreTrait.ORDERED;
import static uk.gov.gchq.gaffer.store.StoreTrait.TRANSFORMATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.commonutil.TestPropertyNames;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.element.IdentifierType;
import uk.gov.gchq.gaffer.data.element.LazyEntity;
import uk.gov.gchq.gaffer.data.elementdefinition.exception.SchemaException;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.Validatable;
import uk.gov.gchq.gaffer.operation.data.ElementSeed;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.CountGroups;
import uk.gov.gchq.gaffer.operation.impl.Deduplicate;
import uk.gov.gchq.gaffer.operation.impl.Validate;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.operation.impl.export.FetchExport;
import uk.gov.gchq.gaffer.operation.impl.export.FetchExporter;
import uk.gov.gchq.gaffer.operation.impl.export.FetchExporters;
import uk.gov.gchq.gaffer.operation.impl.export.UpdateExport;
import uk.gov.gchq.gaffer.operation.impl.export.initialise.InitialiseSetExport;
import uk.gov.gchq.gaffer.operation.impl.generate.GenerateElements;
import uk.gov.gchq.gaffer.operation.impl.generate.GenerateObjects;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentEntitySeeds;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllEdges;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllEntities;
import uk.gov.gchq.gaffer.operation.impl.get.GetEdgesBySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetElementsBySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetEntitiesBySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetRelatedElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetRelatedEntities;
import uk.gov.gchq.gaffer.serialisation.Serialisation;
import uk.gov.gchq.gaffer.serialisation.implementation.StringSerialiser;
import uk.gov.gchq.gaffer.store.operation.handler.CountGroupsHandler;
import uk.gov.gchq.gaffer.store.operation.handler.DeduplicateHandler;
import uk.gov.gchq.gaffer.store.operation.handler.OperationHandler;
import uk.gov.gchq.gaffer.store.operation.handler.export.FetchExportHandler;
import uk.gov.gchq.gaffer.store.operation.handler.export.FetchExporterHandler;
import uk.gov.gchq.gaffer.store.operation.handler.export.FetchExportersHandler;
import uk.gov.gchq.gaffer.store.operation.handler.export.InitialiseExportHandler;
import uk.gov.gchq.gaffer.store.operation.handler.export.UpdateExportHandler;
import uk.gov.gchq.gaffer.store.operation.handler.generate.GenerateElementsHandler;
import uk.gov.gchq.gaffer.store.operation.handler.generate.GenerateObjectsHandler;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaEdgeDefinition;
import uk.gov.gchq.gaffer.store.schema.SchemaEntityDefinition;
import uk.gov.gchq.gaffer.store.schema.SchemaOptimiser;
import uk.gov.gchq.gaffer.store.schema.TypeDefinition;
import uk.gov.gchq.gaffer.store.schema.ViewValidator;
import uk.gov.gchq.gaffer.user.User;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StoreTest {
    private final User user = new User();
    private final Context context = new Context(user);

    private OperationHandler<AddElements, Void> addElementsHandler;
    private OperationHandler<GetElements<ElementSeed, Element>, CloseableIterable<Element>> getElementsHandler;
    private OperationHandler<GetAllElements<Element>, CloseableIterable<Element>> getAllElementsHandler;
    private OperationHandler<GetAdjacentEntitySeeds, CloseableIterable<EntitySeed>> getAdjacentEntitySeedsHandler;
    private OperationHandler<Validatable<Integer>, Integer> validatableHandler;
    private OperationHandler<Validate, CloseableIterable<Element>> validateHandler;
    private Schema schema;
    private SchemaOptimiser schemaOptimiser;

    @Before
    public void setup() {
        schemaOptimiser = mock(SchemaOptimiser.class);

        addElementsHandler = mock(OperationHandler.class);
        getElementsHandler = mock(OperationHandler.class);
        getAllElementsHandler = mock(OperationHandler.class);
        getAdjacentEntitySeedsHandler = mock(OperationHandler.class);
        validatableHandler = mock(OperationHandler.class);
        validateHandler = mock(OperationHandler.class);

        schema = new Schema.Builder()
                .edge(TestGroups.EDGE, new SchemaEdgeDefinition.Builder()
                        .source("string")
                        .destination("string")
                        .directed(Boolean.class)
                        .property(TestPropertyNames.PROP_1, "string")
                        .property(TestPropertyNames.PROP_2, "string")
                        .build())
                .edge(TestGroups.EDGE_2, new SchemaEdgeDefinition.Builder()
                        .source("string")
                        .destination("string")
                        .directed(Boolean.class)
                        .property(TestPropertyNames.PROP_1, "string")
                        .property(TestPropertyNames.PROP_2, "string")
                        .build())
                .entity(TestGroups.ENTITY, new SchemaEntityDefinition.Builder()
                        .vertex("string")
                        .property(TestPropertyNames.PROP_1, "string")
                        .property(TestPropertyNames.PROP_2, "string")
                        .build())
                .entity(TestGroups.ENTITY_2, new SchemaEntityDefinition.Builder()
                        .vertex("string")
                        .property(TestPropertyNames.PROP_1, "string")
                        .property(TestPropertyNames.PROP_2, "string")
                        .build())
                .type("string", new TypeDefinition.Builder()
                        .clazz(String.class)
                        .serialiser(new StringSerialiser())
                        .build())
                .build();
    }

    @Test
    public void shouldThrowExceptionWhenPropertyIsNotSerialisable() throws StoreException {
        // Given
        final Schema mySchema = new Schema.Builder()
                .edge(TestGroups.EDGE, new SchemaEdgeDefinition.Builder()
                        .property(TestPropertyNames.PROP_1, "invalidType")
                        .build())
                .type("invalidType", new TypeDefinition.Builder()
                        .clazz(Object.class)
                        .serialiser(new StringSerialiser())
                        .build())
                .build();
        final StoreProperties properties = mock(StoreProperties.class);
        final StoreImpl store = new StoreImpl();

        // When
        try {
            store.initialise(mySchema, properties);
            fail();
        } catch (final SchemaException exception) {
            assertNotNull(exception.getMessage());
        }
    }

    @Test
    public void shouldCreateStoreWithValidSchemasAndRegisterOperations() throws StoreException {
        // Given
        final StoreProperties properties = mock(StoreProperties.class);
        final StoreImpl store = new StoreImpl();

        // When
        store.initialise(schema, properties);

        // Then
        assertNotNull(store.getOperationHandlerExposed(Validate.class));
        assertSame(addElementsHandler, store.getOperationHandlerExposed(AddElements.class));

        assertSame(getElementsHandler, store.getOperationHandlerExposed(GetElementsBySeed.class));
        assertSame(getElementsHandler, store.getOperationHandlerExposed(GetRelatedElements.class));
        assertSame(getElementsHandler, store.getOperationHandlerExposed(GetEntitiesBySeed.class));
        assertSame(getElementsHandler, store.getOperationHandlerExposed(GetRelatedEntities.class));
        assertSame(getElementsHandler, store.getOperationHandlerExposed(GetEdgesBySeed.class));
        assertSame(getElementsHandler, store.getOperationHandlerExposed(GetRelatedEntities.class));
        assertSame(getAllElementsHandler, store.getOperationHandlerExposed(GetAllElements.class));
        assertSame(getAllElementsHandler, store.getOperationHandlerExposed(GetAllEntities.class));
        assertSame(getAllElementsHandler, store.getOperationHandlerExposed(GetAllEdges.class));
        assertSame(getAdjacentEntitySeedsHandler, store.getOperationHandlerExposed(GetAdjacentEntitySeeds.class));

        assertTrue(store.getOperationHandlerExposed(GenerateElements.class) instanceof GenerateElementsHandler);
        assertTrue(store.getOperationHandlerExposed(GenerateObjects.class) instanceof GenerateObjectsHandler);

        assertTrue(store.getOperationHandlerExposed(CountGroups.class) instanceof CountGroupsHandler);
        assertTrue(store.getOperationHandlerExposed(Deduplicate.class) instanceof DeduplicateHandler);

        assertTrue(store.getOperationHandlerExposed(InitialiseSetExport.class) instanceof InitialiseExportHandler);
        assertTrue(store.getOperationHandlerExposed(UpdateExport.class) instanceof UpdateExportHandler);
        assertTrue(store.getOperationHandlerExposed(FetchExport.class) instanceof FetchExportHandler);
        assertTrue(store.getOperationHandlerExposed(FetchExporter.class) instanceof FetchExporterHandler);
        assertTrue(store.getOperationHandlerExposed(FetchExporters.class) instanceof FetchExportersHandler);

        assertEquals(1, store.getCreateOperationHandlersCallCount());
        assertSame(schema, store.getSchema());
        assertSame(properties, store.getProperties());
        verify(schemaOptimiser).optimise(schema, true);
    }

    @Test
    public void shouldDelegateDoOperationToOperationHandler() throws Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final AddElements addElements = new AddElements();
        final StoreImpl store = new StoreImpl();
        store.initialise(schema, properties);

        // When
        store.execute(addElements, user);

        // Then
        verify(addElementsHandler).doOperation(addElements, context, store);
    }

    @Test
    public void shouldThrowExceptionIfOperationViewIsInvalid() throws OperationException, StoreException {
        // Given
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final AddElements addElements = new AddElements();
        final View view = mock(View.class);
        final ViewValidator viewValidator = mock(ViewValidator.class);
        final StoreImpl store = new StoreImpl(viewValidator);

        addElements.setView(view);
        given(schema.validate()).willReturn(true);
        given(viewValidator.validate(view, schema, true)).willReturn(false);
        store.initialise(schema, properties);

        // When / Then
        try {
            store.execute(addElements, user);
            fail("Exception expected");
        } catch (final SchemaException e) {
            verify(viewValidator).validate(view, schema, true);
            assertTrue(e.getMessage().contains("View"));
        }
    }

    @Test
    public void shouldCallDoUnhandledOperationWhenDoOperationWithUnknownOperationClass() throws Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final Operation<String, String> operation = mock(Operation.class);
        final StoreImpl store = new StoreImpl();

        store.initialise(schema, properties);

        // When
        store.execute(operation, user);

        // Then
        assertEquals(1, store.getDoUnhandledOperationCalls().size());
        assertSame(operation, store.getDoUnhandledOperationCalls().get(0));
    }

    @Test
    public void shouldFullyLoadLazyElement() throws StoreException {
        // Given
        final StoreProperties properties = mock(StoreProperties.class);
        final LazyEntity lazyElement = mock(LazyEntity.class);
        final Entity entity = mock(Entity.class);
        final Store store = new StoreImpl();
        given(lazyElement.getGroup()).willReturn(TestGroups.ENTITY);
        given(lazyElement.getElement()).willReturn(entity);

        store.initialise(schema, properties);

        // When
        final Element result = store.populateElement(lazyElement);

        // Then
        assertSame(entity, result);
        verify(lazyElement).getGroup();
        verify(lazyElement).getProperty(TestPropertyNames.PROP_1);
        verify(lazyElement).getIdentifier(IdentifierType.VERTEX);
    }

    @Test
    public void shouldHandleMultiStepOperations() throws Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final StoreImpl store = new StoreImpl();
        final CloseableIterable<Element> getElementsResult = mock(CloseableIterable.class);

        final AddElements addElements1 = new AddElements();
        final GetElementsBySeed<ElementSeed, Element> getElementsBySeed = new GetElementsBySeed<>();
        final OperationChain<CloseableIterable<Element>> opChain = new OperationChain.Builder()
                .first(addElements1)
                .then(getElementsBySeed)
                .build();


        given(addElementsHandler.doOperation(addElements1, context, store)).willReturn(null);
        given(getElementsHandler.doOperation(getElementsBySeed, context, store)).willReturn(getElementsResult);

        store.initialise(schema, properties);

        // When
        final CloseableIterable<Element> result = store.execute(opChain, user);

        // Then
        assertSame(getElementsResult, result);
    }

    @Test
    public void shouldReturnAllSupportedOperations() throws Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final Validatable<Integer> validatable = mock(Validatable.class);
        final Map<String, String> options = mock(HashMap.class);

        final StoreImpl store = new StoreImpl();
        final int expectedNumberOfOperations = 26;

        given(validatable.isValidate()).willReturn(true);
        given(validatable.getOptions()).willReturn(options);

        given(validatableHandler.doOperation(validatable, context, store)).
                willReturn(expectedNumberOfOperations);

        store.initialise(schema, properties);

        // When
        final Set<Class<? extends Operation>> supportedOperations = store.getSupportedOperations();

        // Then
        assertNotNull(supportedOperations);

        assertEquals(expectedNumberOfOperations, supportedOperations.size());
    }

    @Test
    public void shouldReturnTrueWhenOperationSupported() throws Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final Validatable<Integer> validatable = mock(Validatable.class);
        final Map<String, String> options = mock(HashMap.class);

        final StoreImpl store = new StoreImpl();
        final int expectedNumberOfOperations = 15;

        given(validatable.isValidate()).willReturn(true);
        given(validatable.getOptions()).willReturn(options);
        given(validatableHandler.doOperation(validatable, context, store)).willReturn(expectedNumberOfOperations);
        store.initialise(schema, properties);

        // WHen
        final Set<Class<? extends Operation>> supportedOperations = store.getSupportedOperations();
        for (final Class<? extends Operation> operationClass : supportedOperations) {
            final boolean isOperationClassSupported = store.isSupported(operationClass);

            // Then
            assertTrue(isOperationClassSupported);
        }
    }

    @Test
    public void shouldReturnFalseWhenUnsupportedOperationRequested() throws
            Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final Validatable<Integer> validatable = mock(Validatable.class);
        final Map<String, String> options = mock(HashMap.class);

        final StoreImpl store = new StoreImpl();

        given(validatable.isValidate()).willReturn(true);
        given(validatable.getOptions()).willReturn(options);
        store.initialise(schema, properties);

        // When
        final boolean supported = store.isSupported(GetElements.class);

        // Then
        assertFalse(supported);
    }

    @Test
    public void shouldHandleNullOperationSupportRequest() throws Exception {
        // Given
        final Schema schema = createSchemaMock();
        final StoreProperties properties = mock(StoreProperties.class);
        final Validatable<Integer> validatable = mock(Validatable.class);
        final Map<String, String> options = mock(HashMap.class);

        final StoreImpl store = new StoreImpl();

        given(validatable.isValidate()).willReturn(true);
        given(validatable.getOptions()).willReturn(options);
        store.initialise(schema, properties);

        // When
        final boolean supported = store.isSupported(null);

        // Then
        assertFalse(supported);
    }

    private Schema createSchemaMock() {
        final Schema schema = mock(Schema.class);
        given(schema.validate()).willReturn(true);
        given(schema.getVertexSerialiser()).willReturn(mock(Serialisation.class));
        return schema;
    }

    private class StoreImpl extends Store {
        private final Set<StoreTrait> TRAITS = new HashSet<>(Arrays.asList(AGGREGATION, PRE_AGGREGATION_FILTERING, TRANSFORMATION, ORDERED));

        private int createOperationHandlersCallCount;
        private final ArrayList<Operation> doUnhandledOperationCalls = new ArrayList<>();
        private boolean validationRequired;

        public StoreImpl() {
            setSchemaOptimiser(schemaOptimiser);
        }

        public StoreImpl(final ViewValidator viewValidator) {
            this();
            setViewValidator(viewValidator);
        }

        @Override
        public Set<StoreTrait> getTraits() {
            return TRAITS;
        }

        public OperationHandler getOperationHandlerExposed(final Class<? extends Operation> opClass) {
            return super.getOperationHandler(opClass);
        }

        @Override
        protected void addAdditionalOperationHandlers() {
            createOperationHandlersCallCount++;
            addOperationHandler(mock(AddElements.class).getClass(), (OperationHandler) addElementsHandler);
            addOperationHandler(mock(GetElements.class).getClass(), (OperationHandler) getElementsHandler);
            addOperationHandler(mock(GetAdjacentEntitySeeds.class).getClass(), (OperationHandler) getElementsHandler);
            addOperationHandler(mock(Validatable.class).getClass(), (OperationHandler) validatableHandler);
            addOperationHandler(Validate.class, (OperationHandler) validateHandler);
        }

        @Override
        protected OperationHandler<GetElements<ElementSeed, Element>, CloseableIterable<Element>> getGetElementsHandler() {
            return getElementsHandler;
        }

        @Override
        protected OperationHandler<GetAllElements<Element>, CloseableIterable<Element>> getGetAllElementsHandler() {
            return getAllElementsHandler;
        }

        @Override
        protected OperationHandler<? extends GetAdjacentEntitySeeds, CloseableIterable<EntitySeed>> getAdjacentEntitySeedsHandler() {
            return getAdjacentEntitySeedsHandler;
        }

        @Override
        protected OperationHandler<? extends AddElements, Void> getAddElementsHandler() {
            return addElementsHandler;
        }

        @Override
        protected <OUTPUT> OUTPUT doUnhandledOperation(final Operation<?, OUTPUT> operation, final Context context) {
            doUnhandledOperationCalls.add(operation);
            return null;
        }

        public int getCreateOperationHandlersCallCount() {
            return createOperationHandlersCallCount;
        }

        public ArrayList<Operation> getDoUnhandledOperationCalls() {
            return doUnhandledOperationCalls;
        }

        @Override
        public boolean isValidationRequired() {
            return validationRequired;
        }

        public void setValidationRequired(final boolean validationRequired) {
            this.validationRequired = validationRequired;
        }

        @Override
        protected Context createContext(final User user) {
            return context;
        }
    }
}
