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

package uk.gov.gchq.gaffer.rest.service;

import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.data.ElementSeed;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.operation.impl.generate.GenerateElements;
import uk.gov.gchq.gaffer.operation.impl.generate.GenerateObjects;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentEntitySeeds;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllEdges;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllEntities;
import uk.gov.gchq.gaffer.operation.impl.get.GetEdgesBySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetElementsBySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetEntitiesBySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetRelatedEdges;
import uk.gov.gchq.gaffer.operation.impl.get.GetRelatedElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetRelatedEntities;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * An <code>IOperationService</code> has methods to execute {@link gaffer.operation.Operation}s on the
 * {@link gaffer.graph.Graph}.
 */
@Path("/graph/doOperation")
@Api(value = "/graph/doOperation", description = "Allows operations to be executed on the graph. See <a href='https://github.com/gchq/Gaffer/wiki/operation-examples' target='_blank'>Wiki</a>.")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface IOperationService {

    @POST
    @ApiOperation(value = "Performs the given operation chain on the graph", response = Object.class)
    Object execute(final OperationChain opChain);

    @POST
    @Path("/generate/objects")
    @ApiOperation(value = "Generate objects from elements", response = Object.class, responseContainer = "List")
    CloseableIterable<Object> generateObjects(final GenerateObjects<Element, Object> operation);

    @POST
    @Path("/generate/elements")
    @ApiOperation(value = "Generate elements from objects", response = Element.class, responseContainer = "List")
    CloseableIterable<Element> generateElements(final GenerateElements operation);

    @POST
    @Path("/get/elements/bySeed")
    @ApiOperation(value = "Gets elements by seed from the graph",
            response = Element.class, responseContainer = "List")
    CloseableIterable<Element> getElementsBySeed(final GetElementsBySeed<ElementSeed, Element> operation);

    @POST
    @Path("/get/elements/related")
    @ApiOperation(value = "Gets related elements from the graph", response = Element.class, responseContainer = "List")
    CloseableIterable<Element> getRelatedElements(final GetRelatedElements<ElementSeed, Element> operation);

    @POST
    @Path("/get/entities/bySeed")
    @ApiOperation(value = "Gets entities by seed from the graph", response = Entity.class, responseContainer = "List")
    CloseableIterable<Entity> getEntitiesBySeed(final GetEntitiesBySeed operation);

    @POST
    @Path("/get/entities/related")
    @ApiOperation(value = "Gets related entities from the graph", response = Entity.class, responseContainer = "List")
    CloseableIterable<Entity> getRelatedEntities(final GetRelatedEntities<ElementSeed> operation);

    @POST
    @Path("/get/edges/bySeed")
    @ApiOperation(value = "Gets edge by seed from the graph", response = Edge.class, responseContainer = "List")
    CloseableIterable<Edge> getEdgesBySeed(final GetEdgesBySeed operation);

    @POST
    @Path("/get/edges/related")
    @ApiOperation(value = "Gets related edges from the graph", response = Edge.class, responseContainer = "List")
    CloseableIterable<Edge> getRelatedEdges(final GetRelatedEdges<ElementSeed> operation);

    @POST
    @Path("/get/entitySeeds/adjacent")
    @ApiOperation(value = "Gets adjacent entity seeds", response = EntitySeed.class, responseContainer = "List")
    CloseableIterable<EntitySeed> getAdjacentEntitySeeds(final GetAdjacentEntitySeeds operation);

    @POST
    @Path("/get/elements/all")
    @ApiOperation(value = "Gets all elements", response = Element.class, responseContainer = "List")
    CloseableIterable<Element> getAllElements(final GetAllElements<Element> operation);

    @POST
    @Path("/get/entities/all")
    @ApiOperation(value = "Gets all entities", response = Entity.class, responseContainer = "List")
    CloseableIterable<Entity> getAllEntities(final GetAllEntities operation);

    @POST
    @Path("/get/edges/all")
    @ApiOperation(value = "Gets all edges", response = Edge.class, responseContainer = "List")
    CloseableIterable<Edge> getAllEdges(final GetAllEdges operation);

    @PUT
    @Path("/add/elements")
    @ApiOperation(value = "Add elements to the graph", response = Boolean.class)
    void addElements(final AddElements operation);
}
