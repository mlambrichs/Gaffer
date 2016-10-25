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

package uk.gov.gchq.gaffer.accumulostore.operation.impl;

import uk.gov.gchq.gaffer.accumulostore.utils.Pair;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.operation.AbstractGetOperation;
import uk.gov.gchq.gaffer.operation.GetOperation;
import uk.gov.gchq.gaffer.operation.data.ElementSeed;

/**
 * Returns all {@link gaffer.data.element.Edge}'s between the provided
 * {@link gaffer.operation.data.ElementSeed}s.
 */
public class GetEdgesInRanges<SEED_TYPE extends Pair<? extends ElementSeed>> extends GetElementsInRanges<SEED_TYPE, Edge> {

    public GetEdgesInRanges() {
    }

    public GetEdgesInRanges(final Iterable<SEED_TYPE> seeds) {
        super(seeds);
    }

    public GetEdgesInRanges(final View view) {
        super(view);
    }

    public GetEdgesInRanges(final View view, final Iterable<SEED_TYPE> seeds) {
        super(view, seeds);
    }

    public GetEdgesInRanges(final GetOperation<SEED_TYPE, Edge> operation) {
        super(operation);
    }

    @Override
    public boolean isIncludeEntities() {
        return false;
    }

    @Override
    public void setIncludeEntities(final boolean includeEntities) {
        if (includeEntities) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " does not support including entities");
        }
    }

    @Override
    public void setIncludeEdges(final IncludeEdgeType includeEdges) {
        if (IncludeEdgeType.NONE == includeEdges) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " requires edges to be included");
        }

        super.setIncludeEdges(includeEdges);
    }

    public abstract static class BaseBuilder<SEED_TYPE extends Pair<? extends ElementSeed>, CHILD_CLASS extends BaseBuilder<SEED_TYPE, ?>>
            extends AbstractGetOperation.BaseBuilder<GetEdgesInRanges<SEED_TYPE>, SEED_TYPE, CloseableIterable<Edge>, CHILD_CLASS> {

        public BaseBuilder() {
            super(new GetEdgesInRanges());
        }
    }

    public static final class Builder<SEED_TYPE extends Pair<? extends ElementSeed>>
            extends BaseBuilder<SEED_TYPE, Builder<SEED_TYPE>> {

        @Override
        protected Builder<SEED_TYPE> self() {
            return this;
        }
    }
}
