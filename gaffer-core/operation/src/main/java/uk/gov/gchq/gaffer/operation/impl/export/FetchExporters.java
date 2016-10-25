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

package uk.gov.gchq.gaffer.operation.impl.export;

import uk.gov.gchq.gaffer.export.Exporter;
import uk.gov.gchq.gaffer.operation.VoidInput;
import java.util.Map;

/**
 * A <code>FetchExporters</code> fetches all {@link Exporter}s containing the export
 * information.
 *
 * @see UpdateExport
 * @see FetchExporter
 */
public class FetchExporters extends ExportOperation<Void, Map<String, Exporter>>
        implements VoidInput<Map<String, Exporter>> {
    public abstract static class BaseBuilder<CHILD_CLASS extends BaseBuilder<?>>
            extends ExportOperation.BaseBuilder<FetchExporters, Void, Map<String, Exporter>, CHILD_CLASS> {
        public BaseBuilder() {
            super(new FetchExporters());
        }
    }

    public static final class Builder extends BaseBuilder<Builder> {
        @Override
        protected Builder self() {
            return this;
        }
    }
}
