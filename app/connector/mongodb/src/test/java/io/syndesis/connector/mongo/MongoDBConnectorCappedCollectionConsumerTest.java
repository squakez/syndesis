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

import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.bson.Document;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.CreateCollectionOptions;

import io.syndesis.common.model.integration.Step;

@SuppressWarnings({ "PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert" })
public class MongoDBConnectorCappedCollectionConsumerTest extends MongoDBConnectorTestSupport {

    // **************************
    // Set up
    // **************************

    @Override
    public void doPreSetup() {
        // The feature only works with capped collections!
        CreateCollectionOptions opts = new CreateCollectionOptions().capped(true).sizeInBytes(1024 * 1024);
        database.createCollection("test", opts);
    }

    @Override
    protected List<Step> createSteps() {
        return fromMongoToMock("result", "io.syndesis.connector:connector-mongodb-consumer", DATABASE, COLLECTION,
                "_id");
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
            //try{
                String json = e.getMessage().getBody(String.class);
                System.out.println("RESULT: "+ json);
                /*JsonNode jsonNode = MAPPER.readTree(json);
                String _id = jsonNode.get("_id").asText();
                String key = jsonNode.get("someKey").asText();
                String value = jsonNode.get("someValue").asText();
                // We may test if the json is well formatted, etc...
                return _id != null && "someKey".equals(key) && "someValue".equals(value);   */
                return true;
            /*}catch(IOException ex) {
                return false;
            }*/
        });
        // Given
        Document doc = new Document();
        doc.append("someKey", "someValue");
        collection.insertOne(doc);
        // Then
        System.out.println("MOCK "+ mock);
        mock.assertIsSatisfied();        
    }
    
    /*@Test
    public void mongoTest2() throws Exception {
        // As we are tracking _id, any new insert should trigger the new document only
        mongoTest();
    }*/

}
