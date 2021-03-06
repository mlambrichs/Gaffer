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
package gaffer.operation.simple.spark;

import gaffer.operation.AbstractGetOperation;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

public class GetDataFrameOfElements extends AbstractGetOperation<Void, Dataset<Row>> {

    private SQLContext sqlContext;
    private String group;

    public GetDataFrameOfElements() { }

    public GetDataFrameOfElements(final SQLContext sqlContext,
                                  final String group) {
        this.sqlContext = sqlContext;
        this.group = group;
    }

    public void setSqlContext(final SQLContext sqlContext) {
        this.sqlContext = sqlContext;
    }

    public SQLContext getSqlContext() {
        return sqlContext;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public abstract static class BaseBuilder <CHILD_CLASS extends BaseBuilder<?>>
            extends AbstractGetOperation.BaseBuilder<GetDataFrameOfElements, Void, Dataset<Row>, CHILD_CLASS> {

        public BaseBuilder() {
            this(new GetDataFrameOfElements());
        }

        public BaseBuilder(final GetDataFrameOfElements op) {
            super(op);
        }

        public CHILD_CLASS sqlContext(final SQLContext sqlContext) {
            op.setSqlContext(sqlContext);
            return self();
        }

        public CHILD_CLASS group(final String group) {
            op.setGroup(group);
            return self();
        }
    }

    public static final class Builder extends BaseBuilder<Builder> {
        public Builder() {
        }

        public Builder(final GetDataFrameOfElements op) {
            super(op);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
