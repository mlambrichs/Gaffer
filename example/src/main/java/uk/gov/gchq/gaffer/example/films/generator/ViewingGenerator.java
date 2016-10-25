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

package uk.gov.gchq.gaffer.example.films.generator;

import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.generator.OneToOneElementGenerator;
import uk.gov.gchq.gaffer.example.films.data.Viewing;
import uk.gov.gchq.gaffer.example.films.data.schema.Group;
import uk.gov.gchq.gaffer.example.films.data.schema.Property;

public class ViewingGenerator extends OneToOneElementGenerator<Viewing> {
    @Override
    public Element getElement(final Viewing viewing) {
        final Edge edge = new Edge(Group.VIEWING, viewing.getUserId(), viewing.getFilmId(), true);
        edge.putProperty(Property.START_TIME, viewing.getStartTime());
        edge.putProperty(Property.COUNT, 1);

        return edge;
    }

    @Override
    public Viewing getObject(final Element element) {
        if (Group.VIEWING.equals(element.getGroup()) && element instanceof Edge) {
            return new Viewing(((Edge) element).getDestination().toString(),
                    ((Edge) element).getSource().toString(),
                    (Long) element.getProperty(Property.START_TIME));
        }

        throw new UnsupportedOperationException("Cannot generate Viewing object from " + element);
    }
}
