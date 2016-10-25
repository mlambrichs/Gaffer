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

import uk.gov.gchq.gaffer.data.element.ElementComponentKey;
import uk.gov.gchq.gaffer.data.element.IdentifierType;
import uk.gov.gchq.gaffer.data.element.function.ElementAggregator;
import uk.gov.gchq.gaffer.data.element.function.ElementFilter;
import uk.gov.gchq.gaffer.function.AggregateFunction;
import uk.gov.gchq.gaffer.function.ConsumerFunction;
import uk.gov.gchq.gaffer.function.ConsumerProducerFunction;
import uk.gov.gchq.gaffer.function.context.ConsumerFunctionContext;
import uk.gov.gchq.gaffer.function.context.ConsumerProducerFunctionContext;
import uk.gov.gchq.gaffer.function.context.PassThroughFunctionContext;
import uk.gov.gchq.gaffer.function.processor.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An <code>SchemaElementDefinitionValidator</code> validates a {@link SchemaElementDefinition}.
 * Checks all function input and output types are compatible with the
 * properties and identifiers provided.
 * To be able to aggregate 2 similar elements together ALL properties have to
 * be aggregated together. So this validator checks that either no properties have
 * aggregator functions or all properties have aggregator functions defined.
 */
public class SchemaElementDefinitionValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaElementDefinitionValidator.class);

    /**
     * Checks each identifier and property has a type associated with it.
     * Checks all {@link gaffer.function.FilterFunction}s and {@link gaffer.function.AggregateFunction}s defined are
     * compatible with the identifiers and properties - this is done by comparing the function input and output types with
     * the identifier and property types.
     *
     * @param elementDef the {@link gaffer.data.elementdefinition.ElementDefinition} to validate
     * @return true if the element definition is valid, otherwise false and an error is logged
     */
    public boolean validate(final SchemaElementDefinition elementDef) {
        final ElementFilter validator = elementDef.getValidator();
        final ElementAggregator aggregator = elementDef.getAggregator();
        return validateComponentTypes(elementDef)
                && validateAggregator(aggregator, elementDef)
                && validateFunctionArgumentTypes(validator, elementDef)
                && validateFunctionArgumentTypes(aggregator, elementDef);
    }

    protected boolean validateComponentTypes(final SchemaElementDefinition elementDef) {
        for (IdentifierType idType : elementDef.getIdentifiers()) {
            try {
                if (null == elementDef.getIdentifierClass(idType)) {
                    LOGGER.error("Class for " + idType + " could not be found.");
                    return false;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Class " + elementDef.getIdentifierTypeName(idType) + " for identifier " + idType + " could not be found");
                return false;
            }
        }

        for (String propertyName : elementDef.getProperties()) {
            try {
                if (null == elementDef.getPropertyClass(propertyName)) {
                    LOGGER.error("Class for " + propertyName + " could not be found.");
                    return false;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Class " + elementDef.getPropertyTypeName(propertyName) + " for property " + propertyName + " could not be found");
                return false;
            }
        }

        return true;
    }

    /**
     * Checks all function inputs are compatible with the property and identifier types specified.
     *
     * @param processor  the processor to validate against the element definition types
     * @param elementDef the element definition
     * @return boolean - true if function argument types are valid. Otherwise false and the reason is logged.
     */
    protected boolean validateFunctionArgumentTypes(
            final Processor<ElementComponentKey, ? extends ConsumerFunctionContext<ElementComponentKey, ? extends ConsumerFunction>> processor,
            final SchemaElementDefinition elementDef) {
        if (null != processor && null != processor.getFunctions()) {
            for (ConsumerFunctionContext<ElementComponentKey, ? extends ConsumerFunction> context : processor.getFunctions()) {
                if (null == context.getFunction()) {
                    LOGGER.error(processor.getClass().getSimpleName() + " contains a function context with a null function.");
                    return false;
                }

                if (!validateFunctionSelectionTypes(elementDef, context)) {
                    return false;
                }

                if (context instanceof ConsumerProducerFunctionContext
                        && !validateFunctionProjectionTypes(elementDef, (ConsumerProducerFunctionContext<ElementComponentKey, ? extends ConsumerFunction>) context)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean validateFunctionSelectionTypes(final SchemaElementDefinition elementDef,
                                                   final ConsumerFunctionContext<ElementComponentKey, ? extends ConsumerFunction> context) {
        final ConsumerFunction function = context.getFunction();
        final Class<?>[] inputTypes = function.getInputClasses();
        if (null == inputTypes || 0 == inputTypes.length) {
            LOGGER.error("Function " + function.getClass().getName()
                    + " is invalid. Input types have not been set.");
            return false;
        }

        if (inputTypes.length != context.getSelection().size()) {
            LOGGER.error("Input types for function " + function.getClass().getName()
                    + " are not equal to the selection property types.");
            return false;
        }

        int i = 0;
        for (ElementComponentKey key : context.getSelection()) {
            final Class<?> clazz = elementDef.getClass(key);
            if (null == clazz) {
                final String typeName;
                if (key.isId()) {
                    typeName = elementDef.getIdentifierTypeName(key.getIdentifierType());
                } else {
                    typeName = elementDef.getPropertyTypeName(key.getPropertyName());
                }

                if (null != typeName) {
                    LOGGER.error("No class type found for type definition " + typeName
                            + " used by " + key.getKey()
                            + ". Please ensure it is defined in the schema.");
                } else {
                    LOGGER.error("No type definition defined for " + key.getKey()
                            + ". Please ensure it is defined in the schema.");
                }

                return false;
            }
            if (!inputTypes[i].isAssignableFrom(clazz)) {
                LOGGER.error("Function " + function.getClass().getName()
                        + " is not compatible with selection types. Function input type "
                        + inputTypes[i].getName() + " is not assignable from selection type "
                        + clazz.getName());
                return false;
            }
            i++;
        }
        return true;
    }

    private boolean validateFunctionProjectionTypes(final SchemaElementDefinition elementDef,
                                                    final ConsumerProducerFunctionContext<ElementComponentKey, ? extends ConsumerFunction> consumerProducerContext) {
        final ConsumerProducerFunction function = consumerProducerContext.getFunction();
        final Class<?>[] outputTypes = function.getOutputClasses();
        if (null == outputTypes || 0 == outputTypes.length) {
            LOGGER.error("Function " + function.getClass().getName()
                    + " is invalid. Output types have not been set.");
            return false;
        }

        if (outputTypes.length != consumerProducerContext.getProjection().size()) {
            LOGGER.error("Output types for function " + function.getClass().getName()
                    + " are not equal to the projection property types.");
            return false;
        }

        int i = 0;
        for (ElementComponentKey key : consumerProducerContext.getProjection()) {
            final Class<?> clazz = elementDef.getClass(key);
            if (null == clazz || !outputTypes[i].isAssignableFrom(clazz)) {
                LOGGER.error("Function " + function.getClass().getName()
                        + " is not compatible with output types. Function output type "
                        + outputTypes[i].getName() + " is not assignable from projection type "
                        + (null != clazz ? clazz.getName() : "with a null class."));
                return false;
            }
            i++;
        }
        return true;
    }


    private boolean validateAggregator(final ElementAggregator aggregator, final SchemaElementDefinition elementDef) {
        if (null == aggregator || null == aggregator.getFunctions()) {
            // if aggregate functions are not defined then it is valid
            return true;
        }

        // if aggregate functions are defined then check all properties are aggregated
        final Set<String> aggregatedProperties = new HashSet<>();
        if (aggregator.getFunctions() != null) {
            for (PassThroughFunctionContext<ElementComponentKey, AggregateFunction> context : aggregator.getFunctions()) {
                List<ElementComponentKey> selection = context.getSelection();
                if (selection != null) {
                    for (ElementComponentKey key : selection) {
                        if (!key.isId()) {
                            aggregatedProperties.add(key.getPropertyName());
                        }
                    }
                }
            }
        }

        final Set<String> propertyNamesTmp = new HashSet<>(elementDef.getProperties());
        propertyNamesTmp.removeAll(aggregatedProperties);
        if (propertyNamesTmp.isEmpty()) {
            return true;
        }

        LOGGER.error("no aggregator found for properties '" + propertyNamesTmp.toString() + "' in the supplied schema. This framework requires that either all of the defined properties have a function associated with them, or none of them do.");
        return false;
    }
}
