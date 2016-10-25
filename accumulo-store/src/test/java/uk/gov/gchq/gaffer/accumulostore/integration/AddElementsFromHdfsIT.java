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

package uk.gov.gchq.gaffer.accumulostore.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.Lists;
import uk.gov.gchq.gaffer.accumulostore.AccumuloProperties;
import uk.gov.gchq.gaffer.accumulostore.AccumuloStore;
import uk.gov.gchq.gaffer.accumulostore.MockAccumuloStore;
import uk.gov.gchq.gaffer.accumulostore.key.AccumuloKeyPackage;
import uk.gov.gchq.gaffer.accumulostore.key.core.impl.byteEntity.ByteEntityKeyPackage;
import uk.gov.gchq.gaffer.accumulostore.key.core.impl.classic.ClassicKeyPackage;
import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.data.generator.OneToOneElementGenerator;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.operation.simple.hdfs.handler.job.initialiser.TextJobInitialiser;
import uk.gov.gchq.gaffer.operation.simple.hdfs.mapper.generator.TextMapperGenerator;
import uk.gov.gchq.gaffer.operation.simple.hdfs.operation.AddElementsFromHdfs;
import uk.gov.gchq.gaffer.store.StoreException;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.user.User;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;

public class AddElementsFromHdfsIT {
    private static final String VERTEX_ID_PREFIX = "vertexId";
    public static final int NUM_ENTITIES = 10;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    private String inputDir;
    public String outputDir;
    public String failureDir;

    @Before
    public void setup() {
        inputDir = testFolder.getRoot().getAbsolutePath() + "/inputDir";
        outputDir = testFolder.getRoot().getAbsolutePath() + "/outputDir";
        failureDir = testFolder.getRoot().getAbsolutePath() + "/failureDir";
    }

    @Test
    public void shouldAddElementsFromHdfs() throws Exception {
        addElementsFromHdfs(ByteEntityKeyPackage.class);
        addElementsFromHdfs(ClassicKeyPackage.class);
    }

    @Test
    public void shouldAddElementsFromHdfsWhenOutputDirectoryAlreadyExists() throws Exception {
        final FileSystem fs = FileSystem.getLocal(createLocalConf());
        fs.mkdirs(new Path(outputDir));

        addElementsFromHdfs(ByteEntityKeyPackage.class);
        addElementsFromHdfs(ClassicKeyPackage.class);
    }

    @Test
    public void shouldAddElementsFromHdfsWhenFailureDirectoryAlreadyExists() throws Exception {
        final FileSystem fs = FileSystem.getLocal(createLocalConf());
        fs.mkdirs(new Path(failureDir));

        addElementsFromHdfs(ByteEntityKeyPackage.class);
        addElementsFromHdfs(ClassicKeyPackage.class);
    }

    @Test
    public void shouldThrowExceptionWhenAddElementsFromHdfsWhenOutputDirectoryContainsFiles() throws Exception {
        final FileSystem fs = FileSystem.getLocal(createLocalConf());
        fs.mkdirs(new Path(outputDir));
        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(outputDir + "/someFile.txt"), true)))) {
            writer.write("Some content");
        }

        try {
            addElementsFromHdfs(ByteEntityKeyPackage.class);
            fail("Exception expected");
        } catch (final OperationException e) {
            assertEquals("Output directory exists and is not empty: " + outputDir, e.getCause().getMessage());
        }

        try {
            addElementsFromHdfs(ClassicKeyPackage.class);
            fail("Exception expected");
        } catch (final OperationException e) {
            assertEquals("Output directory exists and is not empty: " + outputDir, e.getCause().getMessage());
        }
    }

    @Test
    public void shouldThrowExceptionWhenAddElementsFromHdfsWhenFailureDirectoryContainsFiles() throws Exception {
        final FileSystem fs = FileSystem.getLocal(createLocalConf());
        fs.mkdirs(new Path(failureDir));
        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(failureDir + "/someFile.txt"), true)))) {
            writer.write("Some content");
        }

        try {
            addElementsFromHdfs(ByteEntityKeyPackage.class);
            fail("Exception expected");
        } catch (final OperationException e) {
            assertEquals("Failure directory is not empty: " + failureDir, e.getCause().getMessage());
        }

        try {
            addElementsFromHdfs(ClassicKeyPackage.class);
            fail("Exception expected");
        } catch (final OperationException e) {
            assertEquals("Failure directory is not empty: " + failureDir, e.getCause().getMessage());
        }
    }

    private void addElementsFromHdfs(Class<? extends AccumuloKeyPackage> keyPackageClass)
            throws Exception {
        // Given
        createInputFile();
        final Graph graph = createGraph(keyPackageClass);

        // When
        graph.execute(new AddElementsFromHdfs.Builder()
                .inputPaths(Collections.singletonList(inputDir))
                .outputPath(outputDir)
                .failurePath(failureDir)
                .mapperGenerator(TextMapperGeneratorImpl.class)
                .jobInitialiser(new TextJobInitialiser())
                .build(), new User());

        // Then
        final CloseableIterable<Element> elements = graph.execute(new GetAllElements<>(), new User());
        final List<Element> elementList = Lists.newArrayList(elements);
        assertEquals(NUM_ENTITIES, elementList.size());
        for (int i = 0; i < NUM_ENTITIES; i++) {
            assertEquals(TestGroups.ENTITY, elementList.get(i).getGroup());
            assertEquals(VERTEX_ID_PREFIX + i, ((Entity) elementList.get(i)).getVertex());
        }
    }

    private void createInputFile() throws IOException, StoreException {
        final Path inputPath = new Path(inputDir);
        final Path inputFilePath = new Path(inputDir + "/file.txt");
        final FileSystem fs = FileSystem.getLocal(createLocalConf());
        fs.mkdirs(inputPath);

        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(inputFilePath, true)))) {
            for (int i = 0; i < NUM_ENTITIES; i++) {
                writer.write(TestGroups.ENTITY + "," + VERTEX_ID_PREFIX + i + "\n");
            }
        }
    }

    private JobConf createLocalConf() throws StoreException {
        // Set up local conf
        final JobConf conf = new JobConf();
        conf.set("fs.defaultFS", "file:///");
        conf.set("mapreduce.jobtracker.address", "local");

        return conf;
    }

    private Graph createGraph(final Class<? extends AccumuloKeyPackage> keyPackageClass) throws StoreException {
        final Schema schema = Schema.fromJson(StreamUtil.schemas(getClass()));
        final AccumuloProperties properties = AccumuloProperties.loadStoreProperties(StreamUtil.storeProps(getClass()));
        properties.setKeyPackageClass(keyPackageClass.getName());
        properties.setInstanceName("instance_" + keyPackageClass.getName());

        final AccumuloStore store = new MockAccumuloStore();
        store.initialise(schema, properties);
        store.updateConfiguration(createLocalConf(), new View());

        return new Graph.Builder()
                .store(store)
                .build();
    }

    public static final class TextMapperGeneratorImpl extends TextMapperGenerator {
        public TextMapperGeneratorImpl() {
            super(new ExampleGenerator());
        }
    }

    public static final class ExampleGenerator extends OneToOneElementGenerator<String> {
        @Override
        public Element getElement(final String domainObject) {
            final String[] parts = domainObject.split(",");
            return new Entity(parts[0], parts[1]);
        }

        @Override
        public String getObject(final Element element) {
            return null;
        }
    }
}
