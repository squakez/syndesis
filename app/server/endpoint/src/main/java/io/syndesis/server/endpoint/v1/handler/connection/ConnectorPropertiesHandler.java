/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.server.endpoint.v1.handler.connection;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;

import io.swagger.annotations.Api;
import io.syndesis.server.verifier.MetadataConfigurationProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(value = "properties")
public class ConnectorPropertiesHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorPropertiesHandler.class);
    private final MetadataConfigurationProperties config;

    public ConnectorPropertiesHandler(final MetadataConfigurationProperties config) {
        this.config = config;
    }

    /**
     * See {@link io.syndesis.server.endpoint.v1.handler.connection.ConnectionActionHandler#enrichWithMetadata(String,Map) ConnectionActionHandler}
     * for real implementation
     * @param connectorId
     * @return
     */
    //@POST TODO using GET to simplify prototype testing
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response enrichWithDynamicProperties(@PathParam("id") final String connectorId/*, final Map<String, Object> props*/) {
        Map<String, Object> props = null;
        LOGGER.info("Calling enrichWithDynamicProperties({})", props);
        // TODO replace properly with circuit breaker as in javadoc method link reference
        String metadataUrl = String.format("http://%s/api/v1/connectors/%s/properties", config.getService(), connectorId);
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(metadataUrl);
        try (Response response = target.request().post(Entity.entity(props, "application/json"));) {
            final Status status = Status.OK;
            return Response.status(status).entity(response.readEntity(String.class)).build();
        }
    }
}
