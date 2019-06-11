package io.syndesis.connector.mongo;

import java.util.Map;

import org.apache.camel.CamelContextAware;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.JndiRegistry;

import com.mongodb.MongoClient;

import io.syndesis.integration.component.proxy.ComponentProxyComponent;

class MongoConnector extends ComponentProxyComponent implements CamelContextAware {

    private static String DEFAULT_CONNECTION_BEAN = "connectionBean";

    MongoConnector(String componentId, String componentScheme) {
        super(componentId, componentScheme);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (remaining == null) {
            remaining = DEFAULT_CONNECTION_BEAN;
        }
        return super.createEndpoint(uri, remaining, parameters);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    protected Map<String, String> buildEndpointOptions(String remaining, Map<String, Object> options) throws Exception {
        System.out.println("io.syndesis.integration.component.proxy.ComponentProxyComponent buildEndpointOptions");
        if(this.getCamelContext().getRegistry().lookup(remaining) == null){
            MongoClient mongoClient = new MongoClient("localhost");
            this.getCamelContext().getRegistry(JndiRegistry.class).bind(remaining, mongoClient);
        }
        return super.buildEndpointOptions(remaining, options);
    }

}
