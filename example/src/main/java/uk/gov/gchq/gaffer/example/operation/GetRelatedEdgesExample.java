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
package uk.gov.gchq.gaffer.example.operation;

import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.function.ElementFilter;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.data.elementdefinition.view.ViewElementDefinition;
import uk.gov.gchq.gaffer.function.simple.filter.IsMoreThan;
import uk.gov.gchq.gaffer.operation.GetOperation.IncludeIncomingOutgoingType;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetRelatedEdges;


public class GetRelatedEdgesExample extends OperationExample {
    public static void main(final String[] args) {
        new GetRelatedEdgesExample().run();
    }

    public GetRelatedEdgesExample() {
        super(GetRelatedEdges.class);
    }

    public void runExamples() {
        getAllEdgesThatAreConnectedToVertex2();
        getAllOutboundEdgesThatAreConnectedToVertex2();
        getAllOutboundEdgesThatAreConnectedToVertex2WithCountGreaterThan1();
    }

    public CloseableIterable<Edge> getAllEdgesThatAreConnectedToVertex2() {
        // ---------------------------------------------------------
        final GetRelatedEdges<EntitySeed> operation = new GetRelatedEdges.Builder<EntitySeed>()
                .addSeed(new EntitySeed(2))
                .build();
        // ---------------------------------------------------------

        return runExample(operation);
    }

    public CloseableIterable<Edge> getAllOutboundEdgesThatAreConnectedToVertex2() {
        // ---------------------------------------------------------
        final GetRelatedEdges<EntitySeed> operation = new GetRelatedEdges.Builder<EntitySeed>()
                .addSeed(new EntitySeed(2))
                .inOutType(IncludeIncomingOutgoingType.OUTGOING)
                .build();
        // ---------------------------------------------------------

        return runExample(operation);
    }

    public CloseableIterable<Edge> getAllOutboundEdgesThatAreConnectedToVertex2WithCountGreaterThan1() {
        // ---------------------------------------------------------
        final GetRelatedEdges<EntitySeed> operation = new GetRelatedEdges.Builder<EntitySeed>()
                .addSeed(new EntitySeed(2))
                .inOutType(IncludeIncomingOutgoingType.OUTGOING)
                .view(new View.Builder()
                        .edge("edge", new ViewElementDefinition.Builder()
                                .preAggregationFilter(new ElementFilter.Builder()
                                        .select("count")
                                        .execute(new IsMoreThan(1))
                                        .build())
                                .build())
                        .build())
                .build();
        // ---------------------------------------------------------

        return runExample(operation);
    }

}
