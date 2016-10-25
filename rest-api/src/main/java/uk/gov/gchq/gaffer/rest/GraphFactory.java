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

package uk.gov.gchq.gaffer.rest;

import uk.gov.gchq.gaffer.data.elementdefinition.exception.SchemaException;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.graph.hook.OperationAuthoriser;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A <code>GraphFactory</code> creates instances of {@link gaffer.graph.Graph} to be reused for all queries.
 */
public class GraphFactory {
    private static Graph graph;

    /**
     * Set to true by default - so the same instance of {@link Graph} will be
     * returned.
     */
    private boolean singletonGraph = true;

    protected GraphFactory() {
        // Graph factories should be constructed via the createGraphFactory static method.
    }

    public static GraphFactory createGraphFactory() {
        final String graphFactoryClass = System.getProperty(SystemProperty.GRAPH_FACTORY_CLASS,
                SystemProperty.GRAPH_FACTORY_CLASS_DEFAULT);

        try {
            return Class.forName(graphFactoryClass)
                    .asSubclass(GraphFactory.class)
                    .newInstance();
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to create graph factory from class: " + graphFactoryClass);
        }
    }

    public Graph getGraph() {
        if (singletonGraph) {
            if (null == graph) {
                setGraph(createGraph());
            }
            return graph;
        }

        return createGraph();
    }

    public void setSingletonGraph(final boolean singletonGraph) {
        this.singletonGraph = singletonGraph;
    }

    public boolean isSingletonGraph() {
        return singletonGraph;
    }

    protected Graph.Builder createGraphBuilder() {
        final Path storePropertiesPath = Paths.get(System.getProperty(SystemProperty.STORE_PROPERTIES_PATH));
        if (null == storePropertiesPath) {
            throw new SchemaException("The path to the Store Properties was not found in system properties for key: " + SystemProperty.STORE_PROPERTIES_PATH);
        }

        final Graph.Builder builder = new Graph.Builder();
        builder.storeProperties(storePropertiesPath);
        for (Path path : getSchemaPaths()) {
            builder.addSchema(path);
        }

        final OperationAuthoriser opAuthoriser = createOpAuthoriser();
        if (null != opAuthoriser) {
            builder.addHook(opAuthoriser);
        }
        return builder;
    }

    protected OperationAuthoriser createOpAuthoriser() {
        OperationAuthoriser opAuthoriser = null;

        final String opAuthsPathStr = System.getProperty(SystemProperty.OP_AUTHS_PATH);
        if (null != opAuthsPathStr) {
            final Path opAuthsPath = Paths.get(System.getProperty(SystemProperty.OP_AUTHS_PATH));
            if (opAuthsPath.toFile().exists()) {
                opAuthoriser = new OperationAuthoriser(opAuthsPath);
            } else {
                throw new IllegalArgumentException("Could not find operation authorisation properties from path: " + opAuthsPathStr);
            }
        }

        return opAuthoriser;
    }

    protected static void setGraph(final Graph graph) {
        GraphFactory.graph = graph;
    }

    protected static Path[] getSchemaPaths() {
        final String schemaPaths = System.getProperty(SystemProperty.SCHEMA_PATHS);
        if (null == schemaPaths) {
            throw new SchemaException("The path to the schema was not found in system properties for key: " + SystemProperty.SCHEMA_PATHS);
        }

        final String[] schemaPathsArray = schemaPaths.split(",");
        final Path[] paths = new Path[schemaPathsArray.length];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = Paths.get(schemaPathsArray[i]);
        }

        return paths;
    }

    private Graph createGraph() {
        return createGraphBuilder().build();
    }
}
