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

package uk.gov.gchq.gaffer.store.schema;

import uk.gov.gchq.gaffer.commonutil.iterable.ChainedIterable;
import uk.gov.gchq.gaffer.data.element.IdentifierType;
import uk.gov.gchq.gaffer.serialisation.Serialisation;
import uk.gov.gchq.gaffer.serialisation.implementation.JavaSerialiser;
import uk.gov.gchq.gaffer.serialisation.implementation.SerialisationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.Set;

public class SchemaOptimiser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOptimiser.class);

    private final SerialisationFactory serialisationFactory;

    public SchemaOptimiser() {
        this(new SerialisationFactory());
    }

    public SchemaOptimiser(final SerialisationFactory serialisationFactory) {
        this.serialisationFactory = serialisationFactory;
    }

    public void optimise(final Schema schema, final boolean isStoreOrdered) {
        removeUnusedTypes(schema);
        addDefaultSerialisers(schema, isStoreOrdered);
    }

    private void removeUnusedTypes(final Schema schema) {
        final Iterable<SchemaElementDefinition> schemaElements =
                new ChainedIterable<>(schema.getEntities().values(), schema.getEdges().values());

        final Set<String> usedTypeNames = new HashSet<>();
        for (SchemaElementDefinition elDef : schemaElements) {
            usedTypeNames.addAll(elDef.getIdentifierTypeNames());
            usedTypeNames.addAll(elDef.getPropertyTypeNames());
        }

        final TypeDefinitions types = schema.getTypes();
        if (null != types) {
            for (final String typeName : new HashSet<>(types.keySet())) {
                if (!usedTypeNames.contains(typeName)) {
                    types.remove(typeName);
                }
            }
        }
    }

    private void addDefaultSerialisers(final Schema schema, final boolean isStoreOrdered) {
        addDefaultPropertySerialisers(schema, isStoreOrdered);
        addDefaultVertexSerialiser(schema, isStoreOrdered);
    }

    private void addDefaultPropertySerialisers(final Schema schema, final boolean isStoreOrdered) {
        final Iterable<SchemaElementDefinition> schemaElements =
                new ChainedIterable<>(schema.getEntities().values(), schema.getEdges().values());

        // Separate type definitions into 2 sets: types that are used in 'group by' properties; other types.
        final Set<TypeDefinition> groupByTypeDefs = new HashSet<>();
        final Set<TypeDefinition> otherTypeDefs = new HashSet<>();
        for (SchemaElementDefinition elDef : schemaElements) {
            for (final String property : elDef.getProperties()) {
                final TypeDefinition typeDef = elDef.getPropertyTypeDef(property);
                if (elDef.getGroupBy().contains(property)) {
                    groupByTypeDefs.add(typeDef);
                } else {
                    otherTypeDefs.add(typeDef);
                }
            }
        }
        otherTypeDefs.removeAll(groupByTypeDefs);

        // Add the default serialisers for the types.
        // If the store is ordered then the group by type defs need to have
        // serialisers that preserves the ordering of bytes.
        for (final TypeDefinition typeDef : groupByTypeDefs) {
            if (null == typeDef.getSerialiser()) {
                typeDef.setSerialiser(serialisationFactory.getSerialiser(typeDef.getClazz(), isStoreOrdered));
            } else if (isStoreOrdered && !typeDef.getSerialiser().isByteOrderPreserved()) {
                LOGGER.warn(typeDef.getSerialiser().getClass().getName() + " serialiser is used for a 'group by' property in an ordered store and it does not preserve the order of bytes.");
            }

            if (typeDef.getSerialiser() instanceof JavaSerialiser) {
                LOGGER.warn("Java serialisation is not recommended for serialisation of 'group by' properties - it may cause aggregation to fail. Please implement your own serialiser or change the properties that are included in the grouped by.");
            }
        }
        for (final TypeDefinition typeDef : otherTypeDefs) {
            if (null == typeDef.getSerialiser()) {
                typeDef.setSerialiser(serialisationFactory.getSerialiser(typeDef.getClazz(), false));
            }
        }
    }

    private void addDefaultVertexSerialiser(final Schema schema, final boolean isStoreOrdered) {
        if (null == schema.getVertexSerialiser()) {
            final Set<Class<?>> vertexClasses = new HashSet<>();
            for (SchemaEntityDefinition definition : schema.getEntities().values()) {
                vertexClasses.add(definition.getIdentifierClass(IdentifierType.VERTEX));
            }
            for (SchemaEdgeDefinition definition : schema.getEdges().values()) {
                vertexClasses.add(definition.getIdentifierClass(IdentifierType.SOURCE));
                vertexClasses.add(definition.getIdentifierClass(IdentifierType.DESTINATION));
            }
            vertexClasses.remove(null);

            if (!vertexClasses.isEmpty()) {
                Serialisation serialiser = null;

                if (vertexClasses.size() == 1) {
                    serialiser = serialisationFactory.getSerialiser(vertexClasses.iterator().next(), isStoreOrdered);
                } else {
                    for (Class<?> clazz : vertexClasses) {
                        serialiser = serialisationFactory.getSerialiser(clazz, isStoreOrdered);
                        boolean canHandlerAll = true;
                        for (Class<?> clazz2 : vertexClasses) {
                            if (!serialiser.canHandle(clazz2)) {
                                canHandlerAll = false;
                                serialiser = null;
                                break;
                            }
                        }

                        if (canHandlerAll) {
                            break;
                        }
                    }
                }

                if (null == serialiser) {
                    throw new IllegalArgumentException("No default serialiser could be found that would support all vertex class types " + vertexClasses.toString() + ", please implement your own or change your vertex class types.");
                }

                if (serialiser instanceof JavaSerialiser) {
                    LOGGER.warn("Java serialisation is not recommended for vertex serialisation - it may cause aggregation to fail. Please implement your own or change your vertex class types.");
                }

                schema.setVertexSerialiser(serialiser);
            }
        }
    }
}
