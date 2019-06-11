package io.syndesis.connector.mongo;

import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyFactory;

public class MongoConnectorFactory implements ComponentProxyFactory{

    @Override
    public ComponentProxyComponent newInstance(String componentId, String componentScheme) {
        return new MongoConnector(componentId,componentScheme);
    }
 
}