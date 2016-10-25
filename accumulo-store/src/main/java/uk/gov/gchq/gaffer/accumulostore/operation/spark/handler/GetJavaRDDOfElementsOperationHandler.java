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
package uk.gov.gchq.gaffer.accumulostore.operation.spark.handler;

import uk.gov.gchq.gaffer.accumulostore.AccumuloStore;
import uk.gov.gchq.gaffer.accumulostore.inputformat.ElementInputFormat;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.data.ElementSeed;
import uk.gov.gchq.gaffer.operation.simple.spark.GetJavaRDDOfElements;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import org.apache.accumulo.core.client.mapreduce.AccumuloInputFormat;
import org.apache.accumulo.core.client.mapreduce.lib.impl.InputConfigurator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

public class GetJavaRDDOfElementsOperationHandler<SEED_TYPE extends ElementSeed>
        extends AbstractGetRDDOperationHandler<JavaRDD<Element>, GetJavaRDDOfElements<SEED_TYPE>> {

    @Override
    public JavaRDD<Element> doOperation(final GetJavaRDDOfElements operation,
                                        final Context context,
                                        final Store store) throws OperationException {
        return doOperation(operation, context, (AccumuloStore) store);
    }

    private JavaRDD<Element> doOperation(final GetJavaRDDOfElements operation,
                                         final Context context,
                                         final AccumuloStore accumuloStore) throws OperationException {
        final JavaSparkContext sparkContext = operation.getJavaSparkContext();
        final Configuration conf = getConfiguration(operation);
        // Use batch scan option when performing seeded operation
        InputConfigurator.setBatchScan(AccumuloInputFormat.class, conf, true);
        addIterators(accumuloStore, conf, operation);
        addRanges(accumuloStore, conf, operation);
        final JavaPairRDD<Element, NullWritable> pairRDD = sparkContext.newAPIHadoopRDD(conf,
                ElementInputFormat.class,
                Element.class,
                NullWritable.class);
        final JavaRDD<Element> rdd = pairRDD.map(new FirstElement());
        return rdd;
    }

    static class FirstElement implements Function<Tuple2<Element, NullWritable>, Element> {

        private static final long serialVersionUID = -4695668644733530293L;

        public Element call(final Tuple2<Element, NullWritable> tuple) throws Exception {
            return tuple._1();
        }
    }
}
