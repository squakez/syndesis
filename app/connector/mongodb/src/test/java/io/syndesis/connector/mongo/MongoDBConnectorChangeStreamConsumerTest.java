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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.CreateCollectionOptions;
import io.syndesis.common.model.integration.Step;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class MongoDBConnectorChangeStreamConsumerTest extends MongoDBConnectorTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBConnectorChangeStreamConsumerTest.class);
    private static int ID = 1;

    // **************************
    // Set up
    // **************************

    // JUnit will execute this method after the @BeforeClass of the superclass
    @BeforeClass
    public static void doCollectionSetup() {
        database.createCollection("test");
        LOG.debug("Created collection named test");
    }

    @Override
    protected List<Step> createSteps() {
        return fromMongoConsumerChangeStreamToMock("result", "io.syndesis.connector:connector-mongodb-changestream-consumer", DATABASE, COLLECTION,
            "{ $match : { test : \"junit\" } }");
    }

    // **************************
    // Tests
    // **************************

    @Test
    public void mongoTest() throws Exception {
        // When
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        mock.expectedMessagesMatches((Exchange e) -> {
                Document doc = e.getMessage().getBody(Document.class);
                return "junit".equals(doc.get("test"));
        });
        // Given
        Document doc = new Document();
        doc.append("id", ID++);
        doc.append("someKey", "someValue");
        doc.append("test", "junit");
        collection.insertOne(doc);
        // Then
        mock.assertIsSatisfied();
    }

    @Test
    public void repeatMongoTest() throws Exception {
        // As we are filtering, any new insert should trigger the new document only
        mongoTest();
    }

}
