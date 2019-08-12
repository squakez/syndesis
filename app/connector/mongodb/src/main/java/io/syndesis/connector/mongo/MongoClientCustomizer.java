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
package io.syndesis.connector.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyCustomizer;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

public class MongoClientCustomizer implements ComponentProxyCustomizer, CamelContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientCustomizer.class);

    private String operation;
    private CamelContext camelContext;

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void customize(ComponentProxyComponent component, Map<String, Object> options) {
        // Validate operation parameter, set only once
        if (operation == null) {
            Object operation = options.get("operation");
            if (operation instanceof String) {
                this.operation = (String) operation;
            }
            component.setBeforeProducer(this::checkValidOperation);
        }
        // Set connection parameter
        if (!options.containsKey("mongoConnection")) {
            if (options.containsKey("user") && options.containsKey("password") && options.containsKey("host")) {
                try {
                    MongoConfiguration mongoConf = new MongoConfiguration();
                    consumeOption(camelContext, options, "host", String.class, mongoConf::setHost);
                    consumeOption(camelContext, options, "port", Integer.class, mongoConf::setPort);
                    consumeOption(camelContext, options, "user", String.class, mongoConf::setUser);
                    consumeOption(camelContext, options, "password", String.class, mongoConf::setPassword);
                    consumeOption(camelContext, options, "adminDB", String.class, mongoConf::setAdminDB);
                    LOGGER.debug("Creating and registering a client connection to {}", mongoConf);
                    MongoClientURI mongoClientURI = new MongoClientURI(mongoConf.getMongoClientURI());
                    MongoClient mongoClient = new MongoClient(mongoClientURI);
                    options.put("mongoConnection", mongoClient);
                    if (!options.containsKey("connectionBean")) {
                        //We safely put a default name instead of leaving null
                        options.put("connectionBean", String.format("%s-%s", mongoConf.getHost(), mongoConf.getUser()));
                    }
                } catch (@SuppressWarnings("PMD.AvoidCatchingGenericException") Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                LOGGER.warn(
                    "Not enough information provided to set-up the MongoDB client. Required at least host, user and password.");
            }
        }
    }

    // Validate the operation among a list of possible values
    @SuppressWarnings("PMD")
    private void checkValidOperation(Exchange exchange) {
        try {
            MongoProducerOperation.valueOf(operation);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Operation %s is not supported. Supported operations are %s", operation, Arrays.toString(MongoProducerOperation.values())));
        }
    }
}

