package io.syndesis.connector.mongo;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.impl.JndiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

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
            if (options.containsKey("user") && options.containsKey("password") && options.containsKey("host")) {
                try {
                    MongoConfiguration mongoConf = new MongoConfiguration();
                    consumeOption(camelContext, options, "host", String.class, mongoConf::setHost);
                    consumeOption(camelContext, options, "port", Integer.class, mongoConf::setPort);
                    consumeOption(camelContext, options, "user", String.class, mongoConf::setUser);
                    consumeOption(camelContext, options, "password", String.class, mongoConf::setPassword);
                    consumeOption(camelContext, options, "adminDB", String.class, mongoConf::setAdminDB);
                    LOGGER.debug("Creating and registering a client connection to " + mongoConf);
                    MongoClient mongoClient = new MongoClient(mongoConf.getMongoClientURI());
                    // TODO need a change in the upstream component
                    options.put("connectionBean", "connectionBeanRef");
                    camelContext.getRegistry(JndiRegistry.class).bind("connectionBeanRef", mongoClient);
                } catch (@SuppressWarnings("PMD.AvoidCatchingGenericException") Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } else {
                LOGGER.warn(
                        "Not enough information provided to set-up the MongoDB client. Required host, user and password.");
            }

        }
    }
}

class MongoConfiguration {
    String host = "localhost";
    String user;
    String password;
    int port = 27017;
    private String adminDB = "admin";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAdminDB() {
        return adminDB;
    }

    public void setAdminDB(String adminDB) {
        this.adminDB = adminDB;
    }

    public MongoClientURI getMongoClientURI() {
        return new MongoClientURI(String.format("mongodb://%s:%s@%s:%d/%s", this.user, this.password, this.host,
                this.port, this.adminDB));
    }

    @Override
    public String toString() {
        return "MongoConfiguration [host=" + host + ", user=" + user + ", password=***, port=" + port + ", adminDB="
                + adminDB + "]";
    }

}
