package io.syndesis.connector.mongo;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.impl.JndiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;

import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyCustomizer;

public class MongoCustomizer implements ComponentProxyCustomizer, CamelContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoCustomizer.class);

    private CamelContext camelContext;

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public void customize(ComponentProxyComponent component, Map<String, Object> options) {
        if (!options.containsKey("connectionBean")) {
            // if (options.containsKey("user") &&
            // options.containsKey("password") && options.containsKey("url")) {
            try {
                LOGGER.debug("Creating and registering a client connection to localhost");
                MongoClient mongoClient = new MongoClient("localhost");
                /*
                 * consumeOption(camelContext, options, "user", String.class,
                 * mongoClient::setUsername); consumeOption(camelContext,
                 * options, "password", String.class, mongoClient::setPassword);
                 * consumeOption(camelContext, options, "url", String.class,
                 * mongoClient::setUrl);
                 */
                // TODO Probably we will have to make some change to the
                // upstream component
                options.put("connectionBean", "connectionBeanRef");
                camelContext.getRegistry(JndiRegistry.class).bind("connectionBeanRef", mongoClient);
            } catch (@SuppressWarnings("PMD.AvoidCatchingGenericException") Exception e) {
                throw new IllegalArgumentException(e);
            }
            /*
             * } else { LOGGER.
             * debug("Not enough information provided to set-up the Mongo client"
             * ); }
             */
        }
    }
}
